package net.flyingfishflash.loremlist.domain.lrmlist

import kotlinx.datetime.Instant
import net.flyingfishflash.loremlist.domain.lrmitem.LrmItem
import net.flyingfishflash.loremlist.domain.lrmitem.toDto
import net.flyingfishflash.loremlist.domain.lrmlist.data.LrmListResponse
import net.flyingfishflash.loremlist.domain.lrmlist.data.LrmListSuccinct
import java.util.UUID

data class LrmList
constructor(
  var id: UUID,
  var name: String,
  var description: String? = null,
  var public: Boolean = false,
  var created: Instant,
  var createdBy: String,
  var updated: Instant,
  var updatedBy: String,
  val items: Set<LrmItem>? = null,
)

fun LrmList.succinct() = LrmListSuccinct(id = this.id, name = this.name)
fun LrmList.toDto() = LrmListResponse(
  id = this.id,
  name = this.name,
  description = this.description,
  public = this.public,
  created = this.created,
  createdBy = this.createdBy,
  updated = this.updated,
  updatedBy = this.updatedBy,
  items = this.items?.map { it.toDto() }?.toSet(),
)
