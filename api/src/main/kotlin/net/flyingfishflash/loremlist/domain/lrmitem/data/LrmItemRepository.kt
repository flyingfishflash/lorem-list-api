package net.flyingfishflash.loremlist.domain.lrmitem.data

import net.flyingfishflash.loremlist.domain.LrmListItemTable
import net.flyingfishflash.loremlist.domain.LrmListsItemsTable
import net.flyingfishflash.loremlist.domain.lrmitem.ItemNotFoundException
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.nextLongVal
import org.jetbrains.exposed.sql.selectAll
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime
import java.time.ZoneOffset

@Repository
class LrmItemRepository {
  private val repositoryTable = LrmListItemTable
  private val listSequence = org.jetbrains.exposed.sql.Sequence("item_sequence")

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
          it[created] = OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC)
          it[name] = lrmItemRequest.name
          it[description] = lrmItemRequest.description
          it[quantity] = lrmItemRequest.quantity
        }

    val lrmListItem =
      repositoryTable.selectAll()
        .where { repositoryTable.id eq id }
        .map { it.toLrmItem() }
        // TODO: custom exception
        .singleOrNull() ?: throw ItemNotFoundException()
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
}
