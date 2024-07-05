package net.flyingfishflash.loremlist.domain.lrmlist.data

import kotlinx.serialization.Serializable
import net.flyingfishflash.loremlist.core.serialization.UUIDSerializer
import java.util.UUID

@Serializable
data class LrmListSuccinct(
  @Serializable(with = UUIDSerializer::class)
  var uuid: UUID,
  val name: String,
)
