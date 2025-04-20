package net.flyingfishflash.loremlist.domain.lrmlist

import net.flyingfishflash.loremlist.domain.ServiceResponse
import net.flyingfishflash.loremlist.domain.lrmitem.LrmItem
import net.flyingfishflash.loremlist.domain.lrmlist.data.LrmListCreate
import net.flyingfishflash.loremlist.domain.lrmlist.data.LrmListDeleted
import java.util.UUID

interface LrmListService {
  fun countByOwner(owner: String): ServiceResponse<Long>
  fun create(lrmListCreate: LrmListCreate, creator: String): ServiceResponse<LrmList>
  fun deleteByOwner(owner: String): ServiceResponse<LrmListDeleted>
  fun deleteByOwnerAndId(id: UUID, owner: String, removeItemAssociations: Boolean): ServiceResponse<LrmListDeleted>
  fun findEligibleItemsByOwner(id: UUID, owner: String): ServiceResponse<List<LrmItem>>
  fun findByOwner(owner: String): ServiceResponse<List<LrmList>>
  fun findByOwnerAndId(id: UUID, owner: String): ServiceResponse<LrmList>
  fun findByOwnerAndHavingNoItemAssociations(owner: String): ServiceResponse<List<LrmList>>
  fun findByPublic(): ServiceResponse<List<LrmList>>
  fun patchName(patchedLrmList: LrmList)
  fun patchDescription(patchedLrmList: LrmList)
  fun patchIsPublic(patchedLrmList: LrmList)
}
