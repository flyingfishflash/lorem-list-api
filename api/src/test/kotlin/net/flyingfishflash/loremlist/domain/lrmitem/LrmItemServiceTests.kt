package net.flyingfishflash.loremlist.domain.lrmitem

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
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
import net.flyingfishflash.loremlist.core.exceptions.ApiException
import net.flyingfishflash.loremlist.domain.lrmitem.data.LrmItem
import net.flyingfishflash.loremlist.domain.lrmitem.data.LrmItemRepository
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
  }

  describe("findAll()") {
    it("all are returned") {
      every { mockLrmItemRepository.findAll() } returns listOf(lrmItem())
      lrmItemService.findAll()
      verify(exactly = 1) { mockLrmItemRepository.findAll() }
    }
  }

  describe("findAllIncludeLists()") {
    it("all and lists are returned") {
      every { mockLrmItemRepository.findAllIncludeLists() } returns listOf(lrmItem())
      lrmItemService.findAllIncludeLists()
      verify(exactly = 1) { mockLrmItemRepository.findAllIncludeLists() }
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
  }
})
