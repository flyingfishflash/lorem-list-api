package net.flyingfishflash.loremlist.core.response.advice;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;
import net.flyingfishflash.loremlist.core.response.structure.IgnoreResponseBinding;
import net.flyingfishflash.loremlist.core.response.structure.ResponseProblem;
import net.flyingfishflash.loremlist.core.response.structure.ResponseSuccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@RestControllerAdvice
public class CustomResponseBodyAdvice implements ResponseBodyAdvice<Object> {

  private static final Logger logger = LoggerFactory.getLogger(CustomResponseBodyAdvice.class);
  private static final List<String> OPEN_API_CLASSES =
      List.of(
          "class org.springdoc.webmvc.api.OpenApiWebMvcResource",
          "class org.springdoc.webmvc.ui.SwaggerConfigResource");

  private final ObjectMapper objectMapper;

  public CustomResponseBodyAdvice(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  private String toJson(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (JsonProcessingException e) {
      return String.valueOf(value);
    }
  }

  private void logResponseProblem(ResponseProblem applicationResponse) {
    HttpStatus status = HttpStatus.valueOf(applicationResponse.content().status());
    if (status.is5xxServerError()) {
      logger.error(toJson(applicationResponse));
    } else if (status.is4xxClientError()) {
      logger.warn(toJson(applicationResponse));
    } else {
      logger.info(toJson(applicationResponse));
    }
  }

  @Override
  public boolean supports(
      MethodParameter methodParameter, Class<? extends HttpMessageConverter<?>> converterType) {
    String declaringClass = String.valueOf(methodParameter.getDeclaringClass());
    Method method = methodParameter.getMethod();
    String methodName = method != null ? method.getName() : "";
    boolean isOpenApi =
        OPEN_API_CLASSES.contains(declaringClass) && methodName.equals("openapiJson");
    return !isOpenApi;
  }

  @Override
  public Object beforeBodyWrite(
      Object body,
      MethodParameter methodParameter,
      MediaType mediaType,
      Class<? extends HttpMessageConverter<?>> selectedConverterType,
      ServerHttpRequest serverHttpRequest,
      ServerHttpResponse serverHttpResponse) {
    Method method = methodParameter.getMethod();
    boolean methodIgnoresResponseBinding =
        method != null && method.isAnnotationPresent(IgnoreResponseBinding.class);
    boolean methodClassIsRestController =
        methodParameter.getContainingClass().isAnnotationPresent(RestController.class);

    if (!selectedConverterType.getName().contains("KotlinSerializationJsonHttpMessageConverter")) {
      logger.info("message converter type: {}", selectedConverterType.getName());
    }

    if (body instanceof ResponseSuccess<?> responseSuccess) {
      return responseSuccess;
    } else if (body instanceof ResponseProblem responseProblem) {
      logResponseProblem(responseProblem);
      return responseProblem;
    } else if (body instanceof ErrorResponseException errorResponseException) {
      logger.warn("body is ErrorResponseException");
      ResponseProblem responseProblem =
          new ResponseProblem(errorResponseException.getBody(), serverHttpRequest);
      logResponseProblem(responseProblem);
      return responseProblem;
    } else if (body instanceof Throwable) {
      logger.warn("body is Throwable && body !is ErrorResponseException");
      ResponseProblem responseProblem =
          new ResponseProblem(
              ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR), serverHttpRequest);
      logResponseProblem(responseProblem);
      return responseProblem;
    } else if (body instanceof ProblemDetail problemDetail) {
      logger.warn("body is ProblemDetail");
      ResponseProblem responseProblem = new ResponseProblem(problemDetail, serverHttpRequest);
      logResponseProblem(responseProblem);
      return responseProblem;
    } else if (body == null) {
      logger.warn("body == null");
      ResponseProblem responseProblem =
          new ResponseProblem(
              ProblemDetail.forStatusAndDetail(
                  HttpStatus.INTERNAL_SERVER_ERROR,
                  "the response object passed into custom body advice is null"),
              "there was a problem generating a response to this request",
              serverHttpRequest);
      logResponseProblem(responseProblem);
      return responseProblem;
    } else if (!methodIgnoresResponseBinding && methodClassIsRestController) {
      ResponseSuccess<Object> responseSuccess = new ResponseSuccess<>(body, "", serverHttpRequest);
      logger.warn("ResponseSuccess for: {}", body);
      return responseSuccess;
    } else {
      UUID id = UUID.randomUUID();
      logger.warn(
          "[{}] Returning object from CustomResponseBodyAdvice.beforeBodyWrite() without examination",
          id);
      logger.warn("[{}] methodIgnoresResponseBinding: {}", id, methodIgnoresResponseBinding);
      logger.warn("[{}] methodClassIsRestController: {}", id, methodClassIsRestController);
      logger.warn("[{}] type: {}", id, body.getClass());
      logger.warn("[{}] value: {}", id, body);
      return body;
    }
  }
}
