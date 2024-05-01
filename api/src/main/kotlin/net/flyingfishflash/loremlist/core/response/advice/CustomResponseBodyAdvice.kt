package net.flyingfishflash.loremlist.core.response.advice

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.flyingfishflash.loremlist.core.response.structure.IgnoreResponseBinding
import net.flyingfishflash.loremlist.core.response.structure.ResponseProblem
import net.flyingfishflash.loremlist.core.response.structure.ResponseSuccess
import org.springframework.core.MethodParameter
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ProblemDetail
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServerHttpResponse
import org.springframework.web.ErrorResponseException
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice
import java.util.UUID

@RestControllerAdvice
class CustomResponseBodyAdvice : ResponseBodyAdvice<Any?> {
  private val logger = KotlinLogging.logger {}

  private fun logResponseProblem(applicationResponse: ResponseProblem) {
    when {
      HttpStatus.valueOf(applicationResponse.content.status).is5xxServerError -> {
        logger.error { Json.encodeToString(applicationResponse) }
      }

      HttpStatus.valueOf(applicationResponse.content.status).is4xxClientError -> {
        logger.warn { Json.encodeToString(applicationResponse) }
      }

      else -> logger.info {
        Json.encodeToString(applicationResponse)
      }
    }
  }

  override fun supports(methodParameter: MethodParameter, converterType: Class<out HttpMessageConverter<*>>): Boolean {
    val openApiClasses = listOf(
      "class org.springdoc.webmvc.api.OpenApiWebMvcResource",
      "class org.springdoc.webmvc.ui.SwaggerConfigResource",
    )
    val declaringClass = methodParameter.declaringClass.toString()
    val methodName = methodParameter.method?.name ?: ""
    val isOpenApi = openApiClasses.contains(declaringClass) && methodName == "openapiJson"
    val supported = !isOpenApi
    return supported
  }

  override fun beforeBodyWrite(
    o: Any?,
    methodParameter: MethodParameter,
    mediaType: MediaType,
    selectedConverterType: Class<out HttpMessageConverter<*>>,
    serverHttpRequest: ServerHttpRequest,
    serverHttpResponse: ServerHttpResponse,
  ): Any? {
    val method = methodParameter.method
    val methodIgnoresResponseBinding = method?.isAnnotationPresent(IgnoreResponseBinding::class.java) ?: false
    val methodClassIsRestController = methodParameter.containingClass.isAnnotationPresent(RestController::class.java)

    if (!selectedConverterType.name.contains("KotlinSerializationJsonHttpMessageConverter")) {
      logger.info { "message converter type: ${selectedConverterType.name}" }
    }

    val applicationResponse = when {
      o is ResponseSuccess<*> -> o
      o is ResponseProblem -> o.also {
        logResponseProblem(it)
      }
      o is Throwable && o !is ErrorResponseException -> {
        logger.warn { "o is Throwable && o !is ErrorResponseException" }
        ResponseProblem(
          problemDetail = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR),
          request = serverHttpRequest,
        ).also {
          logResponseProblem(it)
        }
      }
      o is ErrorResponseException -> {
        logger.warn { "o is ErrorResponseException" }
        ResponseProblem(problemDetail = o.body, request = serverHttpRequest).also { logResponseProblem(it) }
      }
      o is ProblemDetail -> {
        logger.warn { "o is ProblemDetail" }
        ResponseProblem(problemDetail = o, request = serverHttpRequest).also { logResponseProblem(it) }
      }
      o == null -> {
        logger.warn { "o == null" }
        ResponseProblem(
          problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "the response object passed into custom body advice is null",
          ),
          responseMessage = "there was a problem generating a response to this request",
          request = serverHttpRequest,
        ).also {
          logResponseProblem(it)
        }
      }
      !methodIgnoresResponseBinding && methodClassIsRestController -> {
        ResponseSuccess(
          responseContent = o,
          responseMessage = "",
          request = serverHttpRequest,
        ).also {
          logger.warn { "ResponseSuccess for: $o" }
        }
      }
      else -> {
        val id = UUID.randomUUID()
        logger.warn { "[$id] Returning object from CustomResponseBodyAdvice.beforeBodyWrite() without examination" }
        logger.warn { "[$id] methodIgnoresResponseBinding: $methodIgnoresResponseBinding" }
        logger.warn { "[$id] methodClassIsRestController: $methodClassIsRestController" }
        logger.warn { "[$id] type: ${o.javaClass}" }
        logger.warn { "[$id] value: $o" }
        o
      }
    }
    return applicationResponse
  }
}
