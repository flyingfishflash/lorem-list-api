package net.flyingfishflash.loremlist.api.data.request

import kotlinx.serialization.Serializable
import net.flyingfishflash.loremlist.core.serialization.UuidSerializer
import net.flyingfishflash.loremlist.core.validation.ValidUuidSet
import java.util.*

@Serializable
data class LrmListItemAddRequest(
  @field:ValidUuidSet(allowEmpty = false)
  val itemIdCollection: Set<
    @Serializable(UuidSerializer::class)
    UUID,
    >,
)
