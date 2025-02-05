package net.flyingfishflash.loremlist.domain.association

import java.util.UUID

class LrmAssociations {
  private val associations: MutableSet<LrmAssociation> = mutableSetOf()

  // Add an association to the set
  fun addAssociation(association: LrmAssociation) {
    associations.add(association)
  }

  // Remove an association from the set
  fun removeAssociation(association: LrmAssociation) {
    associations.remove(association)
  }

  fun findItemAssociationsByList(listId: UUID): List<ItemAssociationContext> {
    return associations.filter { it.list.id == listId }.map { it.getItemAssociationContext() }
  }

  // Find all associations for a specific list
  fun findAssociationsByList(listId: UUID): List<LrmAssociation> {
    return associations.filter { it.list.id == listId }
  }

  // Find all associations for a specific item
  fun findAssociationsByItem(itemId: UUID): List<LrmAssociation> {
    return associations.filter { it.item.id == itemId }
  }

  // Get all associations in the set
  fun getAllAssociations(): Set<LrmAssociation> {
    return associations
  }
}
