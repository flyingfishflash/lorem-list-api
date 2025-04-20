package net.flyingfishflash.loremlist.domain.lrmlistitem.data

import net.flyingfishflash.loremlist.domain.SuccinctLrmComponent
import net.flyingfishflash.loremlist.domain.lrmlistitem.LrmListItem

data class LrmListItemCreated(val lrmListItem: LrmListItem, val associatedComponents: List<SuccinctLrmComponent>)
