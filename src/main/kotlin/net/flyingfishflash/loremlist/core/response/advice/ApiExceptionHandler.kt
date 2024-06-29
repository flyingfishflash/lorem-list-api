package net.flyingfishflash.loremlist.core.response.advice

import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.ConstraintViolationException
import net.flyingfishflash.loremlist.core.exceptions.ApiException
import net.flyingfishflash.loremlist.core.response.structure.ApiProblemDetail
import net.flyingfishflash.loremlist.core.response.structure.ExceptionCauseDetail
import net.flyingfishflash.loremlist.core.response.structure.ResponseProblem
import org.springframework.core.env.Environment
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.WebRequest
import org.springframework.web.method.annotation.HandlerMethodValidationException
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler

/**
 * Handles API Exceptions
 */
@RestControllerAdvice
class ApiExceptionHandler(private val environment: Environment) : ResponseEntityExceptionHandler() {
  companion object {
    const val VALIDATION_FAILURE_MESSAGE = "The following fields caused a validation failure:"
    const val EXCEPTION_MESSAGE_NOT_PRESENT = "Exception message not present."
  }

  private fun exceptionCause(exception: Throwable): Throwable {
    var rootCause = exception
    while (rootCause.cause != null && rootCause.cause != rootCause) {
      rootCause = rootCause.cause!!
    }
    return rootCause
  }

  private fun extractCauses(exception: Throwable): ExceptionCauseDetail {
    val rootCause = exceptionCause(exception)
    val exceptionCauseDetails =
      ExceptionCauseDetail(
        name = rootCause.javaClass.simpleName,
        message = rootCause.message ?: "Exception message not present.",
      )
    return exceptionCauseDetails
  }

  private fun isJsonRenderedByKotlin(exception: Exception): Boolean {
    return when (exception) {
      is HttpMessageNotReadableException -> false
      is HandlerMethodValidationException -> false
      is MethodArgumentNotValidException -> false
      else -> true
    }
  }

  private fun isStacktraceEnabled(): Boolean {
    return (environment.getProperty("server.error.include-stacktrace") ?: "never") == "always"
  }

  private fun buildCause(exception: Exception): ExceptionCauseDetail? {
    return if (isJsonRenderedByKotlin(exception) && (exception.cause != null)) extractCauses(exception) else null
  }

  private fun buildStackTrace(exception: Exception): List<String>? {
    return if (isStacktraceEnabled()) exception.stackTrace.toListOfString() else null
  }

  private fun Array<StackTraceElement>.toListOfString(): List<String> {
    return this.map { "${it.className}.${it.methodName} (${it.fileName}:${it.lineNumber})" }
  }

  // -----

  @ExceptionHandler(ApiException::class)
  fun handleAbstractApiException(request: HttpServletRequest, exception: ApiException): ResponseEntity<ResponseProblem> {
    val apiProblemDetail = ApiProblemDetail(
      type = exception.type.toString(),
      title = exception.title,
      status = exception.httpStatus.value(),
      detail = exception.message,
      supplemental = exception.supplemental,
      cause = buildCause(exception),
      stackTrace = buildStackTrace(exception),
    )

    val applicationResponse = ResponseProblem(apiProblemDetail, exception.responseMessage, request)
    return ResponseEntity<ResponseProblem>(applicationResponse, exception.httpStatus)
  }

  @ExceptionHandler(ConstraintViolationException::class)
  fun handleConstraintViolationException(
    request: HttpServletRequest,
    exception: ConstraintViolationException,
  ): ResponseEntity<ResponseProblem> {
    val fields = exception.constraintViolations.mapNotNull { it.propertyPath.toString() }.distinct().sorted()
    val errors = exception.constraintViolations.mapNotNull { it.message }.sorted()
    val responseMessage = "$VALIDATION_FAILURE_MESSAGE ${fields.joinToString()}."
    val apiProblemDetail = ApiProblemDetail(
      type = ApiException.DEFAULT_PROBLEM_TYPE.toString(),
      title = ConstraintViolationException::class.java.simpleName,
      status = HttpStatus.BAD_REQUEST.value(),
      detail = responseMessage,
      validationErrors = errors,
      cause = buildCause(exception),
      stackTrace = buildStackTrace(exception),
    )
    val applicationResponse = ResponseProblem(apiProblemDetail, responseMessage, request)
    return ResponseEntity<ResponseProblem>(applicationResponse, HttpStatus.BAD_REQUEST)
  }

