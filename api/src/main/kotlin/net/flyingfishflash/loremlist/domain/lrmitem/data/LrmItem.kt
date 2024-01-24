package net.flyingfishflash.loremlist.domain.lrmitem.data

import net.flyingfishflash.loremlist.domain.lrmlist.data.LrmList
import java.time.OffsetDateTime

data class LrmItem(
  var id: Long,
  var created: OffsetDateTime? = null,
  var name: String,
  var description: String? = null,
  var quantity: Long? = null,
  var lists: Set<LrmList>? = null,
)
