package net.flyingfishflash.loremlist.domain.lrmitem.data

import kotlinx.datetime.Clock.System.now
import net.flyingfishflash.loremlist.domain.LrmListItemTable
import net.flyingfishflash.loremlist.domain.LrmListTable
import net.flyingfishflash.loremlist.domain.LrmListsItemsTable
import net.flyingfishflash.loremlist.domain.lrmitem.ItemInsertException
import net.flyingfishflash.loremlist.domain.lrmitem.ItemNotFoundException
import net.flyingfishflash.loremlist.domain.lrmlist.data.LrmListSuccinct
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Sequence
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.nextLongVal
import org.jetbrains.exposed.sql.selectAll
import org.springframework.stereotype.Repository

@Repository
class LrmItemRepository {
  private val repositoryTable = LrmListItemTable
  private val listSequence = Sequence("item_sequence")

  fun deleteById(id: Long): Int = repositoryTable.deleteWhere { repositoryTable.id eq LrmListItemTable.id }

  fun findAll(): List<LrmItem> =
    repositoryTable.select(
      repositoryTable.id,
      repositoryTable.name,
      repositoryTable.description,
      repositoryTable.created,
      repositoryTable.quantity,
    )
      .map { it.toLrmItem() }
      .toList()

  fun findAllItemsAndLists(): List<LrmItem> =
    (repositoryTable leftJoin LrmListsItemsTable leftJoin LrmListTable)
      .select(
        repositoryTable.id,
        repositoryTable.name,
        repositoryTable.created,
        repositoryTable.description,
        repositoryTable.quantity,
        LrmListTable.id,
        LrmListTable.name,
      )
      .toLrmItemsWithLists()

  fun findByIdOrNull(id: Long): LrmItem? =
    repositoryTable.selectAll()
      .where { repositoryTable.id eq id }
      .map { it.toLrmItem() }
      .firstOrNull()

  fun insert(lrmItemRequest: LrmItemRequest): LrmItem {
    val id =
      repositoryTable
        .insertAndGetId {
          it[id] = listSequence.nextLongVal()
          it[created] = now()
          it[name] = lrmItemRequest.name
          it[description] = lrmItemRequest.description
          it[quantity] = lrmItemRequest.quantity
        }

    val lrmListItem =
      repositoryTable.selectAll()
        .where { repositoryTable.id eq id }
        .map { it.toLrmItem() }
        .singleOrNull() ?: throw ItemInsertException(cause = ItemNotFoundException(id.value))
    return lrmListItem
  }

  fun addItemToList(
    listId: Long,
    itemId: Long,
  ) {
    LrmListsItemsTable.insert {
      it[list] = listId
      it[item] = itemId
    }
  }

  private fun ResultRow.toLrmItem(): LrmItem = LrmItemConverter.toLrmItem(this)

  private fun Iterable<ResultRow>.toLrmItemsWithLists(): List<LrmItem> =
    fold(initial = mutableMapOf<Long, LrmItem>(), operation = { map, resultRow ->
      val item = resultRow.toLrmItem()
      val listId = resultRow.getOrNull(LrmListTable.id)
      val listSuccinct = listId?.let { LrmListSuccinct(id = it.value, name = resultRow[LrmListTable.name]) }
      val current = map.getOrDefault(item.id, item)
      // if lists is null return an empty set otherwise add create a list of non-null items
      // so a serialized list explicitly returns [] when a list has no items otherwise the items key will not be rendered
      map[item.id] = current.copyWith(
        lists =
        if (current.lists != null) {
          current.lists.plus(listOfNotNull(listSuccinct))
        } else {
          setOf()
        },
      )
      map
    }).values.toList()
}
