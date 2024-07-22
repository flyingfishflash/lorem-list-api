package net.flyingfishflash.loremlist.domain.lrmitem

import net.flyingfishflash.loremlist.core.exceptions.ApiException
import net.flyingfishflash.loremlist.toJsonElement
import org.springframework.http.HttpStatus
import java.util.UUID

class ItemNotFoundException : ApiException {
  constructor(id: UUID, message: String? = null) : this(setOf(id), message = message)

  constructor(idCollection: Set<UUID>, message: String? = null) : super(
    httpStatus = HTTP_STATUS,
    title = "ItemNotFoundException",
    message = message ?: defaultMessage(idCollection),
    supplemental = mapOf("notFound" to idCollection.map { it.toString() }.toJsonElement()),
  )

  companion object {
    val HTTP_STATUS = HttpStatus.NOT_FOUND
    fun defaultMessage() = "Item could not be found."
    fun defaultMessage(idCollection: Set<UUID>) =
      if (idCollection.size > 1) "Items (${idCollection.size}) could not be found." else defaultMessage()
  }
}
