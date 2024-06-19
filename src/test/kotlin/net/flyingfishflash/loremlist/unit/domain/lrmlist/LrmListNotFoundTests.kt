package net.flyingfishflash.loremlist.unit.domain.lrmlist

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import net.flyingfishflash.loremlist.domain.lrmlist.ListNotFoundException

class LrmListNotFoundTests : DescribeSpec({
  describe("ListNotFoundException()") {
    it("non-default message") {
      val itemNotFoundException = ListNotFoundException(id = 1, message = "Lorem Ipsum")
      itemNotFoundException.message?.shouldBe("Lorem Ipsum")
    }
  }
})
