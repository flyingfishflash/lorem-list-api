package net.flyingfishflash.loremlist.api.data.response

import kotlinx.datetime.Instant
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import net.flyingfishflash.loremlist.core.serialization.UuidSerializer
import net.flyingfishflash.loremlist.domain.lrmitem.LrmItem
import net.flyingfishflash.loremlist.domain.lrmlist.LrmListSuccinct
import java.util.UUID

/**
 * Representation of a list item outside the context of a list association
 */
@Serializable
@OptIn(ExperimentalSerializationApi::class)
data class LrmItemResponse(
  @Serializable(with = UuidSerializer::class)
  val id: UUID,
  val name: String,
  val description: String? = null,
  @EncodeDefault val quantity: Int = 0,
  val owner: String,
  val created: Instant,
  val creator: String,
  val updated: Instant,
  val updater: String,
  val lists: Set<LrmListSuccinct>,
) {
  companion object {
    fun fromLrmItem(lrmItem: LrmItem): LrmItemResponse {
      return LrmItemResponse(
        id = lrmItem.id,
        name = lrmItem.name,
        description = lrmItem.description,
        owner = lrmItem.owner,
        created = lrmItem.created,
        creator = lrmItem.creator,
        updated = lrmItem.updated,
        updater = lrmItem.updater,
        lists = lrmItem.lists,
      )
    }
  }
}