  @ExceptionHandler(Exception::class)
  fun handleException(request: HttpServletRequest, exception: Exception): ResponseEntity<ResponseProblem> {
    val apiProblemDetail = ApiProblemDetail(
      type = ApiException.DEFAULT_PROBLEM_TYPE.toString(),
      title = exception.javaClass.simpleName,
      status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
      detail = exception.message ?: EXCEPTION_MESSAGE_NOT_PRESENT,
      cause = buildCause(exception),
      stackTrace = buildStackTrace(exception),
    )
    val applicationResponse = ResponseProblem(apiProblemDetail, "There was an error processing the request.", request)
    return ResponseEntity<ResponseProblem>(applicationResponse, HttpStatus.INTERNAL_SERVER_ERROR)
  }

  // -----

  public override fun handleHandlerMethodValidationException(
    exception: HandlerMethodValidationException,
    headers: HttpHeaders,
    status: HttpStatusCode,
    request: WebRequest,
  ): ResponseEntity<Any> {
    val beanFields = exception.beanResults.asSequence().map { it.fieldErrors }.flatten().mapNotNull { it.field }.toList()
    val paramFields = exception.valueResults.mapNotNull { it.methodParameter.parameterName }
    val fields = (beanFields + paramFields).distinct().sorted()
    val beanErrors = exception.beanResults.asSequence().map { it.fieldErrors }.flatten().mapNotNull { it.defaultMessage }.toList()
    val paramErrors = exception.valueResults.asSequence().map { it.resolvableErrors }.flatten().mapNotNull { it.defaultMessage }.toList()
    val errors = (beanErrors + paramErrors).distinct().sorted()
    val responseMessage = "$VALIDATION_FAILURE_MESSAGE ${fields.joinToString()}."
    val apiProblemDetail = ApiProblemDetail(
      type = ApiException.DEFAULT_PROBLEM_TYPE.toString(),
      title = exception.javaClass.simpleName,
      status = status.value(),
      detail = responseMessage,
      validationErrors = errors,
      cause = buildCause(exception),
      stackTrace = buildStackTrace(exception),
    )
    val applicationResponse = ResponseProblem(apiProblemDetail, responseMessage, request)
    return ResponseEntity<Any>(applicationResponse, status)
  }

  public override fun handleHttpMessageNotReadable(
    exception: HttpMessageNotReadableException,
    headers: HttpHeaders,
    status: HttpStatusCode,
    request: WebRequest,
  ): ResponseEntity<Any> {
    val apiProblemDetail = ApiProblemDetail(
      type = ApiException.DEFAULT_PROBLEM_TYPE.toString(),
      title = exception.javaClass.simpleName,
      status = status.value(),
      detail = exception.message ?: EXCEPTION_MESSAGE_NOT_PRESENT,
      cause = buildCause(exception),
      stackTrace = buildStackTrace(exception),
    )
    val applicationResponse = ResponseProblem(apiProblemDetail, "Failed to read request.", request)
    return ResponseEntity<Any>(applicationResponse, status)
  }

  public override fun handleMethodArgumentNotValid(
    exception: MethodArgumentNotValidException,
    headers: HttpHeaders,
    status: HttpStatusCode,
    request: WebRequest,
  ): ResponseEntity<Any> {
    val fields = exception.bindingResult.fieldErrors.mapNotNull { it.field }.distinct().sorted()
    val errors = exception.bindingResult.fieldErrors.mapNotNull { it.defaultMessage }.sorted()
    val responseMessage = "$VALIDATION_FAILURE_MESSAGE ${fields.joinToString()}."
    val apiProblemDetail = ApiProblemDetail(
      type = ApiException.DEFAULT_PROBLEM_TYPE.toString(),
      title = exception::class.java.simpleName,
      status = status.value(),
      detail = responseMessage,
      validationErrors = errors,
      cause = buildCause(exception),
      stackTrace = buildStackTrace(exception),
    )
    val applicationResponse = ResponseProblem(apiProblemDetail, responseMessage, request)
    return ResponseEntity<Any>(applicationResponse, status)
  }
}
