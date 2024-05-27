package net.flyingfishflash.loremlist.domain.association.data

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotEmpty
import kotlinx.serialization.Serializable

@Serializable
data class ItemToListAssociationUpdateRequest(
  @field:NotEmpty
  @field:Min(1)
  val fromListId: Long,
  @field:NotEmpty
  @field:Min(1)
  val toListId: Long,
)
