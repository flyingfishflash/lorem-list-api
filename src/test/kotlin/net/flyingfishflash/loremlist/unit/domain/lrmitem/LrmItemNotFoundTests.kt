package net.flyingfishflash.loremlist.unit.domain.lrmitem

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import net.flyingfishflash.loremlist.domain.lrmitem.ItemNotFoundException

class LrmItemNotFoundTests : DescribeSpec({
  describe("ItemNotFoundException()") {
    it("non-default message") {
      val itemNotFoundException = ItemNotFoundException(id = 1, message = "Lorem Ipsum")
      itemNotFoundException.message.shouldBe("Lorem Ipsum")
    }
  }
})
