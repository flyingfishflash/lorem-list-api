package net.flyingfishflash.loremlist.domain.lrmlist.data

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import net.flyingfishflash.loremlist.domain.lrmitem.data.LrmItem

@Serializable
open class LrmList(
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
    items: Set<LrmItem>? = this.items,
  ): LrmList {
    return LrmList(id, created, name, description, items)
  }
}
