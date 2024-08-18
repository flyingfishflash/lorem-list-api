package net.flyingfishflash.loremlist.domain.lrmlist

import kotlinx.datetime.Instant
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import net.flyingfishflash.loremlist.core.serialization.UUIDSerializer
import net.flyingfishflash.loremlist.domain.lrmitem.LrmItem
import net.flyingfishflash.loremlist.domain.lrmlist.data.LrmListSuccinct
import java.util.UUID

@Serializable
data class LrmList
@OptIn(ExperimentalSerializationApi::class)
constructor(
  @Serializable(with = UUIDSerializer::class)
  var id: UUID,
  var name: String,
  var description: String? = null,
  @EncodeDefault var public: Boolean = false,
  var created: Instant? = null,
  var updated: Instant? = null,
  val items: Set<LrmItem>? = null,
)

fun LrmList.succinct() = LrmListSuccinct(id = this.id, name = this.name)
