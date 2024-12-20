package net.flyingfishflash.loremlist.unit.domain.association

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import net.flyingfishflash.loremlist.domain.association.AssociationNotFoundException
import java.util.UUID

class AssociationNotFoundTests :
  DescribeSpec({

    describe("AssociationNotFoundException()") {
      it("id is null") {
        val exception = AssociationNotFoundException()
        exception.responseMessage.shouldBe("Association not found.")
      }

      it("id is not null") {
        val id = UUID.randomUUID()
        val exception = AssociationNotFoundException(id = id)
        exception.responseMessage.shouldBe("Association id $id not found.")
      }

      it("custom message") {
        val exception = AssociationNotFoundException(message = "Lorem Ipsum")
        exception.message.shouldBe("Lorem Ipsum")
        exception.responseMessage.shouldBe("Lorem Ipsum")
      }
    }
  })
