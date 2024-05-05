package net.flyingfishflash.loremlist.domain.lrmlist

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import net.flyingfishflash.loremlist.core.serialization.UUIDSerializer
import net.flyingfishflash.loremlist.domain.lrmitem.LrmItem
import java.util.UUID

@Serializable
data class LrmList(
  var id: Long,
  @Serializable(with = UUIDSerializer::class)
  var uuid: UUID,
  var created: Instant? = null,
  var name: String,
  var description: String? = null,
  val items: Set<LrmItem>? = null,
)
