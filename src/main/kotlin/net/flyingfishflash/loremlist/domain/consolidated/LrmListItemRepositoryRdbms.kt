package net.flyingfishflash.loremlist.domain.consolidated

import kotlinx.datetime.Clock.System.now
import net.flyingfishflash.loremlist.domain.LrmListItemTable
import net.flyingfishflash.loremlist.domain.LrmListTable
import net.flyingfishflash.loremlist.domain.LrmListsItemsTable
import net.flyingfishflash.loremlist.domain.lrmitem.LrmItem
import net.flyingfishflash.loremlist.domain.lrmitem.data.LrmItemRequest
import net.flyingfishflash.loremlist.domain.lrmlist.data.LrmListSuccinct
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.count
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class LrmListItemRepositoryRdbms : LrmListItemRepository {
  private val repositoryTable = LrmListItemTable

  override fun countByOwner(owner: String): Long {
    val idCount = repositoryTable.id.count()
    val count = repositoryTable.select(idCount).where { repositoryTable.createdBy eq owner }.first()[idCount]
    return count
  }

  override fun delete(): Int = repositoryTable.deleteAll()

  override fun deleteById(ids: Set<UUID>): Int = repositoryTable.deleteWhere { repositoryTable.id inList ids }

  override fun deleteByOwnerAndId(id: UUID, owner: String): Int = repositoryTable.deleteWhere {
    repositoryTable.id eq id and (repositoryTable.createdBy eq owner)
  }

  override fun findByOwner(owner: String): List<LrmItem> = repositoryTable
    .selectAll()
    .where { repositoryTable.createdBy eq owner }
    .map { it.toLrmItem() }
    .toList()

  override fun findByOwnerIncludeLists(owner: String): List<LrmItem> {
    val resultRows = (repositoryTable leftJoin LrmListsItemsTable leftJoin LrmListTable)
      .selectAll()
      .where { repositoryTable.createdBy eq owner }
      .toList()

    val listsByItems = resultRows
      .filter {
        @Suppress("SENSELESS_COMPARISON")
        it[LrmListTable.id] != null
      }
      .groupBy(
        keySelector = { it[repositoryTable.id].value },
        valueTransform = {
          LrmListSuccinct(
            id = it[LrmListTable.id].value,
            name = it[LrmListTable.name],
          )
        },
      )

    val itemsAndLists = resultRows.map {
      it.toLrmItem()
    }.distinct().map {
      it.copy(lists = listsByItems[it.id]?.sortedBy { succinctList -> succinctList.name }?.toSet() ?: setOf())
    }

    return itemsAndLists
  }

  override fun findByOwnerAndIdOrNull(id: UUID, owner: String): LrmItem? = repositoryTable.selectAll()
    .where { repositoryTable.id eq id }
    .andWhere { repositoryTable.createdBy eq owner }
    .map { it.toLrmItem() }
    .firstOrNull()

  override fun findByOwnerAndIdOrNullIncludeLists(id: UUID, owner: String): LrmItem? {
    val resultRows = (repositoryTable leftJoin LrmListsItemsTable leftJoin LrmListTable)
      .selectAll()
      .where { repositoryTable.id eq id }
      .andWhere { repositoryTable.createdBy eq owner }
      .toList()

    if (resultRows.isEmpty()) return null

    val listsByItems = resultRows
      .filter {
        @Suppress("SENSELESS_COMPARISON")
        it[LrmListTable.id] != null
      }.map {
        LrmListSuccinct(
          id = it[LrmListTable.id].value,
          name = it[LrmListTable.name],
        )
      }

    val itemAndLists = resultRows.first().toLrmItem().copy(lists = listsByItems.sortedBy { succinctList -> succinctList.name }.toSet())

    return itemAndLists
  }

  override fun findByOwnerAndHavingNoListAssociations(owner: String): List<LrmItem> {
    val result = (repositoryTable leftJoin LrmListsItemsTable)
      .selectAll()
      .where { repositoryTable.createdBy eq owner }
      .andWhere { LrmListsItemsTable.item.isNull() }
      .map { it.toLrmItem() }
    return result
  }

  override fun findIdsByOwnerAndIds(itemIdCollection: List<UUID>, owner: String): List<UUID> {
    val resultIdCollection = repositoryTable.select(repositoryTable.id)
      .where { repositoryTable.id inList (itemIdCollection) }
      .andWhere { repositoryTable.createdBy eq owner }
      .map { row -> row[repositoryTable.id].value }.toList()
    return resultIdCollection
  }

  override fun notFoundByOwnerAndId(itemIdCollection: List<UUID>, owner: String): Set<UUID> {
    val resultIdCollection = findIdsByOwnerAndIds(itemIdCollection = itemIdCollection, owner = owner)
    val notFoundItemUuidCollection = (itemIdCollection subtract resultIdCollection.toSet())
    return notFoundItemUuidCollection
  }

  override fun insert(lrmItemRequest: LrmItemRequest, owner: String): UUID {
    val now = now()
    val id =
      repositoryTable
        .insertAndGetId {
          it[name] = lrmItemRequest.name
          it[description] = lrmItemRequest.description
          it[quantity] = lrmItemRequest.quantity
          it[created] = now
          it[createdBy] = owner
          it[updated] = now
          it[updatedBy] = owner
        }

    return id.value
  }

  override fun update(lrmItem: LrmItem): Int {
    val updatedCount =
      repositoryTable.update({ repositoryTable.id eq lrmItem.id }) {
        it[repositoryTable.name] = lrmItem.name
        it[repositoryTable.description] = lrmItem.description
        it[repositoryTable.quantity] = lrmItem.quantity
        it[repositoryTable.updated] = now()
      }

    return updatedCount
  }

  fun ResultRow.toLrmItem(): LrmItem {
    return LrmItem(
      id = this[repositoryTable.id].value,
      name = this[repositoryTable.name],
      description = this[repositoryTable.description],
      quantity = this[repositoryTable.quantity],
      created = this[repositoryTable.created],
      createdBy = this[repositoryTable.createdBy],
      updated = this[repositoryTable.updated],
      updatedBy = this[repositoryTable.updatedBy],
    )
  }
}
