package net.flyingfishflash.loremlist.core.response.advice

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.flyingfishflash.loremlist.core.response.structure.IgnoreResponseBinding
import net.flyingfishflash.loremlist.core.response.structure.Response
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

  override fun supports(returnType: MethodParameter, converterType: Class<out HttpMessageConverter<*>>): Boolean {
    // exclude swagger from beforeBodyWrite
    return (returnType.declaringClass.toString() != "class org.springdoc.webmvc.ui.SwaggerWelcomeWebMvc") &&
      (returnType.method?.name != "openapiJson")
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
    var applicationResponse: Response<*>? = null

    when {
      o is ResponseSuccess<*> -> {
        applicationResponse = o
      }
      // --
      o is ResponseProblem -> {
        applicationResponse = o
        logResponseProblem(applicationResponse)
      }
      // --
      o is Throwable && o !is ErrorResponseException -> {
        applicationResponse = ResponseProblem(ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR), serverHttpRequest)
        logger.warn { "o is Throwable && o !is ErrorResponseException" }
        logResponseProblem(applicationResponse)
      }
      // --
      o is ErrorResponseException -> {
        applicationResponse = ResponseProblem(problemDetail = o.body, serverHttpRequest)
        logger.warn { "o is ErrorResponseException" }
        logResponseProblem(applicationResponse)
      }
      // --
      o is ProblemDetail -> {
        val responseProblem = ResponseProblem(problemDetail = o, serverHttpRequest)
        applicationResponse = responseProblem
        logger.warn { "o is ProblemDetail" }
        logResponseProblem(applicationResponse)
      }
      // --
      methodParameter.containingClass.isAnnotationPresent(RestController::class.java) &&
        (method != null) &&
        !method.isAnnotationPresent(IgnoreResponseBinding::class.java)
      -> {
        applicationResponse = ResponseSuccess(
          responseContent = o,
          responseMessage = "from Custom Response Body Advice, Success by Default",
          request = serverHttpRequest,
        ).also {
          logger.warn { "ResponseSuccess for: $o" }
        }
      }
    }

    if (!selectedConverterType.name.contains("KotlinSerializationJsonHttpMessageConverter")) {
      logger.info { "message converter type: ${selectedConverterType.name}" }
    }

    return applicationResponse ?: o.also {
      val id = UUID.randomUUID()
      logger.warn { "[$id] Returning object from CustomResponseBodyAdvice.beforeBodyWrite() without examination" }
      logger.warn { "[$id] type: ${it?.javaClass}" }
      logger.warn { "[$id] value: $it" }
    }
  }
}
