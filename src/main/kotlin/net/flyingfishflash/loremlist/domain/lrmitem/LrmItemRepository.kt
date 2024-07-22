package net.flyingfishflash.loremlist.domain.lrmitem

import kotlinx.datetime.Clock.System.now
import net.flyingfishflash.loremlist.domain.LrmListItemTable
import net.flyingfishflash.loremlist.domain.LrmListTable
import net.flyingfishflash.loremlist.domain.LrmListsItemsTable
import net.flyingfishflash.loremlist.domain.lrmitem.data.LrmItemRequest
import net.flyingfishflash.loremlist.domain.lrmlist.data.LrmListSuccinct
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
class LrmItemRepository {
  private val repositoryTable = LrmListItemTable

  fun count(): Long {
    val idCount = repositoryTable.id.count()
    val count = repositoryTable.select(idCount).first()[idCount]
    return count
  }

  fun deleteAll(): Int = repositoryTable.deleteAll()

  fun deleteById(id: UUID): Int = repositoryTable.deleteWhere { repositoryTable.id eq id }

  fun findAll(): List<LrmItem> = repositoryTable.select(
    repositoryTable.id,
    repositoryTable.name,
    repositoryTable.description,
    repositoryTable.quantity,
    repositoryTable.created,
    repositoryTable.updated,
  )
    .map { it.toLrmItem() }
    .toList()

  fun findAllIncludeLists(): List<LrmItem> {
    val resultRows = (repositoryTable leftJoin LrmListsItemsTable leftJoin LrmListTable)
      .select(
        repositoryTable.id,
        repositoryTable.name,
        repositoryTable.description,
        repositoryTable.quantity,
        repositoryTable.created,
        repositoryTable.updated,
        LrmListTable.id,
        LrmListTable.name,
      ).toList()

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

  fun notFoundByIdCollection(itemIdCollection: List<UUID>): Set<UUID> {
    val resultIdCollection = findByIdCollection(itemIdCollection)
    val notFoundItemUuidCollection = (itemIdCollection subtract resultIdCollection.toSet())
    return notFoundItemUuidCollection
  }

  fun findByIdCollection(itemIdCollection: List<UUID>): List<UUID> {
    val resultIdCollection = repositoryTable.select(repositoryTable.id).where {
      repositoryTable.id inList (itemIdCollection)
    }.map { row -> row[repositoryTable.id].value }.toList()
    return resultIdCollection
  }

  fun findByIdOrNull(id: UUID): LrmItem? = repositoryTable.selectAll()
    .where { repositoryTable.id eq id }
    .map { it.toLrmItem() }
    .firstOrNull()

  fun findByIdOrNullIncludeLists(id: UUID): LrmItem? {
    val resultRows = (repositoryTable leftJoin LrmListsItemsTable leftJoin LrmListTable)
      .select(
        repositoryTable.id,
        repositoryTable.name,
        repositoryTable.description,
        repositoryTable.quantity,
        repositoryTable.created,
        repositoryTable.updated,
        LrmListTable.id,
        LrmListTable.name,
      ).where { repositoryTable.id eq id }
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

  fun insert(lrmItemRequest: LrmItemRequest): UUID {
    val now = now()
    val id =
      repositoryTable
        .insertAndGetId {
          it[name] = lrmItemRequest.name
          it[description] = lrmItemRequest.description
          it[quantity] = lrmItemRequest.quantity
          it[created] = now
          it[updated] = now
        }

    return id.value
  }

  fun update(lrmItem: LrmItem): Int {
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
      updated = this[repositoryTable.updated],
    )
  }
}
