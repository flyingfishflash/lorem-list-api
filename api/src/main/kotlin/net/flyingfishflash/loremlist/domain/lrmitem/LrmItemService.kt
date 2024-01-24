package net.flyingfishflash.loremlist.domain.lrmitem

import net.flyingfishflash.loremlist.domain.lrmitem.data.LrmItem
import net.flyingfishflash.loremlist.domain.lrmitem.data.LrmItemRepository
import net.flyingfishflash.loremlist.domain.lrmitem.data.LrmItemRequest
import net.flyingfishflash.loremlist.domain.lrmlist.ListNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class LrmItemService(val lrmItemRepository: LrmItemRepository) {
  fun create(lrmItemRequest: LrmItemRequest): LrmItem = lrmItemRepository.insert(lrmItemRequest)

  fun deleteById(id: Long) {
    if (lrmItemRepository.deleteById(id) < 1) throw ListNotFoundException()
  }

  fun findAll(): MutableList<LrmItem> = lrmItemRepository.findAll()

  fun assignItemToList(
    listId: Long,
    itemId: Long,
  ) = lrmItemRepository.addItemToList(listId, itemId)
}
