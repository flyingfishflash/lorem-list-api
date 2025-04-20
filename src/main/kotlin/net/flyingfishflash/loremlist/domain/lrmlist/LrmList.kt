package net.flyingfishflash.loremlist.domain.lrmlist

import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import kotlinx.datetime.Instant
import net.flyingfishflash.loremlist.core.validation.ValidUuid
import net.flyingfishflash.loremlist.domain.lrmlistitem.LrmListItem
import java.util.UUID

data class LrmList(
  @ValidUuid
  val id: UUID,
  @field:Pattern(regexp = "^(?!\\s*$).+", message = "List name must not consist only of whitespace characters.")
  @field:Size(min = 1, max = 64, message = "List name must have at least 1, and no more than 64 characters.")
  val name: String,
  @field:Pattern(regexp = "^(?!\\s*$).+", message = "List description must not consist only of whitespace characters.")
  @field:Size(min = 1, max = 2048, message = "List description must have at least 1, and no more than 2048 characters.")
  val description: String? = null,
  val public: Boolean = false,
  val owner: String,
  val created: Instant,
  val creator: String,
  val updated: Instant,
  val updater: String,
  val items: Set<LrmListItem> = emptySet(),
)
