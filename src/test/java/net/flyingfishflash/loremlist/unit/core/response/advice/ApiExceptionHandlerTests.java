package net.flyingfishflash.loremlist.unit.core.response.advice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import net.flyingfishflash.loremlist.core.exceptions.CoreException;
import net.flyingfishflash.loremlist.core.response.advice.CoreExceptionHandler;
import net.flyingfishflash.loremlist.core.response.structure.ResponseProblem;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

class ApiExceptionHandlerTests {

  private static Environment mockEnvironment(String includeStacktrace) {
    Environment mockEnvironment = mock(Environment.class);
    when(mockEnvironment.getProperty("server.error.include-stacktrace"))
        .thenReturn(includeStacktrace);
    return mockEnvironment;
  }

  private static ResponseProblem body(ResponseEntity<?> responseEntity) {
    assertThat(responseEntity.getBody()).isInstanceOf(ResponseProblem.class);
    return (ResponseProblem) responseEntity.getBody();
  }

  private static HttpServletRequest mockHttpServletRequest() {
    HttpServletRequest mockHttpServletRequest = mock(HttpServletRequest.class);
    when(mockHttpServletRequest.getMethod()).thenReturn("GET");
    when(mockHttpServletRequest.getRequestURI()).thenReturn("/irrelevant");
    return mockHttpServletRequest;
  }

  private static ServletWebRequest mockWebRequest() {
    ServletWebRequest mockWebRequest = mock(ServletWebRequest.class);
    HttpServletRequest request = mockHttpServletRequest();
    when(mockWebRequest.getRequest()).thenReturn(request);
    when(mockWebRequest.getDescription(false)).thenReturn("uri=/irrelevant");
    return mockWebRequest;
  }

  @Nested
  class HandleAbstractApiException {

    @Test
    void extensionsNone() {
      HttpServletRequest mockHttpServletRequest = mockHttpServletRequest();
      CoreException apiException = CoreException.builder().build();
      CoreExceptionHandler coreExceptionHandler =
          new CoreExceptionHandler(mockEnvironment("never"));
      ResponseEntity<ResponseProblem> responseEntity =
          coreExceptionHandler.handleAbstractCoreException(mockHttpServletRequest, apiException);
      assertThat(responseEntity.getBody().content().cause()).isNull();
      assertThat(responseEntity.getBody().content().stackTrace()).isNull();
      assertThat(responseEntity.getBody().content().validationErrors()).isNull();
    }

    @Test
    void extensionsCause() {
      HttpServletRequest mockHttpServletRequest = mockHttpServletRequest();
      CoreException apiException = CoreException.builder().cause(new RuntimeException()).build();
      CoreExceptionHandler coreExceptionHandler =
          new CoreExceptionHandler(mockEnvironment("never"));
      ResponseEntity<ResponseProblem> responseEntity =
          coreExceptionHandler.handleAbstractCoreException(mockHttpServletRequest, apiException);
      assertThat(responseEntity.getBody().content().cause()).isNotNull();
      assertThat(responseEntity.getBody().content().stackTrace()).isNull();
      assertThat(responseEntity.getBody().content().validationErrors()).isNull();
    }

    @Test
    void extensionsStacktrace() {
      HttpServletRequest mockHttpServletRequest = mockHttpServletRequest();
      CoreException apiException = CoreException.builder().build();
      CoreExceptionHandler coreExceptionHandler =
          new CoreExceptionHandler(mockEnvironment("always"));
      ResponseEntity<ResponseProblem> responseEntity =
          coreExceptionHandler.handleAbstractCoreException(mockHttpServletRequest, apiException);
      assertThat(responseEntity.getBody().content().cause()).isNull();
      assertThat(responseEntity.getBody().content().stackTrace()).isNotNull();
      assertThat(responseEntity.getBody().content().validationErrors()).isNull();
    }

