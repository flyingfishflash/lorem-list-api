package net.flyingfishflash.loremlist.domain.lrmlist

import kotlinx.datetime.Clock.System.now
import net.flyingfishflash.loremlist.domain.LrmListItemTable
import net.flyingfishflash.loremlist.domain.LrmListTable
import net.flyingfishflash.loremlist.domain.LrmListTable.created
import net.flyingfishflash.loremlist.domain.LrmListTable.description
import net.flyingfishflash.loremlist.domain.LrmListTable.name
import net.flyingfishflash.loremlist.domain.LrmListTable.public
import net.flyingfishflash.loremlist.domain.LrmListTable.updated
import net.flyingfishflash.loremlist.domain.LrmListsItemsTable
import net.flyingfishflash.loremlist.domain.lrmitem.LrmItem
import net.flyingfishflash.loremlist.domain.lrmlist.data.LrmListRequest
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
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

  fun count(): Long {
    val idCount = repositoryTable.id.count()
    val count = repositoryTable.select(idCount).first()[idCount]
    return count
  }

  fun deleteAll(): Int = repositoryTable.deleteAll()

  fun deleteById(id: UUID): Int = repositoryTable.deleteWhere { repositoryTable.id eq id }

  fun findAll(): List<LrmList> = repositoryTable.selectAll().map { it.toLrmList() }.toList()

  fun findAllIncludeItems(): List<LrmList> {
    val resultRows = (repositoryTable leftJoin LrmListsItemsTable leftJoin LrmListItemTable).selectAll().toList()

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

  fun findAllPublic(): List<LrmList> = repositoryTable.selectAll().where { repositoryTable.public.eq(true) }.map { it.toLrmList() }.toList()

  fun findAllPublicIncludeItems(): List<LrmList> {
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

  fun findByIdOrNull(id: UUID): LrmList? = repositoryTable.selectAll().where { repositoryTable.id eq id }
    .firstOrNull()?.let {
      LrmList(
        id = it[repositoryTable.id].value,
        name = it[name],
        description = it[description],
        public = it[public],
        created = it[created],
        updated = it[updated],
      )
    }

  fun findByIdOrNullIncludeItems(id: UUID): LrmList? {
    val resultRows = (repositoryTable leftJoin LrmListsItemsTable leftJoin LrmListItemTable)
      .selectAll()
      .where { repositoryTable.id eq id }
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

  fun notFoundByIdCollection(listIdCollection: List<UUID>): Set<UUID> {
    val resultIdCollection = findByIdCollection(listIdCollection)
    val notFoundListIdCollection = (listIdCollection subtract resultIdCollection.toSet())
    return notFoundListIdCollection
  }

  fun findByIdCollection(listIdCollection: List<UUID>): List<UUID> {
    val resultIdCollection = repositoryTable.select(repositoryTable.id).where {
      repositoryTable.id inList (listIdCollection)
    }.map { row -> row[repositoryTable.id].value }.toList()
    return resultIdCollection
  }

  fun findWithNoItemAssociations(): List<LrmList> {
    val result = (repositoryTable leftJoin LrmListsItemsTable)
      .select(
        repositoryTable.id,
        repositoryTable.name,
        repositoryTable.description,
        repositoryTable.public,
        repositoryTable.created,
        repositoryTable.updated,
        LrmListsItemsTable.list,
      )
      .where { LrmListsItemsTable.list.isNull() }
      .map { it.toLrmList() }
    return result
  }

  fun insert(lrmListRequest: LrmListRequest): UUID {
    val now = now()
    val id =
      repositoryTable
        .insertAndGetId {
          it[id] = UUID.randomUUID()
          it[name] = lrmListRequest.name
          it[description] = lrmListRequest.description
          it[public] = lrmListRequest.public
          it[created] = now
          it[updated] = now
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
      updated = this[repositoryTable.updated],
    )
  }
}
