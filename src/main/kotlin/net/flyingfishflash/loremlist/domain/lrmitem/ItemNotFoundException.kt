package net.flyingfishflash.loremlist.domain.lrmitem

import net.flyingfishflash.loremlist.core.exceptions.ApiException
import org.springframework.http.HttpStatus

class ItemNotFoundException(
  id: Long,
  message: String? = null,
) : ApiException(
  httpStatus = HTTP_STATUS,
  title = "ItemNotFoundException",
  message = message ?: defaultMessage(id),
) {
  companion object {
    val HTTP_STATUS = HttpStatus.NOT_FOUND
    fun defaultMessage(id: Long) = "Item id $id could not be found."
  }
}
