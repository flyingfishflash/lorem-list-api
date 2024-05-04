package net.flyingfishflash.loremlist.domain.lrmitem

import net.flyingfishflash.loremlist.core.exceptions.ApiException
import net.flyingfishflash.loremlist.domain.lrmitem.data.LrmItem
import net.flyingfishflash.loremlist.domain.lrmitem.data.LrmItemRepository
import net.flyingfishflash.loremlist.domain.lrmitem.data.LrmItemRequest
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class LrmItemService(val lrmItemRepository: LrmItemRepository) {
// TODO: Ensure the transaction is rolled back if an exception is thrown

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

  fun deleteSingleById(id: Long) {
    val deletedCount = try {
      lrmItemRepository.deleteById(id)
    } catch (cause: Exception) {
      throw ApiException(
        cause = cause,
        message = "Item id $id could not be deleted.",
        responseMessage = "Item id $id could not be deleted.",
      )
    }
    if (deletedCount < 1) {
      throw ApiException(
        httpStatus = HttpStatus.BAD_REQUEST,
        cause = ItemNotFoundException(id = id),
        responseMessage = "Item id $id was not deleted because it could be found to delete.",
      )
    } else if (deletedCount > 1) {
      throw ApiException(
        responseMessage = "More than one item with id $id were found. No items have been deleted.",
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

  fun findById(id: Long): LrmItem {
    val item = try {
      lrmItemRepository.findByIdOrNull(id)
    } catch (cause: Exception) {
      throw ApiException(
        cause = cause,
        message = "Item id $id could not be retrieved.",
        responseMessage = "Item id $id could not be retrieved.",
      )
    }
    return item ?: throw ItemNotFoundException(id)
  }

  fun findByIdIncludeLists(id: Long): LrmItem {
    val item = try {
      lrmItemRepository.findByIdOrNullIncludeLists(id)
    } catch (cause: Exception) {
      throw ApiException(
        cause = cause,
        message = "Item id $id (including associated lists) could not be retrieved.",
        responseMessage = "Item id $id (including associated lists) could not be retrieved.",
      )
    }
    return item ?: throw ItemNotFoundException(id)
  }
}
