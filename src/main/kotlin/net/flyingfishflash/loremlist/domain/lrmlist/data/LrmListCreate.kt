package net.flyingfishflash.loremlist.domain.lrmlist.data

import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class LrmListCreate(
  @field:Pattern(regexp = "^(?!\\s*$).+", message = "List name must not consist only of whitespace characters.")
  @field:Size(min = 1, max = 64, message = "List name must have at least 1, and no more than 64 characters.")
  val name: String,
  @field:Pattern(regexp = "^(?!\\s*$).+", message = "List description must not consist only of whitespace characters.")
  @field:Size(min = 1, max = 2048, message = "List description must have at least 1, and no more than 2048 characters.")
  val description: String? = null,
  val public: Boolean,
)
