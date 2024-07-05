package net.flyingfishflash.loremlist.domain.lrmitem

import net.flyingfishflash.loremlist.core.exceptions.ApiException
import org.springframework.http.HttpStatus
import java.util.UUID

class ItemNotFoundException(uuid: UUID, message: String? = null) : ApiException(
  httpStatus = HTTP_STATUS,
  title = "ItemNotFoundException",
  message = message ?: defaultMessage(uuid),
) {

  companion object {
    val HTTP_STATUS = HttpStatus.NOT_FOUND
    fun defaultMessage(uuid: UUID) = "Item id $uuid could not be found."
  }
}
