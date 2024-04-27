package net.flyingfishflash.loremlist.domain.lrmlist

import jakarta.validation.ConstraintViolation
import jakarta.validation.ConstraintViolationException
import jakarta.validation.Validation
import net.flyingfishflash.loremlist.core.exceptions.ApiException
import net.flyingfishflash.loremlist.domain.lrmlist.data.LrmList
import net.flyingfishflash.loremlist.domain.lrmlist.data.LrmListConverter
import net.flyingfishflash.loremlist.domain.lrmlist.data.LrmListRepository
import net.flyingfishflash.loremlist.domain.lrmlist.data.dto.LrmListRequest
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class LrmListService(val lrmListRepository: LrmListRepository) {

  fun create(lrmListRequest: LrmListRequest): LrmList {
    try {
      val id = lrmListRepository.insert(lrmListRequest)
      return findById(id)
    } catch (cause: Exception) {
      throw ApiException(
        httpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
        cause = cause,
        message = "List could not be created.",
        responseMessage = "List could not be created.",
      )
    }
  }

  fun deleteSingleById(id: Long) {
    val deletedCount: Int
    try {
      deletedCount = lrmListRepository.deleteById(id)
    } catch (cause: Exception) {
      throw ApiException(
        httpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
        cause = cause,
        message = "List $id could not be deleted.",
        responseMessage = "List $id could not be deleted.",
      )
    }
    if (deletedCount < 1) {
      throw ListNotFoundException(id)
    } else if (deletedCount > 1) {
      // TODO: Ensure the transaction is rolled back if an exception is thrown
      throw ApiException(
        httpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
        message = "List id $id could not be deleted. $deletedCount records would have been updated rather than 1.",
        responseMessage = "List id $id could not be deleted. $deletedCount records would have been updated rather than 1.",
      )
    }
  }

  @Suppress("kotlin:S3776")
  fun patch(id: Long, patchRequest: Map<String, Any>): Pair<LrmList, Boolean> {
    var patched = false
    val lrmList = lrmListRepository.findByIdOrNull(id)
    if (lrmList == null) {
      throw ListNotFoundException(id)
    } else {
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

        val updatedCount: Int

        try {
          updatedCount = lrmListRepository.update(LrmListConverter.toLrmList(lrmListRequest, lrmList))
        } catch (exception: Exception) {
          throw ApiException(
            httpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
            cause = exception,
            message = "List id ${lrmList.id} could not be updated. The list was found and patch request is valid" +
              " but an exception was thrown by the list repository.",
            responseMessage = "List id ${lrmList.id} could not be updated.",
          )
        }

        if (updatedCount != 1) {
          throw ApiException(
            httpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
            message = "List id ${lrmList.id} could not be updated. $updatedCount records would have been updated rather than 1.",
            responseMessage = "List id ${lrmList.id} could not be updated. $updatedCount records would have been updated rather than 1.",
          )
        }
      }
    }
    return Pair(lrmList, patched)
  }

  fun findAll(): List<LrmList> = lrmListRepository.findAll()

  fun findAllIncludeItems(): List<LrmList> = lrmListRepository.findAllIncludeItems()

  fun findById(id: Long): LrmList = lrmListRepository.findByIdOrNull(id) ?: throw ListNotFoundException(id)

  fun findByIdIncludeItems(id: Long): LrmList {
    return lrmListRepository.findByIdOrNullIncludeItems(id) ?: throw ListNotFoundException(id)
  }
}
