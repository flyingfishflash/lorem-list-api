package net.flyingfishflash.loremlist.unit.core.response.advice

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import jakarta.servlet.http.HttpServletRequest
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import net.flyingfishflash.loremlist.core.exceptions.ApiException
import net.flyingfishflash.loremlist.core.response.advice.ApiExceptionHandler
import org.springframework.core.env.Environment

class ApiExceptionHandlerTests : DescribeSpec({

  afterEach { clearAllMocks() }
  afterSpec { unmockkAll() }

  describe("handleAbstractApiException") {
    it("with stacktrace") {
      val mockEnvironment = mockk<Environment>(relaxed = true)
      every { mockEnvironment.getProperty("server.error.include-stacktrace") } returns "always"
      val mockHttpServletRequest = mockk<HttpServletRequest>(relaxed = true)
      val apiException = ApiException()
      val apiExceptionHandler = ApiExceptionHandler(mockEnvironment)
      val responseEntity = apiExceptionHandler.handleAbstractApiException(mockHttpServletRequest, apiException)
      responseEntity.body?.content?.extensions?.jsonObject?.keys.shouldBe(setOf("stacktrace"))
      responseEntity.body?.content?.extensions?.jsonObject?.get("stacktrace")?.shouldBeInstanceOf<JsonArray>()
      responseEntity.body?.content?.extensions?.jsonObject?.get("stacktrace")?.toString()
        .shouldContain("net.flyingfishflash.loremlist.unit.core.response.advice.ApiExceptionHandlerTests")
    }

    it("without stacktrace") {
      val mockEnvironment = mockk<Environment>(relaxed = true)
      every { mockEnvironment.getProperty("server.error.include-stacktrace") } returns null
      val mockHttpServletRequest = mockk<HttpServletRequest>(relaxed = true)
      val apiException = ApiException()
      val apiExceptionHandler = ApiExceptionHandler(mockEnvironment)
      val responseEntity = apiExceptionHandler.handleAbstractApiException(mockHttpServletRequest, apiException)
      responseEntity.body?.content?.extensions?.jsonObject?.get("stacktrace")?.shouldBe(JsonPrimitive("disabled"))
    }
  }

  describe("handleException") {
    it("with stacktrace") {
      val mockEnvironment = mockk<Environment>(relaxed = true)
      every { mockEnvironment.getProperty("server.error.include-stacktrace") } returns "always"
      val mockHttpServletRequest = mockk<HttpServletRequest>(relaxed = true)
      val exception = Exception()
      val apiExceptionHandler = ApiExceptionHandler(mockEnvironment)
      val responseEntity = apiExceptionHandler.handleException(mockHttpServletRequest, exception)
      responseEntity.body?.content?.extensions?.jsonObject?.keys.shouldBe(setOf("stacktrace"))
      responseEntity.body?.content?.extensions?.jsonObject?.get("stacktrace")?.shouldBeInstanceOf<JsonArray>()
      responseEntity.body?.content?.extensions?.jsonObject?.get("stacktrace")?.toString()
        .shouldContain("net.flyingfishflash.loremlist.unit.core.response.advice.ApiExceptionHandlerTests")
    }

    it("without stacktrace") {
      val mockEnvironment = mockk<Environment>(relaxed = true)
      every { mockEnvironment.getProperty("server.error.include-stacktrace") } returns null
      val mockHttpServletRequest = mockk<HttpServletRequest>(relaxed = true)
      val exception = Exception()
      val apiExceptionHandler = ApiExceptionHandler(mockEnvironment)
      val responseEntity = apiExceptionHandler.handleException(mockHttpServletRequest, exception)
      responseEntity.body?.content?.extensions?.jsonObject?.get("stacktrace")?.shouldBe(JsonPrimitive("disabled"))
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
})
