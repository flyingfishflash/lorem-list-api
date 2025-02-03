package net.flyingfishflash.loremlist.domain.lrmlist

import kotlinx.datetime.Instant
import net.flyingfishflash.loremlist.domain.lrmitem.LrmItem
import java.util.*

class LrmListDomain(
  var id: UUID,
  var name: String,
  var description: String? = null,
  var public: Boolean = false,
  var created: Instant? = null,
  var createdBy: String? = null,
  var updated: Instant? = null,
  var updatedBy: String? = null,
  var items: MutableSet<LrmItem> = mutableSetOf(),
) {

  fun addItem(lrmItem: LrmItem) {
    items.add(lrmItem)
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    other as LrmListDomain
    return id == other.id
  }

  override fun hashCode(): Int {
    return id.hashCode()
  }
}
