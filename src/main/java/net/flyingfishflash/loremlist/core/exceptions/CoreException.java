package net.flyingfishflash.loremlist.core.exceptions;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.util.Map;
import org.springframework.http.HttpStatus;

public class CoreException extends RuntimeException {

  public static final String DEFAULT_TITLE = "CoreException";
  public static final String DEFAULT_MESSAGE = DEFAULT_TITLE;
  public static final HttpStatus DEFAULT_HTTP_STATUS = HttpStatus.INTERNAL_SERVER_ERROR;
  public static final URI DEFAULT_PROBLEM_TYPE = URI.create("about:config");

  private final HttpStatus httpStatus;
  private final String responseMessage;
  private final String title;
  private final URI type;
  private final Map<String, JsonNode> supplemental;

  public CoreException(
      Throwable cause,
      HttpStatus httpStatus,
      String message,
      String responseMessage,
      String title,
      URI type,
      Map<String, JsonNode> supplemental) {
    super(message != null ? message : DEFAULT_MESSAGE, cause);
    this.httpStatus = httpStatus != null ? httpStatus : DEFAULT_HTTP_STATUS;
    this.responseMessage = responseMessage != null ? responseMessage : getMessage();
    this.title = title != null ? title : DEFAULT_TITLE;
    this.type = type != null ? type : DEFAULT_PROBLEM_TYPE;
    this.supplemental = supplemental;
  }

  public HttpStatus getHttpStatus() {
    return httpStatus;
  }

  public String getResponseMessage() {
    return responseMessage;
  }

  public String getTitle() {
    return title;
  }

  public URI getType() {
    return type;
  }

  public Map<String, JsonNode> getSupplemental() {
    return supplemental;
  }

  public static Builder<?> builder() {
    return new Builder<>();
  }

  /**
   * Self-referential builder (same shape as Lombok's {@code @SuperBuilder}) so that {@link
   * DomainException} can extend this builder and still return its own type from {@code build()}
   * without the two classes' static {@code builder()} factory methods conflicting.
   */
  public static class Builder<B extends Builder<B>> {
    protected Throwable cause;
    protected HttpStatus httpStatus;
    protected String message;
    protected String responseMessage;
    protected String title;
    protected URI type;
    protected Map<String, JsonNode> supplemental;

    @SuppressWarnings("unchecked")
    protected B self() {
      return (B) this;
    }

    public B cause(Throwable cause) {
      this.cause = cause;
      return self();
    }

    public B httpStatus(HttpStatus httpStatus) {
      this.httpStatus = httpStatus;
      return self();
    }

    public B message(String message) {
      this.message = message;
      return self();
    }

    public B responseMessage(String responseMessage) {
      this.responseMessage = responseMessage;
      return self();
    }

    public B title(String title) {
      this.title = title;
      return self();
    }

    public B type(URI type) {
      this.type = type;
      return self();
    }

    public B supplemental(Map<String, JsonNode> supplemental) {
      this.supplemental = supplemental;
      return self();
    }

    public CoreException build() {
      return new CoreException(
          cause, httpStatus, message, responseMessage, title, type, supplemental);
    }
  }
}
