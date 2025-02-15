package net.flyingfishflash.loremlist.api

import net.flyingfishflash.loremlist.api.data.request.LrmItemCreateRequest
import net.flyingfishflash.loremlist.api.data.response.ApiServiceResponse
import net.flyingfishflash.loremlist.api.data.response.AssociationCreatedResponse
import net.flyingfishflash.loremlist.api.data.response.AssociationDeletedResponse
import net.flyingfishflash.loremlist.api.data.response.AssociationsDeletedResponse
import net.flyingfishflash.loremlist.api.data.response.ListAssociationUpdatedResponse
import net.flyingfishflash.loremlist.api.data.response.LrmItemDeletedResponse
import net.flyingfishflash.loremlist.api.data.response.LrmItemResponse
import net.flyingfishflash.loremlist.core.response.structure.ApiMessageNumeric
import java.util.*

interface LrmItemApiService {
  fun countByOwner(owner: String): ApiServiceResponse<ApiMessageNumeric>
  fun create(lrmItemCreateRequest: LrmItemCreateRequest, owner: String): ApiServiceResponse<LrmItemResponse>
  fun deleteByOwner(owner: String): ApiServiceResponse<LrmItemDeletedResponse>
  fun deleteByOwnerAndId(id: UUID, owner: String, removeListAssociations: Boolean): ApiServiceResponse<LrmItemDeletedResponse>
  fun findByOwner(owner: String): ApiServiceResponse<List<LrmItemResponse>>
  fun findByOwnerAndId(id: UUID, owner: String): ApiServiceResponse<LrmItemResponse>
  fun findByOwnerAndHavingNoListAssociations(owner: String): ApiServiceResponse<List<LrmItemResponse>>
  fun patchByOwnerAndId(id: UUID, owner: String, patchRequest: Map<String, Any>): ApiServiceResponse<LrmItemResponse>
  fun countListAssociationsByItemIdAndItemOwner(itemId: UUID, itemOwner: String): ApiServiceResponse<ApiMessageNumeric>
  fun createListAssociations(itemId: UUID, listIdCollection: Set<UUID>, owner: String): ApiServiceResponse<AssociationCreatedResponse>
  fun deleteListAssociationByItemIdAndListIdAndItemOwner(
    itemId: UUID,
    listId: UUID,
    itemOwner: String,
  ): ApiServiceResponse<AssociationDeletedResponse>
  fun deleteListAssociationsByItemIdAndItemOwner(itemId: UUID, itemOwner: String): ApiServiceResponse<AssociationsDeletedResponse>
  fun updateListAssociation(
    itemId: UUID,
    currentListId: UUID,
    destinationListId: UUID,
    owner: String,
  ): ApiServiceResponse<ListAssociationUpdatedResponse>
}
