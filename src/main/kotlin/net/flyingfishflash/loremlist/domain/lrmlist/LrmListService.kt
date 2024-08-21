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
  fun countByOwner(owner: String): Long {
    try {
      val count = lrmListRepository.countByOwner(owner = owner)
      return count
    } catch (cause: Exception) {
      throw ApiException(
        cause = cause,
        message = "Total list count couldn't be generated.",
      )
    }
  }

  fun create(lrmListRequest: LrmListRequest, owner: String): LrmList {
    try {
      val id = lrmListRepository.insert(lrmListRequest, owner)
      return findByOwnerAndId(id = id, owner = owner)
    } catch (cause: Exception) {
      throw ApiException(
        cause = cause,
        message = "List could not be created.",
      )
    }
  }

  fun deleteByOwner(owner: String): LrmListDeleteResponse {
    try {
      val lists = findByOwnerIncludeItems(owner = owner)
      if (lists.isNotEmpty()) {
        lists.filter { it.items.orEmpty().isNotEmpty() }.forEach {
          associationService.deleteByListOwnerAndListId(listId = it.id, listOwner = owner)
        }
        lrmListRepository.deleteById(ids = lists.map { it.id }.toSet())
      }
      val lrmListDeleteResponse = LrmListDeleteResponse(
        listNames = lists.map { it.name }.sorted(),
        associatedItemNames = lists.flatMap { it.items.orEmpty() }.map { it.name }.sorted(),
      )
      return lrmListDeleteResponse
    } catch (apiException: ApiException) {
      val message = "No lists were deleted: ${apiException.responseMessage}"
      throw ApiException(
        cause = apiException,
        httpStatus = apiException.httpStatus,
        message = message,
        supplemental = apiException.supplemental,
      )
    } catch (exception: Exception) {
      val message = "No lists were deleted."
      throw ApiException(
        cause = exception,
        message = message,
      )
    }
  }

  fun deleteByOwnerAndId(id: UUID, owner: String, removeItemAssociations: Boolean): LrmListDeleteResponse {
    try {
      val list = findByOwnerAndIdIncludeItems(id = id, owner = owner)
      val lrmListDeleteResponse = LrmListDeleteResponse(
        listNames = listOf(list.name),
        associatedItemNames = list.items.orEmpty().map { it.name }.sorted(),
      )
      if (lrmListDeleteResponse.associatedItemNames.isNotEmpty()) {
        if (removeItemAssociations) {
          associationService.deleteByListOwnerAndListId(listId = id, listOwner = owner)
          val deletedCount = lrmListRepository.deleteByOwnerAndId(id = id, owner = owner)
          if (deletedCount > 1) {
            throw ApiException(
              message = "More than one list with id $id were found.",
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
          )
        }
      } else {
        // list is not associated with any items
        val deletedCount = lrmListRepository.deleteByOwnerAndId(id = id, owner = owner)
        if (deletedCount > 1) {
          throw ApiException(
            message = "More than one list with id $id were found.",
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
        message = message,
      )
    }
  }

  fun findByOwner(owner: String): List<LrmList> {
    try {
      return lrmListRepository.findByOwner(owner = owner)
    } catch (cause: Exception) {
      throw ApiException(
        cause = cause,
        message = "Lists could not be retrieved.",
      )
    }
  }

  fun findByOwnerIncludeItems(owner: String): List<LrmList> {
    try {
      return lrmListRepository.findByOwnerIncludeItems(owner = owner)
    } catch (cause: Exception) {
      throw ApiException(
        cause = cause,
        message = "Lists (including associated items) could not be retrieved.",
      )
    }
  }

  fun findByOwnerAndId(id: UUID, owner: String): LrmList {
    val list = try {
      lrmListRepository.findByOwnerAndIdOrNull(id = id, owner = owner)
    } catch (cause: Exception) {
      throw ApiException(
        cause = cause,
        message = "List id $id could not be retrieved.",
      )
    }
    return list ?: throw ListNotFoundException(id)
  }

  fun findByOwnerAndIdIncludeItems(id: UUID, owner: String): LrmList {
    val list = try {
      lrmListRepository.findByOwnerAndIdOrNullIncludeItems(id = id, owner = owner)
    } catch (cause: Exception) {
      throw ApiException(
        cause = cause,
        message = "List id $id (including associated items) could not be retrieved.",
      )
    }
    return list ?: throw ListNotFoundException(id)
  }

  fun findByOwnerAndHavingNoItemAssociations(owner: String): List<LrmList> {
    val exceptionMessage = "Lists without item associations could not be retrieved."
    val lrmLists = try {
      lrmListRepository.findByOwnerAndHavingNoItemAssociations(owner = owner)
    } catch (cause: Exception) {
      throw ApiException(
        cause = cause,
        message = exceptionMessage,
      )
    }
    return lrmLists
  }

  @Suppress("kotlin:S3776")
  fun patchByOwnerAndId(id: UUID, owner: String, patchRequest: Map<String, Any>): Pair<LrmList, Boolean> {
    var patched = false
    var lrmList = findByOwnerAndId(id = id, owner = owner)
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
        lrmList = findByOwnerAndId(id = id, owner = owner)
      }
    }
    return Pair(lrmList, patched)
  }

  fun findByPublic(): List<LrmList> {
    try {
      return lrmListRepository.findByPublic()
    } catch (cause: Exception) {
      throw ApiException(
        cause = cause,
        message = "Public lists could not be retrieved.",
      )
    }
  }

  fun findByPublicIncludeItems(): List<LrmList> {
    try {
      return lrmListRepository.findByPublicIncludeItems()
    } catch (cause: Exception) {
      throw ApiException(
        cause = cause,
        message = "Public lists (including associated items) could not be retrieved.",
      )
    }
  }
}
