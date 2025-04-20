package net.flyingfishflash.loremlist.domain.lrmlistitem

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import kotlinx.datetime.Instant
import net.flyingfishflash.loremlist.core.validation.ValidUuid
import net.flyingfishflash.loremlist.domain.lrmlist.LrmListSuccinct
import java.util.UUID

// TODO: Make more domain-like, remove id and other entity fields
data class LrmListItem(
  @ValidUuid
  val id: UUID,
  @ValidUuid
  val listId: UUID,
  @field:Pattern(regexp = "^(?!\\s*$).+", message = "Item name must not consist only of whitespace characters.")
  @field:Size(min = 1, max = 64, message = "Item name must have at least 1, and no more than 64 characters.")
  val name: String,
  @field:Pattern(regexp = "^(?!\\s*$).+", message = "Item description must not consist only of whitespace characters.")
  @field:Size(min = 1, max = 2048, message = "Item description must have at least 1, and no more than 2048 characters.")
  val description: String? = null,
  @field:Min(value = 0, message = "Item quantity must be zero or greater.")
  val quantity: Int = 0,
  val isSuppressed: Boolean,
  val owner: String,
  val created: Instant,
  val creator: String,
  val updated: Instant,
  val updater: String,
  val lists: Set<LrmListSuccinct> = emptySet(),
)
