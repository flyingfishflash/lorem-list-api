package net.flyingfishflash.loremlist.core.response.advice

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import net.flyingfishflash.loremlist.core.response.structure.ApplicationResponse
import net.flyingfishflash.loremlist.core.response.structure.Response
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
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
  @Autowired
  private val objectMapper: ObjectMapper? = null

  override fun supports(
    returnType: MethodParameter,
    converterType: Class<out HttpMessageConverter<*>>,
  ): Boolean {
    // exclude springdoc/swagger from beforeBodyWrite
    var supported: Boolean
    returnType.method?.name.let {
      supported = !it.equals("openapiJson")
    }
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
    // ApplicationResponse
    var response: ApplicationResponse<*>? = null

    // pristine
    when {
      // ApplicationResponse
      o is ApplicationResponse<*> -> {
        response = o
      }
      o is Throwable && o !is ErrorResponseException -> {
        val problemDetail = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR)
        // ProblemDetailUtility.setCustomPropertiesFromThrowable(problemDetail, o)
        response = Response(problemDetail, "Problem found in CustomResponseBodyAdvice", serverHttpRequest.method.toString())
      }
      o is ErrorResponseException -> {
        val pd: ProblemDetail = o.body
        // ProblemDetailUtility.setCustomPropertiesFromThrowable(pd, o)
        response = Response(pd, "CustomResponseBodyAdvice <- ProblemDetail <- ErrorResponseException", serverHttpRequest.method.toString())
      }
      o is ProblemDetail -> {
        response = Response(o, "CustomResponseBodyAdvice <- ProblemDetail", serverHttpRequest.method.toString())
      }
      methodParameter.containingClass.isAnnotationPresent(RestController::class.java) &&
        (method != null)
      // && !method.isAnnotationPresent(IgnoreResponseBinding::class.java)
      -> {
        logger.warn("Object wrapped in Response with successful disposition by default {}", o)
        response =
          Response(
            o,
            "Object wrapped in Response with successful disposition by default",
            serverHttpRequest.method.toString(),
            serverHttpRequest.uri,
          )
      }
    }

    if (response != null) {
      if (logger.isInfoEnabled) {
        try {
          logger.info(objectMapper!!.writeValueAsString(response))
        } catch (e: JsonProcessingException) {
          logger.error("JsonProcessingException while converting response object to json")
          logger.info("response object: {}", response)
        }
      }
      return response
    } else {
      logger.warn(
        "Returning object from CustomResponseBodyAdvice.beforeBodyWrite() without examination: {}",
        o,
      )
      return o
    }
  }

  companion object {
    private val logger: Logger = LoggerFactory.getLogger(CustomResponseBodyAdvice::class.java)
  }
}
