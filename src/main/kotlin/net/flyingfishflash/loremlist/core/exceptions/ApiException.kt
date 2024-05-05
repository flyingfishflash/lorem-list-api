package net.flyingfishflash.loremlist.core.exceptions

import org.springframework.http.HttpStatus

class ApiException(
  httpStatus: HttpStatus? = null,
  title: String? = null,
  cause: Throwable? = null,
  message: String? = null,
  responseMessage: String? = null,
) : AbstractApiException(
  httpStatus = httpStatus ?: HttpStatus.INTERNAL_SERVER_ERROR,
  title = title ?: TITLE,
  cause = cause,
  detail = message ?: buildMessage(cause),
  responseMessage = responseMessage ?: buildMessage(cause),
) {
  companion object {
    const val TITLE = "API Exception"
    fun buildMessage(cause: Throwable?): String {
      val default = TITLE
      return when (cause) {
        is AbstractApiException -> "${cause.message}"
        else -> default
      }
    }
  }
}
