package net.flyingfishflash.loremlist.domain.lrmlist

import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import kotlinx.datetime.Instant
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import net.flyingfishflash.loremlist.core.serialization.UUIDSerializer
import net.flyingfishflash.loremlist.domain.lrmitem.LrmItemEntity
import java.util.*

@Serializable
@OptIn(ExperimentalSerializationApi::class)
data class LrmListEntity(
  @Serializable(with = UUIDSerializer::class)
  val id: UUID,
  @field:Pattern(regexp = "^(?!\\s*$).+", message = "List name must not consist only of whitespace characters.")
  @field:Size(min = 1, max = 64, message = "List name must have at least 1, and no more than 64 characters.")
  val name: String,
  @field:Pattern(regexp = "^(?!\\s*$).+", message = "List description must not consist only of whitespace characters.")
  @field:Size(min = 1, max = 2048, message = "List description must have at least 1, and no more than 2048 characters.")
  val description: String? = null,
  @EncodeDefault val public: Boolean = false,
  val created: Instant,
  val createdBy: String,
  val updated: Instant,
  val updatedBy: String,
  val items: Set<LrmItemEntity>,
) {
  companion object {
    fun fromLrmList(lrmList: LrmList): LrmListEntity {
      return LrmListEntity(
        id = lrmList.id,
        name = lrmList.name,
        description = lrmList.description,
        public = lrmList.public,
        created = lrmList.created,
        createdBy = lrmList.createdBy,
        updated = lrmList.updated,
        updatedBy = lrmList.updatedBy,
        items = lrmList.items.map { LrmItemEntity.fromLrmItem(it) }.toSet(),
      )
    }
  }
}
