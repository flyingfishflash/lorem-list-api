package net.flyingfishflash.loremlist.domain.lrmitem

import kotlinx.datetime.Instant
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import net.flyingfishflash.loremlist.core.serialization.UUIDSerializer
import net.flyingfishflash.loremlist.domain.lrmitem.data.LrmItemSuccinct
import net.flyingfishflash.loremlist.domain.lrmlist.data.LrmListSuccinct
import java.util.UUID

@Serializable
@OptIn(ExperimentalSerializationApi::class)
data class LrmItem(
  @Serializable(with = UUIDSerializer::class)
  var id: UUID,
  var name: String,
  var description: String? = null,
  @EncodeDefault var quantity: Int = 0,
  var created: Instant? = null,
  var createdBy: String? = null,
  var updated: Instant? = null,
  var updatedBy: String? = null,
  val lists: Set<LrmListSuccinct>? = null,
)

fun LrmItem.succinct() = LrmItemSuccinct(id = this.id, name = this.name)
