package net.flyingfishflash.loremlist.core.response.advice

import jakarta.servlet.http.HttpServletRequest
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import net.flyingfishflash.loremlist.core.exceptions.AbstractApiException
import net.flyingfishflash.loremlist.core.response.structure.ApiProblemDetail
import net.flyingfishflash.loremlist.core.response.structure.ResponseProblem
import net.flyingfishflash.loremlist.toJsonElement
import org.springframework.core.env.Environment
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

/**
 * Handles API Exceptions
 */
@RestControllerAdvice
class ApiExceptionHandler(val environment: Environment) {

  @ExceptionHandler(AbstractApiException::class)
  fun handleAbstractApiException(request: HttpServletRequest, exception: AbstractApiException): ResponseEntity<ResponseProblem?> {
    val includeStacktrace: String = environment.getProperty("server.error.include-stacktrace") ?: "undefined"
    val apiProblemDetail = ApiProblemDetail(
      type = exception.type.toString(),
      title = exception.title,
      status = exception.httpStatus.value(),
      detail = exception.detail,
      extensions = JsonObject(
        mapOf(
          "stacktrace" to if (includeStacktrace == "always") {
            exception.stackTrace.toJsonArray()
          } else {
            JsonPrimitive("disabled")
          },
        ),
      ),
    )
    val applicationResponse = ResponseProblem(apiProblemDetail, exception.responseMessage, request)
    return ResponseEntity<ResponseProblem?>(applicationResponse, exception.httpStatus)
  }

  @ExceptionHandler(Exception::class)
  fun handleException(request: HttpServletRequest, exception: Exception): ResponseEntity<ResponseProblem?> {
    val includeStacktrace: String = environment.getProperty("server.error.include-stacktrace") ?: "undefined"
    val apiProblemDetail = ApiProblemDetail(
      type = "about:config",
      title = "Exception",
      status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
      detail = exception.message ?: "Exception message not present.",
      extensions = JsonObject(
        mapOf(
          "stacktrace" to if (includeStacktrace == "always") {
            exception.stackTrace.toJsonArray()
          } else {
            JsonPrimitive("disabled")
          },
        ),
      ),
    )
    val applicationResponse = ResponseProblem(apiProblemDetail, "There was an error processing the request.", request)
    return ResponseEntity<ResponseProblem?>(applicationResponse, HttpStatus.INTERNAL_SERVER_ERROR)
  }
}

fun Array<StackTraceElement>.toJsonArray(): JsonElement {
  return JsonArray(
    this.map { "${it.className}.${it.methodName} (${it.fileName}:${it.lineNumber})".toJsonElement() },
  )
}
