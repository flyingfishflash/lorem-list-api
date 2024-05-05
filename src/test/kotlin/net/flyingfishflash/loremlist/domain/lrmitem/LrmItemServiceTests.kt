package net.flyingfishflash.loremlist.domain.lrmitem

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import jakarta.validation.ConstraintViolationException
import net.flyingfishflash.loremlist.core.exceptions.ApiException
import net.flyingfishflash.loremlist.domain.lrmitem.data.LrmItemRequest
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.statements.StatementContext
import org.springframework.http.HttpStatus
import java.sql.SQLException

class LrmItemServiceTests : DescribeSpec({
  val mockLrmItemRepository = mockk<LrmItemRepository>()
  val lrmItemService = LrmItemService(mockLrmItemRepository)

  val lrmItemRequest = LrmItemRequest("Lorem Item Name", "Lorem Item Description")
  fun lrmItem(): LrmItem = LrmItem(id = 0, name = lrmItemRequest.name, description = lrmItemRequest.description)

  fun exposedSQLExceptionGeneric(): ExposedSQLException = ExposedSQLException(
    cause = SQLException("Cause of ExposedSQLException"),
    transaction = mockk<Transaction>(relaxed = true),
    contexts = listOf(mockk<StatementContext>(relaxed = true)),
  )

  afterEach { clearAllMocks() }
  afterSpec { unmockkAll() }

  describe("create()") {
    it("item is created") {
      every { mockLrmItemRepository.insert(ofType(LrmItemRequest::class)) } returns 1
      every { mockLrmItemRepository.findByIdOrNull(1) } returns lrmItem()
      lrmItemService.create(lrmItemRequest)
      verify(exactly = 1) { mockLrmItemRepository.insert(lrmItemRequest) }
      verify(exactly = 1) { mockLrmItemRepository.findByIdOrNull(any()) }
    }

    it("item repository throws exposed sql exception") {
      every { mockLrmItemRepository.insert(ofType(LrmItemRequest::class)) } throws exposedSQLExceptionGeneric()
      val exception = shouldThrow<ApiException> { lrmItemService.create(lrmItemRequest) }
      exception.cause.shouldBeInstanceOf<ExposedSQLException>()
      exception.httpStatus.shouldBe(HttpStatus.INTERNAL_SERVER_ERROR)
      exception.message.shouldNotBeNull().shouldBeEqual("Item could not be inserted.")
      exception.responseMessage.shouldBeEqual("Item could not be inserted.")
      exception.title.shouldBeEqual("API Exception")
      verify(exactly = 1) { mockLrmItemRepository.insert(lrmItemRequest) }
    }
  }

  describe("deleteSingleById()") {
    it("item is deleted") {
      every { mockLrmItemRepository.findByIdOrNull(1) } returns lrmItem()
      every { mockLrmItemRepository.deleteById(1) } returns 1
      lrmItemService.deleteSingleById(1)
      verify(exactly = 1) { mockLrmItemRepository.deleteById(1) }
    }

    it("item repository returns 0 deleted records") {
      every { mockLrmItemRepository.deleteById(1) } returns 0
      val exception = shouldThrow<ApiException> { lrmItemService.deleteSingleById(1) }
      exception.cause.shouldBeInstanceOf<ItemNotFoundException>()
      exception.httpStatus.shouldBe(HttpStatus.BAD_REQUEST)
      exception.title.shouldBeEqual("API Exception")
    }

    it("item repository returns > 1 deleted records") {
      every { mockLrmItemRepository.deleteById(1) } returns 2
      val exception = shouldThrow<ApiException> { lrmItemService.deleteSingleById(1) }
      exception.cause.shouldBeNull()
      exception.httpStatus.shouldBe(HttpStatus.INTERNAL_SERVER_ERROR)
      exception.title.shouldBeEqual("API Exception")
    }

    it("item repository throws exception") {
      every { mockLrmItemRepository.deleteById(1) } throws Exception("Lorem Ipsum")
      val exception = shouldThrow<ApiException> { lrmItemService.deleteSingleById(1) }
      exception.httpStatus.shouldBeEqual(HttpStatus.INTERNAL_SERVER_ERROR)
      exception.cause.shouldBeInstanceOf<Exception>()
      exception.message.shouldBe("Item id 1 could not be deleted.")
      exception.responseMessage.shouldBe("Item id 1 could not be deleted.")
    }
  }

  describe("findAll()") {
    it("all are returned") {
      every { mockLrmItemRepository.findAll() } returns listOf(lrmItem())
      lrmItemService.findAll()
      verify(exactly = 1) { mockLrmItemRepository.findAll() }
    }

    it("item repository throws exception") {
      every { mockLrmItemRepository.findAll() } throws Exception("Lorem Ipsum")
      val exception = shouldThrow<ApiException> { lrmItemService.findAll() }
      exception.httpStatus.shouldBeEqual(HttpStatus.INTERNAL_SERVER_ERROR)
      exception.cause.shouldBeInstanceOf<Exception>()
      exception.message.shouldBe("Items could not be retrieved.")
      exception.responseMessage.shouldBe("Items could not be retrieved.")
    }
  }

  describe("findAllIncludeLists()") {
    it("all and lists are returned") {
      every { mockLrmItemRepository.findAllIncludeLists() } returns listOf(lrmItem())
      lrmItemService.findAllIncludeLists()
      verify(exactly = 1) { mockLrmItemRepository.findAllIncludeLists() }
    }

    it("item repository throws exception") {
      every { mockLrmItemRepository.findAllIncludeLists() } throws Exception("Lorem Ipsum")
      val exception = shouldThrow<ApiException> { lrmItemService.findAllIncludeLists() }
      exception.httpStatus.shouldBeEqual(HttpStatus.INTERNAL_SERVER_ERROR)
      exception.cause.shouldBeInstanceOf<Exception>()
      exception.message.shouldBe("Items (including associated lists) could not be retrieved.")
      exception.responseMessage.shouldBe("Items (including associated lists) could not be retrieved.")
    }
  }

  describe("findById()") {
    it("item is returned") {
      every { mockLrmItemRepository.findByIdOrNull(1) } returns lrmItem()
      lrmItemService.findById(1)
      verify(exactly = 1) { mockLrmItemRepository.findByIdOrNull(1) }
    }

    it("item is not returned") {
      every { mockLrmItemRepository.findByIdOrNull(1) } returns null
      shouldThrow<ItemNotFoundException> { lrmItemService.findById(1) }
    }

    it("item repository throws exception") {
      every { mockLrmItemRepository.findByIdOrNull(1) } throws Exception("Lorem Ipsum")
      val exception = shouldThrow<ApiException> { lrmItemService.findById(1) }
      exception.httpStatus.shouldBeEqual(HttpStatus.INTERNAL_SERVER_ERROR)
      exception.cause.shouldBeInstanceOf<Exception>()
      exception.message.shouldBe("Item id 1 could not be retrieved.")
      exception.responseMessage.shouldBe("Item id 1 could not be retrieved.")
    }
  }

  describe("findByIdIncludeLists()") {
    it("item and lists are returned") {
      every { mockLrmItemRepository.findByIdOrNullIncludeLists(1) } returns lrmItem()
      lrmItemService.findByIdIncludeLists(1)
      verify(exactly = 1) { mockLrmItemRepository.findByIdOrNullIncludeLists(1) }
    }

    it("item is not returned") {
      every { mockLrmItemRepository.findByIdOrNullIncludeLists(1) } returns null
      shouldThrow<ItemNotFoundException> { lrmItemService.findByIdIncludeLists(1) }
    }

    it("item repository throws exception") {
      every { mockLrmItemRepository.findByIdOrNullIncludeLists(1) } throws Exception("Lorem Ipsum")
      val exception = shouldThrow<ApiException> { lrmItemService.findByIdIncludeLists(1) }
      exception.httpStatus.shouldBeEqual(HttpStatus.INTERNAL_SERVER_ERROR)
      exception.cause.shouldBeInstanceOf<Exception>()
      exception.message.shouldBe("Item id 1 (including associated lists) could not be retrieved.")
      exception.responseMessage.shouldBe("Item id 1 (including associated lists) could not be retrieved.")
    }
  }

  describe("patch()") {
    it("item is not found") {
      every { mockLrmItemRepository.findByIdOrNull(1) } returns null
      shouldThrow<ItemNotFoundException> { lrmItemService.patch(1, mapOf("name" to "lorum ipsum")) }
    }

    it("update name") {
      val expectedName = "patched lorem item"
      every { mockLrmItemRepository.findByIdOrNull(1) } returns lrmItem()
      every { mockLrmItemRepository.update(ofType(LrmItem::class)) } returns 1
      val patchedLrmItem = lrmItemService.patch(1, mapOf("name" to expectedName)).first
      patchedLrmItem.name.shouldBe(expectedName)
      verify(exactly = 1) { mockLrmItemRepository.findByIdOrNull(1) }
      verify(exactly = 1) { mockLrmItemRepository.update(ofType(LrmItem::class)) }
    }

    it("update name, description, quantity to current values") {
      val expectedName = lrmItem().name
      val expectedDescription = lrmItem().description
      val expectedQuantity = lrmItem().quantity
      every { mockLrmItemRepository.findByIdOrNull(1) } returns lrmItem()
      every { mockLrmItemRepository.update(ofType(LrmItem::class)) } returns 1
      val patchResponse = lrmItemService.patch(
        1,
        mapOf("name" to expectedName, "description" to (expectedDescription ?: ""), "quantity" to expectedQuantity),
      )
      patchResponse.second.shouldBeFalse()
      patchResponse.first.name.shouldBe(expectedName)
      patchResponse.first.description.shouldBe(expectedDescription)
      patchResponse.first.quantity.shouldBe(expectedQuantity)
      verify(exactly = 1) { mockLrmItemRepository.findByIdOrNull(1) }
      verify(exactly = 0) { mockLrmItemRepository.update(ofType(LrmItem::class)) }
    }

    it("update description") {
      val expectedDescription = "patched lorem list description"
      every { mockLrmItemRepository.findByIdOrNull(1) } returns lrmItem()
      every { mockLrmItemRepository.update(ofType(LrmItem::class)) } returns 1
      val patchedLrmList = lrmItemService.patch(1, mapOf("description" to expectedDescription)).first
      patchedLrmList.description.shouldBe(expectedDescription)
      verify(exactly = 1) { mockLrmItemRepository.findByIdOrNull(1) }
      verify(exactly = 1) { mockLrmItemRepository.update(ofType(LrmItem::class)) }
    }

    it("update quantity") {
      val expectedQuantity = 999
      every { mockLrmItemRepository.findByIdOrNull(1) } returns lrmItem()
      every { mockLrmItemRepository.update(ofType(LrmItem::class)) } returns 1
      val patchedLrmList = lrmItemService.patch(1, mapOf("quantity" to expectedQuantity)).first
      patchedLrmList.quantity.shouldBe(expectedQuantity)
      verify(exactly = 1) { mockLrmItemRepository.findByIdOrNull(1) }
      verify(exactly = 1) { mockLrmItemRepository.update(ofType(LrmItem::class)) }
    }

    it("update description to '  '") {
      every { mockLrmItemRepository.findByIdOrNull(1) } returns lrmItem()
      shouldThrow<ConstraintViolationException> { lrmItemService.patch(1, mapOf("description" to "  ")) }
    }

    it("update an undefined list property") {
      every { mockLrmItemRepository.findByIdOrNull(1) } returns lrmItem()
      shouldThrow<IllegalArgumentException> {
        lrmItemService.patch(1, mapOf("undefined property" to "irrelevant value"))
      }
    }

    it("update no properties") {
      every { mockLrmItemRepository.findByIdOrNull(1) } returns lrmItem()
      val patchReturn = lrmItemService.patch(1, mapOf())
      patchReturn.first.shouldBeEqual(lrmItem())
      patchReturn.second.shouldBeFalse()
      verify(exactly = 1) { mockLrmItemRepository.findByIdOrNull(1) }
      verify(exactly = 0) { mockLrmItemRepository.update(ofType(LrmItem::class)) }
    }

    it("item repository updates more than 1 record") {
      val expectedName = "patched lorem list"
      every { mockLrmItemRepository.findByIdOrNull(1) } returns lrmItem()
      every { mockLrmItemRepository.update(ofType(LrmItem::class)) } returns 2
      val exception = shouldThrow<ApiException> { lrmItemService.patch(1, mapOf("name" to expectedName)).first }
      exception.httpStatus.shouldBe(HttpStatus.INTERNAL_SERVER_ERROR)
      exception.cause.shouldBeNull()
      exception.detail.shouldBeEqual(
        "Item id ${lrmItem().id} could not be updated. " +
          "2 records would have been updated rather than 1.",
      )
      exception.responseMessage.shouldBeEqual(
        "Item id ${lrmItem().id} could not be updated. " +
          "2 records would have been updated rather than 1.",
      )
      verify(exactly = 1) { mockLrmItemRepository.findByIdOrNull(1) }
      verify(exactly = 1) { mockLrmItemRepository.update(ofType(LrmItem::class)) }
    }

    it("item repository throws exposed sql exception") {
      val expectedName = "patched lorem list"
      every { mockLrmItemRepository.findByIdOrNull(1) } returns lrmItem()
      every { mockLrmItemRepository.update(ofType(LrmItem::class)) } throws exposedSQLExceptionGeneric()
      val exception = shouldThrow<ApiException> { lrmItemService.patch(1, mapOf("name" to expectedName)).first }
      exception.httpStatus.shouldBe(HttpStatus.INTERNAL_SERVER_ERROR)
      exception.cause.shouldBeInstanceOf<ExposedSQLException>()
      exception.detail.shouldBeEqual(
        "Item id ${lrmItem().id} could not be updated. " +
          "The item was found and patch request is valid but an exception was thrown by the item repository.",
      )
      exception.responseMessage.shouldBeEqual("Item id ${lrmItem().id} could not be updated.")
      verify(exactly = 1) { mockLrmItemRepository.findByIdOrNull(1) }
      verify(exactly = 1) { mockLrmItemRepository.update(ofType(LrmItem::class)) }
    }
  }
})
