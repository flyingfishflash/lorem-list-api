package net.flyingfishflash.loremlist.core.exceptions

import kotlinx.serialization.json.JsonElement
import org.springframework.http.HttpStatus
import java.net.URI

open class ApiException(
  cause: Throwable? = null,
  httpStatus: HttpStatus? = null,
  message: String? = null,
  responseMessage: String? = null,
  title: String? = null,
  type: URI? = null,
  val supplemental: Map<String, JsonElement>? = null,
) : RuntimeException(message ?: DEFAULT_MESSAGE, cause) {
  val httpStatus: HttpStatus = httpStatus ?: DEFAULT_HTTP_STATUS
  val responseMessage: String = responseMessage ?: message ?: DEFAULT_MESSAGE
  val title: String = title ?: DEFAULT_TITLE
  val type: URI = type ?: DEFAULT_PROBLEM_TYPE

  companion object {
    const val DEFAULT_TITLE = "ApiException"
    const val DEFAULT_MESSAGE = DEFAULT_TITLE
    val DEFAULT_HTTP_STATUS = HttpStatus.INTERNAL_SERVER_ERROR
    val DEFAULT_PROBLEM_TYPE: URI = URI.create("about:config")
  }
}
