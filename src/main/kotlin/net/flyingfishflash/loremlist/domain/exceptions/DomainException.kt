package net.flyingfishflash.loremlist.domain.exceptions

import kotlinx.serialization.json.JsonElement
import net.flyingfishflash.loremlist.core.exceptions.CoreException
import org.springframework.http.HttpStatus
import java.net.URI

open class DomainException(
  cause: Throwable? = null,
  httpStatus: HttpStatus? = null,
  message: String? = null,
  responseMessage: String? = null,
  title: String? = null,
  type: URI? = null,
  supplemental: Map<String, JsonElement>? = null,
) : CoreException(
  cause = cause,
  httpStatus = httpStatus,
  message = message ?: DEFAULT_MESSAGE,
  responseMessage = responseMessage,
  title = title ?: DEFAULT_TITLE,
  type = type,
  supplemental = supplemental,
) {
  companion object {
    const val DEFAULT_TITLE = "DomainException"
    const val DEFAULT_MESSAGE = DEFAULT_TITLE
  }
}
