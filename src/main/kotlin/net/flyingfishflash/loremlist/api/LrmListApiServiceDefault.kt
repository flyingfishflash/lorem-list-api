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
import net.flyingfishflash.loremlist.domain.lrmitem.data.LrmItemCreate
import net.flyingfishflash.loremlist.domain.lrmlist.LrmListService
import net.flyingfishflash.loremlist.domain.lrmlist.data.LrmListCreate
import net.flyingfishflash.loremlist.domain.lrmlistitem.LrmListItemService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
@Transactional
class LrmListApiServiceDefault(private val lrmListService: LrmListService, private val lrmListItemService: LrmListItemService) : LrmListApiService {

  override fun countByOwner(owner: String): ApiServiceResponse<ApiMessageNumeric> {
    val serviceResponse = lrmListService.countByOwner(owner)
    return ApiServiceResponse(content = ApiMessageNumeric(serviceResponse.content), message = serviceResponse.message)
  }

  override fun create(lrmListCreateRequest: LrmListCreateRequest, owner: String): ApiServiceResponse<LrmListResponse> {
    // TODO: evaluate where LrmItemRequest or LrmItem should be provided to domain service
    val lrmListCreate = LrmListCreate(name = lrmListCreateRequest.name, description = lrmListCreateRequest.description, public = lrmListCreateRequest.public)
    val serviceResponse = lrmListService.create(lrmListCreate, owner)
    return ApiServiceResponse(content = LrmListResponse.fromLrmList(serviceResponse.content), message = serviceResponse.message)
  }

  override fun deleteByOwner(owner: String): ApiServiceResponse<LrmListDeletedResponse> {
    val serviceResponse = lrmListService.deleteByOwner(owner)
    return ApiServiceResponse(
      content = LrmListDeletedResponse(
        listNames = serviceResponse.content.listNames,
        associatedItemNames = serviceResponse.content.associatedItemNames,
      ),
      message = serviceResponse.message,
    )
  }

  override fun deleteByOwnerAndId(id: UUID, owner: String, removeItemAssociations: Boolean): ApiServiceResponse<LrmListDeletedResponse> {
    val serviceResponse = lrmListService.deleteByOwnerAndId(id = id, owner = owner, removeItemAssociations = removeItemAssociations)
    return ApiServiceResponse(
      content = LrmListDeletedResponse(
        listNames = serviceResponse.content.listNames,
        associatedItemNames = serviceResponse.content.associatedItemNames,
      ),
      message = serviceResponse.message,
    )
  }

  override fun findEligibleItemsByOwner(listId: UUID, owner: String): ApiServiceResponse<List<LrmItemResponse>> {
    TODO("Not yet implemented")
  }

  override fun findByOwner(owner: String): ApiServiceResponse<List<LrmListResponse>> {
    val serviceResponse = lrmListService.findByOwner(owner)
    return ApiServiceResponse(content = serviceResponse.content.map { LrmListResponse.fromLrmList(it) }, message = serviceResponse.message)
  }

  override fun findByOwnerExcludeItems(owner: String): ApiServiceResponse<List<LrmListResponse>> {
    val serviceResponse = lrmListService.findByOwner(owner)
    return ApiServiceResponse(
      content = serviceResponse.content.map { LrmListResponse.fromLrmList(it.copy(items = emptySet())) },
      message = serviceResponse.message,
    )
  }

  override fun findByOwnerAndHavingNoItemAssociations(owner: String): ApiServiceResponse<List<LrmListResponse>> {
    val serviceResponse = lrmListService.findByOwnerAndHavingNoItemAssociations(owner)
    return ApiServiceResponse(content = serviceResponse.content.map { LrmListResponse.fromLrmList(it) }, message = serviceResponse.message)
  }

  override fun findByOwnerAndId(id: UUID, owner: String): ApiServiceResponse<LrmListResponse> {
    val serviceResponse = lrmListService.findByOwnerAndId(id = id, owner = owner)
    return ApiServiceResponse(content = LrmListResponse.fromLrmList(serviceResponse.content), message = serviceResponse.message)
  }

