package net.flyingfishflash.loremlist.core.exceptions

import org.springframework.http.HttpStatus

class ApiException(httpStatus: HttpStatus, detail: String, cause: Exception) : AbstractApiException(
  httpStatus,
  TITLE,
  detail,
  cause,
) {
  companion object {
    private const val TITLE = "API Exception"
  }
}
