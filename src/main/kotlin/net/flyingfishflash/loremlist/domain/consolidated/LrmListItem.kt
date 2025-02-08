package net.flyingfishflash.loremlist.domain.consolidated

import kotlinx.datetime.Instant
import net.flyingfishflash.loremlist.domain.lrmitem.data.LrmItemSuccinct
import java.util.UUID

// @Serializable
// @OptIn(ExperimentalSerializationApi::class)
data class LrmListItem(
//  @Serializable(with = UUIDSerializer::class)
  var id: UUID,
  var name: String,
  var description: String? = null,
//  @EncodeDefault
  var quantity: Int = 0,
  var created: Instant? = null,
  var createdBy: String? = null,
  var updated: Instant? = null,
  var updatedBy: String? = null,
//  val lists: Set<LrmListSuccinct>? = null,
) {
  private val visibilityMap = mutableMapOf<UUID, Boolean>()

  fun setVisibilityInList(listId: UUID, isVisible: Boolean) {
    visibilityMap[listId] = isVisible
  }

  fun getVisibilityInList(listId: UUID): Boolean {
    return visibilityMap.getOrDefault(listId, false) // Default to false if not set
  }
}

fun LrmListItem.succinct() = LrmItemSuccinct(id = this.id, name = this.name)
