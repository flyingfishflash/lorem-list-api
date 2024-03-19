package net.flyingfishflash.loremlist.core.exceptions

import org.springframework.http.HttpStatus

class ApiException(httpStatus: HttpStatus, title: String?, detail: String, cause: Throwable) : AbstractApiException(
  httpStatus = httpStatus,
  title = title ?: TITLE,
  message = detail,
  cause = cause,
  responseMessage = detail,
) {

  companion object {
    private const val TITLE = "API Exception"
  }
}
