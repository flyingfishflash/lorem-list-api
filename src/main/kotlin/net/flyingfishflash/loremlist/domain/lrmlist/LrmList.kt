package net.flyingfishflash.loremlist.domain.lrmlist

import kotlinx.datetime.Instant
import net.flyingfishflash.loremlist.domain.lrmitem.LrmItem
import java.util.UUID

data class LrmList(
  val id: UUID,
  val name: String,
  val description: String? = null,
  val public: Boolean = false,
  val created: Instant,
  val createdBy: String,
  val updated: Instant,
  val updatedBy: String,
  val items: Set<LrmItem> = emptySet(),
)
