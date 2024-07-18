package net.flyingfishflash.loremlist.domain.lrmlist

import net.flyingfishflash.loremlist.core.exceptions.ApiException
import net.flyingfishflash.loremlist.toJsonElement
import org.springframework.http.HttpStatus
import java.util.*

class ListNotFoundException : ApiException {
  constructor(uuid: UUID, message: String? = null) : this(setOf(uuid), message = message)

  constructor(uuidCollection: Set<UUID>, message: String? = null) : super(
    httpStatus = HTTP_STATUS,
    title = "ListNotFoundException",
    message = message ?: defaultMessage(uuidCollection),
    supplemental = mapOf("notFound" to uuidCollection.map { it.toString() }.toJsonElement()),
  )

  companion object {
    val HTTP_STATUS = HttpStatus.NOT_FOUND
    fun defaultMessage() = "List could not be found."
    fun defaultMessage(uuidCollection: Set<UUID>) =
      if (uuidCollection.size > 1) "Lists (${uuidCollection.size}) could not be found." else defaultMessage()
  }
}
