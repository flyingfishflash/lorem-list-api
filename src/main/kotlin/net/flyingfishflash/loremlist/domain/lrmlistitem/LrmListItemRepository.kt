package net.flyingfishflash.loremlist.domain.lrmlistitem

import net.flyingfishflash.loremlist.persistence.SuccinctLrmComponentPair
import java.util.*

interface LrmListItemRepository {
  fun countByOwnerAndListId(listId: UUID, listOwner: String): Long
  fun countByOwnerAndItemId(itemId: UUID, itemOwner: String): Long
  fun create(listId: UUID, itemId: UUID)
  fun create(associationCollection: Set<Pair<UUID, UUID>>): List<SuccinctLrmComponentPair>
  fun findByOwnerAndItemIdAndListIdOrNull(itemId: UUID, listId: UUID, owner: String): LrmListItem?
  fun removeByOwnerAndItemId(itemId: UUID, owner: String): Int
  fun removeByOwnerAndListId(listId: UUID, owner: String): Int
  fun removeByOwnerAndListIdAndItemId(listId: UUID, itemId: UUID, owner: String): Int
  fun updateListId(lrmListItem: LrmListItem, destinationListId: UUID): Int
  fun updateName(lrmListItem: LrmListItem): Int
  fun updateDescription(lrmListItem: LrmListItem): Int
  fun updateQuantity(lrmListItem: LrmListItem): Int
  fun updateIsItemSuppressed(lrmListItem: LrmListItem): Int
}
