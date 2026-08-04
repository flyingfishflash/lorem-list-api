package net.flyingfishflash.loremlist.unit.core.response.advice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import net.flyingfishflash.loremlist.core.response.advice.CustomResponseBodyAdvice;
import net.flyingfishflash.loremlist.core.response.structure.ApiProblemDetail;
import net.flyingfishflash.loremlist.core.response.structure.IgnoreResponseBinding;
import net.flyingfishflash.loremlist.core.response.structure.ResponseProblem;
import net.flyingfishflash.loremlist.core.response.structure.ResponseSuccess;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springdoc.webmvc.api.OpenApiWebMvcResource;
import org.springdoc.webmvc.ui.SwaggerConfigResource;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.json.KotlinSerializationJsonHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.util.ClassUtils;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.annotation.RestController;

class CustomResponseBodyAdviceTests {

  private static MethodParameter mockMethodParameter() {
    MethodParameter methodParameter = mock(MethodParameter.class);
    // avoids an NPE from isAnnotationPresent(RestController.class) inside beforeBodyWrite()
    when(methodParameter.getContainingClass()).thenReturn((Class) Object.class);
    return methodParameter;
  }

  private static HttpServletRequest mockHttpServletRequest() {
    HttpServletRequest mockHttpServletRequest = mock(HttpServletRequest.class);
    when(mockHttpServletRequest.getMethod()).thenReturn("GET");
    when(mockHttpServletRequest.getRequestURI()).thenReturn("/irrelevant");
    return mockHttpServletRequest;
  }

  private static ServerHttpRequest mockServerHttpRequest() {
    ServerHttpRequest mockServerHttpRequest = mock(ServerHttpRequest.class);
    when(mockServerHttpRequest.getMethod()).thenReturn(org.springframework.http.HttpMethod.GET);
    when(mockServerHttpRequest.getURI()).thenReturn(java.net.URI.create("/irrelevant"));
    return mockServerHttpRequest;
  }

  private static class MethodFixtures {
    @SuppressWarnings("unused")
    public void openapiJson() {}

    @SuppressWarnings("unused")
    public void notOpenapiJson() {}
  }

  private static Method fixtureMethod(String name) throws NoSuchMethodException {
    return MethodFixtures.class.getDeclaredMethod(name);
  }

  @Nested
  class Supports {

    @Test
    void classIsOpenApiWebMvcResourceAndMethodNameIsOpenapiJson() throws Exception {
      MethodParameter methodParameter = mockMethodParameter();
      CustomResponseBodyAdvice customResponseBodyAdvice =
          new CustomResponseBodyAdvice(new ObjectMapper());
      when(methodParameter.getDeclaringClass()).thenReturn((Class) OpenApiWebMvcResource.class);
      when(methodParameter.getMethod()).thenReturn(fixtureMethod("openapiJson"));
      assertThat(
              customResponseBodyAdvice.supports(
                  methodParameter, MappingJackson2HttpMessageConverter.class))
          .isFalse();
    }

    @Test
    void classIsSwaggerConfigResourceAndMethodNameIsOpenapiJson() throws Exception {
      MethodParameter methodParameter = mockMethodParameter();
      CustomResponseBodyAdvice customResponseBodyAdvice =
          new CustomResponseBodyAdvice(new ObjectMapper());
      when(methodParameter.getDeclaringClass()).thenReturn((Class) SwaggerConfigResource.class);
      when(methodParameter.getMethod()).thenReturn(fixtureMethod("openapiJson"));
      assertThat(
              customResponseBodyAdvice.supports(
                  methodParameter, MappingJackson2HttpMessageConverter.class))
          .isFalse();
    }

    @Test
    void classIsOpenApiWebMvcResourceAndMethodIsNull() {
      MethodParameter methodParameter = mockMethodParameter();
      CustomResponseBodyAdvice customResponseBodyAdvice =
          new CustomResponseBodyAdvice(new ObjectMapper());
      when(methodParameter.getDeclaringClass()).thenReturn((Class) OpenApiWebMvcResource.class);
      when(methodParameter.getMethod()).thenReturn(null);
      assertThat(
              customResponseBodyAdvice.supports(
                  methodParameter, MappingJackson2HttpMessageConverter.class))
          .isTrue();
    }

    @Test
    void classIsOpenApiWebMvcResourceAndMethodNameIsNotOpenapiJson() throws Exception {
      MethodParameter methodParameter = mockMethodParameter();
      CustomResponseBodyAdvice customResponseBodyAdvice =
          new CustomResponseBodyAdvice(new ObjectMapper());
      when(methodParameter.getDeclaringClass()).thenReturn((Class) OpenApiWebMvcResource.class);
      when(methodParameter.getMethod()).thenReturn(fixtureMethod("notOpenapiJson"));
      assertThat(
              customResponseBodyAdvice.supports(
                  methodParameter, MappingJackson2HttpMessageConverter.class))
          .isTrue();
    }

