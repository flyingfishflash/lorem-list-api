package net.flyingfishflash.loremlist.domain.lrmlist.data

import kotlinx.datetime.Instant
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import net.flyingfishflash.loremlist.domain.lrmitem.data.LrmItem

@Serializable
@OptIn(ExperimentalSerializationApi::class)
open class LrmList(
  var id: Long,
  var created: Instant? = null,
  var name: String,
  var description: String? = null,
  @EncodeDefault(EncodeDefault.Mode.ALWAYS) val items: Set<LrmItem> = setOf(),
) {
  fun copyWith(
    id: Long = this.id,
    created: Instant? = this.created,
    name: String = this.name,
    description: String? = this.description,
    items: Set<LrmItem> = this.items,
  ): LrmList {
    return LrmList(id, created, name, description, items)
  }
}
