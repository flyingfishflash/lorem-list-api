package net.flyingfishflash.loremlist.domain.lrmlist

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class LrmListNotFoundTests : DescribeSpec({
  describe("ListNotFoundException()") {
    it("non-default message") {
      val itemNotFoundException = ListNotFoundException(id = 1, message = "Lorem Ipsum")
      itemNotFoundException.detail.shouldBe("Lorem Ipsum")
    }
  }
})
