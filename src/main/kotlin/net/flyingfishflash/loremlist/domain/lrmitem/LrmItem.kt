package net.flyingfishflash.loremlist.domain.lrmitem

import kotlinx.datetime.Instant
import net.flyingfishflash.loremlist.domain.lrmlist.LrmListSuccinct
import java.util.UUID

// TODO: Make more domain-like, remove id and other entity fields
data class LrmItem(
  val id: UUID,
  val name: String,
  val description: String? = null,
  val quantity: Int = 0,
  val owner: String,
  val created: Instant,
  val creator: String,
  val updated: Instant,
  val updater: String,
  val lists: Set<LrmListSuccinct> = emptySet(),
)
