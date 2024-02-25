package net.flyingfishflash.loremlist.core.response.advice

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.http.HttpServletRequest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.flyingfishflash.loremlist.core.exceptions.AbstractApiException
import net.flyingfishflash.loremlist.core.response.structure.ApiProblemDetail
import net.flyingfishflash.loremlist.core.response.structure.ResponseProblem
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

/**
 * Handles API Exceptions
 *
 * Wraps the problem detail in a {@link ApiProblemResponse}
 *
 * Note that if spring.mvs.problemdetails.enabled is true, then this handler will never be invoked.
 * Spring's internal exception handler will be used instead. This causes the selected message converter
 * to be Jackson instead of kotlin-serialization, which in turn causes the ApiProblemResponse to be
 * rendered a little differently.
 */
@RestControllerAdvice
class ApiExceptionHandler {
  private val logger = KotlinLogging.logger {}

  @ExceptionHandler(AbstractApiException::class)
  fun handleAbstractApiException(request: HttpServletRequest, exception: AbstractApiException): ResponseEntity<ResponseProblem?> {
    val apiProblemDetail = ApiProblemDetail(
      type = exception.type.toString(),
      title = exception.title,
      status = exception.httpStatus.value(),
      detail = exception.detail,
    )
    // TODO initialize ApiProblemDetail extension properties either here or in in ApiProblemDetail constructor
    val applicationResponse = ResponseProblem(apiProblemDetail, request)
    logger.info { Json.encodeToString(applicationResponse) }
    return ResponseEntity<ResponseProblem?>(applicationResponse, exception.httpStatus)
  }
}
