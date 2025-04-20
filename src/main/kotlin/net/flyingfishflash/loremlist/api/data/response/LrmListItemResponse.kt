package net.flyingfishflash.loremlist.api.data.response

import kotlinx.datetime.Instant
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import net.flyingfishflash.loremlist.core.serialization.UuidSerializer
import net.flyingfishflash.loremlist.domain.lrmlist.LrmListSuccinct
import net.flyingfishflash.loremlist.domain.lrmlistitem.LrmListItem
import java.util.UUID

/**
 * Representation of a list item in the context of a list
 */
@Serializable
@OptIn(ExperimentalSerializationApi::class)
data class LrmListItemResponse(
  @Serializable(with = UuidSerializer::class)
  val id: UUID,
  val name: String,
  val description: String? = null,
  @EncodeDefault val quantity: Int = 0,
  val isSuppressed: Boolean,
  val owner: String,
  val created: Instant,
  val creator: String,
  val updated: Instant,
  val updater: String,
  val lists: Set<LrmListSuccinct>,
) {
  companion object {
    fun fromLrmListItem(lrmListItem: LrmListItem): LrmListItemResponse {
      return LrmListItemResponse(
        id = lrmListItem.id,
        name = lrmListItem.name,
        description = lrmListItem.description,
        quantity = lrmListItem.quantity,
        isSuppressed = lrmListItem.isSuppressed,
        owner = lrmListItem.owner,
        created = lrmListItem.created,
        creator = lrmListItem.creator,
        updated = lrmListItem.updated,
        updater = lrmListItem.updater,
        lists = lrmListItem.lists,
      )
    }
  }
}
