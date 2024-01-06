package net.flyingfishflash.loremlist.domain.lrmlist

import jakarta.validation.ConstraintViolation
import jakarta.validation.ConstraintViolationException
import jakarta.validation.Validation
import net.flyingfishflash.loremlist.domain.lrmlist.data.LrmList
import net.flyingfishflash.loremlist.domain.lrmlist.data.LrmListMapper
import net.flyingfishflash.loremlist.domain.lrmlist.data.LrmListRepository
import net.flyingfishflash.loremlist.domain.lrmlist.data.dto.LrmListRequest
import net.flyingfishflash.loremlist.domain.lrmlist.exceptions.ListNotFoundException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class LrmListService(val lrmListRepository: LrmListRepository, val lrmListMapper: LrmListMapper) {
  fun create(lrmListRequest: LrmListRequest): LrmList {
    // map request model to entity model
    val lrmList = LrmList(name = lrmListRequest.name, description = lrmListRequest.description)
    return lrmListRepository.save(lrmList)
  }

  fun delete(id: Long) {
    val lrmList = lrmListRepository.findByIdOrNull(id)
    if (lrmList == null) {
      throw ListNotFoundException()
    } else {
      lrmListRepository.deleteById(id)
    }
  }

  @Suppress("kotlin:S3776")
  fun patch(
    id: Long,
    patchRequest: Map<String, Any>,
  ): Pair<LrmList, Boolean> {
    var patched = false
    val lrmList = lrmListRepository.findByIdOrNull(id)
    if (lrmList == null) {
      throw ListNotFoundException()
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
        lrmListMapper.mapRequestModelToEntityModel(lrmListRequest, lrmList)
      }
    }
    // map entity model to response model
    return Pair(lrmList, patched)
  }

  fun save(lrmList: LrmList): LrmList {
    return lrmListRepository.save(lrmList)
  }

  fun findAll(): MutableList<LrmList> {
    return lrmListRepository.findAll()
  }

  fun findByIdOrListNotFoundException(id: Long): LrmList {
    return lrmListRepository.findByIdOrNull(id) ?: throw ListNotFoundException()
  }
}
