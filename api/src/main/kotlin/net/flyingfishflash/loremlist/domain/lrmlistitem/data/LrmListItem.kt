package net.flyingfishflash.loremlist.domain.lrmlistitem.data

import net.flyingfishflash.loremlist.domain.lrmlist.data.LrmList

data class LrmListItem(
  var id: Long,
  var name: String,
  var details: String? = null,
  var quantity: Short? = null,
  val lists: MutableSet<LrmList> = mutableSetOf(),
)
