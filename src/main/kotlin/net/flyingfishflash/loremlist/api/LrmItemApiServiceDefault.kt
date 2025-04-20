package net.flyingfishflash.loremlist.api

import net.flyingfishflash.loremlist.api.data.response.ApiServiceResponse
import net.flyingfishflash.loremlist.api.data.response.LrmItemDeletedResponse
import net.flyingfishflash.loremlist.api.data.response.LrmItemResponse
import net.flyingfishflash.loremlist.core.response.structure.ApiMessageNumeric
import net.flyingfishflash.loremlist.domain.lrmitem.LrmItemService
import net.flyingfishflash.loremlist.domain.lrmlistitem.LrmListItemService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
@Transactional
class LrmItemApiServiceDefault(private val lrmItemService: LrmItemService, private val lrmListItemService: LrmListItemService) : LrmItemApiService {

  override fun countByOwner(owner: String): ApiServiceResponse<ApiMessageNumeric> {
    val serviceResponse = lrmItemService.countByOwner(owner)
    return ApiServiceResponse(
      content = ApiMessageNumeric(serviceResponse.content),
      message = serviceResponse.message,
    )
  }

  override fun deleteByOwner(owner: String): ApiServiceResponse<LrmItemDeletedResponse> {
    val serviceResponse = lrmItemService.deleteByOwner(owner)
    return ApiServiceResponse(
      content = LrmItemDeletedResponse(serviceResponse.content.itemNames, serviceResponse.content.associatedListNames),
      message = serviceResponse.message,
    )
  }

  override fun deleteByOwnerAndId(id: UUID, owner: String, removeListAssociations: Boolean): ApiServiceResponse<LrmItemDeletedResponse> {
    val serviceResponse = lrmItemService.deleteByOwnerAndId(id = id, owner = owner, removeListAssociations = removeListAssociations)
    return ApiServiceResponse(
      content = LrmItemDeletedResponse(serviceResponse.content.itemNames, serviceResponse.content.associatedListNames),
      message = serviceResponse.message,
    )
  }

  override fun findByOwner(owner: String): ApiServiceResponse<List<LrmItemResponse>> {
    val serviceResponse = lrmItemService.findByOwner(owner)
    return ApiServiceResponse(
      content = serviceResponse.content.map { LrmItemResponse.fromLrmItem(it) },
      message = serviceResponse.message,
    )
  }

  override fun findByOwnerAndId(id: UUID, owner: String): ApiServiceResponse<LrmItemResponse> {
    val serviceResponse = lrmItemService.findByOwnerAndId(id = id, owner = owner)
    return ApiServiceResponse(
      content = LrmItemResponse.fromLrmItem(serviceResponse.content),
      message = serviceResponse.message,
    )
  }

  override fun findByOwnerAndHavingNoListAssociations(owner: String): ApiServiceResponse<List<LrmItemResponse>> {
    val serviceResponse = lrmItemService.findByOwnerAndHavingNoListAssociations(owner)
    return ApiServiceResponse(
      content = serviceResponse.content.map { LrmItemResponse.fromLrmItem(it) },
      message = serviceResponse.message,
    )
  }

  override fun patchByOwnerAndId(id: UUID, owner: String, patchRequest: Map<String, Any>): ApiServiceResponse<LrmItemResponse> {
    val lrmItem = lrmItemService.findByOwnerAndId(id = id, owner = owner).content
    val patchedFields = mutableSetOf<String>()
    patchRequest.forEach { (fieldToPatch, value) ->
      when (fieldToPatch) {
        "name" -> {
          if (value != lrmItem.name) {
            lrmItemService.patchName(lrmItem.copy(name = value as String))
            patchedFields.add(fieldToPatch)
          }
        }
        "description" -> {
          if (value != lrmItem.description) {
            lrmItemService.patchDescription(lrmItem.copy(description = value as String))
            patchedFields.add(fieldToPatch)
          }
        }
        else -> {
          throw IllegalArgumentException("Patch operation is not supported on field: $fieldToPatch")
        }
      }
    }

    val patchedLrmItem = lrmItemService.findByOwnerAndId(id = id, owner = owner).content
    val message = if (patchedLrmItem !=
      lrmItem
    ) {
      "Item '${patchedLrmItem.name}' updated. Fields changed: ${patchedFields.joinToString() + "."}"
    } else {
      "Item '${patchedLrmItem.name}' not updated."
    }
    val lrmItemResponse = LrmItemResponse.fromLrmItem(patchedLrmItem)
    return ApiServiceResponse(
      content = lrmItemResponse,
      message = message,
    )
  }

  override fun countListAssociationsByItemIdAndItemOwner(itemId: UUID, itemOwner: String): ApiServiceResponse<ApiMessageNumeric> {
    val serviceResponse = lrmListItemService.countByOwnerAndItemId(itemId = itemId, owner = itemOwner)
    return ApiServiceResponse(
      content = ApiMessageNumeric(serviceResponse.content),
      message = serviceResponse.message,
    )
  }
}
