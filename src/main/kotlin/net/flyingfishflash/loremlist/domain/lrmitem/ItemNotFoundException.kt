package net.flyingfishflash.loremlist.domain.lrmitem

import net.flyingfishflash.loremlist.core.exceptions.AbstractApiException
import org.springframework.http.HttpStatus

class ItemNotFoundException(
  id: Long,
  cause: Throwable? = null,
  message: String? = null,
) : AbstractApiException(
  httpStatus = HTTP_STATUS,
  title = TITLE,
  cause = cause,
  detail = message ?: defaultMessage(id),
  responseMessage = defaultMessage(id),
) {
  companion object {
    const val TITLE = "Item Not Found Exception"
    val HTTP_STATUS = HttpStatus.NOT_FOUND
    fun defaultMessage(id: Long) = "Item id $id could not be found."
  }
}
