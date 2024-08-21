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
@OptIn(ExperimentalSerializationApi::class)
data class LrmList
constructor(
  @Serializable(with = UUIDSerializer::class)
  var id: UUID,
  var name: String,
  var description: String? = null,
  @EncodeDefault var public: Boolean = false,
  var created: Instant? = null,
  var createdBy: String? = null,
  var updated: Instant? = null,
  var updatedBy: String? = null,
  val items: Set<LrmItem>? = null,
)

fun LrmList.succinct() = LrmListSuccinct(id = this.id, name = this.name)
