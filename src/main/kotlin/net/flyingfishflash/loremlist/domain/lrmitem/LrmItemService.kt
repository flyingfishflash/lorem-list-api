package net.flyingfishflash.loremlist.domain.lrmitem

import jakarta.validation.ConstraintViolation
import jakarta.validation.ConstraintViolationException
import jakarta.validation.Validation
import kotlinx.datetime.Clock.System.now
import net.flyingfishflash.loremlist.core.exceptions.ApiException
import net.flyingfishflash.loremlist.domain.association.AssociationService
import net.flyingfishflash.loremlist.domain.lrmitem.data.LrmItemDeleteResponse
import net.flyingfishflash.loremlist.domain.lrmitem.data.LrmItemRequest
import net.flyingfishflash.loremlist.toJsonElement
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional
class LrmItemService(private val associationService: AssociationService, private val lrmItemRepository: LrmItemRepository) {

  fun countByOwner(owner: String): Long {
    try {
      val count = lrmItemRepository.countByOwner(owner = owner)
      return count
    } catch (cause: Exception) {
      throw ApiException(
        cause = cause,
        message = "Total item count couldn't be generated.",
      )
    }
  }

  fun create(lrmItemRequest: LrmItemRequest, owner: String): LrmItem {
    try {
      val now = now()
      val lrmItem =
        LrmItem(
          id = UUID.randomUUID(),
          name = lrmItemRequest.name,
          description = lrmItemRequest.description,
          quantity = lrmItemRequest.quantity,
          created = now,
          createdBy = owner,
          updated = now,
          updatedBy = owner,
        )
      val id = lrmItemRepository.insert(lrmItem)
      return findByOwnerAndId(id = id, owner = owner)
    } catch (cause: Exception) {
      throw ApiException(
        cause = cause,
        message = "Item could not be created.",
      )
    }
  }

  fun deleteByOwner(owner: String): LrmItemDeleteResponse {
    try {
      val items = findByOwner(owner = owner)
      if (items.isNotEmpty()) {
        items.filter { it.lists.orEmpty().isNotEmpty() }.forEach {
          associationService.deleteByItemOwnerAndItemId(itemId = it.id, itemOwner = owner)
        }
        lrmItemRepository.deleteById(ids = items.map { it.id }.toSet())
      }
      val lrmItemDeleteResponse = LrmItemDeleteResponse(
        itemNames = items.map { it.name }.sorted(),
        associatedListNames = items.flatMap { it.lists.orEmpty() }.map { it.name }.sorted(),
      )
      return lrmItemDeleteResponse
    } catch (apiException: ApiException) {
      val message = "No items were deleted: ${apiException.responseMessage}"
      throw ApiException(
        cause = apiException,
        httpStatus = apiException.httpStatus,
        message = message,
        supplemental = apiException.supplemental,
      )
    } catch (exception: Exception) {
      val message = "No items were deleted."
      throw ApiException(
        cause = exception,
        message = message,
      )
    }
  }

  fun deleteByOwnerAndId(
    id: UUID,
    owner: String,
    removeListAssociations: Boolean,
  ): LrmItemDeleteResponse {
    try {
      val item = findByOwnerAndId(id = id, owner = owner)
      val lrmItemDeleteResponse = LrmItemDeleteResponse(
        itemNames = listOf(item.name),
        associatedListNames = item.lists.orEmpty().map { it.name }.sorted(),
      )
      if (lrmItemDeleteResponse.associatedListNames.isNotEmpty()) {
        if (removeListAssociations) {
          associationService.deleteByItemOwnerAndItemId(itemId = id, itemOwner = owner)
          val deletedCount = lrmItemRepository.deleteByOwnerAndId(id = id, owner = owner)
          if (deletedCount > 1) {
            throw ApiException(
              message = "More than one item with id $id were found.",
            )
          }
        } else {
          // throw an exception rather than removing the item from all lists and then deleting it
          val message = "Item $id is associated with ${lrmItemDeleteResponse.associatedListNames.size} list(s). " +
            "First remove the item from each list."
          throw ApiException(
            httpStatus = HttpStatus.UNPROCESSABLE_ENTITY,
            supplemental = mapOf(
              "itemNames" to lrmItemDeleteResponse.itemNames.toJsonElement(),
              "associatedListNames" to lrmItemDeleteResponse.associatedListNames.toJsonElement(),
            ),
            message = message,
          )
        }
      } else {
        // item is not associated with any lists
        val deletedCount = lrmItemRepository.deleteByOwnerAndId(id = id, owner = owner)
        if (deletedCount > 1) {
          throw ApiException(
            message = "More than one item with id $id were found.",
          )
        }
      }
      return lrmItemDeleteResponse
    } catch (apiException: ApiException) {
      val message = "Item id $id could not be deleted: ${apiException.responseMessage}"
      throw ApiException(
        cause = apiException,
        httpStatus = apiException.httpStatus,
        message = message,
        supplemental = apiException.supplemental,
      )
    } catch (exception: Exception) {
      val message = "Item id $id could not be deleted."
      throw ApiException(
        cause = exception,
        message = message,
      )
    }
  }

