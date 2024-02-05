package net.flyingfishflash.loremlist.core.exceptions

import org.springframework.http.HttpStatusCode
import org.springframework.http.ProblemDetail
import org.springframework.web.ErrorResponseException

abstract class AbstractApiException protected constructor(
  httpStatusCode: HttpStatusCode,
  title: String,
  detail: String,
  cause: Throwable?,
) : ErrorResponseException(
  httpStatusCode,
  ProblemDetail.forStatusAndDetail(httpStatusCode, detail),
  cause,
) {
  init {
    super.setTitle(title)
  }
}
