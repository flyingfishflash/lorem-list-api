package net.flyingfishflash.loremlist.domain.lrmlist

import kotlinx.datetime.Instant
import net.flyingfishflash.loremlist.domain.lrmitem.LrmItemDomain
import java.util.*

class LrmListDomain(
  val id: UUID,
  var name: String,
  var description: String? = null,
  var public: Boolean = false,
  val created: Instant? = null,
  val createdBy: String? = null,
  var updated: Instant? = null,
  var updatedBy: String? = null,
) {

  private var items: MutableSet<LrmItemDomain> = mutableSetOf()

  fun getItems(): Set<LrmItemDomain> = items

  fun addItem(item: LrmItemDomain): Boolean = items.add(item)

  fun removeItem(lrmItem: LrmItemDomain): Boolean = items.remove(lrmItem)
}
