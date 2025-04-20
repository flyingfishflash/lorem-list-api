package net.flyingfishflash.loremlist.domain.lrmlistitem

import net.flyingfishflash.loremlist.domain.ServiceResponse
import net.flyingfishflash.loremlist.domain.lrmitem.data.LrmItemCreate
import net.flyingfishflash.loremlist.domain.lrmlistitem.data.LrmListItemAdded
import java.util.UUID

interface LrmListItemService {
  fun countByOwnerAndListId(listId: UUID, owner: String): ServiceResponse<Long>
  fun add(id: UUID, idCollection: List<UUID>, componentsOwner: String): ServiceResponse<LrmListItemAdded>
  fun create(listId: UUID, lrmItemCreate: LrmItemCreate, creator: String): ServiceResponse<LrmListItem>
  fun findByOwnerAndItemIdAndListId(itemId: UUID, listId: UUID, owner: String): ServiceResponse<LrmListItem>
  fun move(itemId: UUID, currentListId: UUID, destinationListId: UUID, owner: String): ServiceResponse<Triple<String, String, String>>
  fun patchQuantity(patchedLrmListItem: LrmListItem)
  fun patchIsSuppressed(patchedLrmListItem: LrmListItem)
  fun removeByOwnerAndListIdAndItemId(listId: UUID, itemId: UUID, owner: String): ServiceResponse<Pair<String, String>>
  fun removeByOwnerAndItemId(itemId: UUID, owner: String): ServiceResponse<Pair<String, Int>>
  fun removeByOwnerAndListId(listId: UUID, owner: String): ServiceResponse<Pair<String, Int>>

  // item context
  fun countByOwnerAndItemId(itemId: UUID, owner: String): ServiceResponse<Long>
  fun patchName(patchedLrmListItem: LrmListItem)
  fun patchDescription(patchedLrmListItem: LrmListItem)
}
