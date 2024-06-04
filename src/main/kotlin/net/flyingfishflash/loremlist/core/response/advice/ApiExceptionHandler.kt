package net.flyingfishflash.loremlist.core.response.advice

import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.ConstraintViolationException
import net.flyingfishflash.loremlist.core.exceptions.AbstractApiException
import net.flyingfishflash.loremlist.core.response.structure.ApiProblemDetail
import net.flyingfishflash.loremlist.core.response.structure.ApiProblemDetailExtensions
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
    private const val PROBLEM_TYPE = "about:config"
    const val VALIDATION_FAILURE = "Validation Failure"
    const val VALIDATION_FAILURE_MESSAGE = "The following fields caused a validation failure:"
    const val EXCEPTION_MESSAGE_NOT_PRESENT = "Exception message not present."
  }

  private fun isStacktraceEnabled(): Boolean {
    return (environment.getProperty("server.error.include-stacktrace") ?: "never") == "always"
  }

  private fun Array<StackTraceElement>.toListOfString(): List<String> {
    return this.map { "${it.className}.${it.methodName} (${it.fileName}:${it.lineNumber})" }
  }

  @ExceptionHandler(AbstractApiException::class)
  fun handleAbstractApiException(request: HttpServletRequest, exception: AbstractApiException): ResponseEntity<ResponseProblem?> {
    val apiProblemDetail = ApiProblemDetail(
      type = exception.type.toString(),
      title = exception.title,
      status = exception.httpStatus.value(),
      detail = exception.detail,
      extensions = if (isStacktraceEnabled()) ApiProblemDetailExtensions(stackTrace = exception.stackTrace.toListOfString()) else null,
    )
    val applicationResponse = ResponseProblem(apiProblemDetail, exception.responseMessage, request)
    return ResponseEntity<ResponseProblem?>(applicationResponse, exception.httpStatus)
  }

  @ExceptionHandler(ConstraintViolationException::class)
  fun handleConstraintViolationException(
    request: HttpServletRequest,
    exception: ConstraintViolationException,
  ): ResponseEntity<ResponseProblem?> {
    val fields = exception.constraintViolations.mapNotNull { it.propertyPath.toString() }.distinct().sorted()
    val errors = exception.constraintViolations.mapNotNull { it.message }.sorted()
    val message = "$VALIDATION_FAILURE_MESSAGE ${fields.joinToString()}."
    val apiProblemDetail = ApiProblemDetail(
      type = PROBLEM_TYPE,
      title = VALIDATION_FAILURE,
      status = HttpStatus.BAD_REQUEST.value(),
      detail = message,
      extensions = ApiProblemDetailExtensions(
        validationErrors = errors,
        stackTrace = if (isStacktraceEnabled()) exception.stackTrace.toListOfString() else null,
      ),
    )
    val applicationResponse = ResponseProblem(apiProblemDetail, message, request)
    return ResponseEntity<ResponseProblem?>(applicationResponse, HttpStatus.BAD_REQUEST)
  }

  @ExceptionHandler(Exception::class)
  fun handleException(request: HttpServletRequest, exception: Exception): ResponseEntity<ResponseProblem?> {
    val apiProblemDetail = ApiProblemDetail(
      type = PROBLEM_TYPE,
      title = "Exception",
      status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
      detail = exception.message ?: EXCEPTION_MESSAGE_NOT_PRESENT,
      extensions = if (isStacktraceEnabled()) ApiProblemDetailExtensions(stackTrace = exception.stackTrace.toListOfString()) else null,
    )
    val applicationResponse = ResponseProblem(apiProblemDetail, "There was an error processing the request.", request)
    return ResponseEntity<ResponseProblem?>(applicationResponse, HttpStatus.INTERNAL_SERVER_ERROR)
  }

  public override fun handleHandlerMethodValidationException(
    exception: HandlerMethodValidationException,
    headers: HttpHeaders,
    status: HttpStatusCode,
    request: WebRequest,
  ): ResponseEntity<Any>? {
    val beanFields = exception.beanResults.asSequence().map { it.fieldErrors }.flatten().mapNotNull { it.field }.toList()
    val paramFields = exception.valueResults.mapNotNull { it.methodParameter.parameterName }
    val fields = (beanFields + paramFields).distinct().sorted()

    val beanErrors = exception.beanResults.asSequence().map { it.fieldErrors }.flatten().mapNotNull { it.defaultMessage }.toList()
    val paramErrors = exception.valueResults.asSequence().map { it.resolvableErrors }.flatten().mapNotNull { it.defaultMessage }.toList()
    val errors = (beanErrors + paramErrors).distinct().sorted()

    val message = "$VALIDATION_FAILURE_MESSAGE ${fields.joinToString()}."
    val apiProblemDetail = ApiProblemDetail(
      type = PROBLEM_TYPE,
      title = VALIDATION_FAILURE,
      status = status.value(),
      detail = message,
      extensions = ApiProblemDetailExtensions(
        validationErrors = errors,
        stackTrace = if (isStacktraceEnabled()) exception.stackTrace.toListOfString() else null,
      ),
    )
    val applicationResponse = ResponseProblem(apiProblemDetail, message, request)
    return ResponseEntity<Any>(applicationResponse, status)
  }

  public override fun handleHttpMessageNotReadable(
    ex: HttpMessageNotReadableException,
    headers: HttpHeaders,
    status: HttpStatusCode,
    request: WebRequest,
  ): ResponseEntity<Any>? {
    val apiProblemDetail = ApiProblemDetail(
      type = PROBLEM_TYPE,
      title = "Http Message Not Readable",
      status = status.value(),
      detail = ex.message ?: EXCEPTION_MESSAGE_NOT_PRESENT,
      extensions = ApiProblemDetailExtensions(
        stackTrace = if (isStacktraceEnabled()) ex.stackTrace.toListOfString() else null,
      ),
    )
    val applicationResponse = ResponseProblem(apiProblemDetail, "Failed to read request.", request)
    return ResponseEntity<Any>(applicationResponse, status)
  }

  public override fun handleMethodArgumentNotValid(
    exception: MethodArgumentNotValidException,
    headers: HttpHeaders,
    status: HttpStatusCode,
    request: WebRequest,
  ): ResponseEntity<Any>? {
    val fields = exception.bindingResult.fieldErrors.mapNotNull { it.field }.distinct().sorted()
    val errors = exception.bindingResult.fieldErrors.mapNotNull { it.defaultMessage }.sorted()
    val message = "$VALIDATION_FAILURE_MESSAGE ${fields.joinToString()}."
    val apiProblemDetail = ApiProblemDetail(
      type = PROBLEM_TYPE,
      title = VALIDATION_FAILURE,
      status = status.value(),
      detail = message,
      extensions = ApiProblemDetailExtensions(
        validationErrors = errors,
        stackTrace = if (isStacktraceEnabled()) exception.stackTrace.toListOfString() else null,
      ),
    )
    val applicationResponse = ResponseProblem(apiProblemDetail, message, request)
    return ResponseEntity<Any>(applicationResponse, status)
  }
}