    @Test
    void extensionsStacktraceCauseWithNoMessage() {
      HttpServletRequest mockHttpServletRequest = mockHttpServletRequest();
      CoreException apiException = CoreException.builder().cause(new RuntimeException()).build();
      CoreExceptionHandler coreExceptionHandler =
          new CoreExceptionHandler(mockEnvironment("always"));
      ResponseEntity<ResponseProblem> responseEntity =
          coreExceptionHandler.handleAbstractCoreException(mockHttpServletRequest, apiException);
      assertThat(responseEntity.getBody().content().cause()).isNotNull();
      assertThat(responseEntity.getBody().content().stackTrace()).isNotNull();
      assertThat(responseEntity.getBody().content().validationErrors()).isNull();
      assertThat(responseEntity.getBody().content().cause().name()).isEqualTo("RuntimeException");
      assertThat(responseEntity.getBody().content().cause().message())
          .isEqualTo("Exception message not present.");
    }

    @Test
    void extensionsStacktraceCauseIsNestedException() {
      HttpServletRequest mockHttpServletRequest = mockHttpServletRequest();
      IllegalArgumentException rootCause = new IllegalArgumentException("Root cause exception");
      IllegalStateException intermediateCause =
          new IllegalStateException("Intermediate cause exception");
      intermediateCause.initCause(rootCause);
      CoreException topLevelException =
          CoreException.builder().message("Top level exception").cause(intermediateCause).build();
      CoreExceptionHandler coreExceptionHandler =
          new CoreExceptionHandler(mockEnvironment("always"));
      ResponseEntity<ResponseProblem> responseEntity =
          coreExceptionHandler.handleAbstractCoreException(
              mockHttpServletRequest, topLevelException);
      // Note: CoreException.supplemental is only ever populated via an explicit
      // builder().supplemental(...) call; this exception never sets it, so it stays null here.
      assertThat(responseEntity.getBody().content().cause().name())
          .isEqualTo("IllegalArgumentException");
      assertThat(responseEntity.getBody().content().cause().message())
          .isEqualTo("Root cause exception");
      assertThat(responseEntity.getBody().content().stackTrace()).isNotNull();
      assertThat(responseEntity.getBody().content().validationErrors()).isNull();
    }
  }

  @Nested
  class HandleException {

    @Test
    void withStacktrace() {
      HttpServletRequest mockHttpServletRequest = mockHttpServletRequest();
      Exception exception = new Exception();
      CoreExceptionHandler coreExceptionHandler =
          new CoreExceptionHandler(mockEnvironment("always"));
      ResponseEntity<ResponseProblem> responseEntity =
          coreExceptionHandler.handleException(mockHttpServletRequest, exception);
      assertThat(responseEntity.getBody().content().validationErrors()).isNull();
      assertThat(responseEntity.getBody().content().stackTrace()).isNotNull();
    }

    @Test
    void withoutStacktrace() {
      HttpServletRequest mockHttpServletRequest = mockHttpServletRequest();
      Exception exception = new Exception();
      CoreExceptionHandler coreExceptionHandler = new CoreExceptionHandler(mockEnvironment(null));
      ResponseEntity<ResponseProblem> responseEntity =
          coreExceptionHandler.handleException(mockHttpServletRequest, exception);
      assertThat(responseEntity.getBody().content().validationErrors()).isNull();
      assertThat(responseEntity.getBody().content().stackTrace()).isNull();
    }

    @Test
    void withDetailMessage() {
      HttpServletRequest mockHttpServletRequest = mockHttpServletRequest();
      Exception exception = new Exception("Lorem Ipsum");
      CoreExceptionHandler coreExceptionHandler = new CoreExceptionHandler(mockEnvironment(null));
      ResponseEntity<ResponseProblem> responseEntity =
          coreExceptionHandler.handleException(mockHttpServletRequest, exception);
      assertThat(responseEntity.getBody().content().detail()).isEqualTo("Lorem Ipsum");
    }
  }

  @Nested
  class HandleHttpMessageNotReadable {

