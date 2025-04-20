package net.flyingfishflash.loremlist.persistence

import kotlinx.datetime.Clock.System.now
import net.flyingfishflash.loremlist.domain.lrmitem.LrmItemSuccinct
import net.flyingfishflash.loremlist.domain.lrmlist.LrmListSuccinct
import net.flyingfishflash.loremlist.domain.lrmlistitem.LrmListItem
import net.flyingfishflash.loremlist.domain.lrmlistitem.LrmListItemRepository
import net.flyingfishflash.loremlist.persistence.LrmListItemsTable.item
import net.flyingfishflash.loremlist.persistence.LrmListItemsTable.itemIsSuppressed
import net.flyingfishflash.loremlist.persistence.LrmListItemsTable.itemQuantity
import net.flyingfishflash.loremlist.persistence.LrmListItemsTable.list
import org.jetbrains.exposed.sql.Query
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.count
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.statements.UpdateStatement
import org.jetbrains.exposed.sql.update
import org.springframework.stereotype.Repository
import java.util.*

@Repository
class LrmListItemRepositoryRdbms : LrmListItemRepository {

  override fun countByOwnerAndListId(listId: UUID, listOwner: String): Long {
    val listCount = list.count()
    return (LrmListItemsTable leftJoin LrmListTable).select(listCount)
      .where { list eq listId }
      .andWhere { LrmListTable.owner eq listOwner }.map { it[listCount] }.first()
  }

  override fun countByOwnerAndItemId(itemId: UUID, itemOwner: String): Long {
    val itemCount = item.count()
    return (LrmListItemsTable leftJoin LrmItemTable).select(itemCount)
      .where { item eq itemId }
      .andWhere { LrmItemTable.owner eq itemOwner }.map { it[itemCount] }.first()
  }

  override fun create(listId: UUID, itemId: UUID) {
    LrmListItemsTable.insert {
      it[list] = listId
      it[item] = itemId
      it[itemQuantity] = 0
      it[itemIsSuppressed] = false
    }
  }

  // TODO: create custom type for associationCollection
  override fun create(associationCollection: Set<Pair<UUID, UUID>>): List<SuccinctLrmComponentPair> {
    val associationIdSet = LrmListItemsTable.batchInsert(associationCollection) { (listId, itemId) ->
      this[list] = listId
      this[item] = itemId
      this[itemQuantity] = 0
      this[itemIsSuppressed] = false
    }.map { Pair(it[list], it[item]) }.toSet()

    val succinctLrmComponentPairs = (LrmListItemsTable innerJoin LrmListTable innerJoin LrmItemTable)
      .select(LrmListTable.id, LrmListTable.name, LrmItemTable.id, LrmItemTable.name)
      .where { Pair(list, item) inList (associationIdSet) }
      .map {
        SuccinctLrmComponentPair(
          list = LrmListSuccinct(it[LrmListTable.id], it[LrmListTable.name]),
          item = LrmItemSuccinct(it[LrmItemTable.id], it[LrmItemTable.name]),
        )
      }

    return succinctLrmComponentPairs
  }

  override fun findByOwnerAndItemIdAndListIdOrNull(itemId: UUID, listId: UUID, owner: String): LrmListItem? {
    val lrmListItems = mapQueryResultToLrmItems(
      (LrmItemTable leftJoin LrmListItemsTable leftJoin LrmListTable)
        .selectAll()
        .where { item eq itemId }
        .andWhere { list eq listId }
        .andWhere { LrmItemTable.owner eq owner }
        .andWhere { LrmListTable.owner eq owner },
    ).distinct()
    check(lrmListItems.size <= 1) { "This query should return no more than one record." }
    return lrmListItems.firstOrNull()
  }

  // TODO: add owner restriction
  override fun removeByOwnerAndItemId(itemId: UUID, owner: String): Int {
    return LrmListItemsTable.deleteWhere { item eq itemId }
  }

  // TODO: add owner restriction
  override fun removeByOwnerAndListId(listId: UUID, owner: String): Int {
    return LrmListItemsTable.deleteWhere { list eq listId }
  }

  // TODO: add owner restriction
  override fun removeByOwnerAndListIdAndItemId(listId: UUID, itemId: UUID, owner: String): Int {
    val deletedCount = LrmListItemsTable.deleteWhere { list eq listId and (item eq itemId) }
    check(deletedCount <= 1) { "This query should delete no more than one record." }
    return deletedCount
  }

  override fun updateName(lrmListItem: LrmListItem): Int = updateItem(lrmListItem) { it[LrmItemTable.name] = lrmListItem.name }

  override fun updateDescription(lrmListItem: LrmListItem): Int = updateItem(lrmListItem) {
    it[LrmItemTable.description] =
      lrmListItem.description
  }

  override fun updateQuantity(lrmListItem: LrmListItem): Int {
    val updatedCount = LrmListItemsTable.update({ byListIdAndItemId(lrmListItem) }) { it[itemQuantity] = lrmListItem.quantity }
    check(updatedCount <= 1) { "This query should update no more than one record." }
    return updatedCount
  }

  override fun updateIsItemSuppressed(lrmListItem: LrmListItem): Int {
    val updatedCount = LrmListItemsTable.update({ byListIdAndItemId(lrmListItem) }) { it[itemIsSuppressed] = lrmListItem.isSuppressed }
    check(updatedCount <= 1) { "This query should update no more than one record." }
    return updatedCount
  }

  override fun updateListId(lrmListItem: LrmListItem, destinationListId: UUID): Int {
    val updatedCount = LrmListItemsTable.update({ byListIdAndItemId(lrmListItem) }) { it[list] = destinationListId }
    check(updatedCount == 1) { "This query should update exactly one record." }
    return updatedCount
  }

  private fun updateItem(lrmItem: LrmListItem, updateAction: (UpdateStatement) -> Unit): Int {
    return LrmItemTable.update({ LrmItemTable.id eq lrmItem.id }) {
      updateAction(it)
      it[updated] = now()
    }
  }

  private fun mapQueryResultToLrmItems(query: Query): List<LrmListItem> {
    val resultRows = query.toList()

    @Suppress("SENSELESS_COMPARISON")
    val listsByItems = resultRows
      .filter { it[LrmListTable.id] != null }
      .groupBy(
        keySelector = { it[LrmItemTable.id] },
        valueTransform = {
          LrmListSuccinct(
            id = it[LrmListTable.id],
            name = it[LrmListTable.name],
          )
        },
      )

    return resultRows
      .distinctBy { it[LrmItemTable.id] }
      .map {
        it.toLrmListItem().copy(
          lists = listsByItems[it[LrmItemTable.id]]?.sortedBy { list -> list.name }?.toSet() ?: emptySet(),
        )
      }
  }

  private fun byListIdAndItemId(lrmListItem: LrmListItem) = item eq lrmListItem.id and (list eq lrmListItem.listId)

  private fun ResultRow.toLrmListItem(): LrmListItem {
    return LrmListItem(
      id = this[item],
      listId = this[list],
      name = this[LrmItemTable.name],
      description = this[LrmItemTable.description],
      owner = this[LrmItemTable.owner],
      created = this[LrmItemTable.created],
      creator = this[LrmItemTable.creator],
      updated = this[LrmItemTable.updated],
      updater = this[LrmItemTable.updater],
      quantity = this[itemQuantity],
      isSuppressed = this[itemIsSuppressed],
    )
  }
}
