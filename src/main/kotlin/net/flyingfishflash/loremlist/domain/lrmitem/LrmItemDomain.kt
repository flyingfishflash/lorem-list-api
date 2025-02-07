package net.flyingfishflash.loremlist.domain.lrmitem

import kotlinx.datetime.Instant
import net.flyingfishflash.loremlist.domain.lrmlist.LrmListDomain
import java.util.*

class LrmItemDomain(
  val id: UUID,
  var name: String,
  var description: String? = null,
  var quantity: Int = 0,
  val created: Instant? = null,
  val createdBy: String? = null,
  var updated: Instant? = null,
  var updatedBy: String? = null,
) {

  private val visibilityMap = mutableMapOf<LrmListDomain, Boolean>()

  fun setVisibilityInList(list: LrmListDomain, isVisible: Boolean) {
    visibilityMap[list] = isVisible
  }

  fun getVisibilityInList(list: LrmListDomain): Boolean {
    return visibilityMap.getOrDefault(list, false) // Default to false if not set
  }
}