  fun findByOwner(owner: String): List<LrmItem> {
    try {
      return lrmItemRepository.findByOwner(owner = owner)
    } catch (cause: Exception) {
      throw ApiException(
        cause = cause,
        message = "Items (including associated lists) could not be retrieved.",
      )
    }
  }

  fun findByOwnerAndId(id: UUID, owner: String): LrmItem {
    val item = try {
      lrmItemRepository.findByOwnerAndIdOrNull(id = id, owner = owner)
    } catch (cause: Exception) {
      throw ApiException(
        cause = cause,
        message = "Item id $id (including associated lists) could not be retrieved.",
      )
    }
    return item ?: throw ItemNotFoundException(id = id)
  }

  fun findByOwnerAndHavingNoListAssociations(owner: String): List<LrmItem> {
    val exceptionMessage = "Items without list associations could not be retrieved."
    val lrmItems = try {
      lrmItemRepository.findByOwnerAndHavingNoListAssociations(owner = owner)
    } catch (cause: Exception) {
      throw ApiException(
        cause = cause,
        message = exceptionMessage,
      )
    }
    return lrmItems
  }

  @Suppress("kotlin:S3776")
  fun patchByOwnerAndId(
    id: UUID,
    owner: String,
    patchRequest: Map<String, Any>,
  ): Pair<LrmItem, Boolean> {
    var patched = false
    var lrmItem = findByOwnerAndId(id = id, owner = owner)
    var newName = lrmItem.name
    var newDescription = lrmItem.description
    var newQuantity = lrmItem.quantity

    if (patchRequest.isNotEmpty()) {
      for ((change, value) in patchRequest.entries) {
        when (change) {
          "name" -> {
            if (value != lrmItem.name) {
              newName = value as String
              patched = true
            }
          }
          "description" -> {
            if (value != lrmItem.description) {
              newDescription = value as String
              patched = true
            }
          }
          "quantity" -> {
            if (value != lrmItem.quantity) {
              newQuantity = value as Int
              patched = true
            }
          }
          else -> throw IllegalArgumentException("Unexpected value: $change")
        }
      }
    }

    if (patched) {
      val lrmItemRequest = LrmItemRequest(name = newName, description = newDescription, quantity = newQuantity)
      val violations: Set<ConstraintViolation<LrmItemRequest>> =
        Validation.buildDefaultValidatorFactory().validator.validate(lrmItemRequest)
      if (violations.isNotEmpty()) {
        throw ConstraintViolationException(violations)
      }

      val updatedCount = try {
        lrmItemRepository.update(lrmItem = LrmItemConverter.toLrmItem(lrmItemRequest, lrmItem))
      } catch (exception: Exception) {
        throw ApiException(
          cause = exception,
          message = "Item id ${lrmItem.id} could not be updated. The item was found and patch request is valid" +
            " but an exception was thrown by the item repository.",
          responseMessage = "Item id ${lrmItem.id} could not be updated.",
        )
      }

      if (updatedCount != 1) {
        throw ApiException(
          message = "Item id ${lrmItem.id} could not be updated. $updatedCount records would have been updated rather than 1.",
        )
      } else {
        lrmItem = findByOwnerAndId(id = id, owner = owner)
      }
    }
    return Pair(lrmItem, patched)
  }

  // TODO: move to rest API service layer
  // TODO: strip lists from lrmItem
  fun findByOwnerExcludeLists(owner: String): List<LrmItem> {
    try {
      return lrmItemRepository.findByOwner(owner = owner)
    } catch (cause: Exception) {
      throw ApiException(
        cause = cause,
        message = "Items could not be retrieved.",
      )
    }
  }

  // TODO: move to rest API service layer
  // TODO: strip lists from lrmItem
  fun findByOwnerAndIdExcludeLists(id: UUID, owner: String): LrmItem {
    val item = try {
      lrmItemRepository.findByOwnerAndIdOrNull(id = id, owner = owner)
    } catch (cause: Exception) {
      throw ApiException(
        cause = cause,
        message = "Item id $id could not be retrieved.",
      )
    }
    return item ?: throw ItemNotFoundException(id = id)
  }
}
