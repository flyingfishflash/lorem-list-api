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

@RestControllerAdvice
class CustomResponseBodyAdvice : ResponseBodyAdvice<Any?> {
  private val logger = KotlinLogging.logger {}

  override fun supports(
    returnType: MethodParameter,
    converterType: Class<out HttpMessageConverter<*>>,
  ): Boolean {
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
      o is Response<*> -> {
        applicationResponse = o
      }
      // --
      o is Throwable && o !is ErrorResponseException -> {
        applicationResponse = ResponseProblem(ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR), serverHttpRequest)
        logger.info { "response as json: " + Json.encodeToString(applicationResponse) }
      }
      // --
      o is ErrorResponseException -> {
        applicationResponse = ResponseProblem(problemDetail = o.body, serverHttpRequest)
        logger.info { "response as json: " + Json.encodeToString(applicationResponse) }
      }
      // --
      o is ProblemDetail -> {
        val responseProblem = ResponseProblem(problemDetail = o, serverHttpRequest)
        applicationResponse = responseProblem
        logger.info { "response as json: " + Json.encodeToString(responseProblem) }
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
      logger.warn { "message converter type: ${selectedConverterType.name}" }
    }

    return applicationResponse ?: o.also {
      logger.warn { "Returning object from CustomResponseBodyAdvice.beforeBodyWrite() without examination: $it" }
    }
  }
}
