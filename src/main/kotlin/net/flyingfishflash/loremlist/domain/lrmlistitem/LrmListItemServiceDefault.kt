package net.flyingfishflash.loremlist.domain.lrmlistitem

import jakarta.validation.ConstraintViolationException
import jakarta.validation.Validation
import kotlinx.datetime.Clock.System.now
import net.flyingfishflash.loremlist.core.exceptions.CoreException
import net.flyingfishflash.loremlist.domain.LrmComponentType
import net.flyingfishflash.loremlist.domain.ServiceResponse
import net.flyingfishflash.loremlist.domain.exceptions.DomainException
import net.flyingfishflash.loremlist.domain.exceptions.EntityNotFoundException
import net.flyingfishflash.loremlist.domain.lrmitem.ItemNotFoundException
import net.flyingfishflash.loremlist.domain.lrmitem.LrmItem
import net.flyingfishflash.loremlist.domain.lrmitem.LrmItemRepository
import net.flyingfishflash.loremlist.domain.lrmitem.data.LrmItemCreate
import net.flyingfishflash.loremlist.domain.lrmlist.ListNotFoundException
import net.flyingfishflash.loremlist.domain.lrmlist.LrmListRepository
import net.flyingfishflash.loremlist.domain.lrmlistitem.data.LrmListItemAdded
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import java.sql.SQLException
import java.util.*

