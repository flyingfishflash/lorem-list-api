package net.flyingfishflash.loremlist.persistence;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import kotlinx.datetime.Clock;
import kotlinx.datetime.ConvertersKt;
import net.flyingfishflash.loremlist.domain.lrmitem.LrmItem;
import net.flyingfishflash.loremlist.domain.lrmitem.LrmItemRepository;
import net.flyingfishflash.loremlist.domain.lrmlist.LrmListSuccinct;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class LrmItemRepositoryRdbms implements LrmItemRepository {

  private static final String ITEM_COLUMNS =
      "i.id AS item_id, i.name AS item_name, i.description AS item_description, "
          + "i.owner AS item_owner, i.created AS item_created, i.creator AS item_creator, "
          + "i.updated AS item_updated, i.updater AS item_updater";

  private static final String FIND_ITEMS_SQL =
      "SELECT "
          + ITEM_COLUMNS
          + ", l.id AS list_id, l.name AS list_name "
          + "FROM item i LEFT JOIN list_item li ON li.item_id = i.id LEFT JOIN list l ON l.id = li.list_id";

  private static final RowMapper<ItemRow> ITEM_ROW_MAPPER =
      (rs, rowNum) ->
          new ItemRow(
              (UUID) rs.getObject("item_id"),
              rs.getString("item_name"),
              rs.getString("item_description"),
              rs.getString("item_owner"),
              rs.getObject("item_created", Instant.class),
              rs.getString("item_creator"),
              rs.getObject("item_updated", Instant.class),
              rs.getString("item_updater"),
              (UUID) rs.getObject("list_id"),
              rs.getString("list_name"));

  private final JdbcClient jdbcClient;

  LrmItemRepositoryRdbms(JdbcClient jdbcClient) {
    this.jdbcClient = jdbcClient;
  }

  @Override
  public long countByOwner(String owner) {
    return jdbcClient
        .sql("SELECT COUNT(*) FROM item WHERE owner = :owner")
        .param("owner", owner)
        .query(Long.class)
        .single();
  }

  @Override
  public int delete() {
    return jdbcClient.sql("DELETE FROM item").update();
  }

  @Override
  public int deleteById(Set<UUID> ids) {
    return jdbcClient.sql("DELETE FROM item WHERE id IN (:ids)").param("ids", ids).update();
  }

  @Override
  public int deleteByOwnerAndId(UUID id, String owner) {
    return jdbcClient
        .sql("DELETE FROM item WHERE id = :id AND owner = :owner")
        .param("id", id)
        .param("owner", owner)
        .update();
  }

  @Override
  public List<LrmItem> findByOwner(String owner) {
    List<ItemRow> rows =
        jdbcClient
            .sql(FIND_ITEMS_SQL + " WHERE i.owner = :owner")
            .param("owner", owner)
            .query(ITEM_ROW_MAPPER)
            .list();
    return mapRowsToLrmItems(rows);
  }

  @Override
  public LrmItem findByOwnerAndIdOrNull(UUID id, String owner) {
    List<ItemRow> rows =
        jdbcClient
            .sql(FIND_ITEMS_SQL + " WHERE i.owner = :owner AND i.id = :id")
            .param("owner", owner)
            .param("id", id)
            .query(ITEM_ROW_MAPPER)
            .list();
    List<LrmItem> lrmItems = mapRowsToLrmItems(rows);
    if (lrmItems.size() > 1) {
      throw new IllegalStateException("This query should return no more than one record.");
    }
    return lrmItems.isEmpty() ? null : lrmItems.get(0);
  }

  @Override
  public List<LrmItem> findByOwnerAndHavingNoListAssociations(String owner) {
    return jdbcClient
        .sql(
            "SELECT "
                + ITEM_COLUMNS
                + ", CAST(NULL AS UUID) AS list_id, CAST(NULL AS VARCHAR) AS list_name "
                + "FROM item i LEFT JOIN list_item li ON li.item_id = i.id "
                + "WHERE i.owner = :owner AND li.item_id IS NULL")
        .param("owner", owner)
        .query(ITEM_ROW_MAPPER)
        .list()
        .stream()
        .map(LrmItemRepositoryRdbms::toLrmItem)
        .toList();
  }

  @Override
  public List<LrmItem> findByOwnerAndHavingNoListAssociations(String owner, UUID listId) {
    return jdbcClient
        .sql(
            "SELECT "
                + ITEM_COLUMNS
                + ", CAST(NULL AS UUID) AS list_id, CAST(NULL AS VARCHAR) AS list_name "
                + "FROM item i LEFT JOIN list_item li ON li.item_id = i.id "
                + "WHERE (i.owner = :owner AND li.list_id <> :listId) OR li.list_id IS NULL")
        .param("owner", owner)
        .param("listId", listId)
        .query(ITEM_ROW_MAPPER)
        .list()
        .stream()
        .map(LrmItemRepositoryRdbms::toLrmItem)
        .toList();
  }

  @Override
  public List<UUID> findIdsByOwnerAndIds(List<UUID> itemIdCollection, String owner) {
    return jdbcClient
        .sql("SELECT id FROM item WHERE id IN (:ids) AND owner = :owner")
        .param("ids", itemIdCollection)
        .param("owner", owner)
        .query((rs, rowNum) -> (UUID) rs.getObject("id"))
        .list();
  }

  @Override
  public Set<UUID> notFoundByOwnerAndId(List<UUID> itemIdCollection, String owner) {
    Set<UUID> foundIds = new LinkedHashSet<>(findIdsByOwnerAndIds(itemIdCollection, owner));
    Set<UUID> notFound = new LinkedHashSet<>(itemIdCollection);
    notFound.removeAll(foundIds);
    return notFound;
  }

  @Override
  public UUID insert(LrmItem lrmItem) {
    jdbcClient
        .sql(
            "INSERT INTO item (id, name, description, owner, created, creator, updated, updater) "
                + "VALUES (:id, :name, :description, :owner, :created, :creator, :updated, :updater)")
        .param("id", lrmItem.id())
        .param("name", lrmItem.name())
        .param("description", lrmItem.description())
        .param("owner", lrmItem.owner())
        .param("created", toJavaInstant(lrmItem.created()))
        .param("creator", lrmItem.creator())
        .param("updated", toJavaInstant(lrmItem.updated()))
        .param("updater", lrmItem.updater())
        .update();
    return lrmItem.id();
  }

  @Override
  public int update(LrmItem lrmItem) {
    return jdbcClient
        .sql(
            "UPDATE item SET name = :name, description = :description, updated = :updated WHERE id = :id")
        .param("name", lrmItem.name())
        .param("description", lrmItem.description())
        .param("updated", toJavaInstant(Clock.System.INSTANCE.now()))
        .param("id", lrmItem.id())
        .update();
  }

  @Override
  public int updateName(LrmItem lrmItem) {
    return jdbcClient
        .sql("UPDATE item SET name = :name, updated = :updated WHERE id = :id")
        .param("name", lrmItem.name())
        .param("updated", toJavaInstant(Clock.System.INSTANCE.now()))
        .param("id", lrmItem.id())
        .update();
  }

  @Override
  public int updateDescription(LrmItem lrmItem) {
    return jdbcClient
        .sql("UPDATE item SET description = :description, updated = :updated WHERE id = :id")
        .param("description", lrmItem.description())
        .param("updated", toJavaInstant(Clock.System.INSTANCE.now()))
        .param("id", lrmItem.id())
        .update();
  }

  private static List<LrmItem> mapRowsToLrmItems(List<ItemRow> rows) {
    Map<UUID, List<LrmListSuccinct>> listsByItem =
        rows.stream()
            .filter(row -> row.listId() != null)
            .collect(
                Collectors.groupingBy(
                    ItemRow::itemId,
                    LinkedHashMap::new,
                    Collectors.mapping(
                        row -> new LrmListSuccinct(row.listId(), row.listName()),
                        Collectors.toList())));

    Map<UUID, LrmItem> distinctByItemId = new LinkedHashMap<>();
    for (ItemRow row : rows) {
      distinctByItemId.putIfAbsent(row.itemId(), toLrmItem(row));
    }

    List<LrmItem> result = new ArrayList<>();
    for (LrmItem item : distinctByItemId.values()) {
      List<LrmListSuccinct> lists = listsByItem.get(item.id());
      Set<LrmListSuccinct> sortedLists =
          lists == null
              ? Set.of()
              : lists.stream()
                  .sorted(Comparator.comparing(LrmListSuccinct::name))
                  .collect(Collectors.toCollection(LinkedHashSet::new));
      result.add(item.withLists(sortedLists));
    }
    return result;
  }

  private static LrmItem toLrmItem(ItemRow row) {
    return new LrmItem(
        row.itemId(),
        row.itemName(),
        row.itemDescription(),
        row.itemOwner(),
        new kotlinx.datetime.Instant(row.itemCreated()),
        row.itemCreator(),
        new kotlinx.datetime.Instant(row.itemUpdated()),
        row.itemUpdater(),
        Set.of());
  }

  private static Instant toJavaInstant(kotlinx.datetime.Instant instant) {
    return ConvertersKt.toJavaInstant(instant);
  }

  private record ItemRow(
      UUID itemId,
      String itemName,
      String itemDescription,
      String itemOwner,
      Instant itemCreated,
      String itemCreator,
      Instant itemUpdated,
      String itemUpdater,
      UUID listId,
      String listName) {}
}
