package net.flyingfishflash.loremlist.domain.common

import net.flyingfishflash.loremlist.domain.LrmListsItemsTable
import net.flyingfishflash.loremlist.domain.LrmListsItemsTable.item
import net.flyingfishflash.loremlist.domain.LrmListsItemsTable.list
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.count
import org.jetbrains.exposed.sql.deleteWhere
import org.springframework.stereotype.Repository

@Repository
class CommonRepository {
  private val repositoryTable = LrmListsItemsTable

  fun countItemToListAssociations(itemId: Long): Long {
    val itemCount = item.count()
    return repositoryTable.select(itemCount).where { item eq itemId }.map { it[itemCount] }.first()
  }

  fun countListToItemAssociations(listId: Long): Long {
    val listCount = list.count()
    return repositoryTable.select(listCount).where { list eq listId }.map { it[listCount] }.first()
  }

  fun deleteAllItemToListAssociations(itemId: Long): Int {
    return repositoryTable.deleteWhere { repositoryTable.item eq itemId }
  }
}
