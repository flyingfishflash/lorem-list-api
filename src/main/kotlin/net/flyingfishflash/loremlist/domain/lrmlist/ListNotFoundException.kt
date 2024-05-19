package net.flyingfishflash.loremlist.domain.lrmlist

import net.flyingfishflash.loremlist.core.exceptions.AbstractApiException
import org.springframework.http.HttpStatus

class ListNotFoundException(
  id: Long,
  cause: Throwable? = null,
  message: String? = null,
) : AbstractApiException(
  httpStatus = HttpStatus.NOT_FOUND,
  title = TITLE,
  cause = cause,
  detail = message ?: defaultMessage(id),
  responseMessage = defaultMessage(id),
) {
  companion object {
    const val TITLE = "List Not Found Exception"
    val HTTP_STATUS = HttpStatus.NOT_FOUND
    fun defaultMessage(id: Long) = "List id $id could not be found."
  }
}
