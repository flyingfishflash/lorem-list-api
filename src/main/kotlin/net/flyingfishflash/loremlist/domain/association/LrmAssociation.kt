package net.flyingfishflash.loremlist.domain.association

import net.flyingfishflash.loremlist.domain.lrmitem.LrmItemDomain
import net.flyingfishflash.loremlist.domain.lrmlist.LrmListDomain

data class ItemAssociationContext(val lrmItem: LrmItemDomain, val isItemSelected: Boolean)

class LrmAssociation(val list: LrmListDomain, val item: LrmItemDomain, private val isItemSelected: Boolean = true) {

  fun getItemAssociationContext(): ItemAssociationContext {
    return ItemAssociationContext(this.item, this.isItemSelected)
  }
}
