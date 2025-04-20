package net.flyingfishflash.loremlist.api

import net.flyingfishflash.loremlist.api.data.request.LrmItemCreateRequest
import net.flyingfishflash.loremlist.api.data.request.LrmListCreateRequest
import net.flyingfishflash.loremlist.api.data.response.ApiServiceResponse
import net.flyingfishflash.loremlist.api.data.response.AssociationDeletedResponse
import net.flyingfishflash.loremlist.api.data.response.AssociationsDeletedResponse
import net.flyingfishflash.loremlist.api.data.response.LrmItemResponse
import net.flyingfishflash.loremlist.api.data.response.LrmListDeletedResponse
import net.flyingfishflash.loremlist.api.data.response.LrmListItemAddedResponse
import net.flyingfishflash.loremlist.api.data.response.LrmListItemMovedResponse
import net.flyingfishflash.loremlist.api.data.response.LrmListItemResponse
import net.flyingfishflash.loremlist.api.data.response.LrmListResponse
import net.flyingfishflash.loremlist.core.response.structure.ApiMessageNumeric
import java.util.*

interface LrmListApiService {
  // list context scoped by owner
  fun countByOwner(owner: String): ApiServiceResponse<ApiMessageNumeric>
  fun create(lrmListCreateRequest: LrmListCreateRequest, owner: String): ApiServiceResponse<LrmListResponse>
  fun deleteByOwner(owner: String): ApiServiceResponse<LrmListDeletedResponse>
  fun deleteByOwnerAndId(id: UUID, owner: String, removeItemAssociations: Boolean): ApiServiceResponse<LrmListDeletedResponse>
  fun findEligibleItemsByOwner(listId: UUID, owner: String): ApiServiceResponse<List<LrmItemResponse>>
  fun findByOwner(owner: String): ApiServiceResponse<List<LrmListResponse>>
  fun findByOwnerExcludeItems(owner: String): ApiServiceResponse<List<LrmListResponse>>
  fun findByOwnerAndHavingNoItemAssociations(owner: String): ApiServiceResponse<List<LrmListResponse>>
  fun findByOwnerAndId(id: UUID, owner: String): ApiServiceResponse<LrmListResponse>
  fun findByOwnerAndIdExcludeItems(id: UUID, owner: String): ApiServiceResponse<LrmListResponse>
  fun findByPublic(): ApiServiceResponse<List<LrmListResponse>>
  fun findByPublicExcludeItems(): ApiServiceResponse<List<LrmListResponse>>
  fun patchByOwnerAndId(id: UUID, owner: String, patchRequest: Map<String, Any>): ApiServiceResponse<LrmListResponse>

  // list item context scoped by owner
  fun addListItem(listId: UUID, itemIdCollection: Set<UUID>, owner: String): ApiServiceResponse<LrmListItemAddedResponse>
  fun countListItems(listId: UUID, listOwner: String): ApiServiceResponse<ApiMessageNumeric>
  fun createListItem(listId: UUID, itemCreateRequest: LrmItemCreateRequest, creator: String): ApiServiceResponse<LrmListItemResponse>
  fun findListItem(listId: UUID, itemId: UUID, listOwner: String): ApiServiceResponse<LrmListItemResponse>
  fun moveListItem(listId: UUID, itemId: UUID, destinationListId: UUID, owner: String): ApiServiceResponse<LrmListItemMovedResponse>
  fun removeListItem(listId: UUID, itemId: UUID, componentsOwner: String): ApiServiceResponse<AssociationDeletedResponse>
  fun removeAllListItems(listId: UUID, listOwner: String): ApiServiceResponse<AssociationsDeletedResponse>
  fun patchListItem(listId: UUID, itemId: UUID, listOwner: String, patchRequest: Map<String, Any>): ApiServiceResponse<LrmListItemResponse>
}
