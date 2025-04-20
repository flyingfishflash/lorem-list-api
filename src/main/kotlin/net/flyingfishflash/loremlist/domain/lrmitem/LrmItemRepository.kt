package net.flyingfishflash.loremlist.domain.lrmitem

import java.util.UUID

interface LrmItemRepository {
  fun countByOwner(owner: String): Long
  fun delete(): Int
  fun deleteById(ids: Set<UUID>): Int
  fun deleteByOwnerAndId(id: UUID, owner: String): Int
  fun findByOwner(owner: String): List<LrmItem>
  fun findByOwnerAndIdOrNull(id: UUID, owner: String): LrmItem?
  fun findByOwnerAndHavingNoListAssociations(owner: String): List<LrmItem>
  fun findIdsByOwnerAndIds(itemIdCollection: List<UUID>, owner: String): List<UUID>
  fun notFoundByOwnerAndId(itemIdCollection: List<UUID>, owner: String): Set<UUID>
  fun insert(lrmItem: LrmItem): UUID
  fun update(lrmItem: LrmItem): Int
  fun updateName(lrmItem: LrmItem): Int
  fun updateDescription(lrmItem: LrmItem): Int
}
