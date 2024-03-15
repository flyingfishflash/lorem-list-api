package net.flyingfishflash.loremlist.domain.lrmitem

import io.github.oshai.kotlinlogging.KotlinLogging
import net.flyingfishflash.loremlist.core.exceptions.ApiException
import net.flyingfishflash.loremlist.domain.lrmitem.data.LrmItem
import net.flyingfishflash.loremlist.domain.lrmitem.data.LrmItemRepository
import net.flyingfishflash.loremlist.domain.lrmitem.data.LrmItemRequest
import net.flyingfishflash.loremlist.domain.lrmlist.ListNotFoundException
import net.flyingfishflash.loremlist.domain.lrmlist.data.LrmListRepository
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.sql.SQLIntegrityConstraintViolationException

@Service
@Transactional
class LrmItemService(val lrmItemRepository: LrmItemRepository, val lrmListRepository: LrmListRepository) {
  private val logger = KotlinLogging.logger {}

  fun addToList(itemId: Long, listId: Long) {
    try {
      lrmItemRepository.addItemToList(listId, itemId)
    } catch (ex: ExposedSQLException) {
      when (val original = (ex as? ExposedSQLException)?.cause) {
        is SQLIntegrityConstraintViolationException -> {
          logger.error { original.toString() }
          if (lrmItemRepository.findByIdOrNull(itemId) == null) {
            val message = "source item id $itemId not found"
            throw ItemAddToListException(itemId, ItemNotFoundException(itemId))
          } else if (lrmListRepository.findByIdOrNull(listId) == null) {
            val message = "destination list id $listId not found"
            throw ItemAddToListException(itemId, ListNotFoundException(listId))
          } else {
            throw ApiException(HttpStatus.BAD_REQUEST, null, "item $itemId is already assigned to list $listId", ex)
          }
        }
        else -> throw ApiException(HttpStatus.INTERNAL_SERVER_ERROR, null, ex.message.toString(), ex)
      }
    }
  }

  fun create(lrmItemRequest: LrmItemRequest): LrmItem = lrmItemRepository.insert(lrmItemRequest)

  fun deleteSingleById(id: Long) {
    val deletedCount = lrmItemRepository.deleteById(id)
    if (deletedCount < 1) {
      throw ItemDeleteException(id = id, cause = ItemNotFoundException(id = id))
    } else if (deletedCount > 1) {
      // TODO: The exception should capture a message that more than one record was deleted
      // TODO: Ensure the transaction is rolled back if an exception is thrown
      throw ItemDeleteException(id = id)
    }
  }

  fun findAll(): List<LrmItem> = lrmItemRepository.findAll()

  fun findAllAndLists(): List<LrmItem> = lrmItemRepository.findAllAndLists()

  fun moveToList(itemId: Long, fromListId: Long, toListId: Long) {
    addToList(itemId = itemId, listId = toListId)
    removeFromList(itemId = itemId, listId = fromListId)
  }

  fun removeFromList(itemId: Long, listId: Long) {
    val deletedCount = lrmItemRepository.removeItemFromList(itemId, listId)
    if (deletedCount < 1) {
      throw ItemDeleteException(id = itemId, cause = ItemNotFoundException(id = itemId))
    } else if (deletedCount > 1) {
      // TODO: The exception should capture a message that more than one record was deleted
      // TODO: Ensure the transaction is rolled back if an exception is thrown
      throw ItemDeleteException(id = itemId)
    }
  }
}
