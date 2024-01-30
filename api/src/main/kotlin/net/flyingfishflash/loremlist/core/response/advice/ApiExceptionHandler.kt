package net.flyingfishflash.loremlist.core.response.advice

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.servlet.http.HttpServletRequest
import net.flyingfishflash.loremlist.core.exceptions.AbstractApiException
import net.flyingfishflash.loremlist.core.response.structure.Response
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.net.URI

@RestControllerAdvice
class ApiExceptionHandler {
  @Schema(description = "api exception response")
  @ExceptionHandler(AbstractApiException::class)
  fun handleException(
    request: HttpServletRequest,
    exception: AbstractApiException,
  ): ResponseEntity<Response<ProblemDetail>?> {
    val httpMethod = request.method
    val httpStatus = HttpStatus.resolve(exception.statusCode.value()) ?: HttpStatus.NOT_IMPLEMENTED
    val problemDetail = exception.body
    if (problemDetail.instance == null) problemDetail.instance = URI.create(request.requestURI)

    // ProblemDetailUtility.setCustomPropertiesFromThrowable(problemDetail, exception)

    return ResponseEntity<Response<ProblemDetail>?>(
      Response(problemDetail, "Hard Code this Message into the ApiException", httpMethod),
      httpStatus,
    )
  }
}
