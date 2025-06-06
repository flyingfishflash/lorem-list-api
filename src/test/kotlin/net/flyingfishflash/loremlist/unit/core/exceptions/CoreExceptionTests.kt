package net.flyingfishflash.loremlist.unit.core.exceptions

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.equals.shouldNotBeEqual
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import net.flyingfishflash.loremlist.core.exceptions.CoreException
import net.flyingfishflash.loremlist.core.exceptions.CoreException.Companion.DEFAULT_HTTP_STATUS
import net.flyingfishflash.loremlist.core.exceptions.CoreException.Companion.DEFAULT_MESSAGE
import net.flyingfishflash.loremlist.core.exceptions.CoreException.Companion.DEFAULT_PROBLEM_TYPE
import net.flyingfishflash.loremlist.core.exceptions.CoreException.Companion.DEFAULT_TITLE
import org.springframework.http.HttpStatus
import java.net.URI

class CoreExceptionTests :
  DescribeSpec({
    describe("ApiException()") {
      it("default values") {
        DEFAULT_HTTP_STATUS.shouldBe(HttpStatus.INTERNAL_SERVER_ERROR)
        DEFAULT_MESSAGE.shouldBe(DEFAULT_TITLE)
        DEFAULT_TITLE.shouldBe(CoreException::class.java.simpleName)
        DEFAULT_PROBLEM_TYPE.shouldBe(URI.create("about:config"))
      }

      it("default parameter values") {
        val exception = CoreException()
        exception.cause.shouldBeNull()
        exception.httpStatus.shouldBe(DEFAULT_HTTP_STATUS)
        exception.message.shouldBe(DEFAULT_MESSAGE)
        exception.responseMessage.shouldBe(exception.message)
        exception.title.shouldBe(DEFAULT_TITLE)
        exception.type.shouldBe(DEFAULT_PROBLEM_TYPE)
      }

      it("http status") {
        val expectedStatus = HttpStatus.I_AM_A_TEAPOT
        val exception = CoreException(httpStatus = expectedStatus)
        exception.cause.shouldBeNull()
        exception.httpStatus.shouldBe(expectedStatus)
        exception.message.shouldBe(DEFAULT_MESSAGE)
        exception.responseMessage.shouldBe(exception.message)
        exception.title.shouldBe(DEFAULT_TITLE)
        exception.type.shouldBe(DEFAULT_PROBLEM_TYPE)
      }

      it("message") {
        val expectedMessage = "Lorem Ipsum"
        val exception = CoreException(message = expectedMessage)
        exception.cause.shouldBeNull()
        exception.httpStatus.shouldBe(DEFAULT_HTTP_STATUS)
        exception.message.shouldBe(expectedMessage)
        exception.responseMessage.shouldBe(exception.message)
        exception.title.shouldBe(DEFAULT_TITLE)
        exception.type.shouldBe(DEFAULT_PROBLEM_TYPE)
      }

      it("response message") {
        val expectedResponseMessage = "Lorem Ipsum"
        val exception = CoreException(responseMessage = expectedResponseMessage)
        exception.cause.shouldBeNull()
        exception.httpStatus.shouldBe(DEFAULT_HTTP_STATUS)
        exception.message.shouldBe(DEFAULT_MESSAGE)
        exception.responseMessage.shouldBe(expectedResponseMessage)
        exception.responseMessage.shouldNotBeEqual(exception.message.toString())
        exception.title.shouldBe(DEFAULT_TITLE)
        exception.type.shouldBe(DEFAULT_PROBLEM_TYPE)
      }

      it("title") {
        val expectedTitle = "Lorem Ipsum"
        val exception = CoreException(title = expectedTitle)
        exception.cause.shouldBeNull()
        exception.httpStatus.shouldBe(DEFAULT_HTTP_STATUS)
        exception.message.shouldBe(DEFAULT_MESSAGE)
        exception.responseMessage.shouldBe(exception.message)
        exception.title.shouldBe(expectedTitle)
        exception.type.shouldBe(DEFAULT_PROBLEM_TYPE)
      }

      it("type") {
        val expectedType: URI = URI.create("http://example.net")
        val exception = CoreException(type = expectedType)
        exception.cause.shouldBeNull()
        exception.httpStatus.shouldBe(DEFAULT_HTTP_STATUS)
        exception.message.shouldBe(DEFAULT_MESSAGE)
        exception.responseMessage.shouldBe(exception.message)
        exception.title.shouldBe(DEFAULT_TITLE)
        exception.type.shouldBe(expectedType)
      }
    }
  })
