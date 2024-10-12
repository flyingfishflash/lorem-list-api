package net.flyingfishflash.loremlist.domain.lrmlist

import kotlinx.datetime.Clock.System.now
import net.flyingfishflash.loremlist.domain.LrmListItemTable
import net.flyingfishflash.loremlist.domain.LrmListTable
import net.flyingfishflash.loremlist.domain.LrmListTable.created
import net.flyingfishflash.loremlist.domain.LrmListTable.createdBy
import net.flyingfishflash.loremlist.domain.LrmListTable.description
import net.flyingfishflash.loremlist.domain.LrmListTable.name
import net.flyingfishflash.loremlist.domain.LrmListTable.public
import net.flyingfishflash.loremlist.domain.LrmListTable.updated
import net.flyingfishflash.loremlist.domain.LrmListTable.updatedBy
import net.flyingfishflash.loremlist.domain.LrmListsItemsTable
import net.flyingfishflash.loremlist.domain.lrmitem.LrmItem
import net.flyingfishflash.loremlist.domain.lrmlist.data.LrmListCreateRequest
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
class LrmListRepository {
  private val repositoryTable = LrmListTable

  fun countByOwner(owner: String): Long {
    val idCount = repositoryTable.id.count()
    val count = repositoryTable.select(idCount).where { repositoryTable.createdBy eq owner }.first()[idCount]
    return count
  }

  fun delete(): Int = repositoryTable.deleteAll()

  fun deleteById(ids: Set<UUID>): Int = repositoryTable.deleteWhere { repositoryTable.id inList ids }

  fun deleteByOwnerAndId(id: UUID, owner: String): Int = repositoryTable.deleteWhere {
    repositoryTable.id eq id and (repositoryTable.createdBy eq owner)
  }

  fun findByOwner(owner: String): List<LrmList> = repositoryTable.selectAll()
    .where { repositoryTable.createdBy eq owner }
    .map { it.toLrmList() }.toList()

  fun findByOwnerIncludeItems(owner: String): List<LrmList> {
    val resultRows = (repositoryTable leftJoin LrmListsItemsTable leftJoin LrmListItemTable).selectAll()
      .where { repositoryTable.createdBy eq owner }
      .toList()

    val listItemsByList = resultRows
      .filter {
        @Suppress("SENSELESS_COMPARISON")
        it[LrmListItemTable.id] != null
      }
      .groupBy(
        keySelector = { it[repositoryTable.id].value },
        valueTransform = {
          LrmItem(
            id = it[LrmListItemTable.id].value,
            name = it[LrmListItemTable.name],
            description = it[LrmListItemTable.description],
            quantity = it[LrmListItemTable.quantity],
            created = it[LrmListItemTable.created],
            updated = it[LrmListItemTable.updated],
          )
        },
      )

    val listsAndItems = resultRows
      .map { it.toLrmList() }
      .distinct()
      .map { it.copy(items = listItemsByList[it.id]?.sortedBy { item -> item.name }?.toSet() ?: setOf()) }

    return listsAndItems
  }

  fun findByOwnerAndIdOrNull(id: UUID, owner: String): LrmList? = repositoryTable
    .selectAll()
    .where { repositoryTable.id eq id }
    .andWhere { repositoryTable.createdBy eq owner }
    .firstOrNull()?.let {
      LrmList(
        id = it[repositoryTable.id].value,
        name = it[name],
        description = it[description],
        public = it[public],
        created = it[created],
        createdBy = it[createdBy],
        updated = it[updated],
        updatedBy = it[updatedBy],
      )
    }

  fun findByOwnerAndIdOrNullIncludeItems(id: UUID, owner: String): LrmList? {
    val resultRows = (repositoryTable leftJoin LrmListsItemsTable leftJoin LrmListItemTable)
      .selectAll()
      .where { repositoryTable.id eq id }
      .andWhere { repositoryTable.createdBy eq owner }
      .toList()

    val listItems = resultRows
      .asSequence()
      .filter {
        @Suppress("SENSELESS_COMPARISON")
        it[LrmListItemTable.id] != null
      }.map {
        LrmItem(
          id = it[LrmListItemTable.id].value,
          name = it[LrmListItemTable.name],
          description = it[LrmListItemTable.description],
          quantity = it[LrmListItemTable.quantity],
          created = it[LrmListItemTable.created],
          updated = it[LrmListItemTable.updated],
        )
      }.sortedBy { item -> item.name }.toSet()

    val listWithItems = resultRows
      .map { it.toLrmList() }
      .distinct()
      .map { it.copy(items = listItems) }
      .firstOrNull()
    return listWithItems
  }

