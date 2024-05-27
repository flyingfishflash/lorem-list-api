package net.flyingfishflash.loremlist.unit.domain.association

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import net.flyingfishflash.loremlist.domain.association.AssociationNotFoundException
import java.util.UUID

class AssociationNotFoundTests : DescribeSpec({

  describe("AssociationNotFoundException()") {
    it("id is null") {
      val exception = AssociationNotFoundException()
      exception.responseMessage.shouldBe("Association could not be found.")
    }

    it("id is not null") {
      val uuid = UUID.randomUUID()
      val exception = AssociationNotFoundException(id = uuid)
      exception.responseMessage.shouldBe("Association id $uuid could not be found.")
    }

    it("custom message") {
      val exception = AssociationNotFoundException(message = "Lorem Ipsum")
      exception.message.shouldBe("Lorem Ipsum")
      exception.responseMessage.shouldBe("Association could not be found.")
    }
  }
})
