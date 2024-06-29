package net.flyingfishflash.loremlist.unit.core.response.advice

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.ConstraintViolationException
import net.flyingfishflash.loremlist.core.exceptions.ApiException
import net.flyingfishflash.loremlist.core.response.advice.ApiExceptionHandler
import net.flyingfishflash.loremlist.core.response.structure.ResponseProblem
import org.springframework.core.env.Environment
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.context.request.ServletWebRequest
import org.springframework.web.method.annotation.HandlerMethodValidationException

class ApiExceptionHandlerTests : DescribeSpec({

  afterEach { clearAllMocks() }
  afterSpec { unmockkAll() }

  describe("handleAbstractApiException()") {

    it("extensions: none") {
      val mockEnvironment = mockk<Environment>(relaxed = true)
      every { mockEnvironment.getProperty("server.error.include-stacktrace") } returns "never"
      val mockHttpServletRequest = mockk<HttpServletRequest>(relaxed = true)
      val apiException = ApiException()
      val apiExceptionHandler = ApiExceptionHandler(mockEnvironment)
      val responseEntity = apiExceptionHandler.handleAbstractApiException(mockHttpServletRequest, apiException)
      responseEntity.body?.content?.cause.shouldBeNull()
      responseEntity.body?.content?.stackTrace.shouldBeNull()
      responseEntity.body?.content?.validationErrors.shouldBeNull()
    }

    it("extensions: cause") {
      val mockEnvironment = mockk<Environment>(relaxed = true)
      every { mockEnvironment.getProperty("server.error.include-stacktrace") } returns "never"
      val mockHttpServletRequest = mockk<HttpServletRequest>(relaxed = true)
      val apiException = ApiException(cause = RuntimeException())
      val apiExceptionHandler = ApiExceptionHandler(mockEnvironment)
      val responseEntity = apiExceptionHandler.handleAbstractApiException(mockHttpServletRequest, apiException)
      responseEntity.body?.content?.cause.shouldNotBeNull()
      responseEntity.body?.content?.stackTrace.shouldBeNull()
      responseEntity.body?.content?.validationErrors.shouldBeNull()
    }

    it("extensions: stacktrace") {
      val mockEnvironment = mockk<Environment>(relaxed = true)
      every { mockEnvironment.getProperty("server.error.include-stacktrace") } returns "always"
      val mockHttpServletRequest = mockk<HttpServletRequest>(relaxed = true)
      val apiException = ApiException()
      val apiExceptionHandler = ApiExceptionHandler(mockEnvironment)
      val responseEntity = apiExceptionHandler.handleAbstractApiException(mockHttpServletRequest, apiException)
      responseEntity.body?.content?.cause.shouldBeNull()
      responseEntity.body?.content?.stackTrace.shouldNotBeNull()
      responseEntity.body?.content?.validationErrors.shouldBeNull()
    }

    it("extensions: stacktrace, cause wth no message") {
      val mockEnvironment = mockk<Environment>(relaxed = true)
      every { mockEnvironment.getProperty("server.error.include-stacktrace") } returns "always"
      val mockHttpServletRequest = mockk<HttpServletRequest>(relaxed = true)
      val apiException = ApiException(cause = RuntimeException())
      val apiExceptionHandler = ApiExceptionHandler(mockEnvironment)
      val responseEntity = apiExceptionHandler.handleAbstractApiException(mockHttpServletRequest, apiException)
      responseEntity.body?.content?.cause.shouldNotBeNull()
      responseEntity.body?.content?.stackTrace.shouldNotBeNull()
      responseEntity.body?.content?.validationErrors.shouldBeNull()
      responseEntity.body?.content?.cause?.name.shouldBe("RuntimeException")
      responseEntity.body?.content?.cause?.message.shouldBe("Exception message not present.")
    }

    it("extensions: stacktrace, cause is nested exception") {
      val mockEnvironment = mockk<Environment>(relaxed = true)
      every { mockEnvironment.getProperty("server.error.include-stacktrace") } returns "always"
      val mockHttpServletRequest = mockk<HttpServletRequest>(relaxed = true)
      val rootCause = IllegalArgumentException("Root cause exception")
      val intermediateCause = IllegalStateException("Intermediate cause exception")
      intermediateCause.initCause(rootCause)
      val topLevelException = ApiException(message = "Top level exception", cause = intermediateCause)
      val apiExceptionHandler = ApiExceptionHandler(mockEnvironment)
      val responseEntity = apiExceptionHandler.handleAbstractApiException(mockHttpServletRequest, topLevelException)
      responseEntity.body?.content?.supplemental?.shouldNotBeNull()
      responseEntity.body?.content?.cause?.name.shouldBe("IllegalArgumentException")
      responseEntity.body?.content?.cause?.message.shouldBe("Root cause exception")
      responseEntity.body?.content?.stackTrace.shouldNotBeNull()
      responseEntity.body?.content?.validationErrors.shouldBeNull()
    }
  }

  describe("handleException()") {
    it("with stacktrace") {
      val mockEnvironment = mockk<Environment>(relaxed = true)
      every { mockEnvironment.getProperty("server.error.include-stacktrace") } returns "always"
      val mockHttpServletRequest = mockk<HttpServletRequest>(relaxed = true)
      val exception = Exception()
      val apiExceptionHandler = ApiExceptionHandler(mockEnvironment)
      val responseEntity = apiExceptionHandler.handleException(mockHttpServletRequest, exception)
      responseEntity.body?.content?.validationErrors.shouldBeNull()
      responseEntity.body?.content?.stackTrace.shouldNotBeNull()
    }

    it("without stacktrace") {
      val mockEnvironment = mockk<Environment>(relaxed = true)
      every { mockEnvironment.getProperty("server.error.include-stacktrace") } returns null
      val mockHttpServletRequest = mockk<HttpServletRequest>(relaxed = true)
      val exception = Exception()
      val apiExceptionHandler = ApiExceptionHandler(mockEnvironment)
      val responseEntity = apiExceptionHandler.handleException(mockHttpServletRequest, exception)
      responseEntity.body?.content?.validationErrors.shouldBeNull()
      responseEntity.body?.content?.stackTrace.shouldBeNull()
    }

    it("with detail message") {
      val mockEnvironment = mockk<Environment>(relaxed = true)
      every { mockEnvironment.getProperty("server.error.include-stacktrace") } returns null
      val mockHttpServletRequest = mockk<HttpServletRequest>(relaxed = true)
      val exception = Exception("Lorem Ipsum")
      val apiExceptionHandler = ApiExceptionHandler(mockEnvironment)
      val responseEntity = apiExceptionHandler.handleException(mockHttpServletRequest, exception)
      responseEntity.body?.content?.detail.shouldBe("Lorem Ipsum")
    }
  }

  describe("handleHttpMessageNotReadable()") {
    it("with stacktrace") {
      val mockEnvironment = mockk<Environment>(relaxed = true)
      every { mockEnvironment.getProperty("server.error.include-stacktrace") } returns "always"
      val mockHttpMessageNotReadableException = mockk<HttpMessageNotReadableException>(relaxed = true)
      val mockHttpHeaders = mockk<HttpHeaders>(relaxed = true)
      val mockWebRequest = mockk<ServletWebRequest>(relaxed = true)
      val apiExceptionHandler = ApiExceptionHandler(mockEnvironment)
      val responseEntity = apiExceptionHandler.handleHttpMessageNotReadable(
        mockHttpMessageNotReadableException,
        mockHttpHeaders,
        HttpStatus.I_AM_A_TEAPOT,
        mockWebRequest,
      )
      responseEntity.body?.shouldNotBeNull().shouldBeInstanceOf<ResponseProblem>().content.stackTrace.shouldNotBeNull()
      responseEntity.body?.shouldNotBeNull().shouldBeInstanceOf<ResponseProblem>().content.validationErrors.shouldBeNull()
    }

    it("without stacktrace") {
      val mockEnvironment = mockk<Environment>(relaxed = true)
      every { mockEnvironment.getProperty("server.error.include-stacktrace") } returns null
      val mockHttpMessageNotReadableException = mockk<HttpMessageNotReadableException>(relaxed = true)
      val mockHttpHeaders = mockk<HttpHeaders>(relaxed = true)
      val mockWebRequest = mockk<ServletWebRequest>(relaxed = true)
      val apiExceptionHandler = ApiExceptionHandler(mockEnvironment)
      val responseEntity = apiExceptionHandler.handleHttpMessageNotReadable(
        mockHttpMessageNotReadableException,
        mockHttpHeaders,
        HttpStatus.I_AM_A_TEAPOT,
        mockWebRequest,
      )
      responseEntity.body?.shouldNotBeNull().shouldBeInstanceOf<ResponseProblem>().content.stackTrace.shouldBeNull()
      responseEntity.body?.shouldNotBeNull().shouldBeInstanceOf<ResponseProblem>().content.validationErrors.shouldBeNull()
    }

    it("without detail message") {
      val mockEnvironment = mockk<Environment>(relaxed = true)
      every { mockEnvironment.getProperty("server.error.include-stacktrace") } returns null
      val mockHttpMessageNotReadableException = mockk<HttpMessageNotReadableException>(relaxed = true)
      every { mockHttpMessageNotReadableException.message } returns null
      val mockHttpHeaders = mockk<HttpHeaders>(relaxed = true)
      val mockWebRequest = mockk<ServletWebRequest>(relaxed = true)
      val apiExceptionHandler = ApiExceptionHandler(mockEnvironment)
      val responseEntity = apiExceptionHandler.handleHttpMessageNotReadable(
        mockHttpMessageNotReadableException,
        mockHttpHeaders,
        HttpStatus.I_AM_A_TEAPOT,
        mockWebRequest,
      )
      responseEntity.body?.shouldNotBeNull().shouldBeInstanceOf<ResponseProblem>().content.detail.shouldBe(
        ApiExceptionHandler.EXCEPTION_MESSAGE_NOT_PRESENT,
      )
      responseEntity.body?.shouldNotBeNull().shouldBeInstanceOf<ResponseProblem>().content.stackTrace.shouldBeNull()
      responseEntity.body?.shouldNotBeNull().shouldBeInstanceOf<ResponseProblem>().content.validationErrors.shouldBeNull()
    }
  }

  describe("handleHandlerMethodValidationException()") {
    it("with stacktrace") {
      val mockEnvironment = mockk<Environment>(relaxed = true)
      every { mockEnvironment.getProperty("server.error.include-stacktrace") } returns "always"
      val mockHandlerMethodValidationException = mockk<HandlerMethodValidationException>(relaxed = true)
      val mockHttpHeaders = mockk<HttpHeaders>(relaxed = true)
      val mockWebRequest = mockk<ServletWebRequest>(relaxed = true)
      val apiExceptionHandler = ApiExceptionHandler(mockEnvironment)
      val responseEntity = apiExceptionHandler.handleHandlerMethodValidationException(
        mockHandlerMethodValidationException,
        mockHttpHeaders,
        HttpStatus.I_AM_A_TEAPOT,
        mockWebRequest,
      )
      responseEntity.body?.shouldNotBeNull().shouldBeInstanceOf<ResponseProblem>().content.stackTrace.shouldNotBeNull()
      responseEntity.body?.shouldNotBeNull().shouldBeInstanceOf<ResponseProblem>().content.validationErrors.shouldNotBeNull()
    }

    it("without stacktrace") {
      val mockEnvironment = mockk<Environment>(relaxed = true)
      every { mockEnvironment.getProperty("server.error.include-stacktrace") } returns null
      val mockHandlerMethodValidationException = mockk<HandlerMethodValidationException>(relaxed = true)
      val mockHttpHeaders = mockk<HttpHeaders>(relaxed = true)
      val mockWebRequest = mockk<ServletWebRequest>(relaxed = true)
      val apiExceptionHandler = ApiExceptionHandler(mockEnvironment)
      val responseEntity = apiExceptionHandler.handleHandlerMethodValidationException(
        mockHandlerMethodValidationException,
        mockHttpHeaders,
        HttpStatus.I_AM_A_TEAPOT,
        mockWebRequest,
      )
      responseEntity.body?.shouldNotBeNull().shouldBeInstanceOf<ResponseProblem>().content.stackTrace.shouldBeNull()
      responseEntity.body?.shouldNotBeNull().shouldBeInstanceOf<ResponseProblem>().content.validationErrors.shouldNotBeNull()
    }
  }

  describe("handleConstraintViolationException()") {
    it("with stacktrace") {
      val mockEnvironment = mockk<Environment>(relaxed = true)
      every { mockEnvironment.getProperty("server.error.include-stacktrace") } returns "always"
      val mockConstraintViolationException = mockk<ConstraintViolationException>(relaxed = true)
      every { mockConstraintViolationException.cause } returns Exception("Constraint Violation Cause")
      val mockHttpServletRequest = mockk<HttpServletRequest>(relaxed = true)
      val apiExceptionHandler = ApiExceptionHandler(mockEnvironment)
      val responseEntity = apiExceptionHandler.handleConstraintViolationException(
        mockHttpServletRequest,
        mockConstraintViolationException,
      )
      responseEntity.body?.shouldNotBeNull().shouldBeInstanceOf<ResponseProblem>().content.stackTrace.shouldNotBeNull()
      responseEntity.body?.shouldNotBeNull().shouldBeInstanceOf<ResponseProblem>().content.validationErrors.shouldNotBeNull()
    }

    it("without stacktrace") {
      val mockEnvironment = mockk<Environment>(relaxed = true)
      every { mockEnvironment.getProperty("server.error.include-stacktrace") } returns null
      val mockConstraintViolationException = mockk<ConstraintViolationException>(relaxed = true)
      every { mockConstraintViolationException.cause } returns Exception("Constraint Violation Cause")
      val mockHttpServletRequest = mockk<HttpServletRequest>(relaxed = true)
      val apiExceptionHandler = ApiExceptionHandler(mockEnvironment)
      val responseEntity = apiExceptionHandler.handleConstraintViolationException(
        mockHttpServletRequest,
        mockConstraintViolationException,
      )
      responseEntity.body?.shouldNotBeNull().shouldBeInstanceOf<ResponseProblem>().content.stackTrace.shouldBeNull()
      responseEntity.body?.shouldNotBeNull().shouldBeInstanceOf<ResponseProblem>().content.validationErrors.shouldNotBeNull()
    }
  }

  describe("handleMethodArgumentNotValid()") {
    it("with stacktrace") {
      val mockEnvironment = mockk<Environment>(relaxed = true)
      every { mockEnvironment.getProperty("server.error.include-stacktrace") } returns "always"
      val mockMethodArgumentNotValidException = mockk<MethodArgumentNotValidException>(relaxed = true)
      val mockHttpHeaders = mockk<HttpHeaders>(relaxed = true)
      val mockWebRequest = mockk<ServletWebRequest>(relaxed = true)
      val apiExceptionHandler = ApiExceptionHandler(mockEnvironment)
      val responseEntity = apiExceptionHandler.handleMethodArgumentNotValid(
        mockMethodArgumentNotValidException,
        mockHttpHeaders,
        HttpStatus.I_AM_A_TEAPOT,
        mockWebRequest,
      )
      responseEntity.body?.shouldNotBeNull().shouldBeInstanceOf<ResponseProblem>().content.stackTrace.shouldNotBeNull()
      responseEntity.body?.shouldNotBeNull().shouldBeInstanceOf<ResponseProblem>().content.validationErrors.shouldNotBeNull()
    }

    it("without stacktrace") {
      val mockEnvironment = mockk<Environment>(relaxed = true)
      every { mockEnvironment.getProperty("server.error.include-stacktrace") } returns null
      val mockMethodArgumentNotValidException = mockk<MethodArgumentNotValidException>(relaxed = true)
      val mockHttpHeaders = mockk<HttpHeaders>(relaxed = true)
      val mockWebRequest = mockk<ServletWebRequest>(relaxed = true)
      val apiExceptionHandler = ApiExceptionHandler(mockEnvironment)
      val responseEntity = apiExceptionHandler.handleMethodArgumentNotValid(
        mockMethodArgumentNotValidException,
        mockHttpHeaders,
        HttpStatus.I_AM_A_TEAPOT,
        mockWebRequest,
      )
      responseEntity.body?.shouldNotBeNull().shouldBeInstanceOf<ResponseProblem>().content.stackTrace.shouldBeNull()
      responseEntity.body?.shouldNotBeNull().shouldBeInstanceOf<ResponseProblem>().content.validationErrors.shouldNotBeNull()
    }
  }
})
