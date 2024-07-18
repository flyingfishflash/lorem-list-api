package net.flyingfishflash.loremlist.unit.domain.lrmitem

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import net.flyingfishflash.loremlist.domain.lrmitem.ItemNotFoundException
import java.util.*

class LrmItemNotFoundTests : DescribeSpec({
  describe("ItemNotFoundException()") {
    it("primary constructor: default message") {
      val uuid = UUID.randomUUID()
      val itemNotFoundException = ItemNotFoundException(uuid = uuid)
      itemNotFoundException.message.shouldBe(ItemNotFoundException.defaultMessage())
    }

    it("primary constructor: non-default message") {
      val itemNotFoundException = ItemNotFoundException(uuid = UUID.randomUUID(), message = "Lorem Ipsum")
      itemNotFoundException.message.shouldBe("Lorem Ipsum")
    }

    it("secondary constructor: default message, set size = 1") {
      val uuids = setOf(UUID.randomUUID())
      val itemNotFoundException = ItemNotFoundException(uuidCollection = uuids)
      itemNotFoundException.message.shouldBe(ItemNotFoundException.defaultMessage(uuids))
    }

    it("secondary constructor: default message, set size > 1") {
      val uuids = setOf(UUID.randomUUID(), UUID.randomUUID())
      val itemNotFoundException = ItemNotFoundException(uuidCollection = uuids)
      itemNotFoundException.message.shouldBe(ItemNotFoundException.defaultMessage(uuids))
    }

    it("secondary constructor: non-default message") {
      val itemNotFoundException = ItemNotFoundException(uuidCollection = setOf(UUID.randomUUID()), message = "Lorem Ipsum")
      itemNotFoundException.message.shouldBe("Lorem Ipsum")
    }
  }
})
