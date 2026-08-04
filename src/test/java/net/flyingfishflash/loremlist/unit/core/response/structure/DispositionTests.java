package net.flyingfishflash.loremlist.unit.core.response.structure;

import static org.assertj.core.api.Assertions.assertThat;

import net.flyingfishflash.loremlist.core.response.structure.DispositionOfProblem;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class DispositionTests {

  @Test
  void failureWhenHttpStatusIs4xx() {
    assertThat(DispositionOfProblem.calcDisposition(HttpStatus.I_AM_A_TEAPOT))
        .isEqualTo(DispositionOfProblem.FAILURE);
  }

  @Test
  void errorWhenHttpStatusIs5xx() {
    assertThat(DispositionOfProblem.calcDisposition(HttpStatus.INTERNAL_SERVER_ERROR))
        .isEqualTo(DispositionOfProblem.ERROR);
  }

  @Test
  void undefinedWhenHttpStatusIsNot4xxOr5xx() {
    assertThat(DispositionOfProblem.calcDisposition(HttpStatus.CONTINUE))
        .isEqualTo(DispositionOfProblem.UNDEFINED);
    assertThat(DispositionOfProblem.calcDisposition(HttpStatus.OK))
        .isEqualTo(DispositionOfProblem.UNDEFINED);
    assertThat(DispositionOfProblem.calcDisposition(HttpStatus.MULTIPLE_CHOICES))
        .isEqualTo(DispositionOfProblem.UNDEFINED);
  }
}
