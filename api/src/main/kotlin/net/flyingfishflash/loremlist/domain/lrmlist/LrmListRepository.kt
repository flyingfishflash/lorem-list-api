package net.flyingfishflash.loremlist.domain.lrmlist

import kotlinx.datetime.Clock.System.now
import net.flyingfishflash.loremlist.domain.LrmListItemTable
import net.flyingfishflash.loremlist.domain.LrmListTable
import net.flyingfishflash.loremlist.domain.LrmListTable.created
import net.flyingfishflash.loremlist.domain.LrmListTable.description
import net.flyingfishflash.loremlist.domain.LrmListTable.name
import net.flyingfishflash.loremlist.domain.LrmListsItemsTable
import net.flyingfishflash.loremlist.domain.lrmitem.LrmItem
import net.flyingfishflash.loremlist.domain.lrmlist.data.LrmListRequest
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Sequence
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.nextLongVal
import org.jetbrains.exposed.sql.update
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class LrmListRepository {
  private val repositoryTable = LrmListTable
  private val listSequence = Sequence("list_sequence")

  fun deleteById(id: Long): Int = repositoryTable.deleteWhere { repositoryTable.id eq id }

  fun findAll(): List<LrmList> = repositoryTable.select(
    repositoryTable.id,
    repositoryTable.uuid,
    repositoryTable.name,
    repositoryTable.description,
    repositoryTable.created,
  )
    .map { it.toLrmlist() }
    .toList()

  fun findAllIncludeItems(): List<LrmList> {
    val resultRows = (repositoryTable leftJoin LrmListsItemsTable leftJoin LrmListItemTable)
      .select(
        repositoryTable.id,
        repositoryTable.uuid,
        repositoryTable.created,
        repositoryTable.name,
        repositoryTable.description,
        LrmListItemTable.id,
        LrmListItemTable.uuid,
        LrmListItemTable.created,
        LrmListItemTable.name,
        LrmListItemTable.description,
        LrmListItemTable.quantity,
      ).toList()

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
            uuid = it[LrmListItemTable.uuid],
            created = it[LrmListItemTable.created],
            name = it[LrmListItemTable.name],
            description = it[LrmListItemTable.description],
            quantity = it[LrmListItemTable.quantity],
          )
        },
      )

    val listsAndItems = resultRows
      .map { it.toLrmlist() }
      .distinct()
      .map { it.copy(items = listItemsByList[it.id]?.toSet() ?: setOf()) }

    return listsAndItems
  }

  fun findByIdOrNull(id: Long): LrmList? = repositoryTable.select(
    repositoryTable.id,
    repositoryTable.uuid,
    repositoryTable.name,
    repositoryTable.description,
    repositoryTable.created,
  )
    .where { repositoryTable.id eq id }
    .firstOrNull()?.let {
      LrmList(
        id = it[repositoryTable.id].value,
        uuid = it[repositoryTable.uuid],
        created = it[created],
        name = it[name],
        description = it[description],
      )
    }

  fun findByIdOrNullIncludeItems(id: Long): LrmList? {
    val resultRows = (repositoryTable leftJoin LrmListsItemsTable leftJoin LrmListItemTable)
      .select(
        repositoryTable.id,
        repositoryTable.uuid,
        repositoryTable.created,
        repositoryTable.name,
        repositoryTable.description,
        LrmListItemTable.id,
        LrmListItemTable.uuid,
        LrmListItemTable.created,
        LrmListItemTable.name,
        LrmListItemTable.description,
        LrmListItemTable.quantity,
      ).where { repositoryTable.id eq id }.toList()

    val listItems = resultRows
      .asSequence()
      .filter {
        @Suppress("SENSELESS_COMPARISON")
        it[LrmListItemTable.id] != null
      }
      .map {
        LrmItem(
          id = it[LrmListItemTable.id].value,
          uuid = it[LrmListItemTable.uuid],
          created = it[LrmListItemTable.created],
          name = it[LrmListItemTable.name],
          description = it[LrmListItemTable.description],
          quantity = it[LrmListItemTable.quantity],
        )
      }.toSet()

    val listWithItems = resultRows
      .map { it.toLrmlist() }
      .distinct()
      .map { it.copy(items = listItems) }
      .firstOrNull()
    return listWithItems
  }

  fun insert(lrmListRequest: LrmListRequest): Long {
    val id =
      repositoryTable
        .insertAndGetId {
          it[id] = listSequence.nextLongVal()
          it[uuid] = UUID.randomUUID()
          it[created] = now()
          it[name] = lrmListRequest.name
          it[description] = lrmListRequest.description
        }

    return id.value
  }

  fun update(lrmList: LrmList): Int {
    val updatedCount =
      repositoryTable.update({ repositoryTable.id eq lrmList.id }) {
        it[name] = lrmList.name
        it[description] = lrmList.description
      }

    return updatedCount
  }

  private fun ResultRow.toLrmlist(): LrmList {
    return LrmList(
      id = this[repositoryTable.id].value,
      uuid = this[repositoryTable.uuid],
      created = this[repositoryTable.created],
      name = this[repositoryTable.name],
      description = this[repositoryTable.description],
    )
  }
}