  override fun findByOwnerAndIdExcludeItems(id: UUID, owner: String): ApiServiceResponse<LrmListResponse> {
    val serviceResponse = lrmListService.findByOwnerAndId(id = id, owner = owner)
    return ApiServiceResponse(content = LrmListResponse.fromLrmList(serviceResponse.content.copy(items = emptySet())), message = serviceResponse.message)
  }

  override fun findByPublic(): ApiServiceResponse<List<LrmListResponse>> {
    val serviceResponse = lrmListService.findByPublic()
    return ApiServiceResponse(content = serviceResponse.content.map { LrmListResponse.fromLrmList(it) }, message = serviceResponse.message)
  }

  override fun findByPublicExcludeItems(): ApiServiceResponse<List<LrmListResponse>> {
    val serviceResponse = lrmListService.findByPublic()
    return ApiServiceResponse(
      content = serviceResponse.content.map { LrmListResponse.fromLrmList(it.copy(items = emptySet())) },
      message = serviceResponse.message,
    )
  }

  override fun patchByOwnerAndId(id: UUID, owner: String, patchRequest: Map<String, Any>): ApiServiceResponse<LrmListResponse> {
    val lrmList = lrmListService.findByOwnerAndId(id = id, owner = owner).content
    val patchedFields = mutableSetOf<String>()
    patchRequest.forEach { (fieldToPatch, value) ->
      when (fieldToPatch) {
        "name" -> {
          if (value != lrmList.name) {
            lrmListService.patchName(lrmList.copy(name = value as String))
            patchedFields.add(fieldToPatch)
          }
        }
        "description" -> {
          if (value != lrmList.description) {
            lrmListService.patchDescription(lrmList.copy(description = value as String))
            patchedFields.add(fieldToPatch)
          }
        }
        "public" -> {
          if (value != lrmList.public) {
            lrmListService.patchIsPublic(lrmList.copy(public = value as Boolean))
            patchedFields.add(fieldToPatch)
          }
        }
        else -> {
          throw IllegalArgumentException("Patch operation is not supported on field: $fieldToPatch")
        }
      }
    }
    val patchedLrmList = lrmListService.findByOwnerAndId(id = id, owner = owner).content
    val message = if (patchedLrmList != lrmList) {
      "List '${patchedLrmList.name}' updated. Fields changed: ${patchedFields.joinToString() + "."}"
    } else {
      "List '${patchedLrmList.name}' not updated."
    }
    val lrmItemResponse = LrmListResponse.fromLrmList(patchedLrmList)
    return ApiServiceResponse(
      content = lrmItemResponse,
      message = message,
    )
  }

// list item context

  override fun addListItem(listId: UUID, itemIdCollection: Set<UUID>, owner: String): ApiServiceResponse<LrmListItemAddedResponse> {
    val serviceResponse = lrmListItemService.add(id = listId, idCollection = itemIdCollection.toList(), componentsOwner = owner)
    val lrmListItemAddedResponse = LrmListItemAddedResponse(
      componentName = serviceResponse.content.listName,
      associatedComponents = serviceResponse.content.items,
    )
    return ApiServiceResponse(content = lrmListItemAddedResponse, message = serviceResponse.message)
  }

  override fun countListItems(listId: UUID, listOwner: String): ApiServiceResponse<ApiMessageNumeric> {
    val serviceResponse = lrmListItemService.countByOwnerAndListId(listId = listId, owner = listOwner)
    return ApiServiceResponse(content = ApiMessageNumeric(serviceResponse.content), message = serviceResponse.message)
  }

  override fun createListItem(listId: UUID, itemCreateRequest: LrmItemCreateRequest, creator: String): ApiServiceResponse<LrmListItemResponse> {
    val lrmItemCreate =
      LrmItemCreate(
        name = itemCreateRequest.name,
        description = itemCreateRequest.description,
        quantity = itemCreateRequest.quantity,
        isSuppressed = itemCreateRequest.isSuppressed,
      )

    val serviceResponse = lrmListItemService.create(listId = listId, lrmItemCreate, creator = creator)
    return ApiServiceResponse(content = LrmListItemResponse.fromLrmListItem(serviceResponse.content), message = serviceResponse.message)
  }

