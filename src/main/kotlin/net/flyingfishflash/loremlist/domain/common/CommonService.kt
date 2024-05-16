package net.flyingfishflash.loremlist.domain.common

import io.github.oshai.kotlinlogging.KotlinLogging
import net.flyingfishflash.loremlist.core.exceptions.AbstractApiException
import net.flyingfishflash.loremlist.core.exceptions.ApiException
import net.flyingfishflash.loremlist.domain.lrmitem.ItemNotFoundException
import net.flyingfishflash.loremlist.domain.lrmitem.LrmItemRepository
import net.flyingfishflash.loremlist.domain.lrmitem.LrmItemService
import net.flyingfishflash.loremlist.domain.lrmlist.ListNotFoundException
import net.flyingfishflash.loremlist.domain.lrmlist.LrmListService
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.sql.SQLException

@Service
@Transactional
class CommonService(
  val lrmItemRepository: LrmItemRepository,
  val lrmItemService: LrmItemService,
  val lrmListService: LrmListService,
) {
  private val logger = KotlinLogging.logger {}

  fun addToList(itemId: Long, listId: Long): Pair<String, String> {
    try {
      lrmItemService.findById(itemId)
      lrmListService.findById(listId)
      lrmItemRepository.addItemToList(listId, itemId)
    } catch (itemNotFound: ItemNotFoundException) {
      throw ApiException(
        httpStatus = itemNotFound.httpStatus,
        responseMessage = "Item id $itemId could not be added to list id $listId because the item couldn't be found.",
        cause = itemNotFound,
      )
    } catch (listNotFound: ListNotFoundException) {
      throw ApiException(
        httpStatus = listNotFound.httpStatus,
        responseMessage = "Item id $itemId could not be added to list id $listId because the list couldn't be found.",
        cause = listNotFound,
      )
    } catch (sqlException: SQLException) {
      when {
        sqlException.message?.contains("duplicate key value violates unique constraint") == true || // postgresql
          sqlException.message?.contains("Unique index or primary key violation") == true -> { // h2
          throw ApiException(
            httpStatus = HttpStatus.UNPROCESSABLE_ENTITY,
            responseMessage = "Item id $itemId could not be added to list id $listId because it's already been added.",
            cause = sqlException,
          )
        }
        else -> {
          throw ApiException(
            responseMessage = "Item id $itemId could not be added to list id $listId because of an unanticipated sql exception.",
            cause = sqlException,
          )
        }
      }
    } catch (exception: Exception) {
      throw ApiException(
        responseMessage = "Item id $itemId could not be added to list id $listId because of an unanticipated exception.",
        cause = exception,
      )
    }
    return Pair(lrmItemService.findById(itemId).name, lrmListService.findById(listId).name)
  }

  fun moveToList(itemId: Long, fromListId: Long, toListId: Long): Triple<String, String, String> {
    try {
      addToList(itemId = itemId, listId = toListId)
      removeFromList(itemId = itemId, listId = fromListId)
    } catch (exception: ApiException) {
      throw ApiException(
        httpStatus = if (exception.cause is AbstractApiException) exception.cause.httpStatus else HttpStatus.INTERNAL_SERVER_ERROR,
        responseMessage = "Item was not moved: " + exception.message,
        cause = exception.cause,
      )
    } catch (exception: Exception) {
      throw ApiException(
        responseMessage = "Item was not moved: " + (exception.message ?: "exception cause detail not available"),
        cause = exception,
      )
    }
    return Triple(
      lrmItemService.findById(itemId).name,
      lrmListService.findById(fromListId).name,
      lrmListService.findById(toListId).name,
    )
  }

  fun removeFromList(itemId: Long, listId: Long): Pair<String, String> {
    val deletedCount = try {
      lrmItemRepository.removeItemFromList(itemId, listId)
    } catch (cause: Exception) {
      throw ApiException(
        cause = cause,
        message = "Item id $itemId could not be removed from list id $listId.",
        responseMessage = "Item id $itemId could not be removed from list id $listId.",
      )
    }

    when {
      deletedCount == 1 -> {
        return Pair(lrmItemService.findById(itemId).name, lrmListService.findById(listId).name)
      }
      deletedCount < 1 -> {
        try {
          lrmItemService.findById(itemId)
        } catch (itemNotFound: ItemNotFoundException) {
          throw ApiException(
            httpStatus = HttpStatus.BAD_REQUEST,
            cause = itemNotFound,
            responseMessage = "Item id $itemId could not be removed from list id $listId " +
              "because item id $itemId could not be found",
          )
        }
        try {
          lrmListService.findById(listId)
        } catch (listNotFound: ListNotFoundException) {
          throw ApiException(
            httpStatus = HttpStatus.BAD_REQUEST,
            cause = listNotFound,
            responseMessage = "Item id $itemId could not be removed from list id $listId " +
              "because list id $listId could not be found",
          )
        }
        throw ApiException(
          httpStatus = HttpStatus.BAD_REQUEST,
          message = "Item id $itemId exists and list id $listId exists but 0 records were deleted.",
          responseMessage = "Item id $itemId is not associated with list id $listId.",
        )
      }
      else -> {
        throw ApiException(
          httpStatus = HttpStatus.BAD_REQUEST,
          message = "Delete transaction rolled back because the count of deleted records was > 1.",
          responseMessage = "Item id $itemId is associated with list id $listId multiple times.",
        )
      }
    }
  }
}
