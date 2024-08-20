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

  fun deleteById(id: UUID, removeItemAssociations: Boolean): LrmListDeleteResponse {
    try {
      val listName = findById(id).name
      val lrmListDeleteResponse = LrmListDeleteResponse(
        listNames = listOf(listName),
        associatedItemNames = (findByIdIncludeItems(id).items.orEmpty().map { it.name }).sorted(),
      )

      if (lrmListDeleteResponse.associatedItemNames.isNotEmpty()) {
        if (removeItemAssociations) {
          associationService.deleteAllOfList(id)
          val deletedCount = lrmListRepository.deleteById(id)
          if (deletedCount > 1) {
            throw ApiException(
              message = "More than one list with id $id were found.",
              responseMessage = "More than one list with id $id were found.",
            )
          }
        } else {
          // throw an exception rather than removing the item from all lists and then deleting it
          val message = "List $id is associated with ${lrmListDeleteResponse.associatedItemNames.size} item(s). " +
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
        val deletedCount = lrmListRepository.deleteById(id)
        if (deletedCount > 1) {
          throw ApiException(
            message = "More than one list with id $id were found.",
            responseMessage = "More than one list with id $id were found.",
          )
        }
      }

      return lrmListDeleteResponse
    } catch (apiException: ApiException) {
      val message = "List id $id could not be deleted: ${apiException.responseMessage}"
      throw ApiException(
        cause = apiException,
        httpStatus = apiException.httpStatus,
        responseMessage = message,
        message = message,
        supplemental = apiException.supplemental,
      )
    } catch (exception: Exception) {
      val message = "List id $id could not be deleted."
      throw ApiException(
        cause = exception,
        responseMessage = message,
        message = message,
      )
    }
  }

  @Suppress("kotlin:S3776")
  fun patch(id: UUID, patchRequest: Map<String, Any>): Pair<LrmList, Boolean> {
    var patched = false
    var lrmList = findById(id)
    var newName = lrmList.name
    var newDescription = lrmList.description
    var newIsPublic = lrmList.public

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
          "public" -> {
            if (value != lrmList.public) {
              newIsPublic = value as Boolean
              patched = true
            }
          }
          else -> throw IllegalArgumentException("Unexpected value: $change")
        }
      }
    }

    if (patched) {
      val lrmListRequest = LrmListRequest(name = newName, description = newDescription, public = newIsPublic)
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
          message = "List id ${lrmList.id} could not be updated. The list was found and patch request is valid" +
            " but an exception was thrown by the list repository.",
          responseMessage = "List id ${lrmList.id} could not be updated.",
        )
      }

      if (updatedCount != 1) {
        throw ApiException(
          message = "List id ${lrmList.id} could not be updated. $updatedCount records would have been updated rather than 1.",
          responseMessage = "List id ${lrmList.id} could not be updated. $updatedCount records would have been updated rather than 1.",
        )
      } else {
        lrmList = findById(id)
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

  fun findAllPublic(): List<LrmList> {
    try {
      return lrmListRepository.findAllPublic()
    } catch (cause: Exception) {
      throw ApiException(
        cause = cause,
        message = "Public lists could not be retrieved.",
        responseMessage = "Public lists could not be retrieved.",
      )
    }
  }

  fun findAllPublicIncludeItems(): List<LrmList> {
    try {
      return lrmListRepository.findAllPublicIncludeItems()
    } catch (cause: Exception) {
      throw ApiException(
        cause = cause,
        message = "Public lists (including associated items) could not be retrieved.",
        responseMessage = "Public lists (including associated items) could not be retrieved.",
      )
    }
  }

  fun findById(id: UUID): LrmList {
    val list = try {
      lrmListRepository.findByIdOrNull(id)
    } catch (cause: Exception) {
      throw ApiException(
        cause = cause,
        message = "List id $id could not be retrieved.",
        responseMessage = "List id $id could not be retrieved.",
      )
    }
    return list ?: throw ListNotFoundException(id)
  }

  fun findByIdIncludeItems(id: UUID): LrmList {
    val list = try {
      lrmListRepository.findByIdOrNullIncludeItems(id)
    } catch (cause: Exception) {
      throw ApiException(
        cause = cause,
        message = "List id $id (including associated items) could not be retrieved.",
        responseMessage = "List id $id (including associated items) could not be retrieved.",
      )
    }
    return list ?: throw ListNotFoundException(id)
  }

  fun findWithNoItems(): List<LrmList> {
    val exceptionMessage = "Lists without item associations could not be retrieved."
    val lrmLists = try {
      lrmListRepository.findWithNoItemAssociations()
    } catch (cause: Exception) {
      throw ApiException(
        cause = cause,
        message = exceptionMessage,
      )
    }
    return lrmLists
  }
}
