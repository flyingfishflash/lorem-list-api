package net.flyingfishflash.loremlist.domain.lrmitem

import net.flyingfishflash.loremlist.core.exceptions.ApiException
import net.flyingfishflash.loremlist.toJsonElement
import org.springframework.http.HttpStatus
import java.util.UUID

class ItemNotFoundException : ApiException {
  constructor(uuid: UUID, message: String? = null) : this(setOf(uuid), message = message)

  constructor(uuidCollection: Set<UUID>, message: String? = null) : super(
    httpStatus = HTTP_STATUS,
    title = "ItemNotFoundException",
    message = message ?: defaultMessage(uuidCollection),
    supplemental = mapOf("notFound" to uuidCollection.map { it.toString() }.toJsonElement()),
  )

  companion object {
    val HTTP_STATUS = HttpStatus.NOT_FOUND
    fun defaultMessage() = "Item could not be found."
    fun defaultMessage(uuidCollection: Set<UUID>) =
      if (uuidCollection.size > 1) "Items (${uuidCollection.size}) could not be found." else defaultMessage()
  }
}
