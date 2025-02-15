package net.flyingfishflash.loremlist.domain.lrmlist

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.flyingfishflash.loremlist.core.serialization.UUIDSerializer
import net.flyingfishflash.loremlist.domain.SuccinctLrmComponent
import java.util.UUID

@Serializable
@SerialName("list")
data class LrmListSuccinct(
  @Serializable(with = UUIDSerializer::class)
  override val id: UUID,
  override val name: String,
) : SuccinctLrmComponent {
  companion object {
    fun fromLrmList(lrmList: LrmList): LrmListSuccinct {
      return LrmListSuccinct(id = lrmList.id, name = lrmList.name)
    }
  }
}
