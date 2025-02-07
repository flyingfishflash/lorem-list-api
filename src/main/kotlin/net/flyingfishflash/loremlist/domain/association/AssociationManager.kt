package net.flyingfishflash.loremlist.domain.association

import net.flyingfishflash.loremlist.domain.lrmitem.LrmItemDomain
import net.flyingfishflash.loremlist.domain.lrmlist.LrmListDomain

class AssociationManager {
  private val allItems = mutableSetOf<LrmItemDomain>()
  private val allLists = mutableSetOf<LrmListDomain>()

  fun addItem(item: LrmItemDomain) {
    allItems.add(item)
  }

  fun addList(list: LrmListDomain) {
    allLists.add(list)
  }

  fun addItemToList(
    item: LrmItemDomain,
    list: LrmListDomain,
    isVisible: Boolean,
  ) {
    list.addItem(item)
    item.setVisibilityInList(list, isVisible)
  }

  fun getItemsForList(list: LrmListDomain): Set<LrmItemDomain> {
    return list.getItems()
  }

  fun getListsForItem(item: LrmItemDomain): Set<LrmListDomain> {
    return allLists.filter { it.getItems().contains(item) }.toSet()
  }

  fun displayAllItems() {
    println("All Items:")
    allItems.forEach { println(it) }
  }

  fun displayAllLists() {
    println("All Lists:")
    allLists.forEach { println(it) }
  }

  fun displayVisibilityForItem(item: LrmItemDomain) {
    println("Visibility for Item '${item.name}':")
    allLists.forEach { list ->
      val isVisible = item.getVisibilityInList(list)
      println(" - In ${list.name}: ${if (isVisible) "Visible" else "Invisible"}")
    }
  }
}
