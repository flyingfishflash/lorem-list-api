package net.flyingfishflash.loremlist.unit.domain.lrmlist

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import net.flyingfishflash.loremlist.domain.lrmlist.ListNotFoundException
import java.util.UUID

class LrmListNotFoundTests : DescribeSpec({
  describe("ListNotFoundException()") {
    it("non-default message") {
      val listNotFoundException = ListNotFoundException(uuid = UUID.randomUUID(), message = "Lorem Ipsum")
      listNotFoundException.message?.shouldBe("Lorem Ipsum")
    }
  }
})
