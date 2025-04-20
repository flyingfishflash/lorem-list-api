package net.flyingfishflash.loremlist.domain.association

import java.util.UUID

interface AssociationRepository {
  fun listIsConsistent(listId: UUID): Boolean
  fun delete(): Int
}
