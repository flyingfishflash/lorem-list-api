package net.flyingfishflash.loremlist.domain.lrmitem

import kotlinx.datetime.Instant
import net.flyingfishflash.loremlist.domain.lrmlist.data.LrmListSuccinct
import java.util.*

class LrmItemDomain(
  val id: UUID,
  var name: String,
  var description: String? = null,
  var quantity: Int = 0,
  val created: Instant? = null,
  val createdBy: String? = null,
  var updated: Instant? = null,
  var updatedBy: String? = null,
  private var lists: MutableSet<LrmListSuccinct> = mutableSetOf(),
)
