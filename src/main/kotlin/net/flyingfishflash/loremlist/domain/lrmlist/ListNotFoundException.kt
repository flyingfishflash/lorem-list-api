package net.flyingfishflash.loremlist.domain.lrmlist

import net.flyingfishflash.loremlist.domain.LrmComponentType
import net.flyingfishflash.loremlist.domain.exceptions.EntityNotFoundException
import java.util.UUID

class ListNotFoundException(idCollection: Set<UUID> = emptySet(), message: String? = null) :
  EntityNotFoundException(
    idCollection = idCollection,
    message = message,
    lrmComponentType = LrmComponentType.List,
  ) {
  constructor(id: UUID, message: String? = null) : this(setOf(id), message = message)
}
