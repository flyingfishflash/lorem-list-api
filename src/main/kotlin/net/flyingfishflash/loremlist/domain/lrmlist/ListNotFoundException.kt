package net.flyingfishflash.loremlist.domain.lrmlist

import net.flyingfishflash.loremlist.core.exceptions.ApiException
import net.flyingfishflash.loremlist.toJsonElement
import org.springframework.http.HttpStatus
import java.util.*

class ListNotFoundException : ApiException {
  constructor(id: UUID, message: String? = null) : this(setOf(id), message = message)

  constructor(idCollection: Set<UUID>, message: String? = null) : super(
    httpStatus = HTTP_STATUS,
    title = "ListNotFoundException",
    message = message ?: defaultMessage(idCollection),
    supplemental = mapOf("notFound" to idCollection.map { it.toString() }.toJsonElement()),
  )

  companion object {
    val HTTP_STATUS = HttpStatus.NOT_FOUND
    fun defaultMessage() = "List could not be found."
    fun defaultMessage(idCollection: Set<UUID>) =
      if (idCollection.size > 1) "Lists (${idCollection.size}) could not be found." else defaultMessage()
  }
}
