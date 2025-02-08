package net.flyingfishflash.loremlist.domain.consolidated

// import net.flyingfishflash.loremlist.domain.association.Association
import net.flyingfishflash.loremlist.domain.lrmitem.data.LrmItemSuccinct
import net.flyingfishflash.loremlist.domain.lrmlist.data.LrmListSuccinct
import java.util.*

data class SuccinctLrmComponentPair(val list: LrmListSuccinct, val item: LrmItemSuccinct)

interface LrmAssociationRepository {
  fun listIsConsistent(listId: UUID): Boolean
  fun count(): Long
  fun countItemToListByIdAndItemOwner(itemId: UUID, itemOwner: String): Long
  fun countListToItemByIdAndListOwner(listId: UUID, listOwner: String): Long
  fun create(associationCollection: Set<Pair<UUID, UUID>>): List<SuccinctLrmComponentPair>
  fun create(listId: UUID, itemId: UUID, itemIsSelectedInList: Boolean)
  fun deleteByItemIdAndListId(itemId: UUID, listId: UUID): Int
  fun delete(): Int
  fun deleteByItemId(itemId: UUID): Int
  fun deleteByListId(listId: UUID): Int

//  fun findByItemIdAndListIdOrNull(itemId: UUID, listId: UUID): Association?
//  fun update(association: Association): Int
}
