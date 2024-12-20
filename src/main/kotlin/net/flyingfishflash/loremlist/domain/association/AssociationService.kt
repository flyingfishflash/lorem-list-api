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
   *  @param itemOwner item owner
   *  @return association count
   */
  fun countByIdAndItemOwnerForItem(itemId: UUID, itemOwner: String): Long {
    val exceptionMessage = "Count of lists associated with item id $itemId could not be retrieved."
    val associations = try {
      lrmItemRepository.findByOwnerAndIdOrNull(
        id = itemId,
        owner = itemOwner,
      ) ?: throw ItemNotFoundException(itemId)
      associationRepository.countItemToListByIdAndItemOwner(itemId = itemId, itemOwner = itemOwner)
    } catch (itemNotFoundException: ItemNotFoundException) {
      throw ApiException(
        cause = itemNotFoundException,
        httpStatus = itemNotFoundException.httpStatus,
        message = "$exceptionMessage: ${itemNotFoundException.message}",
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
   * @param listOwner list owner
   * @return association count
   */
  fun countByOwnerForList(listId: UUID, listOwner: String): Long {
    val exceptionMessage = "Count of items associated with list id $listId could not be retrieved"
    val associations = try {
      lrmListRepository.findByOwnerAndIdOrNull(
        id = listId,
        owner = listOwner,
      ) ?: throw ListNotFoundException(listId)
      associationRepository.countListToItemByIdandListOwner(listId, listOwner)
    } catch (listNotFoundException: ListNotFoundException) {
      throw ApiException(
        cause = listNotFoundException,
        httpStatus = listNotFoundException.httpStatus,
        message = "$exceptionMessage: ${listNotFoundException.message}",
      )
    } catch (exception: Exception) {
      throw ApiException(
        cause = exception,
        message = "$exceptionMessage.",
      )
    }
    return associations
  }

  private fun doCreateForItem(
    itemId: UUID,
    owner: String,
    listIdCollection: List<UUID>,
  ): AssociationCreatedResponse {
    // ensure the item exists and has the correct owner
    val item = lrmItemRepository.findByOwnerAndIdOrNull(
      id = itemId,
      owner = owner,
    ) ?: throw ItemNotFoundException(itemId)

    // ensure the lists exist and have the correct owner
    val notFoundListIdCollection: Set<UUID> = lrmListRepository.notFoundByOwnerAndId(
      listIdCollection = listIdCollection,
      owner = owner,
    )

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

  private fun doCreateForList(
    listId: UUID,
    owner: String,
    itemIdCollection: List<UUID>,
  ): AssociationCreatedResponse {
    // ensure the list exists
    val list = lrmListRepository.findByOwnerAndIdOrNull(
      id = listId,
      owner = owner,
    ) ?: throw ListNotFoundException(listId)

    // ensure the items exist
    val notFoundItemUuidCollection: Set<UUID> = lrmItemRepository.notFoundByOwnerAndId(
      itemIdCollection = itemIdCollection,
      owner = owner,
    )
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
   * Create a new association.
   * - Each associated component must have the same owner.
   *
   * @param id id of primary component
   * @param idCollection collection of id's to associate with primary component
   * @param type type of primary component
   * @param componentsOwner owner of primary and secondary components
   * @return [AssociationCreatedResponse]
   */
  fun create(
    id: UUID,
    idCollection: List<UUID>,
    type: LrmComponentType,
    componentsOwner: String,
  ): AssociationCreatedResponse {
    val exceptionMessage = "Could not create a new association"
    try {
      val result = when (type) {
        // associate a single item with an arbitrary number of lists
        LrmComponentType.Item -> {
          doCreateForItem(itemId = id, owner = componentsOwner, listIdCollection = idCollection)
        }
        // associate a single list with an arbitrary number of items
        LrmComponentType.List -> {
          doCreateForList(listId = id, owner = componentsOwner, itemIdCollection = idCollection)
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
        sqlException.message?.contains("duplicate key value violates unique constraint") == true ||
          // postgresql
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
  fun delete(): Int {
    val exceptionMessage = "Could not delete any associations."
    val deletedCount = try {
      associationRepository.delete()
    } catch (exception: Exception) {
      throw ApiException(
        cause = exception,
        message = "$exceptionMessage.",
      )
    }
    return deletedCount
  }

  /**
   * Delete all of an item's list associations.
   *
   * @param itemId item id
   * @param itemOwner item owner
   * @return item name, count of deleted associations
   */
  fun deleteByItemOwnerAndItemId(itemId: UUID, itemOwner: String): Pair<String, Int> {
    val item: LrmItem
    val exceptionMessage = "Item id $itemId could not be removed from any/all lists"

    try {
      item = lrmItemRepository.findByOwnerAndIdOrNull(
        id = itemId,
        owner = itemOwner,
      ) ?: throw ItemNotFoundException(itemId)
      val deletedCount = associationRepository.deleteByItemId(itemId = itemId)
      return Pair(item.name, deletedCount)
    } catch (itemNotFoundException: ItemNotFoundException) {
      throw ApiException(
        cause = itemNotFoundException,
        httpStatus = itemNotFoundException.httpStatus,
        message = "$exceptionMessage: ${itemNotFoundException.message}",
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
   * @param listOwner list owner
   * @return list name, count of deleted associations
   */
  fun deleteByListOwnerAndListId(listId: UUID, listOwner: String): Pair<String, Int> {
    val list: LrmList
    val exceptionMessage = "Could not remove any/all items from List id $listId"

    try {
      list = lrmListRepository.findByOwnerAndIdOrNull(
        id = listId,
        owner = listOwner,
      ) ?: throw ListNotFoundException(listId)
      val deletedCount = associationRepository.deleteByListId(listId = listId)
      return Pair(list.name, deletedCount)
    } catch (listNotFoundException: ListNotFoundException) {
      throw ApiException(
        cause = listNotFoundException,
        httpStatus = listNotFoundException.httpStatus,
        message = "$exceptionMessage: ${listNotFoundException.message}",
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
   * @param componentsOwner expected owner of item and list
   * @return item name, list name
   */
  fun deleteByItemIdAndListId(
    itemId: UUID,
    listId: UUID,
    componentsOwner: String,
  ): Pair<String, String> {
    val item: LrmItem
    val list: LrmList
    val association: Association
    val exceptionMessage = "Item id $itemId could not be removed from list id $listId"

    try {
      item = lrmItemRepository.findByOwnerAndIdOrNull(
        id = itemId,
        owner = componentsOwner,
      ) ?: throw ItemNotFoundException(itemId)

      list = lrmListRepository.findByOwnerAndIdOrNull(
        id = listId,
        owner = componentsOwner,
      ) ?: throw ListNotFoundException(listId)

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
      associationRepository.deleteById(id = association.id)
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
   * @param destinationListId new list id
   * @param componentsOwner expected owner of item and list
   * @return item name, current list name, new list name
   */
  fun updateList(
    itemId: UUID,
    currentListId: UUID,
    destinationListId: UUID,
    componentsOwner: String,
  ): Triple<String, String, String> {
    val item: LrmItem
    val currentList: LrmList
    val destinationList: LrmList
    val association: Association
    val exceptionMessage = "Item id $itemId was not moved from list id $currentListId to list id $destinationListId"

    try {
      item = lrmItemRepository.findByOwnerAndIdOrNull(
        id = itemId,
        owner = componentsOwner,
      ) ?: throw ItemNotFoundException(itemId)

      currentList = lrmListRepository.findByOwnerAndIdOrNull(
        id = currentListId,
        owner = componentsOwner,
      ) ?: throw ListNotFoundException(currentListId)

      destinationList = lrmListRepository.findByOwnerAndIdOrNull(
        id = destinationListId,
        owner = componentsOwner,
      ) ?: throw ListNotFoundException(destinationListId)

      association = associationRepository.findByItemIdAndListIdOrNull(
        itemId = itemId,
        listId = currentListId,
      ) ?: throw AssociationNotFoundException()

      val updatedAssociation = association.copy(listId = destinationListId)
      associationRepository.update(association = updatedAssociation)
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
      destinationList.name,
    )
  }
}
