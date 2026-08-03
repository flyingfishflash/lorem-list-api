package net.flyingfishflash.loremlist.unit.domain.lrmitem

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import net.flyingfishflash.loremlist.domain.lrmitem.ItemNotFoundException
import java.util.*

class LrmItemNotFoundTests :
  DescribeSpec({
    describe("ItemNotFoundException()") {
      it("primary constructor: default message") {
        val id = UUID.randomUUID()
        val itemNotFoundException = ItemNotFoundException(id)
        itemNotFoundException.message.shouldBe("Item could not be found.")
      }

      it("primary constructor: non-default message") {
        val itemNotFoundException = ItemNotFoundException(UUID.randomUUID(), "Lorem Ipsum")
        itemNotFoundException.message.shouldBe("Lorem Ipsum")
      }

      it("secondary constructor: default message, set size = 1") {
        val ids = setOf(UUID.randomUUID())
        val itemNotFoundException = ItemNotFoundException(ids)
        itemNotFoundException.message.shouldBe("Item could not be found.")
      }

      it("secondary constructor: default message, set size > 1") {
        val ids = setOf(UUID.randomUUID(), UUID.randomUUID())
        val itemNotFoundException = ItemNotFoundException(ids)
        itemNotFoundException.message.shouldBe("Items (2) could not be found.")
      }

      it("secondary constructor: non-default message") {
        val itemNotFoundException = ItemNotFoundException(setOf(UUID.randomUUID()), "Lorem Ipsum")
        itemNotFoundException.message.shouldBe("Lorem Ipsum")
      }
    }
  })
