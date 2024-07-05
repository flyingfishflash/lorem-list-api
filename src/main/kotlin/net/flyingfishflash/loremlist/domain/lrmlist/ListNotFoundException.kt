package net.flyingfishflash.loremlist.domain.lrmlist

import net.flyingfishflash.loremlist.core.exceptions.ApiException
import org.springframework.http.HttpStatus
import java.util.*

class ListNotFoundException(uuid: UUID, message: String? = null) : ApiException(
  httpStatus = HTTP_STATUS,
  title = "ListNotFoundException",
  message = message ?: defaultMessage(uuid),
) {

  companion object {
    val HTTP_STATUS = HttpStatus.NOT_FOUND
    fun defaultMessage(uuid: UUID) = "List id $uuid could not be found."
  }
}
