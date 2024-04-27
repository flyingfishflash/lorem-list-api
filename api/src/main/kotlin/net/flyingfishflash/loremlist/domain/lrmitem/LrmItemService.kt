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

  fun create(lrmItemRequest: LrmItemRequest): LrmItem {
    try {
      val id = lrmItemRepository.insert(lrmItemRequest)
      return findById(id)
    } catch (cause: Exception) {
      throw ApiException(
        httpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
        cause = cause,
        message = "Item could not be inserted.",
        responseMessage = "Item could not be inserted.",
      )
    }
  }

  fun deleteSingleById(id: Long) {
    val deletedCount = lrmItemRepository.deleteById(id)
    if (deletedCount < 1) {
      throw ApiException(
        httpStatus = HttpStatus.BAD_REQUEST,
        cause = ItemNotFoundException(id = id),
        responseMessage = "Item id $id was not deleted because it could be found to delete.",
      )
    } else if (deletedCount > 1) {
      // TODO: Ensure the transaction is rolled back if an exception is thrown
      throw ApiException(
        httpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
        responseMessage = "More than one item with id $id were found. No items have been deleted.",
      )
    }
  }

  fun findAll(): List<LrmItem> = lrmItemRepository.findAll()

  fun findAllIncludeLists(): List<LrmItem> = lrmItemRepository.findAllIncludeLists()

  fun findById(id: Long): LrmItem = lrmItemRepository.findByIdOrNull(id) ?: throw ItemNotFoundException(id)

  fun findByIdIncludeLists(id: Long): LrmItem = lrmItemRepository.findByIdOrNullIncludeLists(id) ?: throw ItemNotFoundException(id)
}
