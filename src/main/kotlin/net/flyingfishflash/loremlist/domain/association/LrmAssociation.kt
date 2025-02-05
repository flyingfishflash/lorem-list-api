package net.flyingfishflash.loremlist.domain.association

import net.flyingfishflash.loremlist.domain.lrmitem.LrmItem
import net.flyingfishflash.loremlist.domain.lrmlist.LrmList

data class ItemAssociationContext(val lrmItem: LrmItem, val isItemSelected: Boolean)

class LrmAssociation(val list: LrmList, val item: LrmItem, val isItemSelected: Boolean = true) {

  fun getItemAssociationContext(): ItemAssociationContext {
    return ItemAssociationContext(this.item, this.isItemSelected)
  }
}
