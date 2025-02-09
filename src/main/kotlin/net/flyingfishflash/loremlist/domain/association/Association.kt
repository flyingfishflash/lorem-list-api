package net.flyingfishflash.loremlist.domain.association

import java.util.UUID

data class Association(val id: UUID, val itemId: UUID, val listId: UUID)
