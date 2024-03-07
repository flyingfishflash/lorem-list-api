package net.flyingfishflash.loremlist.domain.lrmlist.data

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import net.flyingfishflash.loremlist.domain.lrmitem.data.LrmItem

@Serializable
data class LrmList(
  var id: Long,
  var created: Instant? = null,
  var name: String,
  var description: String? = null,
  val items: Set<LrmItem>? = null,
) {
  fun copyWith(
    id: Long = this.id,
    created: Instant? = this.created,
    name: String = this.name,
    description: String? = this.description,
    items: Set<LrmItem>? = null,
  ): LrmList {
    return LrmList(id, created, name, description, items)
  }
}

// class LrmListWithItems(
//  id: Long,
//  created: Instant? = null,
//  name: String,
//  description: String? = null,
//  val items: Set<LrmItem>? = null // Replace List<String> with the actual type of your items
// ) : LrmList(id, created, name, description) {
//  fun copyWith(
//    id: Long = this.id,
//    created: Instant? = this.created,
//    name: String = this.name,
//    description: String? = this.description,
//    items: Set<LrmItem>? = this.items
//  ): LrmListWithItems {
//    return LrmListWithItems(id, created, name, description, items)
//  }
// }

@Serializable
data class LrmListSuccinct(
  val id: Long,
  val name: String,
)
