package net.flyingfishflash.loremlist.api

import net.flyingfishflash.loremlist.api.data.response.ApiServiceResponse
import net.flyingfishflash.loremlist.api.data.response.LrmItemDeletedResponse
import net.flyingfishflash.loremlist.api.data.response.LrmItemResponse
import net.flyingfishflash.loremlist.core.response.structure.ApiMessageNumeric
import java.util.*

interface LrmItemApiService {
  // item context scoped by owner
  fun countByOwner(owner: String): ApiServiceResponse<ApiMessageNumeric>

  fun deleteByOwner(owner: String): ApiServiceResponse<LrmItemDeletedResponse>
  fun deleteByOwnerAndId(id: UUID, owner: String, removeListAssociations: Boolean): ApiServiceResponse<LrmItemDeletedResponse>
  fun findByOwner(owner: String): ApiServiceResponse<List<LrmItemResponse>>
  fun findByOwnerAndId(id: UUID, owner: String): ApiServiceResponse<LrmItemResponse>
  fun findByOwnerAndHavingNoListAssociations(owner: String): ApiServiceResponse<List<LrmItemResponse>>
  fun patchByOwnerAndId(id: UUID, owner: String, patchRequest: Map<String, Any>): ApiServiceResponse<LrmItemResponse>

  // list item context scoped by owner
  fun countListAssociationsByItemIdAndItemOwner(itemId: UUID, itemOwner: String): ApiServiceResponse<ApiMessageNumeric>
}
