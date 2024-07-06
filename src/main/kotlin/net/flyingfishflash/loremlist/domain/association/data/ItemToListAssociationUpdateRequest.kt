package net.flyingfishflash.loremlist.domain.association.data

import kotlinx.serialization.Serializable
import net.flyingfishflash.loremlist.core.serialization.UUIDSerializer
import net.flyingfishflash.loremlist.core.validation.ValidUuid
import java.util.*

@Serializable
data class ItemToListAssociationUpdateRequest(
  @field:ValidUuid
  @Serializable(with = UUIDSerializer::class)
  val currentListUuid: UUID,
  @field:ValidUuid
  @Serializable(with = UUIDSerializer::class)
  val newListUuid: UUID,
)
