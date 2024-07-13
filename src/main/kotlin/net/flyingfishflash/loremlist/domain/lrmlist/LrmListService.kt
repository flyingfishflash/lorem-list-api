package net.flyingfishflash.loremlist.domain.lrmlist

import jakarta.validation.ConstraintViolation
import jakarta.validation.ConstraintViolationException
import jakarta.validation.Validation
import net.flyingfishflash.loremlist.core.exceptions.ApiException
import net.flyingfishflash.loremlist.domain.association.AssociationService
import net.flyingfishflash.loremlist.domain.lrmlist.data.LrmListDeleteResponse
import net.flyingfishflash.loremlist.domain.lrmlist.data.LrmListRequest
import net.flyingfishflash.loremlist.toJsonElement
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional
class LrmListService(
  private val associationService: AssociationService,
  private val lrmListRepository: LrmListRepository,
) {

  fun count(): Long {
    try {
      val count = lrmListRepository.count()
      return count
    } catch (cause: Exception) {
      throw ApiException(
        cause = cause,
        message = "Total list count couldn't be generated.",
        responseMessage = "Total list count couldn't be generated.",
      )
    }
  }

  fun create(lrmListRequest: LrmListRequest): LrmList {
    try {
      val id = lrmListRepository.insert(lrmListRequest)
      return findById(id)
    } catch (cause: Exception) {
      throw ApiException(
        cause = cause,
        message = "List could not be created.",
        responseMessage = "List could not be created.",
      )
    }
  }

  fun deleteAll(): LrmListDeleteResponse {
    try {
      val lrmListDeleteResponse = LrmListDeleteResponse(
        listNames = findAll().map { it.name }.sorted(),
        associatedItemNames = findAllIncludeItems().flatMap { it.items.orEmpty() }.map { it.name }.sorted(),
      )
      if (lrmListDeleteResponse.listNames.isNotEmpty()) {
        associationService.deleteAll()
        lrmListRepository.deleteAll()
      }
      return lrmListDeleteResponse
    } catch (apiException: ApiException) {
      val message = "No lists were deleted: ${apiException.responseMessage}"
      throw ApiException(
        cause = apiException,
        httpStatus = apiException.httpStatus,
        responseMessage = message,
        message = message,
        supplemental = apiException.supplemental,
      )
    } catch (exception: Exception) {
      val message = "No lists were deleted."
      throw ApiException(
        cause = exception,
        responseMessage = message,
        message = message,
      )
    }
  }

  fun deleteById(uuid: UUID, removeItemAssociations: Boolean): LrmListDeleteResponse {
    try {
      val listName = findById(uuid).name
      val lrmListDeleteResponse = LrmListDeleteResponse(
        listNames = listOf(listName),
        associatedItemNames = (findByIdIncludeItems(uuid).items.orEmpty().map { it.name }).sorted(),
      )

      if (lrmListDeleteResponse.associatedItemNames.isNotEmpty()) {
        if (removeItemAssociations) {
          associationService.deleteAllOfList(uuid)
          val deletedCount = lrmListRepository.deleteById(uuid)
          if (deletedCount > 1) {
            throw ApiException(
              message = "More than one list with id $uuid were found.",
              responseMessage = "More than one list with id $uuid were found.",
            )
          }
        } else {
          // throw an exception rather than removing the item from all lists and then deleting it
          val message = "List $uuid is associated with ${lrmListDeleteResponse.associatedItemNames.size} item(s). " +
            "First remove each item from the list."
          throw ApiException(
            httpStatus = HttpStatus.UNPROCESSABLE_ENTITY,
            supplemental = mapOf(
              "listNames" to lrmListDeleteResponse.listNames.toJsonElement(),
              "associatedItemNames" to lrmListDeleteResponse.associatedItemNames.toJsonElement(),
            ),
            message = message,
            responseMessage = message,
          )
        }
      } else {
        // list is not associated with any items
        val deletedCount = lrmListRepository.deleteById(uuid)
        if (deletedCount > 1) {
          throw ApiException(
            message = "More than one list with id $uuid were found.",
            responseMessage = "More than one list with id $uuid were found.",
          )
        }
      }

      return lrmListDeleteResponse
    } catch (apiException: ApiException) {
      val message = "List id $uuid could not be deleted: ${apiException.responseMessage}"
      throw ApiException(
        cause = apiException,
        httpStatus = apiException.httpStatus,
        responseMessage = message,
        message = message,
        supplemental = apiException.supplemental,
      )
    } catch (exception: Exception) {
      val message = "List id $uuid could not be deleted."
      throw ApiException(
        cause = exception,
        responseMessage = message,
        message = message,
      )
    }
  }

  @Suppress("kotlin:S3776")
  fun patch(uuid: UUID, patchRequest: Map<String, Any>): Pair<LrmList, Boolean> {
    var patched = false
    var lrmList = findById(uuid)
    var newName = lrmList.name
    var newDescription = lrmList.description

    if (patchRequest.isNotEmpty()) {
      for ((change, value) in patchRequest.entries) {
        when (change) {
          "name" -> {
            if (value != lrmList.name) {
              newName = value as String
              patched = true
            }
          }
          "description" -> {
            if (value != lrmList.description) {
              newDescription = value as String
              patched = true
            }
          }
          else -> throw IllegalArgumentException("Unexpected value: $change")
        }
      }
    }

    if (patched) {
      val lrmListRequest = LrmListRequest(name = newName, description = newDescription)
      val violations: Set<ConstraintViolation<LrmListRequest>> =
        Validation.buildDefaultValidatorFactory().validator.validate(lrmListRequest)
      if (violations.isNotEmpty()) {
        throw ConstraintViolationException(violations)
      }

      val updatedCount = try {
        lrmListRepository.update(LrmListConverter.toLrmList(lrmListRequest, lrmList))
      } catch (exception: Exception) {
        throw ApiException(
          cause = exception,
          message = "List id ${lrmList.uuid} could not be updated. The list was found and patch request is valid" +
            " but an exception was thrown by the list repository.",
          responseMessage = "List id ${lrmList.uuid} could not be updated.",
        )
      }

      if (updatedCount != 1) {
        throw ApiException(
          message = "List id ${lrmList.uuid} could not be updated. $updatedCount records would have been updated rather than 1.",
          responseMessage = "List id ${lrmList.uuid} could not be updated. $updatedCount records would have been updated rather than 1.",
        )
      } else {
        lrmList = findById(uuid)
      }
    }
    return Pair(lrmList, patched)
  }

  fun findAll(): List<LrmList> {
    try {
      return lrmListRepository.findAll()
    } catch (cause: Exception) {
      throw ApiException(
        cause = cause,
        message = "Lists could not be retrieved.",
        responseMessage = "Lists could not be retrieved.",
      )
    }
  }

  fun findAllIncludeItems(): List<LrmList> {
    try {
      return lrmListRepository.findAllIncludeItems()
    } catch (cause: Exception) {
      throw ApiException(
        cause = cause,
        message = "Lists (including associated items) could not be retrieved.",
        responseMessage = "Lists (including associated items) could not be retrieved.",
      )
    }
  }

  fun findById(uuid: UUID): LrmList {
    val list = try {
      lrmListRepository.findByIdOrNull(uuid)
    } catch (cause: Exception) {
      throw ApiException(
        cause = cause,
        message = "List id $uuid could not be retrieved.",
        responseMessage = "List id $uuid could not be retrieved.",
      )
    }
    return list ?: throw ListNotFoundException(uuid)
  }

  fun findByIdIncludeItems(uuid: UUID): LrmList {
    val list = try {
      lrmListRepository.findByIdOrNullIncludeItems(uuid)
    } catch (cause: Exception) {
      throw ApiException(
        cause = cause,
        message = "List id $uuid (including associated items) could not be retrieved.",
        responseMessage = "List id $uuid (including associated items) could not be retrieved.",
      )
    }
    return list ?: throw ListNotFoundException(uuid)
  }
}
