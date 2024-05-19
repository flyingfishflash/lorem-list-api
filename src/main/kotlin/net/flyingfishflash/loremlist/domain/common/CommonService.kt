package net.flyingfishflash.loremlist.domain.common

import net.flyingfishflash.loremlist.core.exceptions.AbstractApiException
import net.flyingfishflash.loremlist.core.exceptions.ApiException
import net.flyingfishflash.loremlist.domain.lrmitem.ItemNotFoundException
import net.flyingfishflash.loremlist.domain.lrmitem.LrmItem
import net.flyingfishflash.loremlist.domain.lrmitem.LrmItemRepository
import net.flyingfishflash.loremlist.domain.lrmlist.ListNotFoundException
import net.flyingfishflash.loremlist.domain.lrmlist.LrmList
import net.flyingfishflash.loremlist.domain.lrmlist.LrmListRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.sql.SQLException

@Service
@Transactional
class CommonService(
  private val commonRepository: CommonRepository,
  private val lrmItemRepository: LrmItemRepository,
  private val lrmListRepository: LrmListRepository,
) {

  fun addToList(itemId: Long, listId: Long): Pair<String, String> {
    val item: LrmItem
    val list: LrmList
    val exceptionMessage = "Item id $itemId could not be added to list id $listId"

    try {
      item = lrmItemRepository.findByIdOrNull(itemId) ?: throw ItemNotFoundException(itemId)
      list = lrmListRepository.findByIdOrNull(listId) ?: throw ListNotFoundException(listId)
      lrmItemRepository.addItemToList(listId, itemId)
    } catch (abstractApiException: AbstractApiException) {
      throw ApiException(
        httpStatus = abstractApiException.httpStatus,
        message = "$exceptionMessage: ${abstractApiException.message}",
        responseMessage = "$exceptionMessage: ${abstractApiException.message}",
        cause = abstractApiException,
      )
    } catch (sqlException: SQLException) {
      when {
        sqlException.message?.contains("duplicate key value violates unique constraint") == true || // postgresql
          sqlException.message?.contains("Unique index or primary key violation") == true -> { // h2
          throw ApiException(
            httpStatus = HttpStatus.UNPROCESSABLE_ENTITY,
            responseMessage = "$exceptionMessage: It's already been added.",
            cause = sqlException,
          )
        }
        else -> {
          throw ApiException(
            responseMessage = "$exceptionMessage: Unanticipated SQL exception.",
            cause = sqlException,
          )
        }
      }
    } catch (exception: Exception) {
      throw ApiException(
        message = "$exceptionMessage.",
        responseMessage = "$exceptionMessage.",
        cause = exception,
      )
    }
    return Pair(item.name, list.name)
  }

  fun countItemToListAssociations(itemId: Long): Long {
    val exceptionMessage = "Count of lists associated with item id $itemId could not be retrieved"
    val associations = try {
      lrmItemRepository.findByIdOrNull(itemId) ?: throw ItemNotFoundException(itemId)
      commonRepository.countItemToListAssociations(itemId)
    } catch (itemNotFoundException: ItemNotFoundException) {
      throw ApiException(
        cause = itemNotFoundException,
        httpStatus = itemNotFoundException.httpStatus,
        message = "$exceptionMessage: ${itemNotFoundException.message}",
        responseMessage = "$exceptionMessage: ${itemNotFoundException.message}",
      )
    } catch (exception: Exception) {
      throw ApiException(
        cause = exception,
        message = "$exceptionMessage.",
        responseMessage = "$exceptionMessage.",
      )
    }
    return associations
  }

  fun countListToItemAssociations(listId: Long): Long {
    val exceptionMessage = "Count of items associated with list id $listId could not be retrieved"
    val associations = try {
      lrmListRepository.findByIdOrNull(listId) ?: throw ListNotFoundException(listId)
      commonRepository.countListToItemAssociations(listId)
    } catch (listNotFoundException: ListNotFoundException) {
      throw ApiException(
        cause = listNotFoundException,
        httpStatus = listNotFoundException.httpStatus,
        message = "$exceptionMessage: ${listNotFoundException.message}",
        responseMessage = "$exceptionMessage: ${listNotFoundException.message}",
      )
    } catch (exception: Exception) {
      throw ApiException(
        cause = exception,
        message = "$exceptionMessage.",
        responseMessage = "$exceptionMessage.",
      )
    }
    return associations
  }

  fun moveToList(itemId: Long, fromListId: Long, toListId: Long): Triple<String, String, String> {
    val item: LrmItem
    val fromList: LrmList
    val toList: LrmList
    val exceptionMessage = "Item id $itemId was not moved from list id $fromListId to list id $toListId"

    try {
      item = lrmItemRepository.findByIdOrNull(itemId) ?: throw ItemNotFoundException(itemId)
      fromList = lrmListRepository.findByIdOrNull(fromListId) ?: throw ListNotFoundException(fromListId)
      toList = lrmListRepository.findByIdOrNull(toListId) ?: throw ListNotFoundException(toListId)
      addToList(itemId = itemId, listId = toListId)
      removeFromList(itemId = itemId, listId = fromListId)
    } catch (exception: AbstractApiException) {
      throw ApiException(
        httpStatus = exception.httpStatus,
        message = "$exceptionMessage $exception.message",
        responseMessage = "$exceptionMessage $exception.message",
        cause = exception,
      )
    } catch (exception: Exception) {
      throw ApiException(
        message = "$exceptionMessage.",
        responseMessage = "$exceptionMessage.",
        cause = exception,
      )
    }
    return Triple(
      item.name,
      fromList.name,
      toList.name,
    )
  }

  fun removeFromAllLists(itemId: Long): Pair<String, Int> {
    val item: LrmItem
    val exceptionMessage = "Item id $itemId could not be removed from any/all lists"

    try {
      item = lrmItemRepository.findByIdOrNull(itemId) ?: throw ItemNotFoundException(itemId)
      val deletedCount = commonRepository.deleteAllItemToListAssociations(itemId)
      return Pair(item.name, deletedCount)
    } catch (itemNotFoundException: ItemNotFoundException) {
      throw ApiException(
        cause = itemNotFoundException,
        httpStatus = itemNotFoundException.httpStatus,
        message = "$exceptionMessage: ${itemNotFoundException.message}",
        responseMessage = "$exceptionMessage: ${itemNotFoundException.message}",
      )
    } catch (exception: Exception) {
      throw ApiException(
        cause = exception,
        message = "$exceptionMessage.",
        responseMessage = "$exceptionMessage.",
      )
    }
  }

  fun removeFromList(itemId: Long, listId: Long): Pair<String, String> {
    val item: LrmItem
    val list: LrmList
    val exceptionMessage = "Item id $itemId could not be removed from list id $listId"

    try {
      item = lrmItemRepository.findByIdOrNull(itemId) ?: throw ItemNotFoundException(itemId)
      list = lrmListRepository.findByIdOrNull(listId) ?: throw ListNotFoundException(listId)
    } catch (abstractApiException: AbstractApiException) {
      throw ApiException(
        httpStatus = abstractApiException.httpStatus,
        message = "$exceptionMessage: $abstractApiException.message",
        responseMessage = "$exceptionMessage: $abstractApiException.message",
        cause = abstractApiException,
      )
    }

    val deletedCount = try {
      lrmItemRepository.removeItemFromList(itemId, listId)
    } catch (cause: Exception) {
      throw ApiException(
        cause = cause,
        message = "$exceptionMessage.",
        responseMessage = "$exceptionMessage.",
      )
    }

    when {
      deletedCount == 1 -> {
        return Pair(item.name, list.name)
      }
      deletedCount < 1 -> {
        throw ApiException(
          httpStatus = HttpStatus.BAD_REQUEST,
          message = "$exceptionMessage: Item id $itemId exists and list id $listId exists but 0 records were deleted.",
          responseMessage = "$exceptionMessage: Item id $itemId is not associated with list id $listId.",
        )
      }
      else -> {
        throw ApiException(
          httpStatus = HttpStatus.BAD_REQUEST,
          message = "$exceptionMessage: Delete transaction rolled back because the count of deleted records was > 1.",
          responseMessage = "$exceptionMessage: Item id $itemId is associated with list id $listId multiple times.",
        )
      }
    }
  }
}
