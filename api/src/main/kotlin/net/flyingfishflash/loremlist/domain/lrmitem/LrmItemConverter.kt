package net.flyingfishflash.loremlist.domain.lrmitem

import net.flyingfishflash.loremlist.domain.lrmitem.data.LrmItemRequest

object LrmItemConverter {
  fun toLrmItem(lrmItemRequest: LrmItemRequest, lrmItem: LrmItem): LrmItem {
    lrmItem.name = lrmItemRequest.name
    lrmItem.description = lrmItemRequest.description
    lrmItem.quantity = lrmItemRequest.quantity
    return lrmItem
  }
}
