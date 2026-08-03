package net.flyingfishflash.loremlist.core.response.advice;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.flyingfishflash.loremlist.core.exceptions.CoreException;
import net.flyingfishflash.loremlist.core.response.structure.ApiProblemDetail;
import net.flyingfishflash.loremlist.core.response.structure.ExceptionCauseDetail;
import net.flyingfishflash.loremlist.core.response.structure.ResponseProblem;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/** Handles API Exceptions */
@RestControllerAdvice
public class CoreExceptionHandler extends ResponseEntityExceptionHandler {

  public static final String VALIDATION_FAILURE_MESSAGE =
      "The following fields caused a validation failure:";
  public static final String EXCEPTION_MESSAGE_NOT_PRESENT = "Exception message not present.";

  private final Environment environment;

  public CoreExceptionHandler(Environment environment) {
    this.environment = environment;
  }

  private Throwable exceptionCause(Throwable exception) {
    Throwable rootCause = exception;
    while (rootCause.getCause() != null && rootCause.getCause() != rootCause) {
      rootCause = rootCause.getCause();
    }
    return rootCause;
  }

  private ExceptionCauseDetail extractCauses(Throwable exception) {
    Throwable rootCause = exceptionCause(exception);
    String message =
        rootCause.getMessage() != null ? rootCause.getMessage() : EXCEPTION_MESSAGE_NOT_PRESENT;
    return new ExceptionCauseDetail(rootCause.getClass().getSimpleName(), message);
  }

  private boolean isJsonRenderedByKotlin(Exception exception) {
    if (exception instanceof HttpMessageNotReadableException
        || exception instanceof HandlerMethodValidationException
        || exception instanceof MethodArgumentNotValidException) {
      return false;
    }
    return true;
  }

  private boolean isStacktraceEnabled() {
    String property = environment.getProperty("server.error.include-stacktrace");
    return "always".equals(property != null ? property : "never");
  }

  private ExceptionCauseDetail buildCause(Exception exception) {
    return (isJsonRenderedByKotlin(exception) && exception.getCause() != null)
        ? extractCauses(exception)
        : null;
  }

  private List<String> buildStackTrace(Exception exception) {
    if (!isStacktraceEnabled()) {
      return null;
    }
    List<String> lines = new ArrayList<>();
    for (StackTraceElement element : exception.getStackTrace()) {
      lines.add(
          element.getClassName()
              + "."
              + element.getMethodName()
              + " ("
              + element.getFileName()
              + ":"
              + element.getLineNumber()
              + ")");
    }
    return lines;
  }

  // -----

