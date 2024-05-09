package net.flyingfishflash.loremlist.unit.core.response.structure

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import net.flyingfishflash.loremlist.core.response.structure.DispositionOfProblem
import org.springframework.http.HttpStatus

class DispositionTests : DescribeSpec({
  describe("calcDisposition()") {
    it("failure when http status is 4xx") {
      DispositionOfProblem.calcDisposition(HttpStatus.I_AM_A_TEAPOT)
        .shouldBe(DispositionOfProblem.FAILURE)
    }

    it("error when http status is 5xx") {
      DispositionOfProblem.calcDisposition(HttpStatus.INTERNAL_SERVER_ERROR)
        .shouldBe(DispositionOfProblem.ERROR)
    }

    it("undefined when http status is not 4xx or 5xx") {
      // 100
      DispositionOfProblem.calcDisposition(HttpStatus.CONTINUE)
        .shouldBe(DispositionOfProblem.UNDEFINED)
      // 200
      DispositionOfProblem.calcDisposition(HttpStatus.OK)
        .shouldBe(DispositionOfProblem.UNDEFINED)
      // 300
      DispositionOfProblem.calcDisposition(HttpStatus.MULTIPLE_CHOICES)
        .shouldBe(DispositionOfProblem.UNDEFINED)
    }
  }
})
