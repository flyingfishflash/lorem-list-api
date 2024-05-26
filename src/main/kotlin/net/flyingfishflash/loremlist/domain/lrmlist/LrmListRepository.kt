package net.flyingfishflash.loremlist.domain.lrmlist

import kotlinx.datetime.Clock.System.now
import net.flyingfishflash.loremlist.domain.LrmListItemTable
import net.flyingfishflash.loremlist.domain.LrmListTable
import net.flyingfishflash.loremlist.domain.LrmListTable.created
import net.flyingfishflash.loremlist.domain.LrmListTable.description
import net.flyingfishflash.loremlist.domain.LrmListTable.name
import net.flyingfishflash.loremlist.domain.LrmListTable.updated
import net.flyingfishflash.loremlist.domain.LrmListTable.uuid
import net.flyingfishflash.loremlist.domain.LrmListsItemsTable
import net.flyingfishflash.loremlist.domain.lrmitem.LrmItem
import net.flyingfishflash.loremlist.domain.lrmlist.data.LrmListRequest
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Sequence
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.count
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

  fun count(): Long {
    val uuidCount = uuid.count()
    val count = repositoryTable.select(uuidCount).first()[uuidCount]
    return count
  }

  fun deleteById(id: Long): Int = repositoryTable.deleteWhere { repositoryTable.id eq id }

  fun findAll(): List<LrmList> = repositoryTable.select(
    repositoryTable.id,
    repositoryTable.uuid,
    repositoryTable.name,
    repositoryTable.description,
    repositoryTable.created,
    repositoryTable.updated,
  )
    .map { it.toLrmlist() }
    .toList()

  fun findAllIncludeItems(): List<LrmList> {
    val resultRows = (repositoryTable leftJoin LrmListsItemsTable leftJoin LrmListItemTable)
      .select(
        repositoryTable.id,
        repositoryTable.uuid,
        repositoryTable.name,
        repositoryTable.description,
        repositoryTable.created,
        repositoryTable.updated,
        LrmListItemTable.id,
        LrmListItemTable.uuid,
        LrmListItemTable.name,
        LrmListItemTable.description,
        LrmListItemTable.quantity,
        LrmListItemTable.created,
        LrmListItemTable.updated,
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
            name = it[LrmListItemTable.name],
            description = it[LrmListItemTable.description],
            quantity = it[LrmListItemTable.quantity],
            created = it[LrmListItemTable.created],
            updated = it[LrmListItemTable.updated],
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
    repositoryTable.updated,
  )
    .where { repositoryTable.id eq id }
    .firstOrNull()?.let {
      LrmList(
        id = it[repositoryTable.id].value,
        uuid = it[repositoryTable.uuid],
        name = it[name],
        description = it[description],
        created = it[created],
        updated = it[updated],
      )
    }

  fun findByIdOrNullIncludeItems(id: Long): LrmList? {
    val resultRows = (repositoryTable leftJoin LrmListsItemsTable leftJoin LrmListItemTable)
      .select(
        repositoryTable.id,
        repositoryTable.uuid,
        repositoryTable.name,
        repositoryTable.description,
        repositoryTable.created,
        repositoryTable.updated,
        LrmListItemTable.id,
        LrmListItemTable.uuid,
        LrmListItemTable.name,
        LrmListItemTable.description,
        LrmListItemTable.quantity,
        LrmListItemTable.created,
        LrmListItemTable.updated,
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
          name = it[LrmListItemTable.name],
          description = it[LrmListItemTable.description],
          quantity = it[LrmListItemTable.quantity],
          created = it[LrmListItemTable.created],
          updated = it[LrmListItemTable.updated],
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
    val now = now()
    val id =
      repositoryTable
        .insertAndGetId {
          it[id] = listSequence.nextLongVal()
          it[uuid] = UUID.randomUUID()
          it[name] = lrmListRequest.name
          it[description] = lrmListRequest.description
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
        it[updated] = now()
      }

    return updatedCount
  }

  private fun ResultRow.toLrmlist(): LrmList {
    return LrmList(
      id = this[repositoryTable.id].value,
      uuid = this[repositoryTable.uuid],
      name = this[repositoryTable.name],
      description = this[repositoryTable.description],
      created = this[repositoryTable.created],
      updated = this[repositoryTable.updated],
    )
  }
}
