package net.flyingfishflash.loremlist.unit.domain.lrmlist

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import net.flyingfishflash.loremlist.domain.lrmlist.ListNotFoundException
import java.util.UUID

class LrmListNotFoundTests : DescribeSpec({
  describe("ListNotFoundException()") {

    it("primary constructor: default message") {
      val uuid = UUID.randomUUID()
      val listNotFoundException = ListNotFoundException(uuid = uuid)
      listNotFoundException.message.shouldBe(ListNotFoundException.defaultMessage())
    }

    it("primary constructor: non-default message") {
      val listNotFoundException = ListNotFoundException(uuid = UUID.randomUUID(), message = "Lorem Ipsum")
      listNotFoundException.message?.shouldBe("Lorem Ipsum")
    }

    it("secondary constructor: default message, set size = 1") {
      val uuids = setOf(UUID.randomUUID())
      val listNotFoundException = ListNotFoundException(uuidCollection = uuids)
      listNotFoundException.message.shouldBe(ListNotFoundException.defaultMessage(uuids))
    }

    it("secondary constructor: default message, set size > 1") {
      val uuids = setOf(UUID.randomUUID(), UUID.randomUUID())
      val listNotFoundException = ListNotFoundException(uuidCollection = uuids)
      listNotFoundException.message.shouldBe(ListNotFoundException.defaultMessage(uuids))
    }

    it("secondary constructor: non-default message") {
      val listNotFoundException = ListNotFoundException(uuidCollection = setOf(UUID.randomUUID()), message = "Lorem Ipsum")
      listNotFoundException.message.shouldBe("Lorem Ipsum")
    }
  }
})
