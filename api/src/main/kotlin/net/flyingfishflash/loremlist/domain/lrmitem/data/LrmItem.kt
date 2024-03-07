package net.flyingfishflash.loremlist.domain.lrmitem.data

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import net.flyingfishflash.loremlist.domain.lrmlist.data.LrmListSuccinct

@Serializable
data class LrmItem(
  var id: Long,
  var created: Instant? = null,
  var name: String,
  var description: String? = null,
  var quantity: Long? = null,
  val lists: Set<LrmListSuccinct>? = null,
) {
  fun copyWith(
    id: Long = this.id,
    created: Instant? = this.created,
    name: String = this.name,
    description: String? = this.description,
    quantity: Long? = this.quantity,
    lists: Set<LrmListSuccinct>? = this.lists,
  ): LrmItem {
    return LrmItem(id, created, name, description, quantity, lists)
  }
}
