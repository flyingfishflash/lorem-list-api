package net.flyingfishflash.loremlist.domain.association

import net.flyingfishflash.loremlist.domain.LrmListsItemsTable
import net.flyingfishflash.loremlist.domain.LrmListsItemsTable.item
import net.flyingfishflash.loremlist.domain.LrmListsItemsTable.list
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.count
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import org.springframework.stereotype.Repository

@Repository
class AssociationRepository {
  private val repositoryTable = LrmListsItemsTable

  fun countItemToList(itemId: Long): Long {
    val itemCount = item.count()
    return repositoryTable.select(itemCount).where { item eq itemId }.map { it[itemCount] }.first()
  }

  fun countListToItem(listId: Long): Long {
    val listCount = list.count()
    return repositoryTable.select(listCount).where { list eq listId }.map { it[listCount] }.first()
  }

  fun create(listId: Long, itemId: Long) {
    repositoryTable.insert {
      it[list] = listId
      it[item] = itemId
    }
  }

  fun delete(itemId: Long, listId: Long): Int {
    return repositoryTable.deleteWhere {
      (item eq itemId).and(list eq listId)
    }
  }

  fun deleteAllItemToListForItem(itemId: Long): Int {
    return repositoryTable.deleteWhere { repositoryTable.item eq itemId }
  }

  fun findByItemIdAndListIdOrNull(itemId: Long, listId: Long): Association? {
    return repositoryTable.selectAll().where { item eq itemId and (list eq listId) }.firstOrNull()?.let {
      Association(
        uuid = it[repositoryTable.id].value,
        listId = it[repositoryTable.list].value,
        itemId = it[repositoryTable.item].value,
      )
    }
  }

  fun update(association: Association): Int {
    return repositoryTable.update({ repositoryTable.id eq association.uuid }) {
      it[repositoryTable.item] = association.itemId
      it[repositoryTable.list] = association.listId
    }
  }
}
