package net.flyingfishflash.loremlist.core.response.advice

import jakarta.servlet.http.HttpServletRequest
import net.flyingfishflash.loremlist.core.exceptions.AbstractApiException
import net.flyingfishflash.loremlist.core.response.structure.Response
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.net.URI
import java.util.*


/**
 * The most basic implementation works:
 * @RestControllerAdvice
 * class ApiExceptionHandler : ResponseEntityExceptionHandler()
 *
 * but springdoc-openapi doesn't pick up the schema  of the ErrorResponse for some reason
 * so declaring an abstract api exception and handling it here
 *
 */

@RestControllerAdvice
class ApiExceptionHandler {

  @ExceptionHandler(AbstractApiException::class)
  fun handleException(
    request: HttpServletRequest, exception: AbstractApiException
  ): ResponseEntity<Response<ProblemDetail>?> {
    val httpMethod = request.method
    val httpStatus =
      Optional.ofNullable(HttpStatus.resolve(exception.statusCode.value()))
        .orElse(HttpStatus.NOT_IMPLEMENTED)

    val problemDetail = exception.body
    if (problemDetail.instance == null) {
      problemDetail.instance = URI.create(request.requestURI)
    }
    //ProblemDetailUtility.setCustomPropertiesFromThrowable(problemDetail, exception)

    return ResponseEntity<Response<ProblemDetail>?>(
      Response(problemDetail, "Hard Code this Message into the ApiException", httpMethod),
      httpStatus
    )
  }
}

// @RestControllerAdvice
// class ApiExceptionHandler : ResponseEntityExceptionHandler() {
// //  @Schema(name = "ErrorResponse", description = "ErrorResponse", implementation = ProblemDetail::class, )
//
//  @ExceptionHandler(ListNotFoundException::class)
//  @ResponseStatus(HttpStatus.NOT_FOUND)
//  fun handleListNotFoundException(
//    request: WebRequest,
//    exception: AbstractApiException,
//  ): ResponseEntity<ProblemDetail>? {
//    val springErrorResponse = handleErrorResponseException(exception, exception.headers, exception.statusCode, request)
//    val pd: ProblemDetail = springErrorResponse?.body as ProblemDetail
//    val statusCode = springErrorResponse.statusCode as HttpStatus
//    val headers = springErrorResponse.headers
//    val myErrorResponse: ResponseEntity<ProblemDetail> = ResponseEntity(pd, headers, statusCode)
//    return myErrorResponse
//  }
// }
