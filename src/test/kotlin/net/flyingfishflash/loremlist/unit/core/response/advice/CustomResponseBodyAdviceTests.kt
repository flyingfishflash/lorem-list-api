package net.flyingfishflash.loremlist.unit.core.response.advice

import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import jakarta.servlet.http.HttpServletRequest
import net.flyingfishflash.loremlist.core.response.advice.CustomResponseBodyAdvice
import net.flyingfishflash.loremlist.core.response.structure.ApiProblemDetail
import net.flyingfishflash.loremlist.core.response.structure.IgnoreResponseBinding
import net.flyingfishflash.loremlist.core.response.structure.ResponseProblem
import net.flyingfishflash.loremlist.core.response.structure.ResponseSuccess
import net.flyingfishflash.loremlist.domain.lrmlist.LrmListServiceDefault
import net.flyingfishflash.loremlist.domain.lrmlist.data.LrmListCreate
import org.springdoc.webmvc.api.OpenApiWebMvcResource
import org.springdoc.webmvc.ui.SwaggerConfigResource
import org.springframework.core.MethodParameter
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ProblemDetail
import org.springframework.http.converter.json.KotlinSerializationJsonHttpMessageConverter
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServerHttpResponse
import org.springframework.util.ClassUtils
import org.springframework.web.ErrorResponseException
import org.springframework.web.bind.annotation.RestController

