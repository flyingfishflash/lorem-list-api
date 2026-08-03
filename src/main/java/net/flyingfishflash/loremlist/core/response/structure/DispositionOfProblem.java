package net.flyingfishflash.loremlist.core.response.structure;

import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Locale;
import org.springframework.http.HttpStatus;

public enum DispositionOfProblem implements Disposition {
  /**
   * Client request disposition of error: http 5xx - unanticipated problem in platform, framework or
   * server
   */
  ERROR,

  /** Client request disposition of failure: http 4xx - anticipated problem */
  FAILURE,

  /** Client request disposition of undefined: http 1xx-&gt;3xx */
  UNDEFINED;

  /** Returns an enum constant name() in lowercase */
  @Override
  @JsonValue
  public String nameAsLowercase() {
    return name().toLowerCase(Locale.getDefault());
  }

  /** Calculate the disposition of the API Event from the Http status */
  public static DispositionOfProblem calcDisposition(HttpStatus httpStatus) {
    if (httpStatus.is4xxClientError()) {
      return FAILURE;
    } else if (httpStatus.is5xxServerError()) {
      return ERROR;
    } else {
      return UNDEFINED;
    }
  }
}