  @ExceptionHandler(CoreException.class)
  public ResponseEntity<ResponseProblem> handleAbstractCoreException(
      HttpServletRequest request, CoreException exception) {
    ApiProblemDetail apiProblemDetail =
        new ApiProblemDetail(
            exception.getType().toString(),
            exception.getTitle(),
            exception.getHttpStatus().value(),
            exception.getMessage(),
            buildCause(exception),
            null,
            exception.getSupplemental(),
            buildStackTrace(exception));

    ResponseProblem applicationResponse =
        new ResponseProblem(apiProblemDetail, exception.getResponseMessage(), request);
    return new ResponseEntity<>(applicationResponse, exception.getHttpStatus());
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ResponseProblem> handleConstraintViolationException(
      HttpServletRequest request, ConstraintViolationException exception) {
    Set<String> fields = new LinkedHashSet<>();
    List<String> errors = new ArrayList<>();
    exception
        .getConstraintViolations()
        .forEach(
            violation -> {
              fields.add(violation.getPropertyPath().toString());
              errors.add(violation.getMessage());
            });
    List<String> sortedFields = fields.stream().sorted().toList();
    List<String> sortedErrors = errors.stream().sorted().toList();
    String responseMessage =
        VALIDATION_FAILURE_MESSAGE + " " + String.join(", ", sortedFields) + ".";
    ApiProblemDetail apiProblemDetail =
        new ApiProblemDetail(
            CoreException.DEFAULT_PROBLEM_TYPE.toString(),
            ConstraintViolationException.class.getSimpleName(),
            HttpStatus.BAD_REQUEST.value(),
            responseMessage,
            buildCause(exception),
            sortedErrors,
            null,
            buildStackTrace(exception));
    ResponseProblem applicationResponse =
        new ResponseProblem(apiProblemDetail, responseMessage, request);
    return new ResponseEntity<>(applicationResponse, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ResponseProblem> handleException(
      HttpServletRequest request, Exception exception) {
    ApiProblemDetail apiProblemDetail =
        new ApiProblemDetail(
            CoreException.DEFAULT_PROBLEM_TYPE.toString(),
            exception.getClass().getSimpleName(),
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            exception.getMessage() != null ? exception.getMessage() : EXCEPTION_MESSAGE_NOT_PRESENT,
            buildCause(exception),
            null,
            null,
            buildStackTrace(exception));
    ResponseProblem applicationResponse =
        new ResponseProblem(
            apiProblemDetail, "There was an error processing the request.", request);
    return new ResponseEntity<>(applicationResponse, HttpStatus.INTERNAL_SERVER_ERROR);
  }

  // -----

  @Override
  public ResponseEntity<Object> handleHandlerMethodValidationException(
      HandlerMethodValidationException exception,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    List<String> beanFields =
        exception.getBeanResults().stream()
            .flatMap(r -> r.getFieldErrors().stream())
            .map(e -> e.getField())
            .filter(f -> f != null)
            .toList();
    List<String> paramFields =
        exception.getValueResults().stream()
            .map(r -> r.getMethodParameter().getParameterName())
            .filter(f -> f != null)
            .toList();
    Set<String> fields = new LinkedHashSet<>();
    fields.addAll(beanFields);
    fields.addAll(paramFields);
    List<String> beanErrors =
        exception.getBeanResults().stream()
            .flatMap(r -> r.getFieldErrors().stream())
            .map(e -> e.getDefaultMessage())
            .filter(m -> m != null)
            .toList();
    List<String> paramErrors =
        exception.getValueResults().stream()
            .flatMap(r -> r.getResolvableErrors().stream())
            .map(e -> e.getDefaultMessage())
            .filter(m -> m != null)
            .toList();
    Set<String> errors = new LinkedHashSet<>();
    errors.addAll(beanErrors);
    errors.addAll(paramErrors);
    String responseMessage =
        VALIDATION_FAILURE_MESSAGE
            + " "
            + String.join(", ", fields.stream().sorted().toList())
            + ".";
    ApiProblemDetail apiProblemDetail =
        new ApiProblemDetail(
            CoreException.DEFAULT_PROBLEM_TYPE.toString(),
            exception.getClass().getSimpleName(),
            status.value(),
            responseMessage,
            buildCause(exception),
            errors.stream().sorted().toList(),
            null,
            buildStackTrace(exception));
    ResponseProblem applicationResponse =
        new ResponseProblem(apiProblemDetail, responseMessage, request);
    return new ResponseEntity<>(applicationResponse, status);
  }

  @Override
  public ResponseEntity<Object> handleHttpMessageNotReadable(
      HttpMessageNotReadableException exception,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    ApiProblemDetail apiProblemDetail =
        new ApiProblemDetail(
            CoreException.DEFAULT_PROBLEM_TYPE.toString(),
            exception.getClass().getSimpleName(),
            status.value(),
            exception.getMessage() != null ? exception.getMessage() : EXCEPTION_MESSAGE_NOT_PRESENT,
            buildCause(exception),
            null,
            null,
            buildStackTrace(exception));
    ResponseProblem applicationResponse =
        new ResponseProblem(apiProblemDetail, "Failed to read request.", request);
    return new ResponseEntity<>(applicationResponse, status);
  }

  @Override
  public ResponseEntity<Object> handleMethodArgumentNotValid(
      MethodArgumentNotValidException exception,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    List<String> fields =
        exception.getBindingResult().getFieldErrors().stream()
            .map(e -> e.getField())
            .filter(f -> f != null)
            .distinct()
            .sorted()
            .toList();
    List<String> errors =
        exception.getBindingResult().getFieldErrors().stream()
            .map(e -> e.getDefaultMessage())
            .filter(m -> m != null)
            .sorted()
            .toList();
    String responseMessage = VALIDATION_FAILURE_MESSAGE + " " + String.join(", ", fields) + ".";
    ApiProblemDetail apiProblemDetail =
        new ApiProblemDetail(
            CoreException.DEFAULT_PROBLEM_TYPE.toString(),
            exception.getClass().getSimpleName(),
            status.value(),
            responseMessage,
            buildCause(exception),
            errors,
            null,
            buildStackTrace(exception));
    ResponseProblem applicationResponse =
        new ResponseProblem(apiProblemDetail, responseMessage, request);
    return new ResponseEntity<>(applicationResponse, status);
  }
}