class CustomResponseBodyAdviceTests :
  DescribeSpec({

    afterEach { clearAllMocks() }
    afterSpec { unmockkAll() }

    describe("supports()") {
      describe("false") {
        it("class is OpenApiWebMvcResource and method name is openapiJson") {
          val methodParameter = mockk<MethodParameter>(relaxed = true)
          val customResponseBodyAdvice = CustomResponseBodyAdvice(ObjectMapper())
          every { methodParameter.declaringClass } returns OpenApiWebMvcResource::class.java
          every { methodParameter.method?.name } returns "openapiJson"
          customResponseBodyAdvice.supports(methodParameter, MappingJackson2HttpMessageConverter::class.java).shouldBeFalse()
        }

        it("class is SwaggerConfigResource and method name is openapiJson") {
          val methodParameter = mockk<MethodParameter>(relaxed = true)
          val customResponseBodyAdvice = CustomResponseBodyAdvice(ObjectMapper())
          every { methodParameter.declaringClass } returns SwaggerConfigResource::class.java
          every { methodParameter.method?.name } returns "openapiJson"
          customResponseBodyAdvice.supports(methodParameter, MappingJackson2HttpMessageConverter::class.java).shouldBeFalse()
        }
      }

      describe("true") {
        it("class is OpenApiWebMvcResource and method is null") {
          val methodParameter = mockk<MethodParameter>(relaxed = true)
          val customResponseBodyAdvice = CustomResponseBodyAdvice(ObjectMapper())
          every { methodParameter.declaringClass } returns OpenApiWebMvcResource::class.java
          every { methodParameter.method } returns null
          customResponseBodyAdvice.supports(methodParameter, MappingJackson2HttpMessageConverter::class.java).shouldBeTrue()
        }

        it("class is OpenApiWebMvcResource and method name is not openapiJson") {
          val methodParameter = mockk<MethodParameter>(relaxed = true)
          val customResponseBodyAdvice = CustomResponseBodyAdvice(ObjectMapper())
          every { methodParameter.declaringClass } returns OpenApiWebMvcResource::class.java
          every { methodParameter.method?.name } returns "Lorem Ipsum"
          customResponseBodyAdvice.supports(methodParameter, MappingJackson2HttpMessageConverter::class.java).shouldBeTrue()
        }

        it("class is not OpenApiWebMvcResource and method name is openapiJson") {
          val methodParameter = mockk<MethodParameter>(relaxed = true)
          val customResponseBodyAdvice = CustomResponseBodyAdvice(ObjectMapper())
          every { methodParameter.declaringClass } returns String::class.java
          every { methodParameter.method?.name } returns "openapiJson"
          customResponseBodyAdvice.supports(methodParameter, MappingJackson2HttpMessageConverter::class.java).shouldBeTrue()
        }

        it("class is not OpenApiWebMvcResource and method name is not openapiJson") {
          val methodParameter = mockk<MethodParameter>(relaxed = true)
          val customResponseBodyAdvice = CustomResponseBodyAdvice(ObjectMapper())
          every { methodParameter.declaringClass } returns String::class.java
          every { methodParameter.method?.name } returns "Lorem Ipsum"
          customResponseBodyAdvice.supports(methodParameter, MappingJackson2HttpMessageConverter::class.java).shouldBeTrue()
        }
      }
    }

    describe("beforeBodyWrite()") {
      describe("body") {
        it("is ResponseSuccess") {
          val customResponseBodyAdvice = CustomResponseBodyAdvice(ObjectMapper())
          val body = customResponseBodyAdvice.beforeBodyWrite(
            ResponseSuccess(
              "responseContent - Lorem Ipsum",
              "responseMessage - Lorem Ipsum",
              mockk<ServerHttpRequest>(relaxed = true),
            ),
            mockk<MethodParameter>(relaxed = true),
            mockk<MediaType>(relaxed = true),
            KotlinSerializationJsonHttpMessageConverter::class.java,
            mockk<ServerHttpRequest>(relaxed = true),
            mockk<ServerHttpResponse>(relaxed = true),
          )
          body.shouldBeInstanceOf<ResponseSuccess<*>>()
        }

        it("is ResponseProblem (3xx)") {
          val customResponseBodyAdvice = CustomResponseBodyAdvice(ObjectMapper())
          val problemDetail = ProblemDetail.forStatus(300)
          val apiProblemDetail = ApiProblemDetail(problemDetail)
          val responseProblem = ResponseProblem(apiProblemDetail, "Lorem Ipsum", mockk<HttpServletRequest>(relaxed = true))
          val body = customResponseBodyAdvice.beforeBodyWrite(
            responseProblem,
            mockk<MethodParameter>(relaxed = true),
            mockk<MediaType>(relaxed = true),
            KotlinSerializationJsonHttpMessageConverter::class.java,
            mockk<ServerHttpRequest>(relaxed = true),
            mockk<ServerHttpResponse>(relaxed = true),
          )
          body.shouldBeInstanceOf<ResponseProblem>()
        }

        it("is ResponseProblem (4xx)") {
          val customResponseBodyAdvice = CustomResponseBodyAdvice(ObjectMapper())
          val problemDetail = ProblemDetail.forStatus(400)
          val apiProblemDetail = ApiProblemDetail(problemDetail)
          val responseProblem = ResponseProblem(apiProblemDetail, "Lorem Ipsum", mockk<HttpServletRequest>(relaxed = true))
          val body = customResponseBodyAdvice.beforeBodyWrite(
            responseProblem,
            mockk<MethodParameter>(relaxed = true),
            mockk<MediaType>(relaxed = true),
            KotlinSerializationJsonHttpMessageConverter::class.java,
            mockk<ServerHttpRequest>(relaxed = true),
            mockk<ServerHttpResponse>(relaxed = true),
          )
          body.shouldBeInstanceOf<ResponseProblem>()
        }

        it("is ResponseProblem (5xx)") {
          val customResponseBodyAdvice = CustomResponseBodyAdvice(ObjectMapper())
          val problemDetail = ProblemDetail.forStatus(500)
          val apiProblemDetail = ApiProblemDetail(problemDetail)
          val responseProblem = ResponseProblem(apiProblemDetail, "Lorem Ipsum", mockk<HttpServletRequest>(relaxed = true))
          val body = customResponseBodyAdvice.beforeBodyWrite(
            responseProblem,
            mockk<MethodParameter>(relaxed = true),
            mockk<MediaType>(relaxed = true),
            KotlinSerializationJsonHttpMessageConverter::class.java,
            mockk<ServerHttpRequest>(relaxed = true),
            mockk<ServerHttpResponse>(relaxed = true),
          )
          body.shouldBeInstanceOf<ResponseProblem>()
        }

        it("is Throwable and is not ErrorResponseException") {
          val customResponseBodyAdvice = CustomResponseBodyAdvice(ObjectMapper())
          val body = customResponseBodyAdvice.beforeBodyWrite(
            Exception("Lorem Ipsum"),
            mockk<MethodParameter>(relaxed = true),
            mockk<MediaType>(relaxed = true),
            KotlinSerializationJsonHttpMessageConverter::class.java,
            mockk<ServerHttpRequest>(relaxed = true),
            mockk<ServerHttpResponse>(relaxed = true),
          )
          body.shouldBeInstanceOf<ResponseProblem>()
        }

        it("is ErrorResponseException") {
          val customResponseBodyAdvice = CustomResponseBodyAdvice(ObjectMapper())
          val body = customResponseBodyAdvice.beforeBodyWrite(
            ErrorResponseException(HttpStatus.I_AM_A_TEAPOT),
            mockk<MethodParameter>(relaxed = true),
            mockk<MediaType>(relaxed = true),
            KotlinSerializationJsonHttpMessageConverter::class.java,
            mockk<ServerHttpRequest>(relaxed = true),
            mockk<ServerHttpResponse>(relaxed = true),
          )
          body.shouldBeInstanceOf<ResponseProblem>()
        }

        it("is ProblemDetail") {
          val customResponseBodyAdvice = CustomResponseBodyAdvice(ObjectMapper())
          val body = customResponseBodyAdvice.beforeBodyWrite(
            ProblemDetail.forStatus(400),
            mockk<MethodParameter>(relaxed = true),
            mockk<MediaType>(relaxed = true),
            KotlinSerializationJsonHttpMessageConverter::class.java,
            mockk<ServerHttpRequest>(relaxed = true),
            mockk<ServerHttpResponse>(relaxed = true),
          )
          body.shouldBeInstanceOf<ResponseProblem>()
        }

        it("is null") {
          val customResponseBodyAdvice = CustomResponseBodyAdvice(ObjectMapper())
          val method = ClassUtils.getMethod(LrmListServiceDefault::class.java, "create", LrmListCreate::class.java, String::class.java)
          val returnType = MethodParameter(method, -1)
          val body =
            customResponseBodyAdvice.beforeBodyWrite(
              null,
              returnType,
              mockk<MediaType>(relaxed = true),
              KotlinSerializationJsonHttpMessageConverter::class.java,
              mockk<ServerHttpRequest>(relaxed = true),
              mockk<ServerHttpResponse>(relaxed = true),
            )
          body.shouldBeInstanceOf<ResponseProblem>()
        }
      }

      describe("method") {
        it("is null (does not ignore response binding)") {
          val customResponseBodyAdvice = CustomResponseBodyAdvice(ObjectMapper())
          val methodParameter = mockk<MethodParameter>(relaxed = true)
          every { methodParameter.method } returns null
          val body = customResponseBodyAdvice.beforeBodyWrite(
            ResponseSuccess(
              "responseContent - Lorem Ipsum",
              "responseMessage - Lorem Ipsum",
              mockk<ServerHttpRequest>(relaxed = true),
            ),
            methodParameter,
            mockk<MediaType>(relaxed = true),
            KotlinSerializationJsonHttpMessageConverter::class.java,
            mockk<ServerHttpRequest>(relaxed = true),
            mockk<ServerHttpResponse>(relaxed = true),
          )
          body.shouldBeInstanceOf<ResponseSuccess<*>>()
        }

        it("ignores response binding and is within a rest controller class") {
          @RestController
          class TestClass {
            @SuppressWarnings("unused")
            @IgnoreResponseBinding
            fun handle(body: String): String {
              return ""
            }
          }
          val customResponseBodyAdvice = CustomResponseBodyAdvice(ObjectMapper())
          val method = ClassUtils.getMethod(TestClass::class.java, "handle", String::class.java)
          val returnType = MethodParameter(method, -1)
          val body = customResponseBodyAdvice.beforeBodyWrite(
            "Lorem Ipsum",
            returnType,
            mockk<MediaType>(relaxed = true),
            KotlinSerializationJsonHttpMessageConverter::class.java,
            mockk<ServerHttpRequest>(relaxed = true),
            mockk<ServerHttpResponse>(relaxed = true),
          )
          body.shouldBeInstanceOf<String>()
        }

        it("ignores response binding and is not within a rest controller class") {
          class TestClass {
            @SuppressWarnings("unused")
            @IgnoreResponseBinding
            fun handle(): String {
              return ""
            }
          }
          val customResponseBodyAdvice = CustomResponseBodyAdvice(ObjectMapper())
          val method = ClassUtils.getMethod(TestClass::class.java, "handle")
          val returnType = MethodParameter(method, -1)
          val body = customResponseBodyAdvice.beforeBodyWrite(
            "Lorem Ipsum",
            returnType,
            mockk<MediaType>(relaxed = true),
            KotlinSerializationJsonHttpMessageConverter::class.java,
            mockk<ServerHttpRequest>(relaxed = true),
            mockk<ServerHttpResponse>(relaxed = true),
          )
          body.shouldBeInstanceOf<String>()
        }

        it("does not ignore response binding and is within a rest controller class") {
          @RestController
          class TestClass {
            @SuppressWarnings("unused")
            fun handle(body: String): String {
              return ""
            }
          }
          val customResponseBodyAdvice = CustomResponseBodyAdvice(ObjectMapper())
          val method = ClassUtils.getMethod(TestClass::class.java, "handle", String::class.java)
          val returnType = MethodParameter(method, -1)
          val body = customResponseBodyAdvice.beforeBodyWrite(
            "Lorem Ipsum",
            returnType,
            mockk<MediaType>(relaxed = true),
            KotlinSerializationJsonHttpMessageConverter::class.java,
            mockk<ServerHttpRequest>(relaxed = true),
            mockk<ServerHttpResponse>(relaxed = true),
          )
          body.shouldBeInstanceOf<ResponseSuccess<*>>()
        }

        it("does not ignore response binding and is not within a rest controller class") {
          class TestClass {
            @SuppressWarnings("unused")
            fun handle(): String {
              return ""
            }
          }
          val customResponseBodyAdvice = CustomResponseBodyAdvice(ObjectMapper())
          val method = ClassUtils.getMethod(TestClass::class.java, "handle")
          val returnType = MethodParameter(method, -1)
          val body = customResponseBodyAdvice.beforeBodyWrite(
            "Lorem Ipsum",
            returnType,
            mockk<MediaType>(relaxed = true),
            KotlinSerializationJsonHttpMessageConverter::class.java,
            mockk<ServerHttpRequest>(relaxed = true),
            mockk<ServerHttpResponse>(relaxed = true),
          )
          body.shouldBeInstanceOf<String>()
        }
      }
    }
  })
