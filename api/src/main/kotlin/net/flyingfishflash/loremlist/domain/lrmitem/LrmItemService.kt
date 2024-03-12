package net.flyingfishflash.loremlist.domain.lrmitem

import net.flyingfishflash.loremlist.domain.lrmitem.data.LrmItem
import net.flyingfishflash.loremlist.domain.lrmitem.data.LrmItemRepository
import net.flyingfishflash.loremlist.domain.lrmitem.data.LrmItemRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class LrmItemService(val lrmItemRepository: LrmItemRepository) {
  fun assignToList(itemId: Long, listId: Long) = lrmItemRepository.addItemToList(listId, itemId)

  fun copyToList(itemId: Long, listId: Long) = assignToList(itemId = itemId, listId = listId)

  fun create(lrmItemRequest: LrmItemRequest): LrmItem = lrmItemRepository.insert(lrmItemRequest)

  fun deleteSingleById(id: Long) {
    val deletedCount = lrmItemRepository.deleteById(id)
    if (deletedCount < 1) {
      throw ItemDeleteException(id = id, cause = ItemNotFoundException(id = id))
    } else if (deletedCount > 1) {
      // TODO: The exception should capture a message that more than one record was deleted
      // TODO: Ensure the transaction is rolled back if an exception is thrown
      throw ItemDeleteException(id = id)
    }
  }

  fun findAll(): List<LrmItem> = lrmItemRepository.findAll()

  fun findAllAndLists(): List<LrmItem> = lrmItemRepository.findAllAndLists()

  fun moveToList(itemId: Long, sourceListId: Long, destListId: Long) {
    copyToList(itemId = itemId, listId = destListId)
    removeFromList(itemId = itemId, listId = sourceListId)
  }

  fun removeFromList(itemId: Long, listId: Long): Nothing = TODO()
}
