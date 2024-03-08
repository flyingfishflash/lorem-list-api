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
)

@Serializable
data class LrmListSuccinct(
  val id: Long,
  val name: String,
)
