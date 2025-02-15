package net.flyingfishflash.loremlist.domain.lrmitem

import jakarta.validation.ConstraintViolationException
import jakarta.validation.Validation
import kotlinx.datetime.Clock.System.now
import net.flyingfishflash.loremlist.core.exceptions.CoreException
import net.flyingfishflash.loremlist.domain.exceptions.DomainException
import net.flyingfishflash.loremlist.domain.ServiceResponse
import net.flyingfishflash.loremlist.domain.association.AssociationService
import net.flyingfishflash.loremlist.domain.lrmitem.data.LrmItemCreate
import net.flyingfishflash.loremlist.domain.lrmitem.data.LrmItemDeleted
import net.flyingfishflash.loremlist.toJsonElement
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional
class LrmItemServiceDefault(private val associationService: AssociationService, private val lrmItemRepository: LrmItemRepository): LrmItemService {
  private val validator = Validation.buildDefaultValidatorFactory().validator

  override fun countByOwner(owner: String): ServiceResponse<Long> {
    return runCatching {
      val repositoryResponse = lrmItemRepository.countByOwner(owner)
      val message = if (repositoryResponse == 1L) "$repositoryResponse item." else "$repositoryResponse items."
      return@runCatching ServiceResponse(content = repositoryResponse, message = message)
    }.getOrElse { cause ->
      throw DomainException(cause = cause, message = "Total item count couldn't be generated.")
    }
  }

  override fun create(lrmItemCreate: LrmItemCreate, owner: String): ServiceResponse<LrmItem> {
    return runCatching {
      val now = now()
      val lrmItem = LrmItem(
        id = UUID.randomUUID(),
        name = lrmItemCreate.name,
        description = lrmItemCreate.description,
        quantity = lrmItemCreate.quantity,
        created = now,
        createdBy = owner,
        updated = now,
        updatedBy = owner,
      )
      val id = lrmItemRepository.insert(lrmItem)
      val newLrmItem = findByOwnerAndId(id = id, owner = owner).content
      return@runCatching ServiceResponse(content = newLrmItem, message = "Created item '${newLrmItem.name}'")
    }.getOrElse { cause -> throw DomainException(cause = cause, message = "Item could not be created.") }
  }

  override fun deleteByOwner(owner: String): ServiceResponse<LrmItemDeleted> {
    return runCatching {
      val items = findByOwner(owner = owner).content
      if (items.isNotEmpty()) {
        items.filter { it.lists.isNotEmpty() }.forEach {
          associationService.deleteByItemOwnerAndItemId(itemId = it.id, itemOwner = owner)
        }
        lrmItemRepository.deleteById(ids = items.map { it.id }.toSet())
      }
      val lrmItemDeleted = LrmItemDeleted(
        itemNames = items.map { it.name }.sorted(),
        associatedListNames = items.flatMap { it.lists }.map { it.name }.sorted(),
      )
      return@runCatching ServiceResponse(
        content = lrmItemDeleted,
        message = "Deleted all (${lrmItemDeleted.itemNames.size}) of your items, and removed them from ${lrmItemDeleted.associatedListNames.size} lists."
      )
    }.getOrElse { cause ->
      when (cause) {
        is CoreException -> throw DomainException(
          cause = cause,
          httpStatus = cause.httpStatus,
          message = "No items were deleted: ${cause.responseMessage}",
          supplemental = cause.supplemental,
        )
        else -> throw DomainException(cause = cause, message = "No items were deleted.")
      }
    }
  }

  override fun deleteByOwnerAndId(id: UUID, owner: String, removeListAssociations: Boolean): ServiceResponse<LrmItemDeleted> {
    return runCatching {
      val item = findByOwnerAndId(id = id, owner = owner).content
      val lrmItemDeleteResponse = createDeleteResponse(item)
      if (lrmItemDeleteResponse.associatedListNames.isNotEmpty()) {
        if (removeListAssociations) {
          deleteListAssociations(id, owner)
        } else {
          throwListAssociationException(lrmItemDeleteResponse)
        }
      } else {
        doDeleteByOwnerAndId(id, owner)
      }
      val noun = if (lrmItemDeleteResponse.associatedListNames.size == 1) "list" else "lists"
      return@runCatching ServiceResponse(
        content = lrmItemDeleteResponse,
        message ="Deleted item '${lrmItemDeleteResponse.itemNames.first()}', and removed it from ${lrmItemDeleteResponse.associatedListNames.size} $noun."
      )
    }.getOrElse { cause ->
      when (cause) {
        is CoreException -> throw DomainException(
          cause = cause,
          httpStatus = cause.httpStatus,
          message = "Item id $id could not be deleted: ${cause.responseMessage}",
          supplemental = cause.supplemental,
        )
        else -> throw DomainException(cause = cause, message = "Item id $id could not be deleted.")
      }
    }
  }

