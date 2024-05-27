package net.flyingfishflash.loremlist.domain.association

import net.flyingfishflash.loremlist.core.exceptions.AbstractApiException
import org.springframework.http.HttpStatus
import java.util.UUID

class AssociationNotFoundException(
  id: UUID? = null,
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
    const val TITLE = "Association Not Found"
    val HTTP_STATUS = HttpStatus.NOT_FOUND
    fun defaultMessage(id: UUID?): String {
      return if (id == null) {
        "Association could not be found."
      } else {
        "Association id $id could not be found."
      }
    }
  }
}
