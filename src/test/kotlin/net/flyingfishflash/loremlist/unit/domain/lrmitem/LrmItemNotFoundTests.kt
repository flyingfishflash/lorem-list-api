package net.flyingfishflash.loremlist.unit.domain.lrmitem

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import net.flyingfishflash.loremlist.domain.lrmitem.ItemNotFoundException
import java.util.*

class LrmItemNotFoundTests : DescribeSpec({
  describe("ItemNotFoundException()") {
    it("primary constructor: default message") {
      val id = UUID.randomUUID()
      val itemNotFoundException = ItemNotFoundException(id = id)
      itemNotFoundException.message.shouldBe(ItemNotFoundException.defaultMessage())
    }

    it("primary constructor: non-default message") {
      val itemNotFoundException = ItemNotFoundException(id = UUID.randomUUID(), message = "Lorem Ipsum")
      itemNotFoundException.message.shouldBe("Lorem Ipsum")
    }

    it("secondary constructor: default message, set size = 1") {
      val ids = setOf(UUID.randomUUID())
      val itemNotFoundException = ItemNotFoundException(idCollection = ids)
      itemNotFoundException.message.shouldBe(ItemNotFoundException.defaultMessage(ids))
    }

    it("secondary constructor: default message, set size > 1") {
      val ids = setOf(UUID.randomUUID(), UUID.randomUUID())
      val itemNotFoundException = ItemNotFoundException(idCollection = ids)
      itemNotFoundException.message.shouldBe(ItemNotFoundException.defaultMessage(ids))
    }

    it("secondary constructor: non-default message") {
      val itemNotFoundException = ItemNotFoundException(idCollection = setOf(UUID.randomUUID()), message = "Lorem Ipsum")
      itemNotFoundException.message.shouldBe("Lorem Ipsum")
    }
  }
})
