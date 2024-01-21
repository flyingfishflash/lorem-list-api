package net.flyingfishflash.loremlist.domain.lrmlist.data

import net.flyingfishflash.loremlist.domain.lrmlistitem.data.LrmListItem
import java.time.OffsetDateTime

data class LrmList(
  var id: Long,
  var created: OffsetDateTime? = null,
  var name: String,
  var description: String? = null,
  val items: MutableSet<LrmListItem> = mutableSetOf(),
) {
  fun addItem(lrmListItem: LrmListItem) {
    this.items.add(lrmListItem)
    lrmListItem.lists.add(this)
  }

  fun removeItem(itemId: Long) {
    val lrmListItem: LrmListItem = this.items.stream().filter { t -> t.id == itemId }.findFirst().orElse(null)
    this.items.remove(lrmListItem)
    lrmListItem.lists.remove(this)
  }
}
