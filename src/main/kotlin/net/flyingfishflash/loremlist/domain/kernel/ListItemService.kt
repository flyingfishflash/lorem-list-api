// A service to handle the many-to-many relationships between lists and items.
class ListItemService(
  private val listRepository: ListRepository,
  private val itemRepository: ItemRepository
) {
  private val listItems = mutableMapOf<Long, MutableSet<Long>>() // List ID -> Set of Item IDs

  // Add an item to a list (many-to-many relationship)
  fun addItemToList(listId: Long, itemId: Long) {
    val listItemsForList = listItems.getOrPut(listId) { mutableSetOf() }
    listItemsForList.add(itemId)
  }

  // Get all items in a list
  fun getItemsInList(listId: Long): List<Item> {
    val itemIds = listItems[listId] ?: return emptyList()
    return itemRepository.findAll().filter { it.id in itemIds }
  }

  // Get all lists an item belongs to
  fun getListsForItem(itemId: Long): List<List> {
    return listItems.filter { it.value.contains(itemId) }
      .mapNotNull { listRepository.findById(it.key) }
  }
}
