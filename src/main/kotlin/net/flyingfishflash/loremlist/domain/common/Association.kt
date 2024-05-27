package net.flyingfishflash.loremlist.domain.common

import kotlinx.serialization.Serializable
import net.flyingfishflash.loremlist.core.serialization.UUIDSerializer
import java.util.UUID

@Serializable
data class Association(
  @Serializable(with = UUIDSerializer::class)
  val uuid: UUID,
  val listId: Long,
  val itemId: Long,
)
