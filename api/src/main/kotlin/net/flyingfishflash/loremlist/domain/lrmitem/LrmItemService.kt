package net.flyingfishflash.loremlist.domain.lrmitem

import io.github.oshai.kotlinlogging.KotlinLogging
import net.flyingfishflash.loremlist.core.exceptions.ApiException
import net.flyingfishflash.loremlist.domain.lrmitem.data.LrmItem
import net.flyingfishflash.loremlist.domain.lrmitem.data.LrmItemRepository
import net.flyingfishflash.loremlist.domain.lrmitem.data.LrmItemRequest
import net.flyingfishflash.loremlist.domain.lrmlist.ListNotFoundException
import net.flyingfishflash.loremlist.domain.lrmlist.data.LrmListRepository
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.sql.SQLIntegrityConstraintViolationException

@Service
@Transactional
class LrmItemService(val lrmItemRepository: LrmItemRepository, val lrmListRepository: LrmListRepository) {
  private val logger = KotlinLogging.logger {}

  fun addToList(itemId: Long, listId: Long): Pair<String, String> {
    try {
      lrmItemRepository.addItemToList(listId, itemId)
      return Pair(findById(itemId).name, lrmListRepository.findByIdOrNull(listId)!!.name)
    } catch (ex: ExposedSQLException) {
      when (val original = (ex as? ExposedSQLException)?.cause) {
        is SQLIntegrityConstraintViolationException -> {
          logger.error { original.toString() }
          when {
            lrmItemRepository.findByIdOrNull(itemId) == null -> throw ItemAddToListException(
              itemId,
              listId,
              ItemNotFoundException(itemId),
              null,
              "Item id $itemId could not be added to list id $listId because the item couldn't be found",
            )
            lrmListRepository.findByIdOrNull(listId) == null -> throw ItemAddToListException(
              itemId,
              listId,
              ListNotFoundException(listId),
              null,
              "Item id $itemId could not be added to list id $listId because the list couldn't be found",
            )
            original.message?.contains("Unique index or primary key violation") == true ->
              throw ItemAddToListException(
                itemId,
                listId,
                original,
                original.message,
                "Item id $itemId could not be added to list id $listId because it's already been added.",
              )
            else -> {
              throw ItemAddToListException(itemId, listId, original, original.message, "Unanticipated sql integrity constraint violation")
            }
          }
        }
        else -> throw ApiException(HttpStatus.INTERNAL_SERVER_ERROR, null, ex.message.toString(), ex)
      }
    }
  }

  fun create(lrmItemRequest: LrmItemRequest): LrmItem = lrmItemRepository.insert(lrmItemRequest)

  fun deleteSingleById(id: Long) {
    val deletedCount = lrmItemRepository.deleteById(id)
    if (deletedCount < 1) {
      throw ItemDeleteException(
        id = id,
        cause = ItemNotFoundException(id = id),
        responseMessage = "Item id $id was not deleted because it could be found to delete.",
      )
    } else if (deletedCount > 1) {
      // TODO: Ensure the transaction is rolled back if an exception is thrown
      throw ItemDeleteException(
        id = id,
        responseMessage = "More than one item with id $id were found. No items have been deleted.",
      )
    }
  }

  fun findAll(): List<LrmItem> = lrmItemRepository.findAll()

  fun findAllAndLists(): List<LrmItem> = lrmItemRepository.findAllAndLists()

  fun findById(id: Long): LrmItem = lrmItemRepository.findByIdOrNull(id) ?: throw ItemNotFoundException(id)

  fun findByIdAndLists(id: Long): LrmItem = lrmItemRepository.findByIdOrNullAndLists(id) ?: throw ItemNotFoundException(id)

  fun moveToList(itemId: Long, fromListId: Long, toListId: Long) {
    addToList(itemId = itemId, listId = toListId)
    removeFromList(itemId = itemId, listId = fromListId)
  }

  fun removeFromList(itemId: Long, listId: Long) {
    val deletedCount = lrmItemRepository.removeItemFromList(itemId, listId)
    if (deletedCount < 1) {
      if (lrmItemRepository.findByIdOrNull(itemId) == null) {
        throw ItemRemoveFromListException(
          itemId,
          listId,
          ItemNotFoundException(itemId),
          responseMessage = "Item id $itemId could not be removed from list id $listId " +
            "because item id $itemId could not be found",
        )
      } else if (lrmListRepository.findByIdOrNull(listId) == null) {
        throw ItemRemoveFromListException(
          itemId,
          listId,
          ListNotFoundException(listId),
          responseMessage = "Item id $itemId could not be removed from list id $listId " +
            "because list id $listId could not be found",
        )
      } else {
        throw ItemRemoveFromListException(
          itemId,
          listId,
          null,
          "Item id $itemId exists and list id $listId exists but 0 records were deleted.",
          "Item id $itemId is not associated with list id $listId",
        )
      }
    } else if (deletedCount > 1) {
      // TODO: Ensure the transaction is rolled back if an exception is thrown
      throw ItemRemoveFromListException(
        itemId,
        listId,
        null,
        "Delete transaction rolled back because the count of deleted records was > 1.",
        "Item id $itemId is associated with list id $listId multiple times.",
      )
    }
  }
}
