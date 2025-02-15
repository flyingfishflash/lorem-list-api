package net.flyingfishflash.loremlist.unit.domain.lrmlist

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import net.flyingfishflash.loremlist.domain.lrmlist.ListNotFoundException
import java.util.UUID

class LrmListNotFoundTests :
  DescribeSpec({
    describe("ListNotFoundException()") {

      it("primary constructor: default message") {
        val id = UUID.randomUUID()
        val listNotFoundException = ListNotFoundException(id = id)
        listNotFoundException.message.shouldBe("List could not be found.")
      }

      it("primary constructor: non-default message") {
        val listNotFoundException = ListNotFoundException(id = UUID.randomUUID(), message = "Lorem Ipsum")
        listNotFoundException.message?.shouldBe("Lorem Ipsum")
      }

      it("secondary constructor: default message, set size = 1") {
        val ids = setOf(UUID.randomUUID())
        val listNotFoundException = ListNotFoundException(idCollection = ids)
        listNotFoundException.message.shouldBe("List could not be found.")
      }

      it("secondary constructor: default message, set size > 1") {
        val ids = setOf(UUID.randomUUID(), UUID.randomUUID())
        val listNotFoundException = ListNotFoundException(idCollection = ids)
        listNotFoundException.message.shouldBe("Lists (2) could not be found.")
      }

      it("secondary constructor: non-default message") {
        val listNotFoundException = ListNotFoundException(idCollection = setOf(UUID.randomUUID()), message = "Lorem Ipsum")
        listNotFoundException.message.shouldBe("Lorem Ipsum")
      }
    }
  })
