package net.flyingfishflash.loremlist.domain.lrmlist

import jakarta.validation.ConstraintViolation
import jakarta.validation.ConstraintViolationException
import jakarta.validation.Validation
import net.flyingfishflash.loremlist.domain.lrmlist.data.LrmList
import net.flyingfishflash.loremlist.domain.lrmlist.data.LrmListConverter
import net.flyingfishflash.loremlist.domain.lrmlist.data.LrmListRepository
import net.flyingfishflash.loremlist.domain.lrmlist.data.dto.LrmListRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class LrmListService(val lrmListRepository: LrmListRepository) {
  fun create(lrmListRequest: LrmListRequest): LrmList = lrmListRepository.insert(lrmListRequest)

  fun deleteSingleById(id: Long) {
    val deletedCount = lrmListRepository.deleteById(id)
    if (deletedCount < 1) {
      throw ListDeleteException(id = id, cause = ListNotFoundException(id))
    } else if (deletedCount > 1) {
      // TODO: The exception should capture a message that more than one record was deleted
      // TODO: Ensure the transaction is rolled back if an exception is thrown
      throw ListDeleteException(id = id)
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
        lrmListRepository.update(LrmListConverter.toLrmList(lrmListRequest, lrmList))
      }
    }
    // map entity model to response model
    return Pair(lrmList, patched)
  }

  fun findAll(): List<LrmList> = lrmListRepository.findAll()

  fun findAllIncludeItems(): List<LrmList> = lrmListRepository.findAllIncludeItems()

  fun findByIdOrListNotFoundException(id: Long): LrmList = lrmListRepository.findByIdOrNull(id) ?: throw ListNotFoundException(id)

  fun findByIdOrListNotFoundExceptionIncludeItems(id: Long): LrmList {
    return lrmListRepository.findByIdOrNullIncludeItems(id) ?: throw ListNotFoundException(id)
  }
}