  fun findByOwnerAndHavingNoItemAssociations(owner: String): List<LrmList> {
    val result = (repositoryTable leftJoin LrmListsItemsTable)
      .selectAll()
      .where { repositoryTable.createdBy eq owner }
      .andWhere { LrmListsItemsTable.list.isNull() }
      .map { it.toLrmList() }
    return result
  }

  fun findByPublic(): List<LrmList> = repositoryTable.selectAll().where { repositoryTable.public.eq(true) }.map { it.toLrmList() }.toList()

  fun findByPublicIncludeItems(): List<LrmList> {
    val resultRows = (repositoryTable leftJoin LrmListsItemsTable leftJoin LrmListItemTable)
      .selectAll()
      .where { repositoryTable.public.eq(true) }
      .toList()

    val listItemsByList = resultRows
      .filter {
        @Suppress("SENSELESS_COMPARISON")
        it[LrmListItemTable.id] != null
      }
      .groupBy(
        keySelector = { it[repositoryTable.id].value },
        valueTransform = {
          LrmItem(
            id = it[LrmListItemTable.id].value,
            name = it[LrmListItemTable.name],
            description = it[LrmListItemTable.description],
            quantity = it[LrmListItemTable.quantity],
            created = it[LrmListItemTable.created],
            updated = it[LrmListItemTable.updated],
          )
        },
      )

    val listsAndItems = resultRows
      .map { it.toLrmList() }
      .distinct()
      .map { it.copy(items = listItemsByList[it.id]?.sortedBy { item -> item.name }?.toSet() ?: setOf()) }

    return listsAndItems
  }

  fun findIdsByOwnerAndIds(listIdCollection: List<UUID>, owner: String): List<UUID> {
    val resultIdCollection = repositoryTable
      .select(repositoryTable.id)
      .where { repositoryTable.id inList (listIdCollection) }
      .andWhere { repositoryTable.createdBy eq owner }
      .map { row -> row[repositoryTable.id].value }.toList()
    return resultIdCollection
  }

  fun notFoundByOwnerAndId(listIdCollection: List<UUID>, owner: String): Set<UUID> {
    val resultIdCollection = findIdsByOwnerAndIds(listIdCollection = listIdCollection, owner = owner)
    val notFoundListIdCollection = (listIdCollection subtract resultIdCollection.toSet())
    return notFoundListIdCollection
  }

  fun insert(lrmListCreateRequest: LrmListCreateRequest, subject: String): UUID {
    val now = now()
    val id =
      repositoryTable
        .insertAndGetId {
          it[id] = UUID.randomUUID()
          it[name] = lrmListCreateRequest.name
          it[description] = lrmListCreateRequest.description
          it[public] = lrmListCreateRequest.public
          it[created] = now
          it[createdBy] = subject
          it[updated] = now
          it[updatedBy] = subject
        }

    return id.value
  }

  fun update(lrmList: LrmList): Int {
    val updatedCount =
      repositoryTable.update({ repositoryTable.id eq lrmList.id }) {
        it[name] = lrmList.name
        it[description] = lrmList.description
        it[public] = lrmList.public
        it[updated] = now()
      }

    return updatedCount
  }

  private fun ResultRow.toLrmList(): LrmList {
    return LrmList(
      id = this[repositoryTable.id].value,
      name = this[repositoryTable.name],
      description = this[repositoryTable.description],
      public = this[repositoryTable.public],
      created = this[repositoryTable.created],
      createdBy = this[repositoryTable.createdBy],
      updated = this[repositoryTable.updated],
      updatedBy = this[repositoryTable.updatedBy],
    )
  }
}
