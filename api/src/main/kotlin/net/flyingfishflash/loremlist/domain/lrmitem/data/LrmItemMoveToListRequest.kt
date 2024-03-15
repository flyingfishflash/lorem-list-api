package net.flyingfishflash.loremlist.domain.lrmitem.data

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotEmpty
import kotlinx.serialization.Serializable

@Serializable
data class LrmItemMoveToListRequest(
  @field:NotEmpty
  @field:Min(1)
  val fromListId: Long,
  @field:NotEmpty
  @field:Min(1)
  val toListId: Long,
)
