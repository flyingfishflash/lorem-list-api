package net.flyingfishflash.loremlist.domain.lrmlist

import kotlinx.datetime.Instant
import java.util.*

class LrmListEntity private constructor(
  val id: UUID,
  val name: String,
  val description: String? = null,
  val public: Boolean = false,
  val created: Instant,
  val createdBy: String,
  val updated: Instant,
  val updatedBy: String,
  val items: Set<UUID>,
) {
  companion object {
    fun fromLrmList(lrmList: LrmList): LrmListEntity {
      return LrmListEntity(
        id = lrmList.id,
        name = lrmList.name,
        description = lrmList.description,
        public = lrmList.public,
        created = lrmList.created,
        createdBy = lrmList.creator,
        updated = lrmList.updated,
        updatedBy = lrmList.updater,
        items = lrmList.items.map { it.id }.toSet(),
      )
    }
  }
}
