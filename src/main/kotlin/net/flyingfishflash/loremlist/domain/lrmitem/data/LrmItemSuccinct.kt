package net.flyingfishflash.loremlist.domain.lrmitem.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.flyingfishflash.loremlist.core.serialization.UUIDSerializer
import net.flyingfishflash.loremlist.domain.SuccinctLrmComponent
import java.util.UUID

@Serializable
@SerialName("item")
data class LrmItemSuccinct(
  @Serializable(with = UUIDSerializer::class)
  override var uuid: UUID,
  override val name: String,
) : SuccinctLrmComponent
