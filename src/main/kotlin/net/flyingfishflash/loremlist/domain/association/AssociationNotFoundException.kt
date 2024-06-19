package net.flyingfishflash.loremlist.domain.association

import net.flyingfishflash.loremlist.core.exceptions.ApiException
import org.springframework.http.HttpStatus
import java.util.UUID

class AssociationNotFoundException(
  id: UUID? = null,
  message: String? = null,
) : ApiException(
  httpStatus = HTTP_STATUS,
  title = "AssociationNotFoundException",
  message = message ?: defaultMessage(id),
) {
  companion object {
    val HTTP_STATUS = HttpStatus.NOT_FOUND
    fun defaultMessage(id: UUID?): String {
      return if (id == null) {
        "Association not found."
      } else {
        "Association id $id not found."
      }
    }
  }
}
