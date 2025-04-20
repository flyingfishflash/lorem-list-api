package net.flyingfishflash.loremlist.unit.domain.lrmlistitem

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import net.flyingfishflash.loremlist.domain.lrmlistitem.ListItemNotFoundException
import java.util.UUID

class ListItemNotFoundTests :
  DescribeSpec({

    describe("ListItemNotFoundException()") {
      it("id is null") {
        val exception = ListItemNotFoundException()
        exception.responseMessage.shouldBe("ListItem could not be found.")
      }

      it("id is not null") {
        val id = UUID.randomUUID()
        val exception = ListItemNotFoundException(id = id)
        exception.responseMessage.shouldBe("ListItem could not be found.")
      }

      it("custom message") {
        val exception = ListItemNotFoundException(message = "Lorem Ipsum")
        exception.message.shouldBe("Lorem Ipsum")
        exception.responseMessage.shouldBe("Lorem Ipsum")
      }
    }
  })
