package net.flyingfishflash.loremlist.persistence

import net.flyingfishflash.loremlist.domain.association.Association
import net.flyingfishflash.loremlist.domain.association.AssociationRepository
import net.flyingfishflash.loremlist.domain.lrmitem.LrmItem
import net.flyingfishflash.loremlist.domain.lrmitem.LrmItemSuccinct
import net.flyingfishflash.loremlist.domain.lrmlist.LrmListSuccinct
import net.flyingfishflash.loremlist.persistence.LrmListsItemsTable.item
import net.flyingfishflash.loremlist.persistence.LrmListsItemsTable.list
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
class AssociationRepositoryRdbms : AssociationRepository {
  private val repositoryTable = LrmListsItemsTable

  override fun listIsConsistent(listId: UUID): Boolean {
    val unEqualListAndItemOwner = (LrmListTable innerJoin LrmListItemTable)
      .select(LrmListTable.id)
      .where { LrmListTable.id eq listId }
      .andWhere { LrmListTable.createdBy neq LrmListItemTable.createdBy }.toList()
    return unEqualListAndItemOwner.isEmpty()
  }

  override fun count(): Long {
    val idCount = repositoryTable.id.count()
    val count = repositoryTable.select(idCount).first()[idCount]
    return count
  }

  override fun countItemToListByIdAndItemOwner(itemId: UUID, itemOwner: String): Long {
    val itemCount = item.count()
    return (repositoryTable leftJoin LrmListItemTable).select(itemCount)
      .where { repositoryTable.item eq itemId }
      .andWhere { LrmListItemTable.createdBy eq itemOwner }.map { it[itemCount] }.first()
  }

  override fun countListToItemByIdandListOwner(listId: UUID, listOwner: String): Long {
    val listCount = list.count()
    return (repositoryTable leftJoin LrmListTable).select(listCount)
      .where { repositoryTable.list eq listId }
      .andWhere { LrmListTable.createdBy eq listOwner }.map { it[listCount] }.first()
  }

  override fun create(associationCollection: Set<Pair<UUID, UUID>>): List<SuccinctLrmComponentPair> {
    val associationIdSet = repositoryTable.batchInsert(associationCollection) { (listId, itemId) ->
      this[repositoryTable.list] = listId
      this[repositoryTable.item] = itemId
    }.map { it[repositoryTable.id].value }.toSet()

    val succinctLrmComponentPairs = (repositoryTable innerJoin LrmListTable innerJoin LrmListItemTable)
      .select(LrmListTable.id, LrmListTable.name, LrmListItemTable.id, LrmListItemTable.name)
      .where { repositoryTable.id inList (associationIdSet) }
      .map {
        SuccinctLrmComponentPair(
          list = LrmListSuccinct(it[LrmListTable.id], it[LrmListTable.name]),
          item = LrmItemSuccinct(it[LrmListItemTable.id], it[LrmListItemTable.name]),
        )
      }

    return succinctLrmComponentPairs
  }

  override fun create(listId: UUID, itemId: UUID) {
    repositoryTable.insert {
      it[list] = listId
      it[item] = itemId
    }
  }

  override fun deleteById(id: UUID): Int {
    return repositoryTable.deleteWhere {
      (this.id eq id)
    }
  }

  override fun deleteByItemIdAndListId(itemId: UUID, listId: UUID): Int {
    return repositoryTable.deleteWhere {
      (item eq itemId).and(list eq itemId)
    }
  }

  override fun delete(): Int {
    return repositoryTable.deleteAll()
  }

  override fun deleteByItemId(itemId: UUID): Int {
    return repositoryTable.deleteWhere { repositoryTable.item eq itemId }
  }

  override fun deleteByListId(listId: UUID): Int {
    return repositoryTable.deleteWhere { repositoryTable.list eq listId }
  }

  override fun findByItemIdAndListIdOrNull(itemId: UUID, listId: UUID): Association? {
    return repositoryTable.selectAll().where { item eq itemId and (list eq listId) }.firstOrNull()?.let {
      Association(
        id = it[repositoryTable.id].value,
        listId = it[repositoryTable.list],
        itemId = it[repositoryTable.item],
      )
    }
  }

  override fun findListsByItem(lrmItem: LrmItem): Set<LrmListSuccinct> {
    return (repositoryTable innerJoin LrmListTable)
      .select(LrmListTable.id, LrmListTable.name)
      .where { repositoryTable.item eq lrmItem.id }
      .map { LrmListSuccinct(it[LrmListTable.id], it[LrmListTable.name]) }
      .toSet()
  }

  override fun update(association: Association): Int {
    return repositoryTable.update({ repositoryTable.id eq association.id }) {
      it[repositoryTable.item] = association.itemId
      it[repositoryTable.list] = association.listId
    }
  }
}
