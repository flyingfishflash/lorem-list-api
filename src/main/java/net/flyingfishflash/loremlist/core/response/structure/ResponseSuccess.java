package net.flyingfishflash.loremlist.core.response.structure;

import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.springframework.http.server.ServerHttpRequest;

public record ResponseSuccess<T>(
    UUID id,
    DispositionOfSuccess disposition,
    String method,
    String instance,
    String message,
    int size,
    T content)
    implements Response<T> {

  /** Create an API ResponseSuccess with content from any object type */
  public ResponseSuccess(T responseContent, String responseMessage, HttpServletRequest request) {
    this(
        UUID.randomUUID(),
        DispositionOfSuccess.SUCCESS,
        request.getMethod().toLowerCase(),
        request.getRequestURI(),
        responseMessage,
        ResponseContentSize.calculate(responseContent),
        responseContent);
  }

  /** Create an API ResponseSuccess with content from any object type */
  public ResponseSuccess(T responseContent, String responseMessage, ServerHttpRequest request) {
    this(
        UUID.randomUUID(),
        DispositionOfSuccess.SUCCESS,
        request.getMethod().name().toLowerCase(),
        request.getURI().getPath().toLowerCase(),
        responseMessage,
        ResponseContentSize.calculate(responseContent),
        responseContent);
  }
}
