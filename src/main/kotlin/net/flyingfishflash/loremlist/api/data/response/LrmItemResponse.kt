package net.flyingfishflash.loremlist.api.data.response

import kotlinx.datetime.Instant
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import net.flyingfishflash.loremlist.core.serialization.UUIDSerializer
import net.flyingfishflash.loremlist.domain.lrmitem.LrmItem
import net.flyingfishflash.loremlist.domain.lrmlist.LrmListSuccinct
import java.util.UUID

@Serializable
@OptIn(ExperimentalSerializationApi::class)
data class LrmItemResponse(
  @Serializable(with = UUIDSerializer::class)
  val id: UUID,
  val name: String,
  val description: String? = null,
  @EncodeDefault val quantity: Int = 0,
  val created: Instant,
  val createdBy: String,
  val updated: Instant,
  val updatedBy: String,
  val lists: Set<LrmListSuccinct>,
) {
  companion object {
    fun fromLrmItem(lrmItem: LrmItem): LrmItemResponse {
      return LrmItemResponse(
        id = lrmItem.id,
        name = lrmItem.name,
        description = lrmItem.description,
        quantity = lrmItem.quantity,
        created = lrmItem.created,
        createdBy = lrmItem.createdBy,
        updated = lrmItem.updated,
        updatedBy = lrmItem.updatedBy,
        lists = lrmItem.lists,
      )
    }
  }
}
