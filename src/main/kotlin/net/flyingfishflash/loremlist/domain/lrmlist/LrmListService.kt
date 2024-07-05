package net.flyingfishflash.loremlist.domain.lrmlist

import jakarta.validation.ConstraintViolation
import jakarta.validation.ConstraintViolationException
import jakarta.validation.Validation
import net.flyingfishflash.loremlist.core.exceptions.ApiException
import net.flyingfishflash.loremlist.domain.lrmlist.data.LrmListRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional
class LrmListService(val lrmListRepository: LrmListRepository) {

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

  fun deleteSingleById(uuid: UUID) {
    val deletedCount =
      try {
        lrmListRepository.deleteById(uuid)
      } catch (cause: Exception) {
        throw ApiException(
          cause = cause,
          message = "List $uuid could not be deleted.",
          responseMessage = "List $uuid could not be deleted.",
        )
      }
    if (deletedCount < 1) {
      throw ListNotFoundException(uuid)
    } else if (deletedCount > 1) {
      throw ApiException(
        message = "List id $uuid could not be deleted. $deletedCount records would have been updated rather than 1.",
        responseMessage = "List id $uuid could not be deleted. $deletedCount records would have been updated rather than 1.",
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
