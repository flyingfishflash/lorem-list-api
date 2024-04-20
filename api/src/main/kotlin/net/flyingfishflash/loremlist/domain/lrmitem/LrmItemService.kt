package net.flyingfishflash.loremlist.domain.lrmitem

import io.github.oshai.kotlinlogging.KotlinLogging
import net.flyingfishflash.loremlist.core.exceptions.AbstractApiException
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

  fun addToList(itemId: Long, listId: Long): Pair<String, String> {
    try {
      lrmItemRepository.addItemToList(listId, itemId)
      return Pair(findById(itemId).name, lrmListRepository.findByIdOrNull(listId)!!.name)
    } catch (ex: ExposedSQLException) {
      when (val original = ex.cause) {
        is SQLIntegrityConstraintViolationException -> {
          logger.error { original.toString() }
          when {
            lrmItemRepository.findByIdOrNull(itemId) == null -> {
              val cause = ItemNotFoundException(id = itemId, cause = original)
              throw ApiException(
                httpStatus = cause.httpStatus,
                responseMessage = "Item id $itemId could not be added to list id $listId because the item couldn't be found",
                cause = cause,
              )
            }
            lrmListRepository.findByIdOrNull(listId) == null -> {
              val cause = ListNotFoundException(id = listId, cause = original)
              throw ApiException(
                httpStatus = cause.httpStatus,
                responseMessage = "Item id $itemId could not be added to list id $listId because the list couldn't be found",
                cause = cause,
              )
            }
            original.message?.contains("Unique index or primary key violation") == true ->
              throw ApiException(
                httpStatus = HttpStatus.UNPROCESSABLE_ENTITY,
                responseMessage = "Item id $itemId could not be added to list id $listId because it's already been added.",
                cause = original,

              )
            else -> {
              throw ApiException(
                httpStatus = HttpStatus.UNPROCESSABLE_ENTITY,
                responseMessage = "Item id $itemId could not be added to list id $listId because of an " +
                  "unanticipated sql integrity constraint violation.",
                cause = original,
              )
            }
          }
        }
        else -> throw ApiException(
          httpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
          responseMessage = "Item id $itemId could not be added to list id $listId because of a sql exception with an undefined cause.",
          cause = ex,
        )
      }
    }
  }

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
      throw ItemDeleteException(
        id = id,
        cause = ItemNotFoundException(id = id),
        responseMessage = "Item id $id was not deleted because it could be found to delete.",
      )
    } else if (deletedCount > 1) {
      // TODO: Ensure the transaction is rolled back if an exception is thrown
      throw ItemDeleteException(
        id = id,
        responseMessage = "More than one item with id $id were found. No items have been deleted.",
      )
    }
  }

  fun findAll(): List<LrmItem> = lrmItemRepository.findAll()

  fun findAllIncludeLists(): List<LrmItem> = lrmItemRepository.findAllIncludeLists()

  fun findById(id: Long): LrmItem = lrmItemRepository.findByIdOrNull(id) ?: throw ItemNotFoundException(id)

  fun findByIdIncludeLists(id: Long): LrmItem = lrmItemRepository.findByIdOrNullIncludeLists(id) ?: throw ItemNotFoundException(id)

  fun moveToList(itemId: Long, fromListId: Long, toListId: Long): Triple<String, String, String> {
    try {
      addToList(itemId = itemId, listId = toListId)
      removeFromList(itemId = itemId, listId = fromListId)
    } catch (exception: ApiException) {
      if (exception.cause != null) {
        throw ApiException(
          httpStatus = if (exception.cause is AbstractApiException) exception.cause.httpStatus else HttpStatus.INTERNAL_SERVER_ERROR,
          responseMessage = "Item was not moved: " + exception.message,
          cause = exception.cause,
        )
      } else {
        throw exception
      }
    } catch (exception: Exception) {
      throw ApiException(
        httpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
        responseMessage = "Item was not moved: " + (exception.message ?: "exception cause detail not available"),
        cause = exception,
      )
    }
    return Triple(
      findById(itemId).name,
      lrmListRepository.findByIdOrNull(fromListId)!!.name,
      lrmListRepository.findByIdOrNull(toListId)!!.name,
    )
  }

  fun removeFromList(itemId: Long, listId: Long): Pair<String, String> {
    val deletedCount = lrmItemRepository.removeItemFromList(itemId, listId)
    when {
      deletedCount == 1 -> {
        return Pair(findById(itemId).name, lrmListRepository.findByIdOrNull(listId)!!.name)
      }
      deletedCount < 1 -> {
        if (lrmItemRepository.findByIdOrNull(itemId) == null) {
          throw ItemRemoveFromListException(
            itemId,
            listId,
            ItemNotFoundException(itemId),
            responseMessage = "Item id $itemId could not be removed from list id $listId " +
              "because item id $itemId could not be found",
          )
        } else if (lrmListRepository.findByIdOrNull(listId) == null) {
          throw ItemRemoveFromListException(
            itemId,
            listId,
            ListNotFoundException(listId),
            responseMessage = "Item id $itemId could not be removed from list id $listId " +
              "because list id $listId could not be found",
          )
        } else {
          // TODO: response message not propogating
          throw ItemRemoveFromListException(
            itemId,
            listId,
            null,
            "Item id $itemId exists and list id $listId exists but 0 records were deleted.",
            "Item id $itemId is not associated with list id $listId",
          )
        }
      }
      else -> {
        // TODO: Ensure the transaction is rolled back if an exception is thrown
        throw ItemRemoveFromListException(
          itemId,
          listId,
          null,
          "Delete transaction rolled back because the count of deleted records was > 1.",
          "Item id $itemId is associated with list id $listId multiple times.",
        )
      }
    }
  }
}
