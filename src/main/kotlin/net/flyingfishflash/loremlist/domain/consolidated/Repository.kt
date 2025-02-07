// In-memory storage for simplicity in this example
interface ListRepository {
  fun save(list: List)
  fun findById(id: Long): List?
  fun findAll(): List<List>
}

interface ItemRepository {
  fun save(item: Item)
  fun findById(id: Long): Item?
  fun findAll(): List<Item>
}

// Concrete in-memory repositories
class InMemoryListRepository : ListRepository {
  private val lists = mutableListOf<List>()

  override fun save(list: List) {
    lists.add(list)
  }

  override fun findById(id: Long): List? {
    return lists.find { it.id == id }
  }

  override fun findAll(): List<List> = lists
}

class InMemoryItemRepository : ItemRepository {
  private val items = mutableListOf<Item>()

  override fun save(item: Item) {
    items.add(item)
  }

  override fun findById(id: Long): Item? {
    return items.find { it.id == id }
  }

  override fun findAll(): List<Item> = items
}
