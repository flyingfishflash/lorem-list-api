package net.flyingfishflash.loremlist.api

import net.flyingfishflash.loremlist.api.data.request.LrmListCreateRequest
import net.flyingfishflash.loremlist.api.data.response.ApiServiceResponse
import net.flyingfishflash.loremlist.api.data.response.AssociationCreatedResponse
import net.flyingfishflash.loremlist.api.data.response.AssociationDeletedResponse
import net.flyingfishflash.loremlist.api.data.response.AssociationsDeletedResponse
import net.flyingfishflash.loremlist.api.data.response.LrmListDeletedResponse
import net.flyingfishflash.loremlist.api.data.response.LrmListResponse
import net.flyingfishflash.loremlist.core.response.structure.ApiMessageNumeric
import java.util.*

interface LrmListApiService {
  fun countByOwner(owner: String): ApiServiceResponse<ApiMessageNumeric>
  fun create(lrmListCreateRequest: LrmListCreateRequest, owner: String): ApiServiceResponse<LrmListResponse>
  fun deleteByOwner(owner: String): ApiServiceResponse<LrmListDeletedResponse>
  fun deleteByOwnerAndId(id: UUID, owner: String, removeItemAssociations: Boolean): ApiServiceResponse<LrmListDeletedResponse>
  fun findByOwner(owner: String): ApiServiceResponse<List<LrmListResponse>>
  fun findByOwnerExcludeItems(owner: String): ApiServiceResponse<List<LrmListResponse>>
  fun findByOwnerAndHavingNoItemAssociations(owner: String): ApiServiceResponse<List<LrmListResponse>>
  fun findByOwnerAndId(id: UUID, owner: String): ApiServiceResponse<LrmListResponse>
  fun findByOwnerAndIdExcludeItems(id: UUID, owner: String): ApiServiceResponse<LrmListResponse>
  fun findByPublic(): ApiServiceResponse<List<LrmListResponse>>
  fun findByPublicExcludeItems(): ApiServiceResponse<List<LrmListResponse>>
  fun patchByOwnerAndId(id: UUID, owner: String, patchRequest: Map<String, Any>): ApiServiceResponse<LrmListResponse>
  fun countItemAssociationsByListIdAndListOwner(listId: UUID, listOwner: String): ApiServiceResponse<ApiMessageNumeric>
  fun createItemAssociations(listId: UUID, itemIdCollection: Set<UUID>, owner: String): ApiServiceResponse<AssociationCreatedResponse>
  fun deleteItemAssociationByItemIdAndListIdAndComponentsOwner(
    itemId: UUID,
    listId: UUID,
    componentsOwner: String,
  ): ApiServiceResponse<AssociationDeletedResponse>
  fun deleteItemAssociationsByListIdAndListOwner(listId: UUID, listOwner: String): ApiServiceResponse<AssociationsDeletedResponse>
}
