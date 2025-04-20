package net.flyingfishflash.loremlist.persistence

import kotlinx.datetime.Clock.System.now
import net.flyingfishflash.loremlist.domain.lrmlist.LrmList
import net.flyingfishflash.loremlist.domain.lrmlist.LrmListRepository
import net.flyingfishflash.loremlist.domain.lrmlistitem.LrmListItem
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
class LrmListRepositoryRdbms : LrmListRepository {

  private val repositoryTable = LrmListTable

  override fun countByOwner(owner: String): Long {
    return repositoryTable
      .select(repositoryTable.id.count())
      .byOwner(owner)
      .first()[repositoryTable.id.count()]
  }

  override fun delete(): Int = repositoryTable.deleteAll()

  override fun deleteById(ids: Set<UUID>): Int = repositoryTable.deleteWhere { repositoryTable.id inList ids }

  override fun deleteByOwnerAndId(id: UUID, owner: String): Int = repositoryTable
    .deleteWhere {
      repositoryTable.id eq id and (repositoryTable.owner eq owner)
    }

  override fun findByOwner(owner: String): List<LrmList> = mapQueryResultToLrmLists(
    (repositoryTable leftJoin LrmListItemsTable leftJoin LrmItemTable)
      .selectAll()
      .byOwner(owner),
  )

  override fun findByOwnerAndIdOrNull(id: UUID, owner: String): LrmList? {
    val lrmLists = mapQueryResultToLrmLists(
      (repositoryTable leftJoin LrmListItemsTable leftJoin LrmItemTable)
        .selectAll()
        .byOwnerAndId(owner, id),
    ).distinct()
    check(lrmLists.size <= 1) { "This query should return only one distinct list." }
    return lrmLists.firstOrNull()
  }

  override fun findByOwnerAndHavingNoItemAssociations(owner: String): List<LrmList> = (repositoryTable leftJoin LrmListItemsTable)
    .selectAll()
    .byOwner(owner)
    .andWhere { LrmListItemsTable.list.isNull() }
    .map { it.toLrmList() }

  override fun findByPublic(): List<LrmList> = mapQueryResultToLrmLists(
    (repositoryTable leftJoin LrmListItemsTable leftJoin LrmItemTable)
      .selectAll()
      .where { repositoryTable.public eq true },
  )

  override fun findIdsByOwnerAndIds(listIdCollection: List<UUID>, owner: String): List<UUID> = repositoryTable
    .select(repositoryTable.id)
    .byOwner(owner)
    .andWhere { repositoryTable.id inList listIdCollection }
    .map { it[repositoryTable.id] }

  override fun notFoundByOwnerAndId(listIdCollection: List<UUID>, owner: String): Set<UUID> {
    val foundIds = findIdsByOwnerAndIds(listIdCollection, owner)
    return listIdCollection.toSet() - foundIds.toSet()
  }

  override fun insert(lrmList: LrmList): UUID {
    repositoryTable.insert {
      it[id] = lrmList.id
      it[name] = lrmList.name
      it[description] = lrmList.description
      it[public] = lrmList.public
      it[owner] = lrmList.owner
      it[created] = lrmList.created
      it[creator] = lrmList.creator
      it[updated] = lrmList.updated
      it[updater] = lrmList.updater
    }
    return lrmList.id
  }

  override fun update(lrmList: LrmList): Int = updateList(lrmList) {
    it[repositoryTable.name] = lrmList.name
    it[repositoryTable.description] = lrmList.description
    it[repositoryTable.public] = lrmList.public
  }

  override fun updateName(lrmList: LrmList): Int = updateList(lrmList) { it[repositoryTable.name] = lrmList.name }

  override fun updateDescription(lrmList: LrmList): Int = updateList(lrmList) { it[repositoryTable.description] = lrmList.description }

  override fun updateIsPublic(lrmList: LrmList): Int = updateList(lrmList) { it[repositoryTable.public] = lrmList.public }

  private fun updateList(lrmList: LrmList, updateAction: (UpdateStatement) -> Unit): Int {
    return repositoryTable.update({ repositoryTable.id eq lrmList.id }) {
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

  private fun mapQueryResultToLrmLists(query: Query): List<LrmList> {
    val resultRows = query.toList()

    @Suppress("SENSELESS_COMPARISON")
    val listItemsByList = resultRows
      .filter { it.fieldIndex.containsKey(LrmItemTable.id) }
      .filterNot { it[LrmItemTable.id] == null }
      .groupBy { it[repositoryTable.id] }
      .mapValues { it.value.map { row -> row.toLrmListItem() }.sortedBy { item -> item.name }.toSet() }

    return resultRows
      .asSequence()
      .map { row ->
        row.toLrmList(listItemsByList[row[repositoryTable.id]] ?: emptySet())
      }
      .distinctBy { it.id } // distinct by the list id, eliminating duplicates
      .toList()
  }

  private fun ResultRow.toLrmList(lrmItems: Set<LrmListItem> = emptySet()): LrmList {
    return LrmList(
      id = this[repositoryTable.id],
      name = this[repositoryTable.name],
      description = this[repositoryTable.description],
      public = this[repositoryTable.public],
      owner = this[repositoryTable.owner],
      created = this[repositoryTable.created],
      creator = this[repositoryTable.creator],
      updated = this[repositoryTable.updated],
      updater = this[repositoryTable.updater],
      items = lrmItems,
    )
  }

  private fun ResultRow.toLrmListItem(): LrmListItem {
    return LrmListItem(
      id = this[LrmItemTable.id],
      listId = this[LrmListItemsTable.list],
      name = this[LrmItemTable.name],
      description = this[LrmItemTable.description],
      quantity = this[LrmListItemsTable.itemQuantity],
      isSuppressed = this[LrmListItemsTable.itemIsSuppressed],
      owner = this[LrmItemTable.owner],
      created = this[LrmItemTable.created],
      updated = this[LrmItemTable.updated],
      creator = this[LrmItemTable.creator],
      updater = this[LrmItemTable.updater],
    )
  }
}
