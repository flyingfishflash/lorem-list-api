package net.flyingfishflash.loremlist.domain.association

import net.flyingfishflash.loremlist.domain.lrmitem.LrmItem
import net.flyingfishflash.loremlist.domain.lrmlist.LrmListSuccinct
import net.flyingfishflash.loremlist.persistence.SuccinctLrmComponentPair
import java.util.UUID

interface AssociationRepository {
  fun listIsConsistent(listId: UUID): Boolean
  fun count(): Long
  fun countItemToListByIdAndItemOwner(itemId: UUID, itemOwner: String): Long
  fun countListToItemByIdandListOwner(listId: UUID, listOwner: String): Long
  fun create(associationCollection: Set<Pair<UUID, UUID>>): List<SuccinctLrmComponentPair>
  fun create(listId: UUID, itemId: UUID)
  fun deleteById(id: UUID): Int
  fun deleteByItemIdAndListId(itemId: UUID, listId: UUID): Int
  fun delete(): Int
  fun deleteByItemId(itemId: UUID): Int
  fun deleteByListId(listId: UUID): Int
  fun findByItemIdAndListIdOrNull(itemId: UUID, listId: UUID): Association?
  fun findListsByItem(lrmItem: LrmItem): Set<LrmListSuccinct>
  fun update(association: Association): Int
}