    @Test
    void classIsNotOpenApiWebMvcResourceAndMethodNameIsOpenapiJson() throws Exception {
      MethodParameter methodParameter = mockMethodParameter();
      CustomResponseBodyAdvice customResponseBodyAdvice =
          new CustomResponseBodyAdvice(new ObjectMapper());
      when(methodParameter.getDeclaringClass()).thenReturn((Class) String.class);
      when(methodParameter.getMethod()).thenReturn(fixtureMethod("openapiJson"));
      assertThat(
              customResponseBodyAdvice.supports(
                  methodParameter, MappingJackson2HttpMessageConverter.class))
          .isTrue();
    }

    @Test
    void classIsNotOpenApiWebMvcResourceAndMethodNameIsNotOpenapiJson() throws Exception {
      MethodParameter methodParameter = mockMethodParameter();
      CustomResponseBodyAdvice customResponseBodyAdvice =
          new CustomResponseBodyAdvice(new ObjectMapper());
      when(methodParameter.getDeclaringClass()).thenReturn((Class) String.class);
      when(methodParameter.getMethod()).thenReturn(fixtureMethod("notOpenapiJson"));
      assertThat(
              customResponseBodyAdvice.supports(
                  methodParameter, MappingJackson2HttpMessageConverter.class))
          .isTrue();
    }
  }

  @Nested
  class BeforeBodyWriteBody {

    private final CustomResponseBodyAdvice customResponseBodyAdvice =
        new CustomResponseBodyAdvice(new ObjectMapper());

    @Test
    void isResponseSuccess() {
      Object body =
          customResponseBodyAdvice.beforeBodyWrite(
              new ResponseSuccess<>(
                  "responseContent - Lorem Ipsum",
                  "responseMessage - Lorem Ipsum",
                  mockServerHttpRequest()),
              mockMethodParameter(),
              mock(MediaType.class),
              KotlinSerializationJsonHttpMessageConverter.class,
              mockServerHttpRequest(),
              mock(ServerHttpResponse.class));
      assertThat(body).isInstanceOf(ResponseSuccess.class);
    }

    @Test
    void isResponseProblem3xx() {
      ProblemDetail problemDetail = ProblemDetail.forStatus(300);
      ApiProblemDetail apiProblemDetail = new ApiProblemDetail(problemDetail);
      ResponseProblem responseProblem =
          new ResponseProblem(apiProblemDetail, "Lorem Ipsum", mockHttpServletRequest());
      Object body =
          customResponseBodyAdvice.beforeBodyWrite(
              responseProblem,
              mockMethodParameter(),
              mock(MediaType.class),
              KotlinSerializationJsonHttpMessageConverter.class,
              mockServerHttpRequest(),
              mock(ServerHttpResponse.class));
      assertThat(body).isInstanceOf(ResponseProblem.class);
    }

    @Test
    void isResponseProblem4xx() {
      ProblemDetail problemDetail = ProblemDetail.forStatus(400);
      ApiProblemDetail apiProblemDetail = new ApiProblemDetail(problemDetail);
      ResponseProblem responseProblem =
          new ResponseProblem(apiProblemDetail, "Lorem Ipsum", mockHttpServletRequest());
      Object body =
          customResponseBodyAdvice.beforeBodyWrite(
              responseProblem,
              mockMethodParameter(),
              mock(MediaType.class),
              KotlinSerializationJsonHttpMessageConverter.class,
              mockServerHttpRequest(),
              mock(ServerHttpResponse.class));
      assertThat(body).isInstanceOf(ResponseProblem.class);
    }

    @Test
    void isResponseProblem5xx() {
      ProblemDetail problemDetail = ProblemDetail.forStatus(500);
      ApiProblemDetail apiProblemDetail = new ApiProblemDetail(problemDetail);
      ResponseProblem responseProblem =
          new ResponseProblem(apiProblemDetail, "Lorem Ipsum", mockHttpServletRequest());
      Object body =
          customResponseBodyAdvice.beforeBodyWrite(
              responseProblem,
              mockMethodParameter(),
              mock(MediaType.class),
              KotlinSerializationJsonHttpMessageConverter.class,
              mockServerHttpRequest(),
              mock(ServerHttpResponse.class));
      assertThat(body).isInstanceOf(ResponseProblem.class);
    }

    @Test
    void isThrowableAndIsNotErrorResponseException() {
      Object body =
          customResponseBodyAdvice.beforeBodyWrite(
              new Exception("Lorem Ipsum"),
              mockMethodParameter(),
              mock(MediaType.class),
              KotlinSerializationJsonHttpMessageConverter.class,
              mockServerHttpRequest(),
              mock(ServerHttpResponse.class));
      assertThat(body).isInstanceOf(ResponseProblem.class);
    }

    @Test
    void isErrorResponseException() {
      Object body =
          customResponseBodyAdvice.beforeBodyWrite(
              new ErrorResponseException(HttpStatus.I_AM_A_TEAPOT),
              mockMethodParameter(),
              mock(MediaType.class),
              KotlinSerializationJsonHttpMessageConverter.class,
              mockServerHttpRequest(),
              mock(ServerHttpResponse.class));
      assertThat(body).isInstanceOf(ResponseProblem.class);
    }

