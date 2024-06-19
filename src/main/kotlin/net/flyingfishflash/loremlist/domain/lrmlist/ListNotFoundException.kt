package net.flyingfishflash.loremlist.domain.lrmlist

import net.flyingfishflash.loremlist.core.exceptions.ApiException
import org.springframework.http.HttpStatus

class ListNotFoundException(
  id: Long,
  message: String? = null,
) : ApiException(
  httpStatus = HttpStatus.NOT_FOUND,
  title = "ListNotFoundException",
  message = message ?: defaultMessage(id),
) {
  companion object {
    val HTTP_STATUS = HttpStatus.NOT_FOUND
    fun defaultMessage(id: Long) = "List id $id could not be found."
  }
}