    @Test
    void withStacktrace() throws Exception {
      HttpMessageNotReadableException mockException = mock(HttpMessageNotReadableException.class);
      when(mockException.getStackTrace()).thenReturn(new StackTraceElement[0]);
      HttpHeaders mockHttpHeaders = mock(HttpHeaders.class);
      ServletWebRequest mockWebRequest = mockWebRequest();
      CoreExceptionHandler coreExceptionHandler =
          new CoreExceptionHandler(mockEnvironment("always"));
      ResponseEntity<Object> responseEntity =
          coreExceptionHandler.handleHttpMessageNotReadable(
              mockException, mockHttpHeaders, HttpStatus.I_AM_A_TEAPOT, mockWebRequest);
      assertThat(body(responseEntity).content().stackTrace()).isNotNull();
      assertThat(body(responseEntity).content().validationErrors()).isNull();
    }

    @Test
    void withoutStacktrace() throws Exception {
      HttpMessageNotReadableException mockException = mock(HttpMessageNotReadableException.class);
      HttpHeaders mockHttpHeaders = mock(HttpHeaders.class);
      ServletWebRequest mockWebRequest = mockWebRequest();
      CoreExceptionHandler coreExceptionHandler = new CoreExceptionHandler(mockEnvironment(null));
      ResponseEntity<Object> responseEntity =
          coreExceptionHandler.handleHttpMessageNotReadable(
              mockException, mockHttpHeaders, HttpStatus.I_AM_A_TEAPOT, mockWebRequest);
      assertThat(body(responseEntity).content().stackTrace()).isNull();
      assertThat(body(responseEntity).content().validationErrors()).isNull();
    }

    @Test
    void withoutDetailMessage() throws Exception {
      HttpMessageNotReadableException mockException = mock(HttpMessageNotReadableException.class);
      when(mockException.getMessage()).thenReturn(null);
      HttpHeaders mockHttpHeaders = mock(HttpHeaders.class);
      ServletWebRequest mockWebRequest = mockWebRequest();
      CoreExceptionHandler coreExceptionHandler = new CoreExceptionHandler(mockEnvironment(null));
      ResponseEntity<Object> responseEntity =
          coreExceptionHandler.handleHttpMessageNotReadable(
              mockException, mockHttpHeaders, HttpStatus.I_AM_A_TEAPOT, mockWebRequest);
      assertThat(body(responseEntity).content().detail())
          .isEqualTo(CoreExceptionHandler.EXCEPTION_MESSAGE_NOT_PRESENT);
      assertThat(body(responseEntity).content().stackTrace()).isNull();
      assertThat(body(responseEntity).content().validationErrors()).isNull();
    }
  }

  @Nested
  class HandleHandlerMethodValidationException {

    @Test
    void withStacktrace() {
      HandlerMethodValidationException mockException = mock(HandlerMethodValidationException.class);
      when(mockException.getStackTrace()).thenReturn(new StackTraceElement[0]);
      HttpHeaders mockHttpHeaders = mock(HttpHeaders.class);
      ServletWebRequest mockWebRequest = mockWebRequest();
      CoreExceptionHandler coreExceptionHandler =
          new CoreExceptionHandler(mockEnvironment("always"));
      ResponseEntity<Object> responseEntity =
          coreExceptionHandler.handleHandlerMethodValidationException(
              mockException, mockHttpHeaders, HttpStatus.I_AM_A_TEAPOT, mockWebRequest);
      assertThat(body(responseEntity).content().stackTrace()).isNotNull();
      assertThat(body(responseEntity).content().validationErrors()).isNotNull();
    }

    @Test
    void withoutStacktrace() {
      HandlerMethodValidationException mockException = mock(HandlerMethodValidationException.class);
      HttpHeaders mockHttpHeaders = mock(HttpHeaders.class);
      ServletWebRequest mockWebRequest = mockWebRequest();
      CoreExceptionHandler coreExceptionHandler = new CoreExceptionHandler(mockEnvironment(null));
      ResponseEntity<Object> responseEntity =
          coreExceptionHandler.handleHandlerMethodValidationException(
              mockException, mockHttpHeaders, HttpStatus.I_AM_A_TEAPOT, mockWebRequest);
      assertThat(body(responseEntity).content().stackTrace()).isNull();
      assertThat(body(responseEntity).content().validationErrors()).isNotNull();
    }
  }

