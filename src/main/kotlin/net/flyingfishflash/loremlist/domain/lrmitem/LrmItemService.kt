package net.flyingfishflash.loremlist.domain.lrmitem

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.validation.ConstraintViolation
import jakarta.validation.ConstraintViolationException
import jakarta.validation.Validation
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
class LrmItemService(
  private val associationService: AssociationService,
  private val lrmItemRepository: LrmItemRepository,
) {
  private val logger = KotlinLogging.logger {}

  fun count(): Long {
    try {
      val count = lrmItemRepository.count()
      return count
    } catch (cause: Exception) {
      throw ApiException(
        cause = cause,
        message = "Total item count couldn't be generated.",
        responseMessage = "Total item count couldn't be generated.",
      )
    }
  }

  fun create(lrmItemRequest: LrmItemRequest): LrmItem {
    try {
      val id = lrmItemRepository.insert(lrmItemRequest)
      return findById(id)
    } catch (cause: Exception) {
      throw ApiException(
        cause = cause,
        message = "Item could not be inserted.",
        responseMessage = "Item could not be inserted.",
      )
    }
  }

  fun deleteSingleById(uuid: UUID, removeListAssociations: Boolean): LrmItemDeleteResponse {
    try {
      findById(uuid)
      val lrmItemDeleteResponse = LrmItemDeleteResponse(
        associatedListCount = associationService.countForItemId(uuid),
        associatedListNames = (findByIdIncludeLists(uuid).lists?.map { it.name })?.sorted() ?: emptyList(),
      )
      if (lrmItemDeleteResponse.associatedListCount > 0) {
        if (removeListAssociations) {
          associationService.deleteAllOfItem(uuid)
          val deletedCount = lrmItemRepository.deleteById(uuid)
          if (deletedCount > 1) {
            throw ApiException(
              message = "More than one item with id $uuid were found.",
              responseMessage = "More than one item with id $uuid were found.",
            )
          }
        } else {
          // throw an exception rather than removing the item from all lists and then deleting it
          val message = "Item $uuid is associated with ${lrmItemDeleteResponse.associatedListCount} list(s). " +
            "First remove the item from each list."
          throw ApiException(
            httpStatus = HttpStatus.UNPROCESSABLE_ENTITY,
            supplemental = mapOf(
              "listAssociations" to lrmItemDeleteResponse.associatedListCount.toJsonElement(),
              "associatedListNames" to lrmItemDeleteResponse.associatedListNames.toJsonElement(),
            ),
            message = message,
            responseMessage = message,
          )
        }
      } else {
        // item is not associated with any lists
        val deletedCount = lrmItemRepository.deleteById(uuid)
        if (deletedCount > 1) {
          throw ApiException(
            message = "More than one item with id $uuid were found.",
            responseMessage = "More than one item with id $uuid were found.",
          )
        }
      }
      return lrmItemDeleteResponse
    } catch (apiException: ApiException) {
      val message = "Item id $uuid could not be deleted: ${apiException.responseMessage}"
      throw ApiException(
        cause = apiException,
        httpStatus = apiException.httpStatus,
        responseMessage = message,
        message = message,
        supplemental = apiException.supplemental,
      )
    } catch (exception: Exception) {
      val message = "Item id $uuid could not be deleted."
      throw ApiException(
        cause = exception,
        responseMessage = message,
        message = message,
      )
    }
  }

  fun findAll(): List<LrmItem> {
    try {
      return lrmItemRepository.findAll()
    } catch (cause: Exception) {
      throw ApiException(
        cause = cause,
        message = "Items could not be retrieved.",
        responseMessage = "Items could not be retrieved.",
      )
    }
  }

  fun findAllIncludeLists(): List<LrmItem> {
    try {
      return lrmItemRepository.findAllIncludeLists()
    } catch (cause: Exception) {
      throw ApiException(
        cause = cause,
        message = "Items (including associated lists) could not be retrieved.",
        responseMessage = "Items (including associated lists) could not be retrieved.",
      )
    }
  }

  fun findById(uuid: UUID): LrmItem {
    val item = try {
      lrmItemRepository.findByIdOrNull(uuid)
    } catch (cause: Exception) {
      logger.error { cause }
      throw ApiException(
        cause = cause,
        message = "Item id $uuid could not be retrieved.",
        responseMessage = "Item id $uuid could not be retrieved.",
      )
    }
    return item ?: throw ItemNotFoundException(uuid)
  }

  fun findByIdIncludeLists(uuid: UUID): LrmItem {
    val item = try {
      lrmItemRepository.findByIdOrNullIncludeLists(uuid)
    } catch (cause: Exception) {
      throw ApiException(
        cause = cause,
        message = "Item id $uuid (including associated lists) could not be retrieved.",
        responseMessage = "Item id $uuid (including associated lists) could not be retrieved.",
      )
    }
    return item ?: throw ItemNotFoundException(uuid)
  }

  @Suppress("kotlin:S3776")
  fun patch(uuid: UUID, patchRequest: Map<String, Any>): Pair<LrmItem, Boolean> {
    var patched = false
    var lrmItem = findById(uuid)
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
        lrmItemRepository.update(LrmItemConverter.toLrmItem(lrmItemRequest, lrmItem))
      } catch (exception: Exception) {
        throw ApiException(
          cause = exception,
          message = "Item id ${lrmItem.uuid} could not be updated. The item was found and patch request is valid" +
            " but an exception was thrown by the item repository.",
          responseMessage = "Item id ${lrmItem.uuid} could not be updated.",
        )
      }

      if (updatedCount != 1) {
        throw ApiException(
          message = "Item id ${lrmItem.uuid} could not be updated. $updatedCount records would have been updated rather than 1.",
          responseMessage = "Item id ${lrmItem.uuid} could not be updated. $updatedCount records would have been updated rather than 1.",
        )
      } else {
        lrmItem = findById(uuid)
      }
    }
    return Pair(lrmItem, patched)
  }
}
