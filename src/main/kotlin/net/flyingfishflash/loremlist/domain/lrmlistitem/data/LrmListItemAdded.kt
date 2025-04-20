package net.flyingfishflash.loremlist.domain.lrmlistitem.data

import net.flyingfishflash.loremlist.domain.SuccinctLrmComponent

data class LrmListItemAdded(val listName: String, val items: List<SuccinctLrmComponent>)
