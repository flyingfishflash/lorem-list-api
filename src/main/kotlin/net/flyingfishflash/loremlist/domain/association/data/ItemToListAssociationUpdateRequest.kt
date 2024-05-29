package net.flyingfishflash.loremlist.domain.association.data

import jakarta.validation.constraints.Min
import kotlinx.serialization.Serializable

@Serializable
data class ItemToListAssociationUpdateRequest(
  @field:Min(value = 1, message = "Source list id must be a positive number.")
  val fromListId: Long,
  @field:Min(value = 1, message = "Destination list id must be a positive number.")
  val toListId: Long,
)
