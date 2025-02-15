package net.flyingfishflash.loremlist.api.data.response

import kotlinx.datetime.Instant
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import net.flyingfishflash.loremlist.core.serialization.UUIDSerializer
import net.flyingfishflash.loremlist.domain.lrmlist.LrmList
import java.util.UUID

@Serializable
@OptIn(ExperimentalSerializationApi::class)
data class LrmListResponse(
  @Serializable(with = UUIDSerializer::class)
  val id: UUID,
  val name: String,
  val description: String? = null,
  @EncodeDefault val public: Boolean = false,
  val created: Instant? = null,
  val createdBy: String? = null,
  val updated: Instant? = null,
  val updatedBy: String? = null,
  val items: Set<LrmItemResponse>,
) {
  companion object {
    fun fromLrmList(lrmList: LrmList): LrmListResponse {
      return LrmListResponse(
        id = lrmList.id,
        name = lrmList.name,
        description = lrmList.description,
        public = lrmList.public,
        created = lrmList.created,
        createdBy = lrmList.createdBy,
        updated = lrmList.updated,
        updatedBy = lrmList.updatedBy,
        // TODO: address warning about useless null-safe operator
        items = lrmList.items?.map { LrmItemResponse.fromLrmItem(it) }?.toSet() ?: emptySet(),
      )
    }
  }
}
