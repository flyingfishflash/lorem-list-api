package net.flyingfishflash.loremlist.domain.lrmitem

import net.flyingfishflash.loremlist.domain.lrmitem.data.LrmItem
import net.flyingfishflash.loremlist.domain.lrmitem.data.LrmItemRepository
import net.flyingfishflash.loremlist.domain.lrmitem.data.LrmItemRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class LrmItemService(val lrmItemRepository: LrmItemRepository) {
  fun create(lrmItemRequest: LrmItemRequest): LrmItem = lrmItemRepository.insert(lrmItemRequest)

  fun deleteSingleById(id: Long) {
    val deletedCount = lrmItemRepository.deleteById(id)
    if (deletedCount < 1) {
      throw ItemDeleteException(cause = ItemNotFoundException())
    } else if (deletedCount > 1) {
      // TODO: The exception should capture a message that more than one record was deleted
      // TODO: Ensure the transaction is rolled back if an exception is thrown
      throw ItemDeleteException()
    }
  }

  fun findAll(): List<LrmItem> = lrmItemRepository.findAll()

  fun assignItemToList(
    listId: Long,
    itemId: Long,
  ) = lrmItemRepository.addItemToList(listId, itemId)
}
