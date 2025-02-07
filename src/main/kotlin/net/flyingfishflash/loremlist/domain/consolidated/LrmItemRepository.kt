package net.flyingfishflash.loremlist.domain.consolidated

import net.flyingfishflash.loremlist.domain.lrmitem.LrmItem
import net.flyingfishflash.loremlist.domain.lrmitem.data.LrmItemRequest
import java.util.*

interface LrmItemRepository {
  fun countByOwner(owner: String): Long
  fun delete(): Int
  fun deleteById(ids: Set<UUID>): Int
  fun deleteByOwnerAndId(id: UUID, owner: String): Int
  fun findByOwner(owner: String): List<LrmItem>
  fun findByOwnerIncludeLists(owner: String): List<LrmItem>
  fun findByOwnerAndIdOrNull(id: UUID, owner: String): LrmItem?
  fun findByOwnerAndIdOrNullIncludeLists(id: UUID, owner: String): LrmItem?
  fun findByOwnerAndHavingNoListAssociations(owner: String): List<LrmItem>
  fun findIdsByOwnerAndIds(itemIdCollection: List<UUID>, owner: String): List<UUID>
  fun notFoundByOwnerAndId(itemIdCollection: List<UUID>, owner: String): Set<UUID>
  fun insert(lrmItemRequest: LrmItemRequest, owner: String): UUID
  fun update(lrmItem: LrmItem): Int
}
