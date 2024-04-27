package net.flyingfishflash.loremlist.domain.common

import io.github.oshai.kotlinlogging.KotlinLogging
import net.flyingfishflash.loremlist.core.exceptions.AbstractApiException
import net.flyingfishflash.loremlist.core.exceptions.ApiException
import net.flyingfishflash.loremlist.domain.lrmitem.ItemNotFoundException
import net.flyingfishflash.loremlist.domain.lrmitem.data.LrmItemRepository
import net.flyingfishflash.loremlist.domain.lrmlist.ListNotFoundException
import net.flyingfishflash.loremlist.domain.lrmlist.data.LrmListRepository
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.sql.SQLIntegrityConstraintViolationException

@Service
@Transactional
class CommonService(
  val lrmItemRepository: LrmItemRepository,
  val lrmListRepository: LrmListRepository,
) {
  private val logger = KotlinLogging.logger {}

  fun addToList(itemId: Long, listId: Long): Pair<String, String> {
    try {
      lrmItemRepository.addItemToList(listId, itemId)
      return Pair(lrmItemRepository.findByIdOrNull(itemId)!!.name, lrmListRepository.findByIdOrNull(listId)!!.name)
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
      lrmItemRepository.findByIdOrNull(itemId)!!.name,
      lrmListRepository.findByIdOrNull(fromListId)!!.name,
      lrmListRepository.findByIdOrNull(toListId)!!.name,
    )
  }

  fun removeFromList(itemId: Long, listId: Long): Pair<String, String> {
    val deletedCount = lrmItemRepository.removeItemFromList(itemId, listId)
    when {
      deletedCount == 1 -> {
        return Pair(lrmItemRepository.findByIdOrNull(itemId)!!.name, lrmListRepository.findByIdOrNull(listId)!!.name)
      }
      deletedCount < 1 -> {
        if (lrmItemRepository.findByIdOrNull(itemId) == null) {
          throw ApiException(
            httpStatus = HttpStatus.BAD_REQUEST,
            cause = ItemNotFoundException(itemId),
            responseMessage = "Item id $itemId could not be removed from list id $listId " +
              "because item id $itemId could not be found",
          )
        } else if (lrmListRepository.findByIdOrNull(listId) == null) {
          throw ApiException(
            httpStatus = HttpStatus.BAD_REQUEST,
            cause = ListNotFoundException(listId),
            responseMessage = "Item id $itemId could not be removed from list id $listId " +
              "because list id $listId could not be found",
          )
        } else {
          throw ApiException(
            httpStatus = HttpStatus.BAD_REQUEST,
            message = "Item id $itemId exists and list id $listId exists but 0 records were deleted.",
            responseMessage = "Item id $itemId is not associated with list id $listId",
          )
        }
      }
      else -> {
        // TODO: Ensure the transaction is rolled back if an exception is thrown
        throw ApiException(
          httpStatus = HttpStatus.BAD_REQUEST,
          message = "Delete transaction rolled back because the count of deleted records was > 1.",
          responseMessage = "Item id $itemId is associated with list id $listId multiple times.",
        )
      }
    }
  }
}
