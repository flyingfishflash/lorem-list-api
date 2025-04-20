package net.flyingfishflash.loremlist.core.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import net.flyingfishflash.loremlist.core.exceptions.CoreException
import org.springframework.http.HttpStatus
import java.util.*

object UuidSerializer : KSerializer<UUID> {
  override val descriptor = PrimitiveSerialDescriptor("UUID", PrimitiveKind.STRING)

  override fun deserialize(decoder: Decoder): UUID {
    try {
      return UUID.fromString(decoder.decodeString())
    } catch (ex: IllegalArgumentException) {
      throw CoreException(
        cause = ex,
        httpStatus = HttpStatus.BAD_REQUEST,
        message = "Error deserializing UUID.",
      )
    }
  }

  override fun serialize(encoder: Encoder, value: UUID) {
    encoder.encodeString(value.toString())
  }
}
