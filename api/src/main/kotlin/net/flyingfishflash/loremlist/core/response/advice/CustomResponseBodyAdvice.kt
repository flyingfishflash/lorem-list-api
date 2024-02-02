package net.flyingfishflash.loremlist.core.response.advice

import io.github.oshai.kotlinlogging.KotlinLogging
import net.flyingfishflash.loremlist.core.response.structure.ApplicationResponse
import net.flyingfishflash.loremlist.core.response.structure.IgnoreResponseBinding
import net.flyingfishflash.loremlist.core.response.structure.Response
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

private val logger = KotlinLogging.logger {}

@RestControllerAdvice
class CustomResponseBodyAdvice : ResponseBodyAdvice<Any?> {
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
    var applicationResponse: ApplicationResponse<*>? = null

    when {
      // --
      o is ApplicationResponse<*> -> {
        applicationResponse = o
      }
      // --
      o is Throwable && o !is ErrorResponseException -> {
        val problemDetail = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR)
        // ProblemDetailUtility.setCustomPropertiesFromThrowable(problemDetail, o)
        applicationResponse =
          Response(
            problemDetail,
            "Problem found in CustomResponseBodyAdvice",
            serverHttpRequest.method.toString(),
          )
      }
      // --
      o is ErrorResponseException -> {
        val pd: ProblemDetail = o.body
        // ProblemDetailUtility.setCustomPropertiesFromThrowable(pd, o)
        applicationResponse =
          Response(
            pd,
            "CustomResponseBodyAdvice <- ProblemDetail <- ErrorResponseException",
            serverHttpRequest.method.toString(),
          )
      }
      // --
      o is ProblemDetail -> {
        applicationResponse =
          Response(
            o,
            "CustomResponseBodyAdvice <- ProblemDetail",
            serverHttpRequest.method.toString(),
          )
      }
      // --
      methodParameter.containingClass.isAnnotationPresent(RestController::class.java) &&
        (method != null) &&
        !method.isAnnotationPresent(IgnoreResponseBinding::class.java)
      -> {
        logger.warn { "Object wrapped in Response with successful disposition by default $o" }
        applicationResponse =
          Response(
            o,
            "Object wrapped in Response with successful disposition by default",
            serverHttpRequest.method.toString(),
            serverHttpRequest.uri,
          )
      }
    }

    // TODO: use kotlinx-serialization to output json response
    applicationResponse.let { logger.info { "json response (todo): $it" } }

    return applicationResponse ?: o.let {
      logger.warn { "Returning object from CustomResponseBodyAdvice.beforeBodyWrite() without examination: $o" }
    }
  }
}
