package net.flyingfishflash.loremlist.persistence

import net.flyingfishflash.loremlist.domain.association.AssociationRepository
import net.flyingfishflash.loremlist.domain.lrmitem.LrmItemSuccinct
import net.flyingfishflash.loremlist.domain.lrmlist.LrmListSuccinct
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.deleteAll
import org.springframework.stereotype.Repository
import java.util.UUID

data class SuccinctLrmComponentPair(val list: LrmListSuccinct, val item: LrmItemSuccinct)

@Repository
class AssociationRepositoryRdbms : AssociationRepository {
  private val repositoryTable = LrmListItemsTable

  override fun listIsConsistent(listId: UUID): Boolean {
    val unEqualListAndItemOwner = (LrmListTable innerJoin LrmItemTable)
      .select(LrmListTable.id)
      .where { LrmListTable.id eq listId }
      .andWhere { LrmListTable.owner neq LrmItemTable.owner }.toList()
    return unEqualListAndItemOwner.isEmpty()
  }

  override fun delete(): Int {
    return repositoryTable.deleteAll()
  }
}
