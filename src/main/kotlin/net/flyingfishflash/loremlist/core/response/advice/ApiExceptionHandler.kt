package net.flyingfishflash.loremlist.core.response.advice

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.http.HttpServletRequest
import net.flyingfishflash.loremlist.core.exceptions.AbstractApiException
import net.flyingfishflash.loremlist.core.response.structure.ApiProblemDetail
import net.flyingfishflash.loremlist.core.response.structure.ResponseProblem
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

/**
 * Handles API Exceptions
 */
@RestControllerAdvice
class ApiExceptionHandler {
  private val log = KotlinLogging.logger {}

  @ExceptionHandler(AbstractApiException::class)
  fun handleAbstractApiException(request: HttpServletRequest, exception: AbstractApiException): ResponseEntity<ResponseProblem?> {
    val apiProblemDetail = ApiProblemDetail(
      type = exception.type.toString(),
      title = exception.title,
      status = exception.httpStatus.value(),
      detail = exception.detail,
    )

    val applicationResponse = ResponseProblem(apiProblemDetail, exception.responseMessage, request)

    return ResponseEntity<ResponseProblem?>(applicationResponse, exception.httpStatus)
  }
}
