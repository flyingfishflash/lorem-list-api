package net.flyingfishflash.loremlist.domain.lrmitem

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import kotlinx.datetime.Instant
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import net.flyingfishflash.loremlist.core.serialization.UUIDSerializer
import net.flyingfishflash.loremlist.domain.lrmlist.LrmListSuccinct
import java.util.UUID

@Serializable
@OptIn(ExperimentalSerializationApi::class)
data class LrmItemEntity(
  @Serializable(with = UUIDSerializer::class)
  val id: UUID,
  @field:Pattern(regexp = "^(?!\\s*$).+", message = "Item name must not consist only of whitespace characters.")
  @field:Size(min = 1, max = 64, message = "Item name must have at least 1, and no more than 64 characters.")
  val name: String,
  @field:Pattern(regexp = "^(?!\\s*$).+", message = "Item description must not consist only of whitespace characters.")
  @field:Size(min = 1, max = 2048, message = "Item description must have at least 1, and no more than 2048 characters.")
  val description: String? = null,
  @field:Min(value = 0, message = "Item quantity must be zero or greater.")
  @EncodeDefault val quantity: Int = 0,
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
        quantity = lrmItem.quantity,
        created = lrmItem.created,
        createdBy = lrmItem.creator,
        updated = lrmItem.updated,
        updatedBy = lrmItem.updater,
        lists = lrmItem.lists,
      )
    }
  }
}