@Service
class LrmListItemServiceDefault(
  private val lrmItemRepository: LrmItemRepository,
  private val lrmListRepository: LrmListRepository,
  private val lrmListItemRepository: LrmListItemRepository,
) : LrmListItemService {
  private val validator = Validation.buildDefaultValidatorFactory().validator

  override fun countByOwnerAndListId(listId: UUID, owner: String): ServiceResponse<Long> {
    val exceptionMessage = "Count of items associated with list id $listId could not be retrieved"
    val associations = runCatching {
      lrmListRepository.findByOwnerAndIdOrNull(id = listId, owner = owner) ?: throw ListNotFoundException(listId)
      lrmListItemRepository.countByOwnerAndListId(listId = listId, listOwner = owner)
    }.getOrElse { exception ->
      throw DomainException(
        cause = exception,
        httpStatus = (exception as? ListNotFoundException)?.httpStatus,
        message = "$exceptionMessage: ${exception.message.takeIf { exception is ListNotFoundException } ?: ""}",
      )
    }
    return ServiceResponse(content = associations, message = "List is associated with $associations items.")
  }

  /** Associate a single list with an arbitrary number of items */
  override fun add(id: UUID, idCollection: List<UUID>, componentsOwner: String): ServiceResponse<LrmListItemAdded> {
    val exceptionMessage = "Could not create a new association"
    val type = LrmComponentType.List

    return runCatching {
      check(idCollection.isNotEmpty()) { "id collection argument must not be an empty list." }
      val listItemCreated = addToList(listId = id, owner = componentsOwner, itemIdCollection = idCollection)

      val message = if (listItemCreated.items.size <= 1) {
        "Assigned ${type.name.lowercase()} '${listItemCreated.listName}' " +
          "to ${type.invert().name.lowercase()} '${listItemCreated.items.first().name}'"
      } else {
        "Assigned ${type.name.lowercase()} '${listItemCreated.listName}' " +
          "to ${listItemCreated.items.size} ${type.invert().name.lowercase()}s."
      }

      return@runCatching ServiceResponse(content = listItemCreated, message = message)
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

  private fun addToList(listId: UUID, owner: String, itemIdCollection: List<UUID>): LrmListItemAdded {
    // ensure the list exists
    val list = lrmListRepository.findByOwnerAndIdOrNull(id = listId, owner = owner) ?: throw ListNotFoundException(listId)

    // ensure the items exist
    val notFoundItemIds = lrmItemRepository.notFoundByOwnerAndId(itemIdCollection = itemIdCollection, owner = owner)
    if (notFoundItemIds.isNotEmpty()) throw ItemNotFoundException(notFoundItemIds)

    // create associations
    val associations = lrmListItemRepository.create(itemIdCollection.map { listId to it }.toSet())
    if (associations.size != itemIdCollection.size) {
      throw DomainException(
        message = "Mismatch in created associations count (created = ${associations.size} / requested = ${itemIdCollection.size})",
      )
    }

    // return created associations with sorted item names
    val lrmListItemAdded = LrmListItemAdded(listName = list.name, items = associations.map { it.item }.sortedBy { it.name })
    return lrmListItemAdded
  }

  private fun createItem(lrmItemCreate: LrmItemCreate, creator: String): LrmItem = runCatching {
    val now = now()
    val lrmItem = LrmItem(
      id = UUID.randomUUID(),
      name = lrmItemCreate.name,
      description = lrmItemCreate.description,
      created = now,
      owner = creator,
      creator = creator,
      updated = now,
      updater = creator,
    )
    val itemId = lrmItemRepository.insert(lrmItem)
    val newLrmItem = lrmItemRepository.findByOwnerAndIdOrNull(id = itemId, owner = creator)
      ?: throw ItemNotFoundException(itemId)
    return@runCatching newLrmItem
  }.getOrElse { cause -> throw DomainException(cause = cause, message = "Item could not be created.") }

  /** Create a new item and associate it with the specified list */
  override fun create(listId: UUID, lrmItemCreate: LrmItemCreate, creator: String): ServiceResponse<LrmListItem> {
    val lrmItem = createItem(lrmItemCreate = lrmItemCreate, creator = creator)
    val lrmListItemAdded = addToList(listId = listId, owner = creator, itemIdCollection = listOf(lrmItem.id))
    val lrmListItem = findByOwnerAndItemIdAndListId(itemId = lrmItem.id, listId = listId, owner = creator).content
    val tmpListItem = lrmListItem.copy(quantity = lrmItemCreate.quantity, isSuppressed = lrmItemCreate.isSuppressed)
    patchQuantity(tmpListItem)
    patchIsSuppressed(tmpListItem)
    val patchedLrmListItem = findByOwnerAndItemIdAndListId(itemId = lrmItem.id, listId = listId, owner = creator).content
    val message = "Created item '${lrmListItemAdded.items.first().name}' and assigned it to list '${lrmListItemAdded.listName}'"
    return ServiceResponse(
      content = patchedLrmListItem,
      message = message,
    )
  }

  override fun findByOwnerAndItemIdAndListId(itemId: UUID, listId: UUID, owner: String): ServiceResponse<LrmListItem> {
    lrmListRepository.findByOwnerAndIdOrNull(id = listId, owner = owner) ?: throw ListNotFoundException(id = listId)
    val listItem = lrmListItemRepository.findByOwnerAndItemIdAndListIdOrNull(
      itemId = itemId,
      listId = listId,
      owner = owner,
    ) ?: throw ListItemNotFoundException(id = itemId)
    return ServiceResponse(content = listItem, message = "Retrieved list item '${listItem.name}'")
  }

  override fun move(itemId: UUID, currentListId: UUID, destinationListId: UUID, owner: String): ServiceResponse<Triple<String, String, String>> = runCatching {
    val item = lrmItemRepository.findByOwnerAndIdOrNull(id = itemId, owner = owner)
      ?: throw ItemNotFoundException(itemId)
    val currentList = lrmListRepository.findByOwnerAndIdOrNull(id = currentListId, owner = owner)
      ?: throw ListNotFoundException(currentListId)
    val destinationList = lrmListRepository.findByOwnerAndIdOrNull(id = destinationListId, owner = owner)
      ?: throw ListNotFoundException(destinationListId)
    val lrmListItem = lrmListItemRepository.findByOwnerAndItemIdAndListIdOrNull(itemId = itemId, listId = currentListId, owner = owner)
      ?: throw ListItemNotFoundException()

    // TODO: should not attempt to update is source and destination list are the same
    lrmListItemRepository.updateListId(lrmListItem = lrmListItem, destinationListId = destinationListId)

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

  // TODO: Wrap in runCatching() and ServiceResponse
  override fun patchQuantity(patchedLrmListItem: LrmListItem) {
    patchItem(patchedLrmListItem) { lrmListItemRepository.updateQuantity(patchedLrmListItem) }
  }

  override fun patchIsSuppressed(patchedLrmListItem: LrmListItem) {
    patchItem(patchedLrmListItem) { lrmListItemRepository.updateIsItemSuppressed(patchedLrmListItem) }
  }

  override fun removeByOwnerAndItemId(itemId: UUID, owner: String): ServiceResponse<Pair<String, Int>> = runCatching {
    val item = lrmItemRepository.findByOwnerAndIdOrNull(id = itemId, owner = owner) ?: throw ItemNotFoundException(itemId)
    val deletedCount = lrmListItemRepository.removeByOwnerAndItemId(itemId = itemId, owner = owner)
    val subject = if (deletedCount == 1) "list" else "lists"
    return@runCatching ServiceResponse(content = Pair(item.name, deletedCount), message = "Removed '${item.name}' from $deletedCount $subject.")
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

  override fun removeByOwnerAndListId(listId: UUID, owner: String): ServiceResponse<Pair<String, Int>> {
    val exceptionMessage = "Could not remove any/all items from List id $listId"
    return runCatching {
      val list = lrmListRepository.findByOwnerAndIdOrNull(id = listId, owner = owner) ?: throw ListNotFoundException(listId)
      val deletedCount = lrmListItemRepository.removeByOwnerAndListId(listId = listId, owner = owner)
      val subject = if (deletedCount == 1) "item" else "items"
      return@runCatching ServiceResponse(content = Pair(list.name, deletedCount), message = "Removed $deletedCount $subject from list '${list.name}'")
    }.getOrElse { exception ->
      when (exception) {
        is ListNotFoundException -> throw DomainException(
          cause = exception,
          httpStatus = exception.httpStatus,
          message = "$exceptionMessage: ${exception.message}",
        )
        else -> throw DomainException(
          cause = exception,
          message = "$exceptionMessage.",
        )
      }
    }
  }

  override fun removeByOwnerAndListIdAndItemId(listId: UUID, itemId: UUID, owner: String): ServiceResponse<Pair<String, String>> {
    val exceptionMessage = "Item id $itemId could not be removed from list id $listId"

    val (item, list, association) = runCatching {
      val item = lrmItemRepository.findByOwnerAndIdOrNull(id = itemId, owner = owner)
        ?: throw ItemNotFoundException(itemId)
      val list = lrmListRepository.findByOwnerAndIdOrNull(id = listId, owner = owner)
        ?: throw ListNotFoundException(listId)
      val association = lrmListItemRepository.findByOwnerAndItemIdAndListIdOrNull(itemId = itemId, listId = listId, owner = owner)
        ?: throw ListItemNotFoundException()

      return@runCatching Triple(item, list, association)
    }.getOrElse { exception ->
      throw DomainException(
        cause = exception,
        httpStatus = (exception as? EntityNotFoundException)?.httpStatus,
        message = "$exceptionMessage: ${exception.message}",
      )
    }

    val deletedCount = runCatching {
      lrmListItemRepository.removeByOwnerAndListIdAndItemId(listId = listId, itemId = itemId, owner = owner)
    }.getOrElse { cause ->
      throw DomainException(
        cause = cause,
        message = "$exceptionMessage.",
      )
    }

    return when {
      deletedCount == 1 -> {
        ServiceResponse(
          content = Pair(item.name, list.name),
          message = "Removed item '${item.name}' from list '${list.name}'",
        )
      }
      deletedCount < 1 -> {
        throw DomainException(
          message = "$exceptionMessage: Item id $itemId exists, list id $listId exists, association id ${association.id} exists, but 0 records were deleted.",
          responseMessage = "$exceptionMessage: Item, list, and association were found, but 0 records were deleted.",
        )
      }
      else -> {
        throw DomainException(
          httpStatus = HttpStatus.BAD_REQUEST,
          message = "$exceptionMessage: Delete transaction rolled back because the count of deleted records was > 1.",
          responseMessage = "$exceptionMessage: Item id $itemId is associated with list id $listId multiple times.",
        )
      }
    }
  }

  // item context

  override fun countByOwnerAndItemId(itemId: UUID, owner: String): ServiceResponse<Long> {
    val exceptionMessage = "Count of lists associated with item id $itemId could not be retrieved."
    val associations = runCatching {
      lrmItemRepository.findByOwnerAndIdOrNull(id = itemId, owner = owner) ?: throw ItemNotFoundException(itemId)
      lrmListItemRepository.countByOwnerAndItemId(itemId = itemId, itemOwner = owner)
    }.getOrElse { exception ->
      throw DomainException(
        cause = exception,
        httpStatus = (exception as? ItemNotFoundException)?.httpStatus,
        message = "$exceptionMessage: ${(exception as? ItemNotFoundException)?.message ?: ""}",
      )
    }
    return ServiceResponse(content = associations, message = "Item is associated with $associations lists.")
  }

  override fun patchName(patchedLrmListItem: LrmListItem) {
    patchItem(patchedLrmListItem) { lrmListItemRepository.updateName(patchedLrmListItem) }
  }

  override fun patchDescription(patchedLrmListItem: LrmListItem) {
    patchItem(patchedLrmListItem) { lrmListItemRepository.updateDescription(patchedLrmListItem) }
  }

  private fun patchItem(patchedLrmItem: LrmListItem, updateAction: () -> Int) {
    validateItemEntity(patchedLrmItem)
    val updatedCount = updateAction()
    if (updatedCount != 1) {
      handleInvalidAffectedRecordCount(updatedCount, patchedLrmItem.id)
    }
  }

  private fun handleInvalidAffectedRecordCount(affectedRecordCount: Int, id: UUID) {
    when {
      affectedRecordCount < 1 -> throw DomainException(message = "No item affected by the repository operation.")
      affectedRecordCount > 1 -> throw DomainException(message = "More than one item with id $id were found.")
      else -> throw IllegalArgumentException("$affectedRecordCount should not be passed to this function")
    }
  }

  private fun validateItemEntity(lrmItem: LrmListItem) {
    val violations = validator.validate(lrmItem)
    if (violations.isNotEmpty()) {
      throw ConstraintViolationException(violations)
    }
  }
}