  override fun findListItem(listId: UUID, itemId: UUID, listOwner: String): ApiServiceResponse<LrmListItemResponse> {
    val serviceResponse = lrmListItemService.findByOwnerAndItemIdAndListId(listId = listId, itemId = itemId, owner = listOwner)
    return ApiServiceResponse(content = LrmListItemResponse.fromLrmListItem(serviceResponse.content), message = serviceResponse.message)
  }

  override fun moveListItem(listId: UUID, itemId: UUID, destinationListId: UUID, owner: String): ApiServiceResponse<LrmListItemMovedResponse> {
    val serviceResponse = lrmListItemService.move(
      currentListId = listId,
      itemId = itemId,
      destinationListId = destinationListId,
      owner = owner,
    )

    val lrmListItemMovedResponse = LrmListItemMovedResponse(
      itemName = serviceResponse.content.first,
      currentListName = serviceResponse.content.second,
      newListName = serviceResponse.content.third,
    )

    return ApiServiceResponse(content = lrmListItemMovedResponse, message = serviceResponse.message)
  }

  override fun removeListItem(listId: UUID, itemId: UUID, componentsOwner: String): ApiServiceResponse<AssociationDeletedResponse> {
    val serviceResponse = lrmListItemService.removeByOwnerAndListIdAndItemId(itemId = itemId, listId = listId, owner = componentsOwner)
    val listItemRemovedResponse = AssociationDeletedResponse(itemName = serviceResponse.content.first, listName = serviceResponse.content.second)
    return ApiServiceResponse(content = listItemRemovedResponse, message = serviceResponse.message)
  }

  override fun removeAllListItems(listId: UUID, listOwner: String): ApiServiceResponse<AssociationsDeletedResponse> {
    val serviceResponse = lrmListItemService.removeByOwnerAndListId(listId = listId, owner = listOwner)
    val listItemsRemovedResponse = AssociationsDeletedResponse(
      itemName = serviceResponse.content.first,
      deletedAssociationsCount = serviceResponse.content.second,
    )
    return ApiServiceResponse(content = listItemsRemovedResponse, message = serviceResponse.message)
  }

  override fun patchListItem(listId: UUID, itemId: UUID, listOwner: String, patchRequest: Map<String, Any>): ApiServiceResponse<LrmListItemResponse> {
    val lrmListItem = lrmListItemService.findByOwnerAndItemIdAndListId(itemId = itemId, listId = listId, owner = listOwner).content
    val patchedFields = mutableSetOf<String>()
    patchRequest.forEach { (fieldToPatch, value) ->
      when (fieldToPatch) {
        "name" -> {
          if (value != lrmListItem.name) {
            lrmListItemService.patchName(lrmListItem.copy(name = value as String))
            patchedFields.add(fieldToPatch)
          }
        }
        "description" -> {
          if (value != lrmListItem.description) {
            lrmListItemService.patchDescription(lrmListItem.copy(description = value as String))
            patchedFields.add(fieldToPatch)
          }
        }
        "quantity" -> {
          if (value != lrmListItem.quantity) {
            lrmListItemService.patchQuantity(lrmListItem.copy(quantity = value as Int))
            patchedFields.add(fieldToPatch)
          }
        }
        "isSuppressed" -> {
          if (value != lrmListItem.isSuppressed) {
            lrmListItemService.patchIsSuppressed(lrmListItem.copy(isSuppressed = value as Boolean))
            patchedFields.add(fieldToPatch)
          }
        }
        else -> {
          throw IllegalArgumentException("Patch operation is not supported on field: $fieldToPatch")
        }
      }
    }

    val patchedLrmListItem = lrmListItemService.findByOwnerAndItemIdAndListId(itemId = itemId, listId = listId, owner = listOwner).content
    val message = if (patchedLrmListItem != lrmListItem) {
      "Item '${patchedLrmListItem.name}' updated. Fields changed: ${patchedFields.joinToString() + "."}"
    } else {
      "Item '${patchedLrmListItem.name}' not updated."
    }
    val lrmItemResponse = LrmListItemResponse.fromLrmListItem(patchedLrmListItem)
    return ApiServiceResponse(content = lrmItemResponse, message = message)
  }
}
