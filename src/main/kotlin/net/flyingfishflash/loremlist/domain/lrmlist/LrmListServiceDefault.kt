package net.flyingfishflash.loremlist.domain.lrmlist

import jakarta.validation.ConstraintViolationException
import jakarta.validation.Validation
import kotlinx.datetime.Clock.System.now
import net.flyingfishflash.loremlist.core.exceptions.CoreException
import net.flyingfishflash.loremlist.domain.exceptions.DomainException
import net.flyingfishflash.loremlist.domain.ServiceResponse
import net.flyingfishflash.loremlist.domain.association.AssociationService
import net.flyingfishflash.loremlist.domain.lrmlist.data.LrmListCreate
import net.flyingfishflash.loremlist.domain.lrmlist.data.LrmListDeleted
import net.flyingfishflash.loremlist.toJsonElement
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional
class LrmListServiceDefault(private val associationService: AssociationService, private val lrmListRepository: LrmListRepository): LrmListService {
  private val validator = Validation.buildDefaultValidatorFactory().validator

  override fun countByOwner(owner: String): ServiceResponse<Long> {
    return runCatching {
      val repositoryResponse = lrmListRepository.countByOwner(owner)
      return@runCatching ServiceResponse(
        content = repositoryResponse,
        message = if (repositoryResponse == 1L) "$repositoryResponse list." else "$repositoryResponse lists.")
    }.getOrElse { cause ->
      throw DomainException(cause = cause, message = "Total list count couldn't be generated.")
    }
  }

  override fun create(lrmListCreate: LrmListCreate, creator: String): ServiceResponse<LrmList> {
    return runCatching {
      val now = now()
      val lrmList = LrmList(
        id = UUID.randomUUID(),
        name = lrmListCreate.name,
        description = lrmListCreate.description,
        public = lrmListCreate.public,
        owner = creator,
        created = now,
        creator = creator,
        updated = now,
        updater = creator,
        items = emptySet(),
      )
      val id = lrmListRepository.insert(lrmList)
      val createdLrmList = findByOwnerAndId(id = id, owner = creator).content
      return@runCatching ServiceResponse(content = createdLrmList, message = "Created list '${createdLrmList.name}'")
    }.getOrElse { cause ->
      throw DomainException(
        cause = cause,
        message = "List could not be created.",
      )
    }
  }

  override fun deleteByOwner(owner: String): ServiceResponse<LrmListDeleted> {
    return runCatching {
      val lists = findByOwner(owner).content
      if (lists.isNotEmpty()) {
        lists.filter { it.items.isNotEmpty() }.forEach {
          associationService.deleteByListOwnerAndListId(listId = it.id, listOwner = owner)
        }
        lrmListRepository.deleteById(ids = lists.map { it.id }.toSet())
      }
      val lrmListDeleteResponse = LrmListDeleted(
        listNames = lists.map { it.name }.sorted(),
        associatedItemNames = lists.flatMap { it.items }.map { it.name }.sorted(),
      )
      return@runCatching ServiceResponse(
        content = lrmListDeleteResponse,
        message = "Deleted all (${lrmListDeleteResponse.listNames.size}) of your lists, and disassociated ${lrmListDeleteResponse.associatedItemNames.size} items.")
    }.getOrElse { cause ->
      when (cause) {
        is CoreException -> throw DomainException(
          cause = cause,
          httpStatus = cause.httpStatus,
          message = "No lists were deleted: ${cause.responseMessage}",
          supplemental = cause.supplemental,
        )
        else -> throw DomainException(cause = cause, message = "No lists were deleted.")
      }
    }
  }

  override fun deleteByOwnerAndId(id: UUID, owner: String, removeItemAssociations: Boolean): ServiceResponse<LrmListDeleted> {
    return runCatching {
      val list = findByOwnerAndId(id = id, owner = owner).content
      val deleteResponse = createDeleteResponse(list)
      if (deleteResponse.associatedItemNames.isEmpty()) {
        doDeleteByOwnerAndId(id, owner)
      } else {
        if (removeItemAssociations) {
          deleteItemAssociations(id, owner)
        } else {
          throwItemAssociationException(deleteResponse, list.name)
        }
      }
      val noun = if (deleteResponse.associatedItemNames.size == 1) "item" else "items"
      return@runCatching ServiceResponse(
        content = deleteResponse,
        message = "Deleted list '${deleteResponse.listNames.first()}', and disassociated ${deleteResponse.associatedItemNames.size} $noun.")
    }.getOrElse { cause ->
      when (cause) {
        is CoreException -> throw DomainException(
          cause = cause,
          httpStatus = cause.httpStatus,
          message = "List could not be deleted: ${cause.responseMessage}",
          supplemental = cause.supplemental,
        )
        else -> throw DomainException(cause = cause, message = "List id $id could not be deleted.")
      }
    }
  }

