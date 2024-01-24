package net.flyingfishflash.loremlist.domain.lrmlist.data

import net.flyingfishflash.loremlist.domain.lrmitem.data.LrmItem
import java.time.OffsetDateTime

data class LrmList(
  var id: Long,
  var created: OffsetDateTime? = null,
  var name: String,
  var description: String? = null,
  val items: Set<LrmItem> = setOf(),
)
