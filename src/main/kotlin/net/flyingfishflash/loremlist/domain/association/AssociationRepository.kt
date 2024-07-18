package net.flyingfishflash.loremlist.domain.association

import net.flyingfishflash.loremlist.domain.LrmListItemTable
import net.flyingfishflash.loremlist.domain.LrmListTable
import net.flyingfishflash.loremlist.domain.LrmListsItemsTable
import net.flyingfishflash.loremlist.domain.LrmListsItemsTable.item
import net.flyingfishflash.loremlist.domain.LrmListsItemsTable.list
import net.flyingfishflash.loremlist.domain.lrmitem.data.LrmItemSuccinct
import net.flyingfishflash.loremlist.domain.lrmlist.data.LrmListSuccinct
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.count
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import org.springframework.stereotype.Repository
import java.util.UUID

data class SuccinctLrmComponentPair(val list: LrmListSuccinct, val item: LrmItemSuccinct)

@Repository
class AssociationRepository {
  private val repositoryTable = LrmListsItemsTable

  fun count(): Long {
    val uuidCount = repositoryTable.id.count()
    val count = repositoryTable.select(uuidCount).first()[uuidCount]
    return count
  }

  fun countItemToList(itemUuid: UUID): Long {
    val itemCount = item.count()
    return repositoryTable.select(itemCount).where { item eq itemUuid }.map { it[itemCount] }.first()
  }

  fun countListToItem(listUuid: UUID): Long {
    val listCount = list.count()
    return repositoryTable.select(listCount).where { list eq listUuid }.map { it[listCount] }.first()
  }

  fun create(associationCollection: Set<Pair<UUID, UUID>>): List<SuccinctLrmComponentPair> {
    val associationIdSet = repositoryTable.batchInsert(associationCollection) { (listUuid, itemUuid) ->
      this[repositoryTable.list] = listUuid
      this[repositoryTable.item] = itemUuid
    }.map { it[repositoryTable.id].value }.toSet()

    val succinctLrmComponentPairs = (repositoryTable innerJoin LrmListTable innerJoin LrmListItemTable)
      .select(LrmListTable.id, LrmListTable.name, LrmListItemTable.id, LrmListItemTable.name)
      .where { repositoryTable.id inList (associationIdSet) }
      .map {
        SuccinctLrmComponentPair(
          list = LrmListSuccinct(it[LrmListTable.id].value, it[LrmListTable.name]),
          item = LrmItemSuccinct(it[LrmListItemTable.id].value, it[LrmListItemTable.name]),
        )
      }

    return succinctLrmComponentPairs
  }

  fun create(listUuid: UUID, itemUuid: UUID) {
    repositoryTable.insert {
      it[list] = listUuid
      it[item] = itemUuid
    }
  }

  fun delete(uuid: UUID): Int {
    return repositoryTable.deleteWhere {
      (id eq uuid)
    }
  }

  fun delete(itemUuid: UUID, listUuid: UUID): Int {
    return repositoryTable.deleteWhere {
      (item eq itemUuid).and(list eq itemUuid)
    }
  }

  fun deleteAll(): Int {
    return repositoryTable.deleteAll()
  }

  fun deleteAllOfItem(itemUuid: UUID): Int {
    return repositoryTable.deleteWhere { repositoryTable.item eq itemUuid }
  }

  fun deleteAllOfList(listUuid: UUID): Int {
    return repositoryTable.deleteWhere { repositoryTable.list eq listUuid }
  }

  fun findByItemIdAndListIdOrNull(itemUuid: UUID, listUuid: UUID): Association? {
    return repositoryTable.selectAll().where { item eq itemUuid and (list eq listUuid) }.firstOrNull()?.let {
      Association(
        uuid = it[repositoryTable.id].value,
        listUuid = it[repositoryTable.list].value,
        itemUuid = it[repositoryTable.item].value,
      )
    }
  }

  fun update(association: Association): Int {
    return repositoryTable.update({ repositoryTable.id eq association.uuid }) {
      it[repositoryTable.item] = association.itemUuid
      it[repositoryTable.list] = association.listUuid
    }
  }
}