    @Test
    void isProblemDetail() {
      Object body =
          customResponseBodyAdvice.beforeBodyWrite(
              ProblemDetail.forStatus(400),
              mockMethodParameter(),
              mock(MediaType.class),
              KotlinSerializationJsonHttpMessageConverter.class,
              mockServerHttpRequest(),
              mock(ServerHttpResponse.class));
      assertThat(body).isInstanceOf(ResponseProblem.class);
    }

    @Test
    void isNull() throws Exception {
      Method method =
          ClassUtils.getMethod(
              net.flyingfishflash.loremlist.domain.lrmlist.LrmListServiceDefault.class,
              "create",
              net.flyingfishflash.loremlist.domain.lrmlist.data.LrmListCreate.class,
              String.class);
      MethodParameter returnType = new MethodParameter(method, -1);
      Object body =
          customResponseBodyAdvice.beforeBodyWrite(
              null,
              returnType,
              mock(MediaType.class),
              KotlinSerializationJsonHttpMessageConverter.class,
              mockServerHttpRequest(),
              mock(ServerHttpResponse.class));
      assertThat(body).isInstanceOf(ResponseProblem.class);
    }
  }

  @Nested
  class BeforeBodyWriteMethod {

    private final CustomResponseBodyAdvice customResponseBodyAdvice =
        new CustomResponseBodyAdvice(new ObjectMapper());

    @Test
    void isNullDoesNotIgnoreResponseBinding() {
      MethodParameter methodParameter = mockMethodParameter();
      when(methodParameter.getMethod()).thenReturn(null);
      Object body =
          customResponseBodyAdvice.beforeBodyWrite(
              new ResponseSuccess<>(
                  "responseContent - Lorem Ipsum",
                  "responseMessage - Lorem Ipsum",
                  mockServerHttpRequest()),
              methodParameter,
              mock(MediaType.class),
              KotlinSerializationJsonHttpMessageConverter.class,
              mockServerHttpRequest(),
              mock(ServerHttpResponse.class));
      assertThat(body).isInstanceOf(ResponseSuccess.class);
    }

    @RestController
    private static class RestControllerWithParam {
      @SuppressWarnings("unused")
      @IgnoreResponseBinding
      public String handle(String body) {
        return "";
      }
    }

    @Test
    void ignoresResponseBindingAndIsWithinARestControllerClass() throws Exception {
      Method method = ClassUtils.getMethod(RestControllerWithParam.class, "handle", String.class);
      MethodParameter returnType = new MethodParameter(method, -1);
      Object body =
          customResponseBodyAdvice.beforeBodyWrite(
              "Lorem Ipsum",
              returnType,
              mock(MediaType.class),
              KotlinSerializationJsonHttpMessageConverter.class,
              mockServerHttpRequest(),
              mock(ServerHttpResponse.class));
      assertThat(body).isInstanceOf(String.class);
    }

    private static class PlainClassWithIgnoredMethod {
      @SuppressWarnings("unused")
      @IgnoreResponseBinding
      public String handle() {
        return "";
      }
    }

    @Test
    void ignoresResponseBindingAndIsNotWithinARestControllerClass() throws Exception {
      Method method = ClassUtils.getMethod(PlainClassWithIgnoredMethod.class, "handle");
      MethodParameter returnType = new MethodParameter(method, -1);
      Object body =
          customResponseBodyAdvice.beforeBodyWrite(
              "Lorem Ipsum",
              returnType,
              mock(MediaType.class),
              KotlinSerializationJsonHttpMessageConverter.class,
              mockServerHttpRequest(),
              mock(ServerHttpResponse.class));
      assertThat(body).isInstanceOf(String.class);
    }

    @RestController
    private static class RestControllerWithParamNotIgnored {
      @SuppressWarnings("unused")
      public String handle(String body) {
        return "";
      }
    }

    @Test
    void doesNotIgnoreResponseBindingAndIsWithinARestControllerClass() throws Exception {
      Method method =
          ClassUtils.getMethod(RestControllerWithParamNotIgnored.class, "handle", String.class);
      MethodParameter returnType = new MethodParameter(method, -1);
      Object body =
          customResponseBodyAdvice.beforeBodyWrite(
              "Lorem Ipsum",
              returnType,
              mock(MediaType.class),
              KotlinSerializationJsonHttpMessageConverter.class,
              mockServerHttpRequest(),
              mock(ServerHttpResponse.class));
      assertThat(body).isInstanceOf(ResponseSuccess.class);
    }

    private static class PlainClassNotIgnored {
      @SuppressWarnings("unused")
      public String handle() {
        return "";
      }
    }

    @Test
    void doesNotIgnoreResponseBindingAndIsNotWithinARestControllerClass() throws Exception {
      Method method = ClassUtils.getMethod(PlainClassNotIgnored.class, "handle");
      MethodParameter returnType = new MethodParameter(method, -1);
      Object body =
          customResponseBodyAdvice.beforeBodyWrite(
              "Lorem Ipsum",
              returnType,
              mock(MediaType.class),
              KotlinSerializationJsonHttpMessageConverter.class,
              mockServerHttpRequest(),
              mock(ServerHttpResponse.class));
      assertThat(body).isInstanceOf(String.class);
    }
  }
}
