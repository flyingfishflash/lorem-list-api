package net.flyingfishflash.loremlist.domain.consolidated

import net.flyingfishflash.loremlist.domain.lrmlist.LrmList
import net.flyingfishflash.loremlist.domain.lrmlist.data.LrmListCreateRequest
import java.util.*

interface LrmListRepository {
  fun countByOwner(owner: String): Long
  fun delete(): Int
  fun deleteById(ids: Set<UUID>): Int
  fun deleteByOwnerAndId(id: UUID, owner: String): Int
  fun findByOwner(owner: String): List<LrmList>
  fun findByOwnerIncludeItems(owner: String): List<LrmList>
  fun findByOwnerAndIdOrNull(id: UUID, owner: String): LrmList?
  fun findByOwnerAndIdOrNullIncludeItems(id: UUID, owner: String): LrmList?
  fun findByOwnerAndHavingNoItemAssociations(owner: String): List<LrmList>
  fun findByPublic(): List<LrmList>
  fun findByPublicIncludeItems(): List<LrmList>
  fun findIdsByOwnerAndIds(listIdCollection: List<UUID>, owner: String): List<UUID>
  fun notFoundByOwnerAndId(listIdCollection: List<UUID>, owner: String): Set<UUID>
  fun insert(lrmListCreateRequest: LrmListCreateRequest, subject: String): UUID
  fun update(lrmList: LrmList): Int
}
