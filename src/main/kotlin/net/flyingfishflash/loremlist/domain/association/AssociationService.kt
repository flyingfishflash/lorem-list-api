package net.flyingfishflash.loremlist.domain.association

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
class AssociationService(
  private val associationRepository: AssociationRepository,
  private val lrmItemRepository: LrmItemRepository,
  private val lrmListRepository: LrmListRepository,
) {

  fun addItemToList(itemId: Long, listId: Long): Pair<String, String> {
    val item: LrmItem
    val list: LrmList
    val exceptionMessage = "Item id $itemId could not be added to list id $listId"

    try {
      item = lrmItemRepository.findByIdOrNull(itemId) ?: throw ItemNotFoundException(itemId)
      list = lrmListRepository.findByIdOrNull(listId) ?: throw ListNotFoundException(listId)
      associationRepository.create(itemId = itemId, listId = listId)
    } catch (apiException: ApiException) {
      throw ApiException(
        cause = apiException,
        httpStatus = apiException.httpStatus,
        message = "$exceptionMessage: ${apiException.message}",
      )
    } catch (sqlException: SQLException) {
      when {
        sqlException.message?.contains("duplicate key value violates unique constraint") == true || // postgresql
          sqlException.message?.contains("Unique index or primary key violation") == true -> { // h2
          throw ApiException(
            cause = sqlException,
            httpStatus = HttpStatus.UNPROCESSABLE_ENTITY,
            message = "$exceptionMessage: It's already been added.",
          )
        }
        else -> {
          throw ApiException(
            cause = sqlException,
            message = "$exceptionMessage: Unanticipated SQL exception.",
          )
        }
      }
    } catch (exception: Exception) {
      throw ApiException(
        cause = exception,
        message = "$exceptionMessage.",
      )
    }
    return Pair(item.name, list.name)
  }

  fun countItemToList(itemId: Long): Long {
    val exceptionMessage = "Count of lists associated with item id $itemId could not be retrieved"
    val associations = try {
      lrmItemRepository.findByIdOrNull(itemId) ?: throw ItemNotFoundException(itemId)
      associationRepository.countItemToList(itemId)
    } catch (itemNotFoundException: ItemNotFoundException) {
      throw ApiException(
        cause = itemNotFoundException,
        httpStatus = itemNotFoundException.httpStatus,
        message = "$exceptionMessage: ${itemNotFoundException.message}",
//        responseMessage = "$exceptionMessage: ${itemNotFoundException.message}",
      )
    } catch (exception: Exception) {
      throw ApiException(
        cause = exception,
        message = "$exceptionMessage.",
      )
    }
    return associations
  }

  fun countListToItem(listId: Long): Long {
    val exceptionMessage = "Count of items associated with list id $listId could not be retrieved"
    val associations = try {
      lrmListRepository.findByIdOrNull(listId) ?: throw ListNotFoundException(listId)
      associationRepository.countListToItem(listId)
    } catch (listNotFoundException: ListNotFoundException) {
      throw ApiException(
        cause = listNotFoundException,
        httpStatus = listNotFoundException.httpStatus,
        message = "$exceptionMessage: ${listNotFoundException.message}",
//        responseMessage = "$exceptionMessage: ${listNotFoundException.message}",
      )
    } catch (exception: Exception) {
      throw ApiException(
        cause = exception,
        message = "$exceptionMessage.",
      )
    }
    return associations
  }

  fun updateItemToList(itemId: Long, fromListId: Long, toListId: Long): Triple<String, String, String> {
    val item: LrmItem
    val fromList: LrmList
    val toList: LrmList
    val association: Association
    val exceptionMessage = "Item id $itemId was not moved from list id $fromListId to list id $toListId"

    try {
      item = lrmItemRepository.findByIdOrNull(itemId) ?: throw ItemNotFoundException(itemId)
      fromList = lrmListRepository.findByIdOrNull(fromListId) ?: throw ListNotFoundException(fromListId)
      toList = lrmListRepository.findByIdOrNull(toListId) ?: throw ListNotFoundException(toListId)
      association = associationRepository.findByItemIdAndListIdOrNull(itemId, fromListId) ?: throw AssociationNotFoundException()
      val updatedAssociation = association.copy(listId = toListId)
      associationRepository.update(updatedAssociation)
    } catch (apiException: ApiException) {
      throw ApiException(
        cause = apiException,
        httpStatus = apiException.httpStatus,
        message = "$exceptionMessage $apiException.message",
      )
    } catch (exception: Exception) {
      throw ApiException(
        cause = exception,
        message = "$exceptionMessage.",
      )
    }
    return Triple(
      item.name,
      fromList.name,
      toList.name,
    )
  }

  fun deleteAllItemToListForItem(itemId: Long): Pair<String, Int> {
    val item: LrmItem
    val exceptionMessage = "Item id $itemId could not be removed from any/all lists"

    try {
      item = lrmItemRepository.findByIdOrNull(itemId) ?: throw ItemNotFoundException(itemId)
      val deletedCount = associationRepository.deleteAllItemToListForItem(itemId)
      return Pair(item.name, deletedCount)
    } catch (itemNotFoundException: ItemNotFoundException) {
      throw ApiException(
        cause = itemNotFoundException,
        httpStatus = itemNotFoundException.httpStatus,
        message = "$exceptionMessage: ${itemNotFoundException.message}",
//        responseMessage = "$exceptionMessage: ${itemNotFoundException.message}",
      )
    } catch (exception: Exception) {
      throw ApiException(
        cause = exception,
        message = "$exceptionMessage.",
      )
    }
  }

  fun deleteItemToList(itemId: Long, listId: Long): Pair<String, String> {
    val item: LrmItem
    val list: LrmList
    val association: Association
    val exceptionMessage = "Item id $itemId could not be removed from list id $listId"

    try {
      item = lrmItemRepository.findByIdOrNull(itemId) ?: throw ItemNotFoundException(itemId)
      list = lrmListRepository.findByIdOrNull(listId) ?: throw ListNotFoundException(listId)
      association = associationRepository.findByItemIdAndListIdOrNull(
        itemId = itemId,
        listId = listId,
      ) ?: throw AssociationNotFoundException()
    } catch (apiException: ApiException) {
      throw ApiException(
        cause = apiException,
        httpStatus = apiException.httpStatus,
        message = "$exceptionMessage: ${apiException.message}",
      )
    }

    val deletedCount = try {
      associationRepository.delete(association.uuid)
    } catch (cause: Exception) {
      throw ApiException(
        cause = cause,
        message = "$exceptionMessage.",
      )
    }

    when {
      deletedCount == 1 -> {
        return Pair(item.name, list.name)
      }
      deletedCount < 1 -> {
        throw ApiException(
          message = "$exceptionMessage: Item id $itemId exists, list id $listId exists and association id ${association.uuid} exists, " +
            "but 0 records were deleted.",
          responseMessage = "$exceptionMessage: Item, list, and association were found, but 0 records were deleted.",
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
