package net.flyingfishflash.loremlist.domain.lrmlist

import kotlinx.datetime.Instant
import net.flyingfishflash.loremlist.domain.association.LrmAssociation
import net.flyingfishflash.loremlist.domain.lrmitem.LrmItem
import java.util.*
import net.flyingfishflash.loremlist.domain.association.ItemAssociationContext

data class LrmListDomain(
  val id: UUID,
  var name: String,
  var description: String? = null,
  var public: Boolean = false,
  val created: Instant? = null,
  val createdBy: String? = null,
  var updated: Instant? = null,
  var updatedBy: String? = null,
  private var associations: MutableSet<LrmAssociation> = mutableSetOf(),
//  private var items: MutableSet<LrmItem> = mutableSetOf(),
) {

  fun getItems(): Set<ItemAssociationContext> {
    return this.associations.
  }

//  fun getItems(): Set<LrmItem> {
//    return items.toSet()
//  }

  fun addItem(lrmItem: LrmItem): Boolean {
    return items.add(lrmItem)
  }

  fun removeItem(lrmItem: LrmItem): Boolean {
    return items.remove(lrmItem)
  }

//  override fun equals(other: Any?): Boolean {
//    if (this === other) return true
//    if (javaClass != other?.javaClass) return false
//    other as LrmListDomain
//    return id == other.id
//  }
//
//  override fun hashCode(): Int {
//    return id.hashCode()
//  }
}
