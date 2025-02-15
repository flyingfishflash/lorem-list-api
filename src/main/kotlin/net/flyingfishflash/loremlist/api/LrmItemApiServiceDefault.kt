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
import net.flyingfishflash.loremlist.domain.LrmComponentType
import net.flyingfishflash.loremlist.domain.association.AssociationService
import net.flyingfishflash.loremlist.domain.lrmitem.LrmItemService
import net.flyingfishflash.loremlist.domain.lrmitem.data.LrmItemCreate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
@Transactional
class LrmItemApiServiceDefault(private val lrmItemService: LrmItemService, private val associationService: AssociationService) :
  LrmItemApiService {

  override fun countByOwner(owner: String): ApiServiceResponse<ApiMessageNumeric> {
    val serviceResponse = lrmItemService.countByOwner(owner)
    return ApiServiceResponse(
      content = ApiMessageNumeric(serviceResponse.content),
      message = serviceResponse.message,
    )
  }

  override fun create(lrmItemCreateRequest: LrmItemCreateRequest, owner: String): ApiServiceResponse<LrmItemResponse> {
    // TODO: evaluate where LrmItemRequest or LrmItem should be provided to domain service
    val lrmItemCreate =
      LrmItemCreate(
        name = lrmItemCreateRequest.name,
        description = lrmItemCreateRequest.description,
        quantity = lrmItemCreateRequest.quantity,
      )
    val serviceResponse = lrmItemService.create(lrmItemCreate, owner)
    return ApiServiceResponse(
      content = LrmItemResponse.fromLrmItem(serviceResponse.content),
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
        "quantity" -> {
          if (value != lrmItem.quantity) {
            lrmItemService.patchQuantity(lrmItem.copy(quantity = value as Int))
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
    val serviceResponse = associationService.countByIdAndItemOwnerForItem(itemId = itemId, itemOwner = itemOwner)
    return ApiServiceResponse(
      content = ApiMessageNumeric(serviceResponse.content),
      message = serviceResponse.message,
    )
  }

  override fun createListAssociations(
    itemId: UUID,
    listIdCollection: Set<UUID>,
    owner: String,
  ): ApiServiceResponse<AssociationCreatedResponse> {
    val serviceResponse = associationService.create(
      id = itemId,
      idCollection = listIdCollection.toList(),
      componentsOwner = owner,
      type = LrmComponentType.Item,
    )
    val associationCreatedResponse = AssociationCreatedResponse(
      componentName = serviceResponse.content.componentName,
      associatedComponents = serviceResponse.content.associatedComponents,
    )
    return ApiServiceResponse(
      content = associationCreatedResponse,
      message = serviceResponse.message,
    )
  }

  override fun deleteListAssociationByItemIdAndListIdAndItemOwner(
    itemId: UUID,
    listId: UUID,
    itemOwner: String,
  ): ApiServiceResponse<AssociationDeletedResponse> {
    val serviceResponse = associationService.deleteByItemIdAndListId(itemId = itemId, listId = listId, componentsOwner = itemOwner)
    return ApiServiceResponse(
      content = AssociationDeletedResponse(itemName = serviceResponse.content.first, listName = serviceResponse.content.second),
      message = serviceResponse.message,
    )
  }

  override fun deleteListAssociationsByItemIdAndItemOwner(
    itemId: UUID,
    itemOwner: String,
  ): ApiServiceResponse<AssociationsDeletedResponse> {
    val serviceResponse = associationService.deleteByItemOwnerAndItemId(itemId = itemId, itemOwner = itemOwner)
    val associationsDeletedResponse = AssociationsDeletedResponse(
      itemName = serviceResponse.content.first,
      deletedAssociationsCount = serviceResponse.content.second,
    )
    return ApiServiceResponse(
      content = associationsDeletedResponse,
      message = serviceResponse.message,
    )
  }

  override fun updateListAssociation(
    itemId: UUID,
    currentListId: UUID,
    destinationListId: UUID,
    owner: String,
  ): ApiServiceResponse<ListAssociationUpdatedResponse> {
    val serviceResponse = associationService.updateList(
      itemId = itemId,
      currentListId = currentListId,
      destinationListId = destinationListId,
      componentsOwner = owner,
    )

    val listAssociationUpdatedResponse = ListAssociationUpdatedResponse(
      itemName = serviceResponse.content.first,
      currentListName = serviceResponse.content.second,
      newListName = serviceResponse.content.third,
    )

    return ApiServiceResponse(
      content = listAssociationUpdatedResponse,
      message = serviceResponse.message,
    )
  }
}