  private fun createDeleteResponse(item: LrmItem): LrmItemDeleted {
    return LrmItemDeleted(
      itemNames = listOf(item.name),
      associatedListNames = item.lists.map { it.name }.sorted(),
    )
  }

  private fun deleteListAssociations(id: UUID, owner: String) {
    associationService.deleteByItemOwnerAndItemId(itemId = id, itemOwner = owner)
    val deletedCount = lrmItemRepository.deleteByOwnerAndId(id = id, owner = owner)
    if (deletedCount != 1) {
      handleInvalidAffectedRecordCount(deletedCount, id)
    }
  }

  private fun throwListAssociationException(lrmItemDeleteResponse: LrmItemDeleted) {
    val message = "Item '${lrmItemDeleteResponse.itemNames.first()}' " +
      "is associated with ${lrmItemDeleteResponse.associatedListNames.size} list(s). First remove the item from each list."
    throw DomainException(
      httpStatus = HttpStatus.UNPROCESSABLE_ENTITY,
      supplemental = mapOf(
        "itemNames" to lrmItemDeleteResponse.itemNames.toJsonElement(),
        "associatedListNames" to lrmItemDeleteResponse.associatedListNames.toJsonElement(),
      ),
      message = message,
    )
  }

  private fun doDeleteByOwnerAndId(id: UUID, owner: String) {
    val deletedCount = lrmItemRepository.deleteByOwnerAndId(id = id, owner = owner)
    if (deletedCount != 1) {
      handleInvalidAffectedRecordCount(deletedCount, id)
    }
  }

  override fun findByOwner(owner: String): ServiceResponse<List<LrmItem>> {
    return runCatching {
      val repositoryResponse = lrmItemRepository.findByOwner(owner = owner)
      return@runCatching ServiceResponse(content = repositoryResponse, message = "Retrieved all items owned by $owner.")
    }.getOrElse { cause ->
      throw DomainException(cause = cause, message = "Items could not be retrieved.")
    }
  }

  override fun findByOwnerAndId(id: UUID, owner: String): ServiceResponse<LrmItem> {
    val repositoryResponse = runCatching {
      return@runCatching lrmItemRepository.findByOwnerAndIdOrNull(id = id, owner = owner)
    }.getOrElse { cause ->
      throw DomainException(cause = cause, message = "Item id $id could not be retrieved.")
    } ?: throw ItemNotFoundException(id = id)
    return ServiceResponse(content = repositoryResponse, message = "Retrieved item '${repositoryResponse.name}'")
  }

  override fun findByOwnerAndHavingNoListAssociations(owner: String): ServiceResponse<List<LrmItem>> {
    val exceptionMessage = "Items without list associations could not be retrieved."
    return runCatching {
      val repositoryResponse = lrmItemRepository.findByOwnerAndHavingNoListAssociations(owner = owner)
      return@runCatching ServiceResponse(content = repositoryResponse, message = "Retrieved ${repositoryResponse.size} items that are not a part of a list.")
    }.getOrElse { cause ->
      throw DomainException(cause = cause, message = exceptionMessage)
    }
  }

  // TODO: Wrap in runCatching() and ServiceResponse
  override fun patchName(patchedLrmItem: LrmItem) {
    patchItem(patchedLrmItem) { lrmItemRepository.updateName(patchedLrmItem) }
  }

  override fun patchDescription(patchedLrmItem: LrmItem) {
    patchItem(patchedLrmItem) { lrmItemRepository.updateDescription(patchedLrmItem) }
  }

  override fun patchQuantity(patchedLrmItem: LrmItem) {
    patchItem(patchedLrmItem) { lrmItemRepository.updateQuantity(patchedLrmItem) }
  }

  private fun patchItem(patchedLrmItem: LrmItem, updateAction: () -> Int) {
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

  private fun validateItemEntity(lrmItem: LrmItem) {
    val violations = validator.validate(LrmItemEntity.fromLrmItem(lrmItem))
    if (violations.isNotEmpty()) {
      throw ConstraintViolationException(violations)
    }
  }
}