  private fun createDeleteResponse(list: LrmList): LrmListDeleted {
    return LrmListDeleted(
      listNames = listOf(list.name),
      associatedItemNames = list.items.map { it.name }.sorted(),
    )
  }

  private fun deleteItemAssociations(id: UUID, owner: String) {
    associationService.deleteByListOwnerAndListId(listId = id, listOwner = owner)
    val deletedCount = lrmListRepository.deleteByOwnerAndId(id = id, owner = owner)
    if (deletedCount > 1) {
      handleInvalidAffectedRecordCount(deletedCount, id)
    }
  }

  private fun throwItemAssociationException(deleteResponse: LrmListDeleted, listName: String) {
    val message = "List '$listName' is associated with ${deleteResponse.associatedItemNames.size} item(s). " +
      "First remove each item from the list."
    throw DomainException(
      httpStatus = HttpStatus.UNPROCESSABLE_ENTITY,
      supplemental = mapOf(
        "listNames" to deleteResponse.listNames.toJsonElement(),
        "associatedItemNames" to deleteResponse.associatedItemNames.toJsonElement(),
      ),
      responseMessage = "'$listName' includes ${deleteResponse.associatedItemNames.size} item(s).",
      message = message,
    )
  }

  private fun doDeleteByOwnerAndId(id: UUID, owner: String) {
    val deletedCount = lrmListRepository.deleteByOwnerAndId(id = id, owner = owner)
    if (deletedCount > 1) {
      handleInvalidAffectedRecordCount(deletedCount, id)
    }
  }

  override fun findByOwner(owner: String): ServiceResponse<List<LrmList>> {
    val exceptionMessage = "Lists (including associated items) could not be retrieved."
    return runCatching {
    val repositoryResponse =  lrmListRepository.findByOwner(owner = owner)
      return@runCatching ServiceResponse(
        content = repositoryResponse,
        message = "Retrieved all lists owned by '$owner'"
      )
    }.getOrElse { cause ->
      throw DomainException(cause = cause, message = exceptionMessage)
    }
  }

  override fun findByOwnerAndId(id: UUID, owner: String): ServiceResponse<LrmList> {
    val exceptionMessage = "List id $id (including associated items) could not be retrieved."
    val list = runCatching {
      lrmListRepository.findByOwnerAndIdOrNull(id = id, owner = owner)
    }.getOrElse { cause ->
      throw DomainException(cause = cause, message = exceptionMessage)
    } ?: throw ListNotFoundException(id)
    return ServiceResponse(content = list, message = "Retrieved list '${list.name}'")
  }

  override fun findByOwnerAndHavingNoItemAssociations(owner: String): ServiceResponse<List<LrmList>> {
    val exceptionMessage = "Lists without item associations could not be retrieved."
    return runCatching {
      val repositoryResponse = lrmListRepository.findByOwnerAndHavingNoItemAssociations(owner = owner)
      return@runCatching ServiceResponse(content = repositoryResponse, message = "Retrieved ${repositoryResponse.size} lists that have no items.")
    }.getOrElse { cause ->
      throw DomainException(cause = cause, message = exceptionMessage)
    }
  }

  override fun findByPublic(): ServiceResponse<List<LrmList>> {
    return runCatching {
      val repositoryResponse = lrmListRepository.findByPublic()
      return@runCatching ServiceResponse(content = repositoryResponse, message = "Retrieved ${repositoryResponse.size} public lists.")
    }.getOrElse { cause ->
      throw DomainException(cause = cause, message = "Public lists (including associated items) could not be retrieved.")
    }
  }

  override fun patchName(patchedLrmList: LrmList) {
    patchList(patchedLrmList) { lrmListRepository.updateName(patchedLrmList) }
  }

  override fun patchDescription(patchedLrmList: LrmList) {
    patchList(patchedLrmList) { lrmListRepository.updateDescription(patchedLrmList) }
  }

  override fun patchIsPublic(patchedLrmList: LrmList) {
    patchList(patchedLrmList) { lrmListRepository.updateIsPublic(patchedLrmList) }
  }

  private fun patchList(patchedLrmList: LrmList, updateAction: () -> Int) {
    validateListEntity(patchedLrmList)
    val updatedCount = updateAction()
    if (updatedCount != 1) {
      handleInvalidAffectedRecordCount(updatedCount, patchedLrmList.id)
    }
  }

  private fun handleInvalidAffectedRecordCount(affectedRecordCount: Int, id: UUID) {
    when {
      affectedRecordCount < 1 -> throw DomainException(message = "No list affected by the repository operation.")
      affectedRecordCount > 1 -> throw DomainException(message = "More than one list with id $id found.")
      else -> throw IllegalArgumentException("$affectedRecordCount should not be passed to this function")
    }
  }

  private fun validateListEntity(lrmList: LrmList) {
    val violations = validator.validate(LrmListEntity.fromLrmList(lrmList))
    if (violations.isNotEmpty()) {
      throw ConstraintViolationException(violations)
    }
  }
}
