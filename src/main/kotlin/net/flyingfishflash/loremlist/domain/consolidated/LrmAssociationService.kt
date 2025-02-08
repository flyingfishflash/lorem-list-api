package net.flyingfishflash.loremlist.domain.consolidated

import net.flyingfishflash.loremlist.domain.lrmitem.data.LrmItemSuccinct
import net.flyingfishflash.loremlist.domain.lrmlist.data.LrmListSuccinct
import java.util.UUID

class LrmAssociationService(
  private val associationRepository: LrmAssociationRepository,
  private val listRepository: LrmListRepository,
  private val itemRepository: LrmListItemRepository,
) {
  private val listItems = mutableMapOf<UUID, MutableSet<UUID>>() // List ID -> Set of Item IDs

  fun addItemToList(listId: UUID, item: LrmListItem) {
    val listItemsForList = listItems.getOrPut(listId) { mutableSetOf() }
    listItemsForList.add(item.id)
    associationRepository.create(listId, item.id, item.getVisibilityInList(listId))
  }

  fun removeItemFromList(listId: UUID, itemId: UUID) {
    listItems[listId]?.remove(itemId)
    associationRepository.deleteByItemIdAndListId(itemId, listId)
  }

  // Get all items in a list
  fun getItemsOfList(listId: UUID, listOwner: String): List<LrmItemSuccinct> {
    val itemIds = listItems[listId] ?: return emptyList()
//    associationRepository.findItemsByListIdAndListOwner(listId, listOwner)
//    return itemRepository.findAll().filter { it.id in itemIds }
    return listOf(LrmItemSuccinct(UUID.randomUUID(), "itemName"))
  }

  // Get all lists an item belongs to
  fun getListsForItem(itemId: UUID, listOwner: String): List<LrmListSuccinct> {
//    associationRepository.findListsByItemIdAndListOwner(itemId, listOwner)
    return listOf(LrmListSuccinct(UUID.randomUUID(), "listName"))
//    return listItems.filter { it.value.contains(itemId) }
//      .mapNotNull { listRepository.findById(it.key) }
  }
}
