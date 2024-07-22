package net.flyingfishflash.loremlist.domain.association

import net.flyingfishflash.loremlist.core.exceptions.ApiException
import net.flyingfishflash.loremlist.domain.LrmComponentType
import net.flyingfishflash.loremlist.domain.association.data.AssociationCreatedResponse
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
   * Count of all associations
   *
   * @return association count
   */
  fun countAll(): Long {
    val exceptionMessage = "Count of associations could not be retrieved."
    val associations = try {
      associationRepository.count()
    } catch (exception: Exception) {
      throw ApiException(
        cause = exception,
        message = "$exceptionMessage.",
      )
    }
    return associations
  }

  /**
   * Count of associations for a specified item
   *
   *  @param itemId item id
   *  @return association count
   */
  fun countForItemId(itemId: UUID): Long {
    val exceptionMessage = "Count of lists associated with item id $itemId could not be retrieved."
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

  /**
   * Count of associations for a specified list
   *
   * @param listId list id
   * @return association count
   */
  fun countForListId(listId: UUID): Long {
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

  private fun doCreateForItem(itemId: UUID, listIdCollection: List<UUID>): AssociationCreatedResponse {
    // ensure the item exists
    val item = lrmItemRepository.findByIdOrNull(itemId) ?: throw ItemNotFoundException(itemId)
    // ensure the lists exist
    val notFoundListIdCollection = lrmListRepository.notFoundByIdCollection(listIdCollection)
    if (notFoundListIdCollection.isNotEmpty()) throw ListNotFoundException(notFoundListIdCollection)
    // create the associations
    val associationCollection = listIdCollection.map { listId -> Pair(listId, itemId) }.toSet()
    val createdAssociations = associationRepository.create(associationCollection)
    if (createdAssociations.size != listIdCollection.size) {
      throw ApiException(
        message = "Count of created associations is not equal to the count of associations requested " +
          "(created = ${createdAssociations.size} / requested = ${listIdCollection.size})",
      )
    }
    // capture the names of the newly associated lists
    val associatedLists = createdAssociations.map { it.list }.sortedBy { it.name }
    return AssociationCreatedResponse(componentName = item.name, associatedComponents = associatedLists)
  }

  private fun doCreateForList(listId: UUID, itemIdCollection: List<UUID>): AssociationCreatedResponse {
    // ensure the list exists
    val list = lrmListRepository.findByIdOrNull(listId) ?: throw ListNotFoundException(listId)
    // ensure the items exist
    val notFoundItemUuidCollection = lrmItemRepository.notFoundByIdCollection(itemIdCollection)
    if (notFoundItemUuidCollection.isNotEmpty()) throw ItemNotFoundException(notFoundItemUuidCollection)
    // create the associations
    val associationCollection = itemIdCollection.map { itemId -> Pair(listId, itemId) }.toSet()
    val createdAssociations = associationRepository.create(associationCollection)
    if (createdAssociations.size != itemIdCollection.size) {
      throw ApiException(
        message = "Count of created associations is not equal to the count of associations requested " +
          "(created = ${createdAssociations.size} / requested = ${itemIdCollection.size})",
      )
    }
    // capture the names of the newly associated items
    val associatedItems = createdAssociations.map { it.item }.sortedBy { it.name }
    return AssociationCreatedResponse(componentName = list.name, associatedComponents = associatedItems)
  }

  /**
   * Create a new association
   *
   * @param id anchor id
   * @param idCollection collection of id's for components to associate
   * @param type component type
   * @return [AssociationCreatedResponse]
   */
  fun create(id: UUID, idCollection: List<UUID>, type: LrmComponentType): AssociationCreatedResponse {
    val exceptionMessage = "Could not create a new association"
    try {
      val result = when (type) {
        LrmComponentType.Item -> {
          doCreateForItem(id, idCollection)
        }
        LrmComponentType.List -> {
          doCreateForList(id, idCollection)
        }
      }
      return result
    } catch (apiException: ApiException) {
      throw ApiException(
        cause = apiException,
        httpStatus = apiException.httpStatus,
        message = "$exceptionMessage: ${apiException.message}",
        supplemental = apiException.supplemental,
      )
    } catch (sqlException: SQLException) {
      when {
        sqlException.message?.contains("duplicate key value violates unique constraint") == true || // postgresql
          sqlException.message?.contains("Unique index or primary key violation") == true -> { // h2
          throw ApiException(
            cause = sqlException,
            httpStatus = HttpStatus.UNPROCESSABLE_ENTITY,
            message = "$exceptionMessage: It already exists.",
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
  }

  /**
   * Delete all associations
   *
   * @return count of deleted associations
   */
  fun deleteAll(): Int {
    val exceptionMessage = "Could not delete any associations."
    val deletedCount = try {
      associationRepository.deleteAll()
    } catch (exception: Exception) {
      throw ApiException(
        cause = exception,
        message = "$exceptionMessage.",
      )
    }
    return deletedCount
  }

  /**
   * Delete all of an item's list associations
   *
   * @param itemId item id
   * @return item name, count of deleted associations
   */
  fun deleteAllOfItem(itemId: UUID): Pair<String, Int> {
    val item: LrmItem
    val exceptionMessage = "Item id $itemId could not be removed from any/all lists"

    try {
      item = lrmItemRepository.findByIdOrNull(itemId) ?: throw ItemNotFoundException(itemId)
      val deletedCount = associationRepository.deleteAllOfItem(itemId)
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
   * @param listId list id
   * @return list name, count of deleted associations
   */
  fun deleteAllOfList(listId: UUID): Pair<String, Int> {
    val list: LrmList
    val exceptionMessage = "Could not remove any/all items from List id $listId"

    try {
      list = lrmListRepository.findByIdOrNull(listId) ?: throw ListNotFoundException(listId)
      val deletedCount = associationRepository.deleteAllOfList(listId)
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
   * @param itemId item id
   * @param listId list id
   * @return item name, list name
   */
  fun deleteByItemIdAndListId(itemId: UUID, listId: UUID): Pair<String, String> {
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
      associationRepository.delete(association.id)
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
          message = "$exceptionMessage: Item id $itemId exists, " +
            "list id $listId exists and " +
            "association id ${association.id} exists, " +
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

  /**
   * Update the list element of an association, effectively moving an item from one list to another
   *
   * @param itemId item id
   * @param currentListId current list id
   * @param newListId new list id
   * @return item name, current list name, new list name
   */
  fun updateList(itemId: UUID, currentListId: UUID, newListId: UUID): Triple<String, String, String> {
    val item: LrmItem
    val currentList: LrmList
    val newList: LrmList
    val association: Association
    val exceptionMessage = "Item id $itemId was not moved from list id $currentListId to list id $newListId"

    try {
      item = lrmItemRepository.findByIdOrNull(itemId) ?: throw ItemNotFoundException(itemId)
      currentList = lrmListRepository.findByIdOrNull(currentListId) ?: throw ListNotFoundException(currentListId)
      newList = lrmListRepository.findByIdOrNull(newListId) ?: throw ListNotFoundException(newListId)
      association = associationRepository.findByItemIdAndListIdOrNull(itemId, currentListId) ?: throw AssociationNotFoundException()
      val updatedAssociation = association.copy(listId = newListId)
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
