package net.flyingfishflash.loremlist.unit.domain.lrmitem

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import net.flyingfishflash.loremlist.domain.lrmitem.ItemNotFoundException
import java.util.*

class LrmItemNotFoundTests : DescribeSpec({
  describe("ItemNotFoundException()") {
    it("non-default message") {
      val itemNotFoundException = ItemNotFoundException(uuid = UUID.randomUUID(), message = "Lorem Ipsum")
      itemNotFoundException.message.shouldBe("Lorem Ipsum")
    }
  }
})
