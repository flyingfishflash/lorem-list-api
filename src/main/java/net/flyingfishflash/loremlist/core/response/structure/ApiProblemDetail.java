package net.flyingfishflash.loremlist.core.response.structure;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;
import org.springframework.http.ProblemDetail;

/** RFC 9457 - Problem Details for HTTP APIs */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ApiProblemDetail(
    String type,
    String title,
    int status,
    String detail,
    ExceptionCauseDetail cause,
    List<String> validationErrors,
    Map<String, JsonNode> supplemental,
    List<String> stackTrace) {

  public ApiProblemDetail(String type, String title, int status, String detail) {
    this(type, title, status, detail, null, null, null, null);
  }

  /** Construct an ApiProblemDetail from a Spring ProblemDetail */
  public ApiProblemDetail(ProblemDetail problemDetail) {
    this(
        problemDetail.getType().toString(),
        problemDetail.getTitle() != null ? problemDetail.getTitle() : "default title",
        problemDetail.getStatus(),
        problemDetail.getDetail() != null ? problemDetail.getDetail() : "default detail");
  }
}
