package net.flyingfishflash.loremlist.domain.association

import net.flyingfishflash.loremlist.core.exceptions.CoreException
import net.flyingfishflash.loremlist.domain.exceptions.DomainException
import net.flyingfishflash.loremlist.domain.exceptions.EntityNotFoundException
import net.flyingfishflash.loremlist.domain.LrmComponentType
import net.flyingfishflash.loremlist.domain.ServiceResponse
import net.flyingfishflash.loremlist.domain.association.data.AssociationCreated
import net.flyingfishflash.loremlist.domain.lrmitem.ItemNotFoundException
import net.flyingfishflash.loremlist.domain.lrmitem.LrmItemRepository
import net.flyingfishflash.loremlist.domain.lrmlist.ListNotFoundException
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
    return runCatching {
      associationRepository.count()
    }.getOrElse { exception ->
      throw DomainException(
        cause = exception,
        message = "$exceptionMessage.",
      )
    }
  }

  /**
   * Count of associations for a specified item
   *
   *  @param itemId item id
   *  @param itemOwner item owner
   *  @return association count
   */
  fun countByIdAndItemOwnerForItem(itemId: UUID, itemOwner: String): ServiceResponse<Long> {
    val exceptionMessage = "Count of lists associated with item id $itemId could not be retrieved."
    val associations = runCatching {
      lrmItemRepository.findByOwnerAndIdOrNull(id = itemId, owner = itemOwner) ?: throw ItemNotFoundException(itemId)
      associationRepository.countItemToListByIdAndItemOwner(itemId = itemId, itemOwner = itemOwner)
    }.getOrElse { exception ->
      throw DomainException(
        cause = exception,
        httpStatus = (exception as? ItemNotFoundException)?.httpStatus,
        message = "$exceptionMessage: ${(exception as? ItemNotFoundException)?.message ?: ""}",
      )
    }
    return ServiceResponse(content = associations, message = "Item is associated with $associations lists.")
  }

  /**
   * Count of associations for a specified list
   *
   * @param listId list id
   * @param listOwner list owner
   * @return association count
   */
  fun countByOwnerForList(listId: UUID, listOwner: String): ServiceResponse<Long> {
    val exceptionMessage = "Count of items associated with list id $listId could not be retrieved"
    val associations = runCatching {
      lrmListRepository.findByOwnerAndIdOrNull(id = listId, owner = listOwner) ?: throw ListNotFoundException(listId)
      associationRepository.countListToItemByIdandListOwner(listId, listOwner)
    }.getOrElse { exception ->
      throw DomainException(
        cause = exception,
        httpStatus = (exception as? ListNotFoundException)?.httpStatus,
        message = "$exceptionMessage: ${exception.message.takeIf { exception is ListNotFoundException } ?: ""}",
      )
    }
    return ServiceResponse(content = associations, message = "List is associated with $associations items.")
  }

  private fun doCreateForItem(itemId: UUID, owner: String, listIdCollection: List<UUID>): AssociationCreated {
    // ensure the item exists and has the correct owner
    val item = lrmItemRepository.findByOwnerAndIdOrNull(id = itemId, owner = owner) ?: throw ItemNotFoundException(itemId)

    // ensure the lists exist and have the correct owner
    val notFoundListIds = lrmListRepository.notFoundByOwnerAndId(listIdCollection = listIdCollection, owner = owner)
    if (notFoundListIds.isNotEmpty()) throw ListNotFoundException(notFoundListIds)

    // create associations
    val associations = associationRepository.create(listIdCollection.map { it to itemId }.toSet())
    if (associations.size != listIdCollection.size) {
      throw DomainException(
        message = "Mismatch in created associations count (created = ${associations.size} / requested = ${listIdCollection.size})",
      )
    }

    // return created associations with sorted list names
    return AssociationCreated(componentName = item.name, associatedComponents = associations.map { it.list }.sortedBy { it.name })
  }

  private fun doCreateForList(listId: UUID, owner: String, itemIdCollection: List<UUID>): AssociationCreated {
    // ensure the list exists
    val list = lrmListRepository.findByOwnerAndIdOrNull(id = listId, owner = owner) ?: throw ListNotFoundException(listId)

    // ensure the items exist
    val notFoundItemIds = lrmItemRepository.notFoundByOwnerAndId(itemIdCollection = itemIdCollection, owner = owner)
    if (notFoundItemIds.isNotEmpty()) throw ItemNotFoundException(notFoundItemIds)

    // create associations
    val associations = associationRepository.create(itemIdCollection.map { listId to it }.toSet())
    if (associations.size != itemIdCollection.size) {
      throw DomainException(
        message = "Mismatch in created associations count (created = ${associations.size} / requested = ${itemIdCollection.size})",
      )
    }

    // return created associations with sorted item names
    return AssociationCreated(componentName = list.name, associatedComponents = associations.map { it.item }.sortedBy { it.name })
  }

  /**
   * Create a new association.
   * - Each associated component must have the same owner.
   *
   * @param id id of primary component
   * @param idCollection collection of id's to associate with primary component
   * @param type type of primary component
   * @param componentsOwner owner of primary and secondary components
   * @return [AssociationCreated]
   */
  fun create(
    id: UUID,
    idCollection: List<UUID>,
    type: LrmComponentType,
    componentsOwner: String,
  ): ServiceResponse<AssociationCreated> {
    val exceptionMessage = "Could not create a new association"

    return runCatching {
      check(idCollection.isNotEmpty()) { "id collection argument must not be an empty list."}
      val associationCreated = when (type) {
        // associate a single item with an arbitrary number of lists
        LrmComponentType.Item -> doCreateForItem(itemId = id, owner = componentsOwner, listIdCollection = idCollection)
        // associate a single list with an arbitrary number of items
        LrmComponentType.List -> doCreateForList(listId = id, owner = componentsOwner, itemIdCollection = idCollection)
        // associations can only be created for item or list component types
        else -> throw IllegalArgumentException("type argument may only be Item or List")
      }

      val message = if (associationCreated.associatedComponents.size <= 1) {
        "Assigned ${type.name.lowercase()} '${associationCreated.componentName}' " +
          "to ${type.invert().name.lowercase()} '${associationCreated.associatedComponents.first().name}'"
      } else {
        "Assigned ${type.name.lowercase()} '${associationCreated.componentName}' " +
          "to ${associationCreated.associatedComponents.size} ${type.invert().name.lowercase()}s."
      }

      return@runCatching ServiceResponse(content = associationCreated, message = message)
    }.getOrElse { exception ->
      when (exception) {
        is CoreException -> throw DomainException(
          cause = exception,
          httpStatus = exception.httpStatus,
          message = "$exceptionMessage: ${exception.message}",
          supplemental = exception.supplemental,
        )
        is SQLException -> {
          when {
            exception.message?.contains("duplicate key value violates unique constraint") == true ||
              exception.message?.contains("Unique index or primary key violation") == true -> {
              throw DomainException(
                cause = exception,
                httpStatus = HttpStatus.UNPROCESSABLE_ENTITY,
                message = "$exceptionMessage: It already exists.",
              )
            }
            else -> throw DomainException(
              cause = exception,
              message = "$exceptionMessage: Unanticipated SQL exception.",
            )
          }
        }
        else -> throw DomainException(
          cause = exception,
          message = "$exceptionMessage.",
        )
      }
    }
  }

  /**
   * Delete all associations
   *
   * @return count of deleted associations
   */
  fun delete(): Int = runCatching {
    associationRepository.delete()
  }.getOrElse { exception ->
    throw DomainException(cause = exception, message = "Could not delete any associations.")
  }

  /**
   * Delete all of an item's list associations.
   *
   * @param itemId item id
   * @param itemOwner item owner
   * @return item name, count of deleted associations
   */
  fun deleteByItemOwnerAndItemId(itemId: UUID, itemOwner: String): ServiceResponse<Pair<String, Int>> = runCatching {
    val item = lrmItemRepository.findByOwnerAndIdOrNull(id = itemId, owner = itemOwner)?: throw ItemNotFoundException(itemId)
    val deletedCount = associationRepository.deleteByItemId(itemId)
    val subject = if (deletedCount == 1) "list" else "lists"
    return@runCatching ServiceResponse(
      content = Pair(item.name, deletedCount),
      message = "Removed '${item.name}' from $deletedCount $subject.",
    )
  }.getOrElse { exception ->
    throw when (exception) {
      is ItemNotFoundException -> DomainException(
        cause = exception,
        httpStatus = exception.httpStatus,
        message = "Item id $itemId could not be removed from any/all lists: ${exception.message}",
      )
      else -> DomainException(
        cause = exception,
        message = "Item id $itemId could not be removed from any/all lists.",
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
  fun deleteByListOwnerAndListId(listId: UUID, listOwner: String): ServiceResponse<Pair<String, Int>> {
    val exceptionMessage = "Could not remove any/all items from List id $listId"
    return runCatching {
      val list = lrmListRepository.findByOwnerAndIdOrNull(id = listId, owner = listOwner) ?: throw ListNotFoundException(listId)
      val deletedCount = associationRepository.deleteByListId(listId)
      val subject = if (deletedCount == 1) "item" else "items"

      return@runCatching ServiceResponse(content = Pair(list.name, deletedCount), message = "Removed $deletedCount $subject from list '${list.name}'")
    }.getOrElse { exception ->
      when (exception) {
        is ListNotFoundException -> throw DomainException(
          cause = exception,
          httpStatus = exception.httpStatus,
          message = "$exceptionMessage: ${exception.message}"
        )
        else -> throw DomainException(
          cause = exception,
          message = "$exceptionMessage."
        )
      }
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
  fun deleteByItemIdAndListId(itemId: UUID, listId: UUID, componentsOwner: String): ServiceResponse<Pair<String, String>> {
    val exceptionMessage = "Item id $itemId could not be removed from list id $listId"

    val (item, list, association) = runCatching {
      val item = lrmItemRepository.findByOwnerAndIdOrNull(id = itemId, owner = componentsOwner)
        ?: throw ItemNotFoundException(itemId)
      val list = lrmListRepository.findByOwnerAndIdOrNull(id = listId, owner = componentsOwner)
        ?: throw ListNotFoundException(listId)
      val association = associationRepository.findByItemIdAndListIdOrNull(itemId = itemId, listId = listId)
        ?: throw AssociationNotFoundException()

      return@runCatching Triple(item, list, association)
    }.getOrElse { exception ->
      throw DomainException(
        cause = exception,
        httpStatus = (exception as? EntityNotFoundException)?.httpStatus,
        message = "$exceptionMessage: ${exception.message}"
      )
    }

    val deletedCount = runCatching {
      associationRepository.deleteById(association.id)
    }.getOrElse { cause ->
      throw DomainException(
        cause = cause,
        message = "$exceptionMessage."
      )
    }

    return when {
      deletedCount == 1 -> {
        ServiceResponse(
          content = Pair(item.name, list.name),
          message = "Removed item '${item.name}' from list '${list.name}'"
        )
      }
      deletedCount < 1 -> {
        throw DomainException(
          message = "$exceptionMessage: Item id $itemId exists, list id $listId exists, association id ${association.id} exists, but 0 records were deleted.",
          responseMessage = "$exceptionMessage: Item, list, and association were found, but 0 records were deleted."
        )
      }
      else -> {
        throw DomainException(
          httpStatus = HttpStatus.BAD_REQUEST,
          message = "$exceptionMessage: Delete transaction rolled back because the count of deleted records was > 1.",
          responseMessage = "$exceptionMessage: Item id $itemId is associated with list id $listId multiple times."
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
  ): ServiceResponse<Triple<String, String, String>> = runCatching {
    val item = lrmItemRepository.findByOwnerAndIdOrNull(id = itemId, owner = componentsOwner)
      ?: throw ItemNotFoundException(itemId)

    val currentList = lrmListRepository.findByOwnerAndIdOrNull(id = currentListId, owner = componentsOwner)
      ?: throw ListNotFoundException(currentListId)

    val destinationList = lrmListRepository.findByOwnerAndIdOrNull(id = destinationListId, owner = componentsOwner)
      ?: throw ListNotFoundException(destinationListId)

    val association = associationRepository.findByItemIdAndListIdOrNull(itemId = itemId, listId = currentListId)
      ?: throw AssociationNotFoundException()

    associationRepository.update(association = association.copy(listId = destinationListId))

    return@runCatching Triple(item.name, currentList.name, destinationList.name)
  }.fold(
    onSuccess = { result ->
      ServiceResponse(
        content = result,
        message = "Moved item '${result.first}' from list '${result.second}' to list '${result.third}'",
      )
    },
    onFailure = { exception ->
      throw DomainException(
        cause = exception,
        httpStatus = (exception as? EntityNotFoundException)?.httpStatus,
        message = "Item id $itemId was not moved from list id $currentListId to list id $destinationListId.",
      )
    },
  )
}
