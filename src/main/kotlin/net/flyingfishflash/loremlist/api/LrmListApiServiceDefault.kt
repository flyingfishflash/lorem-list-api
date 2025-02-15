package net.flyingfishflash.loremlist.api

import net.flyingfishflash.loremlist.api.data.request.LrmListCreateRequest
import net.flyingfishflash.loremlist.api.data.response.ApiServiceResponse
import net.flyingfishflash.loremlist.api.data.response.AssociationCreatedResponse
import net.flyingfishflash.loremlist.api.data.response.AssociationDeletedResponse
import net.flyingfishflash.loremlist.api.data.response.AssociationsDeletedResponse
import net.flyingfishflash.loremlist.api.data.response.LrmListDeletedResponse
import net.flyingfishflash.loremlist.api.data.response.LrmListResponse
import net.flyingfishflash.loremlist.core.response.structure.ApiMessageNumeric
import net.flyingfishflash.loremlist.domain.LrmComponentType
import net.flyingfishflash.loremlist.domain.association.AssociationService
import net.flyingfishflash.loremlist.domain.lrmlist.LrmListService
import net.flyingfishflash.loremlist.domain.lrmlist.data.LrmListCreate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
@Transactional
class LrmListApiServiceDefault(private val lrmListService: LrmListService, private val associationService: AssociationService) :
  LrmListApiService {

  override fun countByOwner(owner: String): ApiServiceResponse<ApiMessageNumeric> {
    val serviceResponse = lrmListService.countByOwner(owner)
    return ApiServiceResponse(
      content = ApiMessageNumeric(serviceResponse.content),
      message = serviceResponse.message,
    )
  }

  override fun create(lrmListCreateRequest: LrmListCreateRequest, owner: String): ApiServiceResponse<LrmListResponse> {
    // TODO: evaluate where LrmItemRequest or LrmItem should be provided to domain service
    val lrmListCreate = LrmListCreate(
      name = lrmListCreateRequest.name,
      description = lrmListCreateRequest.description,
      public = lrmListCreateRequest.public,
    )
    val serviceResponse = lrmListService.create(lrmListCreate, owner)
    return ApiServiceResponse(
      content = LrmListResponse.fromLrmList(serviceResponse.content),
      message = serviceResponse.message,
    )
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

  override fun findByOwner(owner: String): ApiServiceResponse<List<LrmListResponse>> {
    val serviceResponse = lrmListService.findByOwner(owner)
    return ApiServiceResponse(
      content = serviceResponse.content.map { LrmListResponse.fromLrmList(it) },
      message = serviceResponse.message,
    )
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
    return ApiServiceResponse(
      content = serviceResponse.content.map { LrmListResponse.fromLrmList(it) },
      message = serviceResponse.message,
    )
  }

  override fun findByOwnerAndId(id: UUID, owner: String): ApiServiceResponse<LrmListResponse> {
    val serviceResponse = lrmListService.findByOwnerAndId(id = id, owner = owner)
    return ApiServiceResponse(
      content = LrmListResponse.fromLrmList(serviceResponse.content),
      message = serviceResponse.message,
    )
  }

  override fun findByOwnerAndIdExcludeItems(id: UUID, owner: String): ApiServiceResponse<LrmListResponse> {
    val serviceResponse = lrmListService.findByOwnerAndId(id = id, owner = owner)
    return ApiServiceResponse(
      content = LrmListResponse.fromLrmList(serviceResponse.content.copy(items = emptySet())),
      message = serviceResponse.message,
    )
  }

  override fun findByPublic(): ApiServiceResponse<List<LrmListResponse>> {
    val serviceResponse = lrmListService.findByPublic()
    return ApiServiceResponse(
      content = serviceResponse.content.map { LrmListResponse.fromLrmList(it) },
      message = serviceResponse.message,
    )
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

  override fun countItemAssociationsByListIdAndListOwner(listId: UUID, listOwner: String): ApiServiceResponse<ApiMessageNumeric> {
    val serviceResponse = associationService.countByOwnerForList(listId = listId, listOwner = listOwner)
    return ApiServiceResponse(
      content = ApiMessageNumeric(serviceResponse.content),
      message = serviceResponse.message,
    )
  }

  override fun createItemAssociations(
    listId: UUID,
    itemIdCollection: Set<UUID>,
    owner: String,
  ): ApiServiceResponse<AssociationCreatedResponse> {
    val serviceResponse = associationService.create(
      id = listId,
      idCollection = itemIdCollection.toList(),
      type = LrmComponentType.List,
      componentsOwner = owner,
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

  override fun deleteItemAssociationByItemIdAndListIdAndComponentsOwner(
    itemId: UUID,
    listId: UUID,
    componentsOwner: String,
  ): ApiServiceResponse<AssociationDeletedResponse> {
    val serviceResponse = associationService.deleteByItemIdAndListId(itemId = itemId, listId = listId, componentsOwner = componentsOwner)
    val associationDeletedResponse =
      AssociationDeletedResponse(itemName = serviceResponse.content.first, listName = serviceResponse.content.second)
    return ApiServiceResponse(
      content = associationDeletedResponse,
      message = serviceResponse.message,
    )
  }

  override fun deleteItemAssociationsByListIdAndListOwner(
    listId: UUID,
    listOwner: String,
  ): ApiServiceResponse<AssociationsDeletedResponse> {
    val serviceResponse = associationService.deleteByListOwnerAndListId(listId = listId, listOwner = listOwner)
    val associationsDeletedResponse = AssociationsDeletedResponse(
      itemName = serviceResponse.content.first,
      deletedAssociationsCount = serviceResponse.content.second,
    )
    return ApiServiceResponse(
      content = associationsDeletedResponse,
      message = serviceResponse.message,
    )
  }
}
