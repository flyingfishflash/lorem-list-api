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

  /**
   * Count of associations for a specified item
   *
   *  @param UUID item uuid
   *  @return association count
   */
  fun countForItemId(itemUuid: UUID): Long {
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

  /**
   * Count of associations for a specified list
   *
   * @param UUID list uuid
   * @return association count
   */
  fun countForListId(listUuid: UUID): Long {
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

  /**
   * Create a new association
   *
   * @param UUID item uuid
   * @param UUID list uuid
   * @return item name, list name
   */
  fun create(itemUuid: UUID, listUuid: UUID): Pair<String, String> {
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

  /**
   * Delete all of an item's list associations
   *
   * @param UUID item uuid
   * @return item name, count of deleted associations
   */
  fun deleteAllOfItem(itemUuid: UUID): Pair<String, Int> {
    val item: LrmItem
    val exceptionMessage = "Item id $itemUuid could not be removed from any/all lists"

    try {
      item = lrmItemRepository.findByIdOrNull(itemUuid) ?: throw ItemNotFoundException(itemUuid)
      val deletedCount = associationRepository.deleteAllOfItem(itemUuid)
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

  /**
   * Delete all of a list's item associations
   *
   * @param UUID item uuid
   * @return list name, count of deleted associations
   */
  fun deleteAllOfList(listUuid: UUID): Pair<String, Int> {
    val list: LrmList
    val exceptionMessage = "Could not remove any/all items from List id $listUuid"

    try {
      list = lrmListRepository.findByIdOrNull(listUuid) ?: throw ListNotFoundException(listUuid)
      val deletedCount = associationRepository.deleteAllOfList(listUuid)
      return Pair(list.name, deletedCount)
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
  }

  /**
   * Delete an association by item and list ids
   *
   * @param UUID item uuid
   * @param UUID list uuid
   * @return item name, list name
   */
  fun deleteByItemIdAndListId(itemUuid: UUID, listUuid: UUID): Pair<String, String> {
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

  /**
   * Update the list element of an association, effectively moving an item from one list to another
   *
   * @param UUID item uuid
   * @param UUID current list uuid
   * @param UUID new list uuid
   * @return item name, current list name, new list name
   */
  fun updateList(itemUuid: UUID, currentListUuid: UUID, newListUuid: UUID): Triple<String, String, String> {
    val item: LrmItem
    val currentList: LrmList
    val newList: LrmList
    val association: Association
    val exceptionMessage = "Item id $itemUuid was not moved from list id $currentListUuid to list id $newListUuid"

    try {
      item = lrmItemRepository.findByIdOrNull(itemUuid) ?: throw ItemNotFoundException(itemUuid)
      currentList = lrmListRepository.findByIdOrNull(currentListUuid) ?: throw ListNotFoundException(currentListUuid)
      newList = lrmListRepository.findByIdOrNull(newListUuid) ?: throw ListNotFoundException(newListUuid)
      association = associationRepository.findByItemIdAndListIdOrNull(itemUuid, currentListUuid) ?: throw AssociationNotFoundException()
      val updatedAssociation = association.copy(listUuid = newListUuid)
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
      currentList.name,
      newList.name,
    )
  }
}
