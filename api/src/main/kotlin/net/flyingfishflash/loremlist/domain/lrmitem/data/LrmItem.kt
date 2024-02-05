package net.flyingfishflash.loremlist.domain.lrmitem.data

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import net.flyingfishflash.loremlist.domain.lrmlist.data.LrmList

@Serializable
data class LrmItem(
  var id: Long,
  var created: Instant? = null,
  var name: String,
  var description: String? = null,
  var quantity: Long? = null,
  var lists: List<LrmList>? = null,
)
