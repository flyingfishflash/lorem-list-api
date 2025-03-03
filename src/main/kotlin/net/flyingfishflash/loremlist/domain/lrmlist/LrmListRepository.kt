package net.flyingfishflash.loremlist.domain.lrmlist

import java.util.UUID

interface LrmListRepository {
  fun countByOwner(owner: String): Long
  fun delete(): Int
  fun deleteById(ids: Set<UUID>): Int
  fun deleteByOwnerAndId(id: UUID, owner: String): Int
  fun findByOwner(owner: String): List<LrmList>
  fun findByOwnerAndIdOrNull(id: UUID, owner: String): LrmList?
  fun findByOwnerAndHavingNoItemAssociations(owner: String): List<LrmList>
  fun findByPublic(): List<LrmList>
  fun findIdsByOwnerAndIds(listIdCollection: List<UUID>, owner: String): List<UUID>
  fun notFoundByOwnerAndId(listIdCollection: List<UUID>, owner: String): Set<UUID>
  fun insert(lrmList: LrmList, subject: String): UUID
  fun update(lrmList: LrmList): Int
  fun updateName(lrmList: LrmList): Int
  fun updateDescription(lrmList: LrmList): Int
  fun updateIsPublic(lrmList: LrmList): Int
}
