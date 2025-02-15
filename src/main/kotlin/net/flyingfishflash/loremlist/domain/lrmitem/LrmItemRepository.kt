package net.flyingfishflash.loremlist.domain.lrmitem

import kotlinx.datetime.Clock.System.now
import net.flyingfishflash.loremlist.domain.LrmListItemTable
import net.flyingfishflash.loremlist.domain.LrmListTable
import net.flyingfishflash.loremlist.domain.LrmListsItemsTable
import net.flyingfishflash.loremlist.domain.lrmlist.LrmListSuccinct
import org.jetbrains.exposed.sql.Query
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.count
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.statements.UpdateStatement
import org.jetbrains.exposed.sql.update
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class LrmItemRepository {
  private val repositoryTable = LrmListItemTable

  fun countByOwner(owner: String): Long {
    val idCount = repositoryTable.id.count()
    return repositoryTable.select(idCount).where { repositoryTable.createdBy eq owner }.first()[idCount]
  }

  fun delete(): Int = repositoryTable.deleteAll()

  fun deleteById(ids: Set<UUID>): Int = repositoryTable.deleteWhere { repositoryTable.id inList ids }

  fun deleteByOwnerAndId(id: UUID, owner: String): Int = repositoryTable.deleteWhere {
    repositoryTable.id eq id and (repositoryTable.createdBy eq owner)
  }

  fun findByOwner(owner: String): List<LrmItem> {
    return mapQueryResultToLrmItems(
      (repositoryTable leftJoin LrmListsItemsTable leftJoin LrmListTable)
        .selectAll()
        .byOwner(owner),
    )
  }

  fun findByOwnerAndIdOrNull(id: UUID, owner: String): LrmItem? {
    val lrmItems = mapQueryResultToLrmItems(
      (repositoryTable leftJoin LrmListsItemsTable leftJoin LrmListTable)
        .selectAll()
        .byOwnerAndId(owner, id),
    ).distinct()
    check(lrmItems.size <= 1) { "This query should return no more than one record." }
    return lrmItems.firstOrNull()
  }

  fun findByOwnerAndHavingNoListAssociations(owner: String): List<LrmItem> {
    val result = (repositoryTable leftJoin LrmListsItemsTable)
      .selectAll()
      .where { repositoryTable.createdBy eq owner }
      .andWhere { LrmListsItemsTable.item.isNull() }
      .map { it.toLrmItem() }
    return result
  }

  fun findIdsByOwnerAndIds(itemIdCollection: List<UUID>, owner: String): List<UUID> {
    return repositoryTable.select(repositoryTable.id)
      .where { repositoryTable.id inList itemIdCollection }
      .andWhere { repositoryTable.createdBy eq owner }
      .map { it[repositoryTable.id] }
      .toList()
  }

  fun notFoundByOwnerAndId(itemIdCollection: List<UUID>, owner: String): Set<UUID> {
    val resultIdCollection = findIdsByOwnerAndIds(itemIdCollection = itemIdCollection, owner = owner)
    return (itemIdCollection subtract resultIdCollection.toSet())
  }

  fun insert(lrmItem: LrmItem): UUID {
    repositoryTable.insert {
      it[id] = lrmItem.id
      it[name] = lrmItem.name
      it[description] = lrmItem.description
      it[quantity] = lrmItem.quantity
      it[created] = lrmItem.created
      it[createdBy] = lrmItem.createdBy
      it[updated] = lrmItem.updated
      it[updatedBy] = lrmItem.updatedBy
    }
    return lrmItem.id
  }

  fun update(lrmItem: LrmItem): Int = updateItem(lrmItem) {
    it[repositoryTable.name] = lrmItem.name
    it[repositoryTable.description] = lrmItem.description
    it[repositoryTable.quantity] = lrmItem.quantity
  }

  fun updateName(lrmItem: LrmItem): Int = updateItem(lrmItem) { it[repositoryTable.name] = lrmItem.name }

  fun updateDescription(lrmItem: LrmItem): Int = updateItem(lrmItem) { it[repositoryTable.description] = lrmItem.description }

  fun updateQuantity(lrmItem: LrmItem): Int = updateItem(lrmItem) { it[repositoryTable.quantity] = lrmItem.quantity }

  private fun updateItem(lrmItem: LrmItem, updateAction: (UpdateStatement) -> Unit): Int {
    return repositoryTable.update({ repositoryTable.id eq lrmItem.id }) {
      updateAction(it)
      it[repositoryTable.updated] = now()
    }
  }

  private fun Query.byOwner(owner: String): Query {
    return this.where { repositoryTable.createdBy eq owner }
  }

  private fun Query.byOwnerAndId(owner: String, id: UUID): Query {
    return this.byOwner(owner).andWhere { repositoryTable.id eq id }
  }

  private fun mapQueryResultToLrmItems(query: Query): List<LrmItem> {
    val resultRows = query.toList()

    @Suppress("SENSELESS_COMPARISON")
    val listsByItems = resultRows
      .filter { it[LrmListTable.id] != null }
      .groupBy(
        keySelector = { it[repositoryTable.id] },
        valueTransform = {
          LrmListSuccinct(
            id = it[LrmListTable.id],
            name = it[LrmListTable.name],
          )
        },
      )

    return resultRows
      .distinctBy { it[repositoryTable.id] }
      .map {
        it.toLrmItem().copy(
          lists = listsByItems[it[repositoryTable.id]]?.sortedBy { list -> list.name }?.toSet() ?: emptySet(),
        )
      }
  }

  fun ResultRow.toLrmItem(): LrmItem {
    return LrmItem(
      id = this[repositoryTable.id],
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
