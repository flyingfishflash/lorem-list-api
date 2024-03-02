package net.flyingfishflash.loremlist.core.exceptions

import org.springframework.http.HttpStatus

class ApiException(httpStatus: HttpStatus, detail: String, cause: Throwable) : AbstractApiException(
  httpStatus = httpStatus,
  title = TITLE,
  detail = detail,
  cause = cause,
) {

  companion object {
    private const val TITLE = "API Exception"
  }
}
