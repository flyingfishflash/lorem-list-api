package net.flyingfishflash.loremlist.domain.lrmitem

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class LrmItemNotFoundTests : DescribeSpec({
  describe("ItemNotFoundException()") {
    it("non-default message") {
      val itemNotFoundException = ItemNotFoundException(id = 1, message = "Lorem Ipsum")
      itemNotFoundException.detail.shouldBe("Lorem Ipsum")
    }
  }
})
