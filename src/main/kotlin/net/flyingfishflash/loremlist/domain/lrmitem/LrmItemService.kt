package net.flyingfishflash.loremlist.domain.lrmitem

import net.flyingfishflash.loremlist.domain.ServiceResponse
import net.flyingfishflash.loremlist.domain.lrmitem.data.LrmItemDeleted
import java.util.UUID

interface LrmItemService {
  fun countByOwner(owner: String): ServiceResponse<Long>
  fun deleteByOwner(owner: String): ServiceResponse<LrmItemDeleted>
  fun deleteByOwnerAndId(id: UUID, owner: String, removeListAssociations: Boolean): ServiceResponse<LrmItemDeleted>
  fun findByOwner(owner: String): ServiceResponse<List<LrmItem>>
  fun findByOwnerAndId(id: UUID, owner: String): ServiceResponse<LrmItem>
  fun findByOwnerAndHavingNoListAssociations(owner: String): ServiceResponse<List<LrmItem>>
  fun findByOwnerAndHavingNoListAssociations(owner: String, listId: UUID): ServiceResponse<List<LrmItem>>
  fun patchName(patchedLrmItem: LrmItem)
  fun patchDescription(patchedLrmItem: LrmItem)
}
