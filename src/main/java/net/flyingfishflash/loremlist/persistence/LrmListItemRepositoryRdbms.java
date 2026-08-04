package net.flyingfishflash.loremlist.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import kotlin.Pair;
import kotlinx.datetime.Clock;
import kotlinx.datetime.ConvertersKt;
import net.flyingfishflash.loremlist.domain.lrmitem.LrmItemSuccinct;
import net.flyingfishflash.loremlist.domain.lrmlist.LrmListSuccinct;
import net.flyingfishflash.loremlist.domain.lrmlistitem.LrmListItem;
import net.flyingfishflash.loremlist.domain.lrmlistitem.LrmListItemRepository;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class LrmListItemRepositoryRdbms implements LrmListItemRepository {

  private final JdbcClient jdbcClient;

  LrmListItemRepositoryRdbms(JdbcClient jdbcClient) {
    this.jdbcClient = jdbcClient;
  }

  @Override
  public long countByOwnerAndListId(UUID listId, String listOwner) {
    return jdbcClient
        .sql(
            "SELECT COUNT(*) FROM list_item li JOIN list l ON l.id = li.list_id "
                + "WHERE li.list_id = :listId AND l.owner = :listOwner")
        .param("listId", listId)
        .param("listOwner", listOwner)
        .query(Long.class)
        .single();
  }

  @Override
  public long countByOwnerAndItemId(UUID itemId, String itemOwner) {
    return jdbcClient
        .sql(
            "SELECT COUNT(*) FROM list_item li JOIN item i ON i.id = li.item_id "
                + "WHERE li.item_id = :itemId AND i.owner = :itemOwner")
        .param("itemId", itemId)
        .param("itemOwner", itemOwner)
        .query(Long.class)
        .single();
  }

  @Override
  public void create(UUID listId, UUID itemId) {
    jdbcClient
        .sql(
            "INSERT INTO list_item (list_id, item_id, item_quantity, item_is_suppressed) "
                + "VALUES (:listId, :itemId, 0, false)")
        .param("listId", listId)
        .param("itemId", itemId)
        .update();
  }

  @Override
  public List<SuccinctLrmComponentPair> create(Set<Pair<UUID, UUID>> associationCollection) {
    if (associationCollection.isEmpty()) {
      return List.of();
    }

    for (Pair<UUID, UUID> association : associationCollection) {
      create(association.getFirst(), association.getSecond());
    }

    Set<UUID> listIds =
        associationCollection.stream().map(Pair::getFirst).collect(Collectors.toSet());
    Set<UUID> itemIds =
        associationCollection.stream().map(Pair::getSecond).collect(Collectors.toSet());

    Map<UUID, String> listNamesById =
        jdbcClient
            .sql("SELECT id, name FROM list WHERE id IN (:ids)")
            .param("ids", listIds)
            .query((rs, rowNum) -> Map.entry((UUID) rs.getObject("id"), rs.getString("name")))
            .list()
            .stream()
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    Map<UUID, String> itemNamesById =
        jdbcClient
            .sql("SELECT id, name FROM item WHERE id IN (:ids)")
            .param("ids", itemIds)
            .query((rs, rowNum) -> Map.entry((UUID) rs.getObject("id"), rs.getString("name")))
            .list()
            .stream()
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    return associationCollection.stream()
        .map(
            association ->
                new SuccinctLrmComponentPair(
                    new LrmListSuccinct(
                        association.getFirst(), listNamesById.get(association.getFirst())),
                    new LrmItemSuccinct(
                        association.getSecond(), itemNamesById.get(association.getSecond()))))
        .toList();
  }

  @Override
  public LrmListItem findByOwnerAndItemIdAndListIdOrNull(UUID itemId, UUID listId, String owner) {
    return jdbcClient
        .sql(
            "SELECT i.id AS item_id, i.name AS item_name, i.description AS item_description, "
                + "i.owner AS item_owner, i.created AS item_created, i.creator AS item_creator, "
                + "i.updated AS item_updated, i.updater AS item_updater, "
                + "li.list_id AS li_list_id, li.item_quantity AS li_item_quantity, "
                + "li.item_is_suppressed AS li_item_is_suppressed, l.name AS list_name "
                + "FROM item i "
                + "JOIN list_item li ON li.item_id = i.id "
                + "JOIN list l ON l.id = li.list_id "
                + "WHERE i.id = :itemId AND li.list_id = :listId AND i.owner = :owner AND l.owner = :owner")
        .param("itemId", itemId)
        .param("listId", listId)
        .param("owner", owner)
        .query(
            (rs, rowNum) ->
                new LrmListItem(
                    (UUID) rs.getObject("item_id"),
                    (UUID) rs.getObject("li_list_id"),
                    rs.getString("item_name"),
                    rs.getString("item_description"),
                    rs.getInt("li_item_quantity"),
                    rs.getBoolean("li_item_is_suppressed"),
                    rs.getString("item_owner"),
                    new kotlinx.datetime.Instant(rs.getObject("item_created", Instant.class)),
                    rs.getString("item_creator"),
                    new kotlinx.datetime.Instant(rs.getObject("item_updated", Instant.class)),
                    rs.getString("item_updater"),
                    Set.of(
                        new LrmListSuccinct(
                            (UUID) rs.getObject("li_list_id"), rs.getString("list_name")))))
        .optional()
        .orElse(null);
  }

  // TODO: add owner restriction
  @Override
  public int removeByOwnerAndItemId(UUID itemId, String owner) {
    return jdbcClient
        .sql("DELETE FROM list_item WHERE item_id = :itemId")
        .param("itemId", itemId)
        .update();
  }

  // TODO: add owner restriction
  @Override
  public int removeByOwnerAndListId(UUID listId, String owner) {
    return jdbcClient
        .sql("DELETE FROM list_item WHERE list_id = :listId")
        .param("listId", listId)
        .update();
  }

  // TODO: add owner restriction
  @Override
  public int removeByOwnerAndListIdAndItemId(UUID listId, UUID itemId, String owner) {
    int deletedCount =
        jdbcClient
            .sql("DELETE FROM list_item WHERE list_id = :listId AND item_id = :itemId")
            .param("listId", listId)
            .param("itemId", itemId)
            .update();
    if (deletedCount > 1) {
      throw new IllegalStateException("This query should delete no more than one record.");
    }
    return deletedCount;
  }

  @Override
  public int updateName(LrmListItem lrmListItem) {
    return jdbcClient
        .sql("UPDATE item SET name = :name, updated = :updated WHERE id = :id")
        .param("name", lrmListItem.name())
        .param("updated", toJavaInstant(Clock.System.INSTANCE.now()))
        .param("id", lrmListItem.id())
        .update();
  }

  @Override
  public int updateDescription(LrmListItem lrmListItem) {
    return jdbcClient
        .sql("UPDATE item SET description = :description, updated = :updated WHERE id = :id")
        .param("description", lrmListItem.description())
        .param("updated", toJavaInstant(Clock.System.INSTANCE.now()))
        .param("id", lrmListItem.id())
        .update();
  }

  @Override
  public int updateQuantity(LrmListItem lrmListItem) {
    int updatedCount =
        jdbcClient
            .sql(
                "UPDATE list_item SET item_quantity = :quantity WHERE item_id = :itemId AND list_id = :listId")
            .param("quantity", lrmListItem.quantity())
            .param("itemId", lrmListItem.id())
            .param("listId", lrmListItem.listId())
            .update();
    if (updatedCount > 1) {
      throw new IllegalStateException("This query should update no more than one record.");
    }
    return updatedCount;
  }

  @Override
  public int updateIsItemSuppressed(LrmListItem lrmListItem) {
    int updatedCount =
        jdbcClient
            .sql(
                "UPDATE list_item SET item_is_suppressed = :isSuppressed "
                    + "WHERE item_id = :itemId AND list_id = :listId")
            .param("isSuppressed", lrmListItem.isSuppressed())
            .param("itemId", lrmListItem.id())
            .param("listId", lrmListItem.listId())
            .update();
    if (updatedCount > 1) {
      throw new IllegalStateException("This query should update no more than one record.");
    }
    return updatedCount;
  }

  @Override
  public int updateListId(LrmListItem lrmListItem, UUID destinationListId) {
    int updatedCount =
        jdbcClient
            .sql(
                "UPDATE list_item SET list_id = :destinationListId WHERE item_id = :itemId AND list_id = :listId")
            .param("destinationListId", destinationListId)
            .param("itemId", lrmListItem.id())
            .param("listId", lrmListItem.listId())
            .update();
    if (updatedCount != 1) {
      throw new IllegalStateException("This query should update exactly one record.");
    }
    return updatedCount;
  }

  private static Instant toJavaInstant(kotlinx.datetime.Instant instant) {
    return ConvertersKt.toJavaInstant(instant);
  }
}
