package net.flyingfishflash.loremlist.domain.lrmitem.data

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class LrmItemCreate(
  @field:Pattern(regexp = "^(?!\\s*$).+", message = "Item name must not consist only of whitespace characters.")
  @field:Size(min = 1, max = 64, message = "Item name must have at least 1, and no more than 64 characters.")
  val name: String,
  @field:Pattern(regexp = "^(?!\\s*$).+", message = "Item description must not consist only of whitespace characters.")
  @field:Size(min = 1, max = 2048, message = "Item description must have at least 1, and no more than 2048 characters.")
  val description: String? = null,
  @field:Min(value = 0, message = "Item quantity must be zero or greater.")
  val quantity: Int = 0,
  val isSuppressed: Boolean,
)
