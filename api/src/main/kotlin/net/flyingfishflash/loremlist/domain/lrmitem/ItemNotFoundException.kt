package net.flyingfishflash.loremlist.domain.lrmitem

import net.flyingfishflash.loremlist.core.exceptions.AbstractApiException
import org.springframework.http.HttpStatus

class ItemNotFoundException(
  id: Long,
  cause: Throwable? = null,
  message: String? = null,
  responseMessage: String? = null,
) : AbstractApiException(
  cause = cause,
  httpStatus = HttpStatus.NOT_FOUND,
  detail = message ?: defaultMessage(id),
  responseMessage = responseMessage ?: defaultMessage(id),
  title = TITLE,
) {
  companion object {
    const val TITLE = "Item Not Found Exception"
    fun defaultMessage(id: Long) = "Item id $id could not be found."
  }
}
