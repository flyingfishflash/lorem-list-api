package net.flyingfishflash.loremlist.api.data.response;

import java.util.Objects;

public final class ApiServiceResponse<T> {

  private final T content;
  private final String message;

  public ApiServiceResponse(T content, String message) {
    this.content = content;
    this.message = message;
  }

  public T getContent() {
    return content;
  }

  public String getMessage() {
    return message;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ApiServiceResponse<?> other)) {
      return false;
    }
    return Objects.equals(content, other.content) && Objects.equals(message, other.message);
  }

  @Override
  public int hashCode() {
    return Objects.hash(content, message);
  }

  @Override
  public String toString() {
    return "ApiServiceResponse[content=" + content + ", message=" + message + "]";
  }
}
