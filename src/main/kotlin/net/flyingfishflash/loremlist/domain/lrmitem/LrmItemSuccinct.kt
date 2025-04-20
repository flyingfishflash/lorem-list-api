package net.flyingfishflash.loremlist.domain.lrmitem

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.flyingfishflash.loremlist.core.serialization.UuidSerializer
import net.flyingfishflash.loremlist.domain.SuccinctLrmComponent
import java.util.UUID

@Serializable
@SerialName("item")
data class LrmItemSuccinct(
  @Serializable(with = UuidSerializer::class)
  override val id: UUID,
  override val name: String,
) : SuccinctLrmComponent {
  companion object {
    fun fromLrmItem(lrmItem: LrmItem): LrmItemSuccinct {
      return LrmItemSuccinct(id = lrmItem.id, name = lrmItem.name)
    }
  }
}
