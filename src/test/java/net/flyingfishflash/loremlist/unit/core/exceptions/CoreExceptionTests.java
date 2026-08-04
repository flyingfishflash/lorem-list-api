package net.flyingfishflash.loremlist.unit.core.exceptions;

import static net.flyingfishflash.loremlist.core.exceptions.CoreException.DEFAULT_HTTP_STATUS;
import static net.flyingfishflash.loremlist.core.exceptions.CoreException.DEFAULT_MESSAGE;
import static net.flyingfishflash.loremlist.core.exceptions.CoreException.DEFAULT_PROBLEM_TYPE;
import static net.flyingfishflash.loremlist.core.exceptions.CoreException.DEFAULT_TITLE;
import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import net.flyingfishflash.loremlist.core.exceptions.CoreException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class CoreExceptionTests {

  @Test
  void defaultValues() {
    assertThat(DEFAULT_HTTP_STATUS).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    assertThat(DEFAULT_MESSAGE).isEqualTo(DEFAULT_TITLE);
    assertThat(DEFAULT_TITLE).isEqualTo(CoreException.class.getSimpleName());
    assertThat(DEFAULT_PROBLEM_TYPE).isEqualTo(URI.create("about:config"));
  }

  @Test
  void defaultParameterValues() {
    CoreException exception = CoreException.builder().build();
    assertThat(exception.getCause()).isNull();
    assertThat(exception.getHttpStatus()).isEqualTo(DEFAULT_HTTP_STATUS);
    assertThat(exception.getMessage()).isEqualTo(DEFAULT_MESSAGE);
    assertThat(exception.getResponseMessage()).isEqualTo(exception.getMessage());
    assertThat(exception.getTitle()).isEqualTo(DEFAULT_TITLE);
    assertThat(exception.getType()).isEqualTo(DEFAULT_PROBLEM_TYPE);
  }

  @Test
  void httpStatus() {
    HttpStatus expectedStatus = HttpStatus.I_AM_A_TEAPOT;
    CoreException exception = CoreException.builder().httpStatus(expectedStatus).build();
    assertThat(exception.getCause()).isNull();
    assertThat(exception.getHttpStatus()).isEqualTo(expectedStatus);
    assertThat(exception.getMessage()).isEqualTo(DEFAULT_MESSAGE);
    assertThat(exception.getResponseMessage()).isEqualTo(exception.getMessage());
    assertThat(exception.getTitle()).isEqualTo(DEFAULT_TITLE);
    assertThat(exception.getType()).isEqualTo(DEFAULT_PROBLEM_TYPE);
  }

  @Test
  void message() {
    String expectedMessage = "Lorem Ipsum";
    CoreException exception = CoreException.builder().message(expectedMessage).build();
    assertThat(exception.getCause()).isNull();
    assertThat(exception.getHttpStatus()).isEqualTo(DEFAULT_HTTP_STATUS);
    assertThat(exception.getMessage()).isEqualTo(expectedMessage);
    assertThat(exception.getResponseMessage()).isEqualTo(exception.getMessage());
    assertThat(exception.getTitle()).isEqualTo(DEFAULT_TITLE);
    assertThat(exception.getType()).isEqualTo(DEFAULT_PROBLEM_TYPE);
  }

  @Test
  void responseMessage() {
    String expectedResponseMessage = "Lorem Ipsum";
    CoreException exception =
        CoreException.builder().responseMessage(expectedResponseMessage).build();
    assertThat(exception.getCause()).isNull();
    assertThat(exception.getHttpStatus()).isEqualTo(DEFAULT_HTTP_STATUS);
    assertThat(exception.getMessage()).isEqualTo(DEFAULT_MESSAGE);
    assertThat(exception.getResponseMessage()).isEqualTo(expectedResponseMessage);
    assertThat(exception.getResponseMessage()).isNotEqualTo(exception.getMessage());
    assertThat(exception.getTitle()).isEqualTo(DEFAULT_TITLE);
    assertThat(exception.getType()).isEqualTo(DEFAULT_PROBLEM_TYPE);
  }

  @Test
  void title() {
    String expectedTitle = "Lorem Ipsum";
    CoreException exception = CoreException.builder().title(expectedTitle).build();
    assertThat(exception.getCause()).isNull();
    assertThat(exception.getHttpStatus()).isEqualTo(DEFAULT_HTTP_STATUS);
    assertThat(exception.getMessage()).isEqualTo(DEFAULT_MESSAGE);
    assertThat(exception.getResponseMessage()).isEqualTo(exception.getMessage());
    assertThat(exception.getTitle()).isEqualTo(expectedTitle);
    assertThat(exception.getType()).isEqualTo(DEFAULT_PROBLEM_TYPE);
  }

  @Test
  void type() {
    URI expectedType = URI.create("http://example.net");
    CoreException exception = CoreException.builder().type(expectedType).build();
    assertThat(exception.getCause()).isNull();
    assertThat(exception.getHttpStatus()).isEqualTo(DEFAULT_HTTP_STATUS);
    assertThat(exception.getMessage()).isEqualTo(DEFAULT_MESSAGE);
    assertThat(exception.getResponseMessage()).isEqualTo(exception.getMessage());
    assertThat(exception.getTitle()).isEqualTo(DEFAULT_TITLE);
    assertThat(exception.getType()).isEqualTo(expectedType);
  }
}
