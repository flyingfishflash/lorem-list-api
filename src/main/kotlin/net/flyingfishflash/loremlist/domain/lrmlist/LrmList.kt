package net.flyingfishflash.loremlist.domain.lrmlist

import kotlinx.datetime.Instant
import net.flyingfishflash.loremlist.domain.lrmitem.LrmItem
import java.util.UUID

data class LrmList(
  val id: UUID,
  val name: String,
  val description: String? = null,
  val public: Boolean = false,
  val owner: String,
  val created: Instant,
  val creator: String,
  val updated: Instant,
  val updater: String,
  val items: Set<LrmItem> = emptySet(),
)
