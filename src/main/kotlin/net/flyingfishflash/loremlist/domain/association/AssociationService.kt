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
import java.util.UUID

@Service
@Transactional
class AssociationService(
  private val associationRepository: AssociationRepository,
  private val lrmItemRepository: LrmItemRepository,
  private val lrmListRepository: LrmListRepository,
) {

  fun addItemToList(itemUuid: UUID, listUuid: UUID): Pair<String, String> {
    val item: LrmItem
    val list: LrmList
    val exceptionMessage = "Item id $itemUuid could not be added to list id $listUuid"

    try {
      item = lrmItemRepository.findByIdOrNull(itemUuid) ?: throw ItemNotFoundException(itemUuid)
      list = lrmListRepository.findByIdOrNull(listUuid) ?: throw ListNotFoundException(listUuid)
      associationRepository.create(itemUuid = itemUuid, listUuid = listUuid)
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

  fun countItemToList(itemUuid: UUID): Long {
    val exceptionMessage = "Count of lists associated with item id $itemUuid could not be retrieved"
    val associations = try {
      lrmItemRepository.findByIdOrNull(itemUuid) ?: throw ItemNotFoundException(itemUuid)
      associationRepository.countItemToList(itemUuid)
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

  fun countListToItem(listUuid: UUID): Long {
    val exceptionMessage = "Count of items associated with list id $listUuid could not be retrieved"
    val associations = try {
      lrmListRepository.findByIdOrNull(listUuid) ?: throw ListNotFoundException(listUuid)
      associationRepository.countListToItem(listUuid)
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

  fun updateItemToList(itemUuid: UUID, fromListUuid: UUID, toListUuid: UUID): Triple<String, String, String> {
    val item: LrmItem
    val fromList: LrmList
    val toList: LrmList
    val association: Association
    val exceptionMessage = "Item id $itemUuid was not moved from list id $fromListUuid to list id $toListUuid"

    try {
      item = lrmItemRepository.findByIdOrNull(itemUuid) ?: throw ItemNotFoundException(itemUuid)
      fromList = lrmListRepository.findByIdOrNull(fromListUuid) ?: throw ListNotFoundException(fromListUuid)
      toList = lrmListRepository.findByIdOrNull(toListUuid) ?: throw ListNotFoundException(toListUuid)
      association = associationRepository.findByItemIdAndListIdOrNull(itemUuid, fromListUuid) ?: throw AssociationNotFoundException()
      val updatedAssociation = association.copy(listUuid = toListUuid)
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

  fun deleteAllItemToListForItem(itemUuid: UUID): Pair<String, Int> {
    val item: LrmItem
    val exceptionMessage = "Item id $itemUuid could not be removed from any/all lists"

    try {
      item = lrmItemRepository.findByIdOrNull(itemUuid) ?: throw ItemNotFoundException(itemUuid)
      val deletedCount = associationRepository.deleteAllItemToListForItem(itemUuid)
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

  fun deleteItemToList(itemUuid: UUID, listUuid: UUID): Pair<String, String> {
    val item: LrmItem
    val list: LrmList
    val association: Association
    val exceptionMessage = "Item id $itemUuid could not be removed from list id $listUuid"

    try {
      item = lrmItemRepository.findByIdOrNull(itemUuid) ?: throw ItemNotFoundException(itemUuid)
      list = lrmListRepository.findByIdOrNull(listUuid) ?: throw ListNotFoundException(listUuid)
      association = associationRepository.findByItemIdAndListIdOrNull(
        itemUuid = itemUuid,
        listUuid = listUuid,
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
          message = "$exceptionMessage: Item id $itemUuid exists, " +
            "list id $listUuid exists and " +
            "association id ${association.uuid} exists, " +
            "but 0 records were deleted.",
          responseMessage = "$exceptionMessage: Item, list, and association were found, but 0 records were deleted.",
        )
      }
      else -> {
        throw ApiException(
          httpStatus = HttpStatus.BAD_REQUEST,
          message = "$exceptionMessage: Delete transaction rolled back because the count of deleted records was > 1.",
          responseMessage = "$exceptionMessage: Item id $itemUuid is associated with list id $listUuid multiple times.",
        )
      }
    }
  }
}
