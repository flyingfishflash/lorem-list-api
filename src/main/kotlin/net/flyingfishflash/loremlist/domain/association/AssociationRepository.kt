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
import org.jetbrains.exposed.sql.andWhere
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

  fun listIsConsistent(listId: UUID): Boolean {
    val unEqualListAndItemOwner = (LrmListTable innerJoin LrmListItemTable)
      .select(LrmListTable.id)
      .where { LrmListTable.id eq listId }
      .andWhere { LrmListTable.createdBy neq LrmListItemTable.createdBy }.toList()
    return unEqualListAndItemOwner.isEmpty()
  }

  fun count(): Long {
    val idCount = repositoryTable.id.count()
    val count = repositoryTable.select(idCount).first()[idCount]
    return count
  }

  fun countItemToListByIdAndItemOwner(itemId: UUID, itemOwner: String): Long {
    val itemCount = item.count()
    return (repositoryTable leftJoin LrmListItemTable).select(itemCount)
      .where { repositoryTable.item eq itemId }
      .andWhere { LrmListItemTable.createdBy eq itemOwner }.map { it[itemCount] }.first()
  }

  fun countListToItemByIdandListOwner(listId: UUID, listOwner: String): Long {
    val listCount = list.count()
    return (repositoryTable leftJoin LrmListTable).select(listCount)
      .where { repositoryTable.list eq listId }
      .andWhere { LrmListTable.createdBy eq listOwner }.map { it[listCount] }.first()
  }

  fun create(associationCollection: Set<Pair<UUID, UUID>>): List<SuccinctLrmComponentPair> {
    val associationIdSet = repositoryTable.batchInsert(associationCollection) { (listId, itemId) ->
      this[repositoryTable.list] = listId
      this[repositoryTable.item] = itemId
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

  fun create(listId: UUID, itemId: UUID) {
    repositoryTable.insert {
      it[list] = listId
      it[item] = itemId
    }
  }

  fun deleteById(id: UUID): Int {
    return repositoryTable.deleteWhere {
      (this.id eq id)
    }
  }

  fun deleteByItemIdAndListId(itemId: UUID, listId: UUID): Int {
    return repositoryTable.deleteWhere {
      (item eq itemId).and(list eq itemId)
    }
  }

  fun delete(): Int {
    return repositoryTable.deleteAll()
  }

  fun deleteByItemId(itemId: UUID): Int {
    return repositoryTable.deleteWhere { repositoryTable.item eq itemId }
  }

  fun deleteByListId(listId: UUID): Int {
    return repositoryTable.deleteWhere { repositoryTable.list eq listId }
  }

  fun findByItemIdAndListIdOrNull(itemId: UUID, listId: UUID): Association? {
    return repositoryTable.selectAll().where { item eq itemId and (list eq listId) }.firstOrNull()?.let {
      Association(
        id = it[repositoryTable.id].value,
        listId = it[repositoryTable.list].value,
        itemId = it[repositoryTable.item].value,
      )
    }
  }

  fun update(association: Association): Int {
    return repositoryTable.update({ repositoryTable.id eq association.id }) {
      it[repositoryTable.item] = association.itemId
      it[repositoryTable.list] = association.listId
    }
  }
}
