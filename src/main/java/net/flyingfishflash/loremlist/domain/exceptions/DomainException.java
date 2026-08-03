package net.flyingfishflash.loremlist.domain.exceptions;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.util.Map;
import net.flyingfishflash.loremlist.core.exceptions.CoreException;
import org.springframework.http.HttpStatus;

public class DomainException extends CoreException {

  public static final String DEFAULT_TITLE = "DomainException";
  public static final String DEFAULT_MESSAGE = DEFAULT_TITLE;

  public DomainException(
      Throwable cause,
      HttpStatus httpStatus,
      String message,
      String responseMessage,
      String title,
      URI type,
      Map<String, JsonNode> supplemental) {
    super(
        cause,
        httpStatus,
        message != null ? message : DEFAULT_MESSAGE,
        responseMessage,
        title != null ? title : DEFAULT_TITLE,
        type,
        supplemental);
  }

  public static Builder<?> builder() {
    return new Builder<>();
  }

  public static class Builder<B extends Builder<B>> extends CoreException.Builder<B> {
    @Override
    public DomainException build() {
      return new DomainException(
          cause, httpStatus, message, responseMessage, title, type, supplemental);
    }
  }
}
