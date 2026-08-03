package net.flyingfishflash.loremlist.core.response.structure;

import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

public record ResponseProblem(
    UUID id,
    DispositionOfProblem disposition,
    String method,
    String instance,
    String message,
    int size,
    ApiProblemDetail content)
    implements Response<ApiProblemDetail> {

  /** Create an API ResponseProblem from an ApiProblem */
  public ResponseProblem(
      ApiProblemDetail apiProblemDetail, String responseMessage, HttpServletRequest request) {
    this(
        UUID.randomUUID(),
        DispositionOfProblem.calcDisposition(HttpStatus.valueOf(apiProblemDetail.status())),
        request.getMethod().toLowerCase(),
        request.getRequestURI(),
        responseMessage,
        ResponseContentSize.calculate(apiProblemDetail),
        apiProblemDetail);
  }

  /** Create an API ResponseProblem from a Spring ProblemDetail */
  public ResponseProblem(ProblemDetail problemDetail, ServerHttpRequest request) {
    this(
        UUID.randomUUID(),
        DispositionOfProblem.calcDisposition(HttpStatus.valueOf(problemDetail.getStatus())),
        request.getMethod().name().toLowerCase(),
        request.getURI().getPath().toLowerCase(),
        problemDetail.getDetail() != null
            ? problemDetail.getDetail()
            : "problemDetail.detail is null.",
        ResponseContentSize.calculate(problemDetail),
        new ApiProblemDetail(problemDetail));
  }

  /** Create an API ResponseProblem from a Spring ProblemDetail */
  public ResponseProblem(
      ProblemDetail problemDetail, String responseMessage, ServerHttpRequest request) {
    this(
        UUID.randomUUID(),
        DispositionOfProblem.calcDisposition(HttpStatus.valueOf(problemDetail.getStatus())),
        request.getMethod().name().toLowerCase(),
        request.getURI().getPath().toLowerCase(),
        responseMessage != null
            ? responseMessage
            : (problemDetail.getDetail() != null
                ? problemDetail.getDetail()
                : "responseMessage and problemDetail.detail are both null."),
        ResponseContentSize.calculate(problemDetail),
        new ApiProblemDetail(problemDetail));
  }

  /** Create an API ResponseProblem from an ApiProblemDetail */
  public ResponseProblem(
      ApiProblemDetail apiProblemDetail, String responseMessage, WebRequest request) {
    this(
        UUID.randomUUID(),
        DispositionOfProblem.calcDisposition(HttpStatus.valueOf(apiProblemDetail.status())),
        ((ServletWebRequest) request).getRequest().getMethod().toLowerCase(),
        instanceFrom(request),
        responseMessage != null
            ? responseMessage
            : (apiProblemDetail.detail() != null
                ? apiProblemDetail.detail()
                : "responseMessage and problemDetail.detail are both null."),
        ResponseContentSize.calculate(apiProblemDetail),
        apiProblemDetail);
  }

  private static String instanceFrom(WebRequest request) {
    String description = request.getDescription(false);
    int index = description.indexOf("uri=");
    return index >= 0 ? description.substring(index + 4) : description;
  }
}
