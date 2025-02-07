// A class to coordinate commands for managing items and lists.
class ApplicationService(
  private val listRepository: ListRepository,
  private val itemRepository: ItemRepository,
  private val listItemService: ListItemService,
) {

  fun createItem(name: String, description: String): Item {
    val id = itemRepository.findAll().size.toLong() + 1
    val item = Item(id, name, description)
    itemRepository.save(item)
    return item
  }

  fun createList(name: String, description: String): List {
    val id = listRepository.findAll().size.toLong() + 1
    val list = List(id, name, description)
    listRepository.save(list)
    return list
  }

  fun addItemToList(listId: Long, itemId: Long) {
    listItemService.addItemToList(listId, itemId)
  }

  fun listItemsInList(listId: Long): List<Item> {
    return listItemService.getItemsInList(listId)
  }

  fun listListsForItem(itemId: Long): List<List> {
    return listItemService.getListsForItem(itemId)
  }
}

// fun main() {
//  // Set up repositories and services
//  val listRepository = InMemoryListRepository()
//  val itemRepository = InMemoryItemRepository()
//  val listItemService = ListItemService(listRepository, itemRepository)
//  val applicationService = ApplicationService(listRepository, itemRepository, listItemService)
//
//  // Create some items
//  val item1 = applicationService.createItem("Item 1", "Description of Item 1")
//  val item2 = applicationService.createItem("Item 2", "Description of Item 2")
//  val item3 = applicationService.createItem("Item 3", "Description of Item 3")
//
//  // Create some lists
//  val list1 = applicationService.createList("List 1", "Description of List 1")
//  val list2 = applicationService.createList("List 2", "Description of List 2")
//
//  // Add items to lists (many-to-many relationship)
//  applicationService.addItemToList(list1.id, item1.id)
//  applicationService.addItemToList(list1.id, item2.id)
//  applicationService.addItemToList(list2.id, item2.id)
//  applicationService.addItemToList(list2.id, item3.id)
//
//  // Display all items in List 1
//  println("Items in List 1: ${applicationService.listItemsInList(list1.id)}")
//  // Display all items in List 2
//  println("Items in List 2: ${applicationService.listItemsInList(list2.id)}")
//
//  // Display all lists for Item 2
//  println("Lists for Item 2: ${applicationService.listListsForItem(item2.id)}")
// }
