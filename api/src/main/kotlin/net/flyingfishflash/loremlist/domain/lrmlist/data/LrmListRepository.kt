package net.flyingfishflash.loremlist.domain.lrmlist.data

import net.flyingfishflash.loremlist.domain.LrmListItemTable
import net.flyingfishflash.loremlist.domain.LrmListTable
import net.flyingfishflash.loremlist.domain.LrmListTable.created
import net.flyingfishflash.loremlist.domain.LrmListTable.description
import net.flyingfishflash.loremlist.domain.LrmListTable.name
import net.flyingfishflash.loremlist.domain.LrmListsItemsTable
import net.flyingfishflash.loremlist.domain.lrmitem.data.LrmItem
import net.flyingfishflash.loremlist.domain.lrmitem.data.LrmItemConverter
import net.flyingfishflash.loremlist.domain.lrmlist.ListInsertException
import net.flyingfishflash.loremlist.domain.lrmlist.ListNotFoundException
import net.flyingfishflash.loremlist.domain.lrmlist.ListUpdateException
import net.flyingfishflash.loremlist.domain.lrmlist.data.dto.LrmListRequest
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Sequence
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.nextLongVal
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime
import java.time.ZoneOffset

@Repository
class LrmListRepository {
  private val repositoryTable = LrmListTable
  private val listSequence = Sequence("list_sequence")

  fun deleteById(id: Long): Int = repositoryTable.deleteWhere { repositoryTable.id eq id }

  fun findAll(): List<LrmList> =
    repositoryTable.select(repositoryTable.id, repositoryTable.name, repositoryTable.description, repositoryTable.created)
      .map { it.toLrmList() }
      .toList()

  fun findAllListsAndItems(): List<LrmList> =
    (repositoryTable leftJoin LrmListsItemsTable leftJoin LrmListItemTable)
      .selectAll()
      .toLrmListsWithItems()

  fun findByIdOrNull(id: Long): LrmList? =
    repositoryTable.select(repositoryTable.id, repositoryTable.name, repositoryTable.description, repositoryTable.created)
      .where { LrmListTable.id eq id }
      .firstOrNull()?.let {
        LrmList(
          id = it[LrmListTable.id].value,
          created = it[created],
          name = it[name],
          description = it[description],
        )
      }

  fun findByIdOrNullListAndItems(id: Long): LrmList? =
    (repositoryTable leftJoin LrmListsItemsTable leftJoin LrmListItemTable)
      .selectAll()
      .where { LrmListTable.id eq id }
      .map { it.toLrmList() }
      .firstOrNull()

  fun insert(lrmListRequest: LrmListRequest): LrmList {
    val id =
      repositoryTable
        .insertAndGetId {
          it[id] = listSequence.nextLongVal()
          it[created] = OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC)
          it[name] = lrmListRequest.name
          it[description] = lrmListRequest.description
        }

    val lrmList =
      repositoryTable.selectAll()
        .where { repositoryTable.id eq id }
        .map { it.toLrmList() }
        .singleOrNull() ?: throw ListInsertException(cause = ListNotFoundException())
    return lrmList
  }

  fun update(lrmList: LrmList): LrmList {
    val updatedCount =
      repositoryTable.update({ repositoryTable.id eq lrmList.id }) {
        it[name] = lrmList.name
        it[description] = lrmList.description
      }

    if (updatedCount == 1) {
      return findByIdOrNull(lrmList.id) ?: throw ListUpdateException(cause = ListNotFoundException())
    } else {
      throw ListUpdateException()
    }
  }

  private fun ResultRow.toLrmList(): LrmList = LrmListConverter.toLrmList(this)

  private fun ResultRow.toLrmListItem(): LrmItem = LrmItemConverter.toLrmListItem(this)

  private fun Iterable<ResultRow>.toLrmListsWithItems(): List<LrmList> =
    fold(initial = mutableMapOf<Long, LrmList>(), operation = { map, resultRow ->
      val list = resultRow.toLrmList()
      val itemId = resultRow.getOrNull(LrmListItemTable.id)
      val item = itemId?.let { resultRow.toLrmListItem() }
      val current = map.getOrDefault(list.id, list)
      map[list.id] = current.copy(items = current.items + listOfNotNull(item))
      map
    }).values.toList()
}
