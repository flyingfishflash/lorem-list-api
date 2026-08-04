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
import net.flyingfishflash.loremlist.domain.lrmlist.LrmList;
import net.flyingfishflash.loremlist.domain.lrmlist.LrmListRepository;
import net.flyingfishflash.loremlist.domain.lrmlistitem.LrmListItem;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class LrmListRepositoryRdbms implements LrmListRepository {

  private static final String LIST_COLUMNS =
      "l.id AS list_id, l.name AS list_name, l.description AS list_description, "
          + "l.public AS list_public, l.owner AS list_owner, l.created AS list_created, "
          + "l.creator AS list_creator, l.updated AS list_updated, l.updater AS list_updater";

  private static final String FIND_LISTS_SQL =
      "SELECT "
          + LIST_COLUMNS
          + ", li.list_id AS li_list_id, li.item_quantity AS li_item_quantity, "
          + "li.item_is_suppressed AS li_item_is_suppressed, "
          + "i.id AS item_id, i.name AS item_name, i.description AS item_description, i.owner AS item_owner, "
          + "i.created AS item_created, i.creator AS item_creator, i.updated AS item_updated, i.updater AS item_updater "
          + "FROM list l LEFT JOIN list_item li ON li.list_id = l.id LEFT JOIN item i ON i.id = li.item_id";

  private static final RowMapper<ListRow> LIST_ROW_MAPPER =
      (rs, rowNum) ->
          new ListRow(
              (UUID) rs.getObject("list_id"),
              rs.getString("list_name"),
              rs.getString("list_description"),
              rs.getBoolean("list_public"),
              rs.getString("list_owner"),
              rs.getObject("list_created", Instant.class),
              rs.getString("list_creator"),
              rs.getObject("list_updated", Instant.class),
              rs.getString("list_updater"),
              (UUID) rs.getObject("li_list_id"),
              rs.getInt("li_item_quantity"),
              (Boolean) rs.getObject("li_item_is_suppressed"),
              (UUID) rs.getObject("item_id"),
              rs.getString("item_name"),
              rs.getString("item_description"),
              rs.getString("item_owner"),
              rs.getObject("item_created", Instant.class),
              rs.getString("item_creator"),
              rs.getObject("item_updated", Instant.class),
              rs.getString("item_updater"));

  private final JdbcClient jdbcClient;

  LrmListRepositoryRdbms(JdbcClient jdbcClient) {
    this.jdbcClient = jdbcClient;
  }

  @Override
  public long countByOwner(String owner) {
    return jdbcClient
        .sql("SELECT COUNT(*) FROM list WHERE owner = :owner")
        .param("owner", owner)
        .query(Long.class)
        .single();
  }

  @Override
  public int delete() {
    return jdbcClient.sql("DELETE FROM list").update();
  }

  @Override
  public int deleteById(Set<UUID> ids) {
    return jdbcClient.sql("DELETE FROM list WHERE id IN (:ids)").param("ids", ids).update();
  }

  @Override
  public int deleteByOwnerAndId(UUID id, String owner) {
    return jdbcClient
        .sql("DELETE FROM list WHERE id = :id AND owner = :owner")
        .param("id", id)
        .param("owner", owner)
        .update();
  }

  @Override
  public List<LrmList> findByOwner(String owner) {
    List<ListRow> rows =
        jdbcClient
            .sql(FIND_LISTS_SQL + " WHERE l.owner = :owner")
            .param("owner", owner)
            .query(LIST_ROW_MAPPER)
            .list();
    return mapRowsToLrmLists(rows);
  }

  @Override
  public LrmList findByOwnerAndIdOrNull(UUID id, String owner) {
    List<ListRow> rows =
        jdbcClient
            .sql(FIND_LISTS_SQL + " WHERE l.owner = :owner AND l.id = :id")
            .param("owner", owner)
            .param("id", id)
            .query(LIST_ROW_MAPPER)
            .list();
    List<LrmList> lrmLists = mapRowsToLrmLists(rows);
    if (lrmLists.size() > 1) {
      throw new IllegalStateException("This query should return only one distinct list.");
    }
    return lrmLists.isEmpty() ? null : lrmLists.get(0);
  }

  @Override
  public List<LrmList> findByOwnerAndHavingNoItemAssociations(String owner) {
    return jdbcClient
        .sql(
            "SELECT "
                + LIST_COLUMNS
                + " FROM list l LEFT JOIN list_item li ON li.list_id = l.id "
                + "WHERE l.owner = :owner AND li.list_id IS NULL")
        .param("owner", owner)
        .query(
            (rs, rowNum) ->
                new LrmList(
                    (UUID) rs.getObject("list_id"),
                    rs.getString("list_name"),
                    rs.getString("list_description"),
                    rs.getBoolean("list_public"),
                    rs.getString("list_owner"),
                    new kotlinx.datetime.Instant(rs.getObject("list_created", Instant.class)),
                    rs.getString("list_creator"),
                    new kotlinx.datetime.Instant(rs.getObject("list_updated", Instant.class)),
                    rs.getString("list_updater"),
                    Set.of()))
        .list();
  }

  @Override
  public List<LrmList> findByPublic() {
    List<ListRow> rows =
        jdbcClient.sql(FIND_LISTS_SQL + " WHERE l.public = true").query(LIST_ROW_MAPPER).list();
    return mapRowsToLrmLists(rows);
  }

  @Override
  public List<UUID> findIdsByOwnerAndIds(List<UUID> listIdCollection, String owner) {
    return jdbcClient
        .sql("SELECT id FROM list WHERE owner = :owner AND id IN (:ids)")
        .param("owner", owner)
        .param("ids", listIdCollection)
        .query((rs, rowNum) -> (UUID) rs.getObject("id"))
        .list();
  }

  @Override
  public Set<UUID> notFoundByOwnerAndId(List<UUID> listIdCollection, String owner) {
    Set<UUID> foundIds = new LinkedHashSet<>(findIdsByOwnerAndIds(listIdCollection, owner));
    Set<UUID> notFound = new LinkedHashSet<>(listIdCollection);
    notFound.removeAll(foundIds);
    return notFound;
  }

  @Override
  public UUID insert(LrmList lrmList) {
    jdbcClient
        .sql(
            "INSERT INTO list (id, name, description, public, owner, created, creator, updated, updater) "
                + "VALUES (:id, :name, :description, :isPublic, :owner, :created, :creator, :updated, :updater)")
        .param("id", lrmList.id())
        .param("name", lrmList.name())
        .param("description", lrmList.description())
        .param("isPublic", lrmList.isPublic())
        .param("owner", lrmList.owner())
        .param("created", toJavaInstant(lrmList.created()))
        .param("creator", lrmList.creator())
        .param("updated", toJavaInstant(lrmList.updated()))
        .param("updater", lrmList.updater())
        .update();
    return lrmList.id();
  }

  @Override
  public int update(LrmList lrmList) {
    return jdbcClient
        .sql(
            "UPDATE list SET name = :name, description = :description, public = :isPublic, "
                + "updated = :updated WHERE id = :id")
        .param("name", lrmList.name())
        .param("description", lrmList.description())
        .param("isPublic", lrmList.isPublic())
        .param("updated", toJavaInstant(Clock.System.INSTANCE.now()))
        .param("id", lrmList.id())
        .update();
  }

  @Override
  public int updateName(LrmList lrmList) {
    return jdbcClient
        .sql("UPDATE list SET name = :name, updated = :updated WHERE id = :id")
        .param("name", lrmList.name())
        .param("updated", toJavaInstant(Clock.System.INSTANCE.now()))
        .param("id", lrmList.id())
        .update();
  }

  @Override
  public int updateDescription(LrmList lrmList) {
    return jdbcClient
        .sql("UPDATE list SET description = :description, updated = :updated WHERE id = :id")
        .param("description", lrmList.description())
        .param("updated", toJavaInstant(Clock.System.INSTANCE.now()))
        .param("id", lrmList.id())
        .update();
  }

  @Override
  public int updateIsPublic(LrmList lrmList) {
    return jdbcClient
        .sql("UPDATE list SET public = :isPublic, updated = :updated WHERE id = :id")
        .param("isPublic", lrmList.isPublic())
        .param("updated", toJavaInstant(Clock.System.INSTANCE.now()))
        .param("id", lrmList.id())
        .update();
  }

  private static List<LrmList> mapRowsToLrmLists(List<ListRow> rows) {
    Map<UUID, List<LrmListItem>> itemsByList =
        rows.stream()
            .filter(row -> row.itemId() != null)
            .collect(
                Collectors.groupingBy(
                    ListRow::listId,
                    LinkedHashMap::new,
                    Collectors.mapping(
                        LrmListRepositoryRdbms::toLrmListItem, Collectors.toList())));

    Map<UUID, LrmList> distinctByListId = new LinkedHashMap<>();
    for (ListRow row : rows) {
      distinctByListId.putIfAbsent(row.listId(), toLrmList(row, Set.of()));
    }

    List<LrmList> result = new ArrayList<>();
    for (LrmList list : distinctByListId.values()) {
      List<LrmListItem> items = itemsByList.get(list.id());
      Set<LrmListItem> sortedItems =
          items == null
              ? Set.of()
              : items.stream()
                  .sorted(Comparator.comparing(LrmListItem::name))
                  .collect(Collectors.toCollection(LinkedHashSet::new));
      result.add(list.withItems(sortedItems));
    }
    return result;
  }

  private static LrmList toLrmList(ListRow row, Set<LrmListItem> items) {
    return new LrmList(
        row.listId(),
        row.listName(),
        row.listDescription(),
        row.listPublic(),
        row.listOwner(),
        new kotlinx.datetime.Instant(row.listCreated()),
        row.listCreator(),
        new kotlinx.datetime.Instant(row.listUpdated()),
        row.listUpdater(),
        items);
  }

  private static LrmListItem toLrmListItem(ListRow row) {
    return new LrmListItem(
        row.itemId(),
        row.liListId(),
        row.itemName(),
        row.itemDescription(),
        row.liItemQuantity(),
        row.liItemIsSuppressed(),
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

  private record ListRow(
      UUID listId,
      String listName,
      String listDescription,
      boolean listPublic,
      String listOwner,
      Instant listCreated,
      String listCreator,
      Instant listUpdated,
      String listUpdater,
      UUID liListId,
      int liItemQuantity,
      Boolean liItemIsSuppressed,
      UUID itemId,
      String itemName,
      String itemDescription,
      String itemOwner,
      Instant itemCreated,
      String itemCreator,
      Instant itemUpdated,
      String itemUpdater) {}
}
