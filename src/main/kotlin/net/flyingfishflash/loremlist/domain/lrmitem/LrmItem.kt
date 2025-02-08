package net.flyingfishflash.loremlist.domain.lrmitem

import kotlinx.datetime.Instant
import net.flyingfishflash.loremlist.domain.lrmitem.data.LrmItemResponse
import net.flyingfishflash.loremlist.domain.lrmitem.data.LrmItemSuccinct
import net.flyingfishflash.loremlist.domain.lrmlist.data.LrmListSuccinct
import java.util.UUID

data class LrmItem(
  var id: UUID,
  var name: String,
  var description: String? = null,
  var quantity: Int = 0,
  var created: Instant? = null,
  var createdBy: String? = null,
  var updated: Instant? = null,
  var updatedBy: String? = null,
  val lists: Set<LrmListSuccinct>? = null,
)

fun LrmItem.succinct() = LrmItemSuccinct(id = this.id, name = this.name)
fun LrmItem.toDto() = LrmItemResponse(
  id = this.id,
  name = this.name,
  description = this.description,
  quantity = this.quantity,
  created = this.created,
  createdBy = this.createdBy,
  updated = this.updated,
  updatedBy = this.updatedBy,
  lists = this.lists,
)
