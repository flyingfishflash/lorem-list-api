package net.flyingfishflash.loremlist.domain.lrmitem.data

import kotlinx.datetime.Instant
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import net.flyingfishflash.loremlist.core.serialization.UUIDSerializer
import net.flyingfishflash.loremlist.domain.lrmlist.data.LrmListSuccinct
import java.util.UUID

@Serializable
@OptIn(ExperimentalSerializationApi::class)
data class LrmItem(
  var id: Long,
  @Serializable(with = UUIDSerializer::class)
  var uuid: UUID? = null,
  var created: Instant? = null,
  var name: String,
  var description: String? = null,
  @EncodeDefault var quantity: Long = 0,
  val lists: Set<LrmListSuccinct>? = null,
)
