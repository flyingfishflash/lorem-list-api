package net.flyingfishflash.loremlist.domain.lrmlist

import kotlinx.datetime.Clock.System.now
import net.flyingfishflash.loremlist.domain.LrmListItemTable
import net.flyingfishflash.loremlist.domain.LrmListTable
import net.flyingfishflash.loremlist.domain.LrmListsItemsTable
import net.flyingfishflash.loremlist.domain.lrmitem.LrmItem
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
import org.jetbrains.exposed.sql.update
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class LrmListRepository {

  private val repositoryTable = LrmListTable

  fun countByOwner(owner: String): Long {
    return repositoryTable
      .select(repositoryTable.id.count())
      .byOwner(owner)
      .first()[repositoryTable.id.count()]
  }

  fun delete(): Int = repositoryTable.deleteAll()

  fun deleteById(ids: Set<UUID>): Int = repositoryTable.deleteWhere { repositoryTable.id inList ids }

  fun deleteByOwnerAndId(id: UUID, owner: String): Int = repositoryTable
    .deleteWhere {
      repositoryTable.id eq id and (repositoryTable.createdBy eq owner)
    }

  fun findByOwner(owner: String): List<LrmList> = mapQueryResultToLrmLists(
    (repositoryTable leftJoin LrmListsItemsTable leftJoin LrmListItemTable)
      .selectAll()
      .byOwner(owner),
  )

  fun findByOwnerAndIdOrNull(id: UUID, owner: String): LrmList? {
    val lrmLists = mapQueryResultToLrmLists(
      (repositoryTable leftJoin LrmListsItemsTable leftJoin LrmListItemTable)
        .selectAll()
        .byOwnerAndId(owner, id),
    ).distinct()
    check(lrmLists.size <= 1) { "This query should return only one distinct list." }
    return lrmLists.firstOrNull()
  }

  fun findByOwnerAndHavingNoItemAssociations(owner: String): List<LrmList> = (repositoryTable leftJoin LrmListsItemsTable)
    .selectAll()
    .byOwner(owner)
    .andWhere { LrmListsItemsTable.list.isNull() }
    .map { it.toLrmList() }

  fun findByPublic(): List<LrmList> = mapQueryResultToLrmLists(
    (repositoryTable leftJoin LrmListsItemsTable leftJoin LrmListItemTable)
      .selectAll()
      .where { repositoryTable.public eq true },
  )

  fun findIdsByOwnerAndIds(listIdCollection: List<UUID>, owner: String): List<UUID> = repositoryTable
    .select(repositoryTable.id)
    .byOwner(owner)
    .andWhere { repositoryTable.id inList listIdCollection }
    .map { it[repositoryTable.id] }

  fun notFoundByOwnerAndId(listIdCollection: List<UUID>, owner: String): Set<UUID> {
    val foundIds = findIdsByOwnerAndIds(listIdCollection, owner)
    return listIdCollection.toSet() - foundIds.toSet()
  }

  fun insert(lrmList: LrmList, subject: String): UUID {
    repositoryTable.insert {
      it[id] = lrmList.id
      it[name] = lrmList.name
      it[description] = lrmList.description
      it[public] = lrmList.public
      it[created] = lrmList.created
      it[createdBy] = lrmList.createdBy
      it[updated] = lrmList.updated
      it[updatedBy] = lrmList.updatedBy
    }
    return lrmList.id
  }

  fun update(lrmList: LrmList): Int {
    return repositoryTable.update({ repositoryTable.id eq lrmList.id }) {
      it[name] = lrmList.name
      it[description] = lrmList.description
      it[public] = lrmList.public
      it[updated] = now()
    }
  }

  private fun Query.byOwner(owner: String): Query {
    return this.where { repositoryTable.createdBy eq owner }
  }

  private fun Query.byOwnerAndId(owner: String, id: UUID): Query {
    return this.byOwner(owner).andWhere { repositoryTable.id eq id }
  }

  private fun mapQueryResultToLrmLists(query: Query): List<LrmList> {
    val resultRows = query.toList()

    @Suppress("SENSELESS_COMPARISON")
    val listItemsByList = resultRows
      .filter { it.fieldIndex.containsKey(LrmListItemTable.id) }
      .filterNot { it[LrmListItemTable.id] == null }
      .groupBy { it[repositoryTable.id] }
      .mapValues { it.value.map { row -> row.toLrmItem() }.sortedBy { item -> item.name }.toSet() }

    return resultRows
      .asSequence()
      .map { row ->
        row.toLrmList(listItemsByList[row[repositoryTable.id]] ?: emptySet())
      }
      .distinctBy { it.id } // distinct by the list id, eliminating duplicates
      .toList()
  }

  private fun ResultRow.toLrmList(lrmItems: Set<LrmItem> = emptySet()): LrmList {
    return LrmList(
      id = this[repositoryTable.id],
      name = this[repositoryTable.name],
      description = this[repositoryTable.description],
      public = this[repositoryTable.public],
      created = this[repositoryTable.created],
      createdBy = this[repositoryTable.createdBy],
      updated = this[repositoryTable.updated],
      updatedBy = this[repositoryTable.updatedBy],
      items = lrmItems,
    )
  }

  private fun ResultRow.toLrmItem(): LrmItem {
    return LrmItem(
      id = this[LrmListItemTable.id],
      name = this[LrmListItemTable.name],
      description = this[LrmListItemTable.description],
      quantity = this[LrmListItemTable.quantity],
      created = this[LrmListItemTable.created],
      updated = this[LrmListItemTable.updated],
      createdBy = this[LrmListItemTable.createdBy],
      updatedBy = this[LrmListItemTable.updatedBy],
      lists = null,
    )
  }
}
