package net.flyingfishflash.loremlist.domain.lrmitem

import kotlinx.datetime.Instant
import net.flyingfishflash.loremlist.domain.lrmlist.LrmListSuccinct
import java.util.UUID

class LrmItemEntity private constructor(
  val id: UUID,
  val name: String,
  val description: String? = null,
  val quantity: Int = 0,
  val created: Instant,
  val createdBy: String,
  val updated: Instant,
  val updatedBy: String,
  val lists: Set<LrmListSuccinct>,
) {
  companion object {
    fun fromLrmItem(lrmItem: LrmItem): LrmItemEntity {
      return LrmItemEntity(
        id = lrmItem.id,
        name = lrmItem.name,
        description = lrmItem.description,
//        quantity = lrmItem.quantity,
        created = lrmItem.created,
        createdBy = lrmItem.creator,
        updated = lrmItem.updated,
        updatedBy = lrmItem.updater,
        lists = lrmItem.lists,
      )
    }
  }
}
