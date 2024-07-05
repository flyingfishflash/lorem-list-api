package net.flyingfishflash.loremlist.domain.association

import kotlinx.serialization.Serializable
import net.flyingfishflash.loremlist.core.serialization.UUIDSerializer
import java.util.UUID

@Serializable
data class Association(
  @Serializable(with = UUIDSerializer::class)
  val uuid: UUID,
  @Serializable(with = UUIDSerializer::class)
  val listUuid: UUID,
  @Serializable(with = UUIDSerializer::class)
  val itemUuid: UUID,
)
