package net.flyingfishflash.loremlist.persistence

import kotlinx.datetime.Clock.System.now
import net.flyingfishflash.loremlist.domain.lrmitem.LrmItem
import net.flyingfishflash.loremlist.domain.lrmitem.LrmItemRepository
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
import org.jetbrains.exposed.sql.orWhere
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.statements.UpdateStatement
import org.jetbrains.exposed.sql.update
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class LrmItemRepositoryRdbms : LrmItemRepository {
  private val repositoryTable = LrmItemTable

  override fun countByOwner(owner: String): Long {
    val idCount = repositoryTable.id.count()
    return repositoryTable.select(idCount).where { repositoryTable.owner eq owner }.first()[idCount]
  }

  override fun delete(): Int = repositoryTable.deleteAll()

  override fun deleteById(ids: Set<UUID>): Int = repositoryTable.deleteWhere { repositoryTable.id inList ids }

  override fun deleteByOwnerAndId(id: UUID, owner: String): Int = repositoryTable.deleteWhere {
    repositoryTable.id eq id and (repositoryTable.owner eq owner)
  }

  override fun findByOwner(owner: String): List<LrmItem> {
    return mapQueryResultToLrmItems(
      (repositoryTable leftJoin LrmListItemsTable leftJoin LrmListTable)
        .selectAll()
        .byOwner(owner),
    )
  }

  override fun findByOwnerAndIdOrNull(id: UUID, owner: String): LrmItem? {
    val lrmItems = mapQueryResultToLrmItems(
      (repositoryTable leftJoin LrmListItemsTable leftJoin LrmListTable)
        .selectAll()
        .byOwnerAndId(owner, id),
    ).distinct()
    check(lrmItems.size <= 1) { "This query should return no more than one record." }
    return lrmItems.firstOrNull()
  }

  override fun findByOwnerAndHavingNoListAssociations(owner: String): List<LrmItem> {
    val result = (repositoryTable leftJoin LrmListItemsTable)
      .selectAll()
      .where { repositoryTable.owner eq owner }
      .andWhere { LrmListItemsTable.item.isNull() }
      .map { it.toLrmItem() }
    return result
  }

  override fun findByOwnerAndHavingNoListAssociations(owner: String, listId: UUID): List<LrmItem> {
    val result = (repositoryTable leftJoin LrmListItemsTable)
      .selectAll()
      .where { repositoryTable.owner eq owner }
      .andWhere { LrmListItemsTable.list neq listId }.orWhere { LrmListItemsTable.list.isNull() }
      .map { it.toLrmItem() }
    return result
  }

  override fun findIdsByOwnerAndIds(itemIdCollection: List<UUID>, owner: String): List<UUID> {
    return repositoryTable.select(repositoryTable.id)
      .where { repositoryTable.id inList itemIdCollection }
      .andWhere { repositoryTable.owner eq owner }
      .map { it[repositoryTable.id] }
      .toList()
  }

  override fun notFoundByOwnerAndId(itemIdCollection: List<UUID>, owner: String): Set<UUID> {
    val resultIdCollection = findIdsByOwnerAndIds(itemIdCollection = itemIdCollection, owner = owner)
    return (itemIdCollection subtract resultIdCollection.toSet())
  }

  override fun insert(lrmItem: LrmItem): UUID {
    repositoryTable.insert {
      it[id] = lrmItem.id
      it[name] = lrmItem.name
      it[description] = lrmItem.description
//      it[quantity] = lrmItem.quantity
      it[owner] = lrmItem.owner
      it[created] = lrmItem.created
      it[creator] = lrmItem.creator
      it[updated] = lrmItem.updated
      it[updater] = lrmItem.updater
    }
    return lrmItem.id
  }

  override fun update(lrmItem: LrmItem): Int = updateItem(lrmItem) {
    it[repositoryTable.name] = lrmItem.name
    it[repositoryTable.description] = lrmItem.description
//    it[repositoryTable.quantity] = lrmItem.quantity
  }

  override fun updateName(lrmItem: LrmItem): Int = updateItem(lrmItem) { it[repositoryTable.name] = lrmItem.name }

  override fun updateDescription(lrmItem: LrmItem): Int = updateItem(lrmItem) { it[repositoryTable.description] = lrmItem.description }

  private fun updateItem(lrmItem: LrmItem, updateAction: (UpdateStatement) -> Unit): Int {
    return repositoryTable.update({ repositoryTable.id eq lrmItem.id }) {
      updateAction(it)
      it[repositoryTable.updated] = now()
    }
  }

  private fun Query.byOwner(owner: String): Query {
    return this.where { repositoryTable.owner eq owner }
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
      owner = this[repositoryTable.owner],
      created = this[repositoryTable.created],
      creator = this[repositoryTable.creator],
      updated = this[repositoryTable.updated],
      updater = this[repositoryTable.updater],
    )
  }
}