  @Nested
  class HandleConstraintViolationException {

    @Test
    void withStacktrace() {
      ConstraintViolationException mockException = mock(ConstraintViolationException.class);
      when(mockException.getCause()).thenReturn(new Exception("Constraint Violation Cause"));
      when(mockException.getStackTrace()).thenReturn(new StackTraceElement[0]);
      HttpServletRequest mockHttpServletRequest = mockHttpServletRequest();
      CoreExceptionHandler coreExceptionHandler =
          new CoreExceptionHandler(mockEnvironment("always"));
      ResponseEntity<ResponseProblem> responseEntity =
          coreExceptionHandler.handleConstraintViolationException(
              mockHttpServletRequest, mockException);
      assertThat(responseEntity.getBody().content().stackTrace()).isNotNull();
      assertThat(responseEntity.getBody().content().validationErrors()).isNotNull();
    }

    @Test
    void withoutStacktrace() {
      ConstraintViolationException mockException = mock(ConstraintViolationException.class);
      when(mockException.getCause()).thenReturn(new Exception("Constraint Violation Cause"));
      HttpServletRequest mockHttpServletRequest = mockHttpServletRequest();
      CoreExceptionHandler coreExceptionHandler = new CoreExceptionHandler(mockEnvironment(null));
      ResponseEntity<ResponseProblem> responseEntity =
          coreExceptionHandler.handleConstraintViolationException(
              mockHttpServletRequest, mockException);
      assertThat(responseEntity.getBody().content().stackTrace()).isNull();
      assertThat(responseEntity.getBody().content().validationErrors()).isNotNull();
    }
  }

  @Nested
  class HandleMethodArgumentNotValid {

    @Test
    void withStacktrace() {
      MethodArgumentNotValidException mockException = mock(MethodArgumentNotValidException.class);
      when(mockException.getStackTrace()).thenReturn(new StackTraceElement[0]);
      org.springframework.validation.BindingResult mockBindingResult =
          mock(org.springframework.validation.BindingResult.class);
      when(mockBindingResult.getFieldErrors()).thenReturn(java.util.List.of());
      when(mockException.getBindingResult()).thenReturn(mockBindingResult);
      HttpHeaders mockHttpHeaders = mock(HttpHeaders.class);
      ServletWebRequest mockWebRequest = mockWebRequest();
      CoreExceptionHandler coreExceptionHandler =
          new CoreExceptionHandler(mockEnvironment("always"));
      ResponseEntity<Object> responseEntity =
          coreExceptionHandler.handleMethodArgumentNotValid(
              mockException, mockHttpHeaders, HttpStatus.I_AM_A_TEAPOT, mockWebRequest);
      assertThat(body(responseEntity).content().stackTrace()).isNotNull();
      assertThat(body(responseEntity).content().validationErrors()).isNotNull();
    }

    @Test
    void withoutStacktrace() {
      MethodArgumentNotValidException mockException = mock(MethodArgumentNotValidException.class);
      org.springframework.validation.BindingResult mockBindingResult =
          mock(org.springframework.validation.BindingResult.class);
      when(mockBindingResult.getFieldErrors()).thenReturn(java.util.List.of());
      when(mockException.getBindingResult()).thenReturn(mockBindingResult);
      HttpHeaders mockHttpHeaders = mock(HttpHeaders.class);
      ServletWebRequest mockWebRequest = mockWebRequest();
      CoreExceptionHandler coreExceptionHandler = new CoreExceptionHandler(mockEnvironment(null));
      ResponseEntity<Object> responseEntity =
          coreExceptionHandler.handleMethodArgumentNotValid(
              mockException, mockHttpHeaders, HttpStatus.I_AM_A_TEAPOT, mockWebRequest);
      assertThat(body(responseEntity).content().stackTrace()).isNull();
      assertThat(body(responseEntity).content().validationErrors()).isNotNull();
    }
  }
}
