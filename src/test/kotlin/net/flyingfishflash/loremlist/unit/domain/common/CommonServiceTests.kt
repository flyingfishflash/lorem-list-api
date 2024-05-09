package net.flyingfishflash.loremlist.unit.domain.common

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.unmockkAll
import io.mockk.verify
import net.flyingfishflash.loremlist.core.exceptions.ApiException
import net.flyingfishflash.loremlist.domain.common.CommonService
import net.flyingfishflash.loremlist.domain.lrmitem.ItemNotFoundException
import net.flyingfishflash.loremlist.domain.lrmitem.LrmItem
import net.flyingfishflash.loremlist.domain.lrmitem.LrmItemRepository
import net.flyingfishflash.loremlist.domain.lrmitem.LrmItemService
import net.flyingfishflash.loremlist.domain.lrmitem.data.LrmItemRequest
import net.flyingfishflash.loremlist.domain.lrmlist.ListNotFoundException
import net.flyingfishflash.loremlist.domain.lrmlist.LrmList
import net.flyingfishflash.loremlist.domain.lrmlist.LrmListService
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.statements.Statement
import org.jetbrains.exposed.sql.statements.StatementContext
import org.jetbrains.exposed.sql.statements.StatementType
import org.springframework.http.HttpStatus
import java.sql.SQLException
import java.sql.SQLIntegrityConstraintViolationException
import java.util.UUID

class CommonServiceTests : DescribeSpec({
  val mockLrmItemRepository = mockk<LrmItemRepository>()
  val mockLrmItemService = mockk<LrmItemService>()
  val mockLrmListService = mockk<LrmListService>()
  val commonService = CommonService(mockLrmItemRepository, mockLrmItemService, mockLrmListService)

  val lrmItemRequest = LrmItemRequest("Lorem Item Name", "Lorem Item Description")
  val itemUuid = UUID.randomUUID()
  val listUuid = UUID.randomUUID()
  fun lrmItem(): LrmItem = LrmItem(id = 0, uuid = itemUuid, name = lrmItemRequest.name, description = lrmItemRequest.description)
  fun lrmList(): LrmList = LrmList(id = 0, uuid = listUuid, name = "Lorem List Name", description = "Lorem List Description")

  fun exposedSQLExceptionConstraintViolation(): ExposedSQLException = ExposedSQLException(
    cause = mockk<SQLIntegrityConstraintViolationException>(),
    transaction = mockk<Transaction>(relaxed = true),
    contexts = listOf(mockk<StatementContext>(relaxed = true)),
  )

  fun exposedSQLExceptionGeneric(): ExposedSQLException = ExposedSQLException(
    cause = SQLException("Cause of ExposedSQLException"),
    transaction = mockk<Transaction>(relaxed = true),
    contexts = listOf(mockk<StatementContext>(relaxed = true)),
  )

  afterEach { clearAllMocks() }
  afterSpec { unmockkAll() }

  describe("addToList()") {
    it("item is added to list") {
      every { mockLrmItemRepository.addItemToList(1, 1) } just Runs
      every { mockLrmItemService.findById(1) } returns lrmItem()
      every { mockLrmListService.findById(1) } returns lrmList()
      val response = commonService.addToList(itemId = 1, listId = 1)
      response.first.shouldBeEqual(lrmItem().name)
      response.second.shouldBeEqual(lrmList().name)
      verify(exactly = 1) { mockLrmItemRepository.addItemToList(1, 1) }
      verify(exactly = 1) { mockLrmItemService.findById(1) }
      verify(exactly = 1) { mockLrmListService.findById(1) }
    }

    it("item not found") {
      every { mockLrmItemRepository.addItemToList(1, 1) } throws exposedSQLExceptionConstraintViolation()
      every { mockLrmItemService.findById(1) } throws ItemNotFoundException(1)
      shouldThrow<ApiException> { commonService.addToList(itemId = 1, listId = 1) }
        .cause.shouldBeInstanceOf<ItemNotFoundException>()
      verify(exactly = 1) { mockLrmItemRepository.addItemToList(1, 1) }
    }

    it("list not found") {
      every { mockLrmItemRepository.addItemToList(1, 1) } throws exposedSQLExceptionConstraintViolation()
      every { mockLrmItemService.findById(1) } returns lrmItem()
      every { mockLrmListService.findById(1) } throws ListNotFoundException(1)
      shouldThrow<ApiException> { commonService.addToList(itemId = 1, listId = 1) }
        .cause.shouldBeInstanceOf<ListNotFoundException>()
      verify(exactly = 1) { mockLrmItemRepository.addItemToList(1, 1) }
    }

    it("already added to list") {
      val exposedSQLExceptionConstraintViolation = exposedSQLExceptionConstraintViolation()
      every { mockLrmItemRepository.addItemToList(1, 1) } throws exposedSQLExceptionConstraintViolation
      every { mockLrmItemService.findById(1) } returns lrmItem()
      every { mockLrmListService.findById(1) } returns lrmList()
      every { exposedSQLExceptionConstraintViolation.contexts[0].statement } returns mockk<Statement<String>>()
      every { exposedSQLExceptionConstraintViolation.contexts[0].statement.type } returns mockk<StatementType>()
      every { exposedSQLExceptionConstraintViolation.cause!!.message } returns "Unique index or primary key violation"
      val exception = shouldThrow<ApiException> { commonService.addToList(itemId = 1, listId = 1) }
      exception.cause.shouldBeInstanceOf<SQLIntegrityConstraintViolationException>()
      exception.responseMessage.shouldBe("Item id 1 could not be added to list id 1 because it's already been added.")
      verify(exactly = 1) { mockLrmItemRepository.addItemToList(1, 1) }
    }

    it("unanticipated sql integrity constraint violation (original exception message is null)") {
      val exposedSQLExceptionConstraintViolation = exposedSQLExceptionConstraintViolation()
      every { mockLrmItemRepository.addItemToList(1, 1) } throws exposedSQLExceptionConstraintViolation
      every { mockLrmItemService.findById(1) } returns lrmItem()
      every { mockLrmListService.findById(1) } returns lrmList()
      every { exposedSQLExceptionConstraintViolation.contexts[0].statement } returns mockk<Statement<String>>()
      every { exposedSQLExceptionConstraintViolation.contexts[0].statement.type } returns mockk<StatementType>()
      every { exposedSQLExceptionConstraintViolation.cause?.message } returns null
      val exception = shouldThrow<ApiException> { commonService.addToList(itemId = 1, listId = 1) }
      exception.cause.shouldBeInstanceOf<SQLIntegrityConstraintViolationException>()
      exception.responseMessage.shouldBe(
        "Item id 1 could not be added to list id 1 because of an unanticipated sql integrity constraint violation.",
      )
      verify(exactly = 1) { mockLrmItemRepository.addItemToList(1, 1) }
    }

    it("unanticipated sql integrity constraint violation (original exception message is not null)") {
      val exposedSQLExceptionConstraintViolation = exposedSQLExceptionConstraintViolation()
      every { mockLrmItemRepository.addItemToList(1, 1) } throws exposedSQLExceptionConstraintViolation
      every { mockLrmItemService.findById(1) } returns lrmItem()
      every { mockLrmListService.findById(1) } returns lrmList()
      every { exposedSQLExceptionConstraintViolation.cause!!.message } returns "unanticipated sql integrity constraint violation"
      shouldThrow<ApiException> { commonService.addToList(itemId = 1, listId = 1) }
        .cause.shouldBeInstanceOf<SQLIntegrityConstraintViolationException>()
      verify(exactly = 1) { mockLrmItemRepository.addItemToList(1, 1) }
    }

    it("unanticipated exposed sql exception with undefined cause") {
      val exposedSQLExceptionNullCause = ExposedSQLException(
        cause = null,
        transaction = mockk<Transaction>(relaxed = true),
        contexts = listOf(mockk<StatementContext>(relaxed = true)),
      )
      val mockContexts = exposedSQLExceptionNullCause.contexts
      every { mockLrmItemRepository.addItemToList(1, 1) } throws exposedSQLExceptionNullCause
      every { mockContexts[0].statement } returns mockk<Statement<String>>()
      every { mockContexts[0].statement.type } returns mockk<StatementType>()
      val exception = shouldThrow<ApiException> { commonService.addToList(itemId = 1, listId = 1) }
      exception.cause.shouldBeInstanceOf<ExposedSQLException>()
      exception.httpStatus.shouldBe(HttpStatus.INTERNAL_SERVER_ERROR)
      exception.responseMessage.shouldBe("Item id 1 could not be added to list id 1 because of a sql exception with an undefined cause.")
      verify(exactly = 1) { mockLrmItemRepository.addItemToList(1, 1) }
    }

    it("unanticipated exposed sql exception") {
      every { mockLrmItemRepository.addItemToList(1, 1) } throws exposedSQLExceptionGeneric()
      every { exposedSQLExceptionGeneric().contexts[0].statement } returns mockk<Statement<String>>()
      every { exposedSQLExceptionGeneric().contexts[0].statement.type } returns mockk<StatementType>()
      val exception = shouldThrow<ApiException> { commonService.addToList(itemId = 1, listId = 1) }
      exception.cause.shouldBeInstanceOf<SQLException>()
      exception.cause!!.message.shouldContain("Cause of ExposedSQLException")
      exception.httpStatus.shouldBe(HttpStatus.INTERNAL_SERVER_ERROR)
      verify(exactly = 1) { mockLrmItemRepository.addItemToList(1, 1) }
    }
  }

  describe("moveToList()") {
    it("item is moved from one list to another list") {
      val spy = spyk(CommonService(mockLrmItemRepository, mockLrmItemService, mockLrmListService))
      every { mockLrmItemService.findById(1) } returns lrmItem()
      every { mockLrmListService.findById(2) } returns lrmList()
      every { mockLrmListService.findById(3) } returns lrmList()
      every { spy.addToList(any(), any()) } returns Pair("1", "3")
      every { spy.removeFromList(any(), any()) } returns Pair("1", "2")
      spy.moveToList(1, 2, 3)
      verify(exactly = 1) { spy.addToList(1, 3) }
      verify(exactly = 1) { spy.removeFromList(1, 2) }
    }

    it("anticipated api exception with cause of type abstract api exception") {
      val spy = spyk(CommonService(mockLrmItemRepository, mockLrmItemService, mockLrmListService))
      every { mockLrmItemRepository.addItemToList(3, 1) } throws exposedSQLExceptionConstraintViolation()
      every { mockLrmItemService.findById(1) } throws ItemNotFoundException(1)
      val exception = shouldThrow<ApiException> { spy.moveToList(1, 2, 3) }
      val causeHttpStatus = exception.cause.shouldBeInstanceOf<ItemNotFoundException>().httpStatus
      exception.httpStatus.shouldBe(causeHttpStatus)
      verify(exactly = 1) { spy.addToList(any(), any()) }
      verify(exactly = 0) { spy.removeFromList(any(), any()) }
    }

    it("anticipated api exception with cause not of type api exception with detail message") {
      val spy = spyk(CommonService(mockLrmItemRepository, mockLrmItemService, mockLrmListService))
      every { mockLrmItemRepository.addItemToList(3, 1) } throws Exception("Not of Type ApiException")
      every { mockLrmItemRepository.findByIdOrNull(1) } returns null
      val exception = shouldThrow<ApiException> { spy.moveToList(1, 2, 3) }
      exception.cause.shouldBeInstanceOf<Exception>()
      exception.httpStatus.shouldBe(HttpStatus.INTERNAL_SERVER_ERROR)
      verify(exactly = 1) { spy.addToList(any(), any()) }
      verify(exactly = 0) { spy.removeFromList(any(), any()) }
    }

    it("anticipated api exception with cause not of type api exception without detail message") {
      val spy = spyk(CommonService(mockLrmItemRepository, mockLrmItemService, mockLrmListService))
      every { mockLrmItemRepository.addItemToList(3, 1) } throws Exception()
      every { mockLrmItemRepository.findByIdOrNull(1) } returns null
      val exception = shouldThrow<ApiException> { spy.moveToList(1, 2, 3) }
      exception.cause.shouldBeInstanceOf<Exception>()
      exception.httpStatus.shouldBe(HttpStatus.INTERNAL_SERVER_ERROR)
      verify(exactly = 1) { spy.addToList(any(), any()) }
      verify(exactly = 0) { spy.removeFromList(any(), any()) }
    }

    it("anticipated api exception with root cause not of type api exception") {
      val spy = spyk(CommonService(mockLrmItemRepository, mockLrmItemService, mockLrmListService))
      every { mockLrmItemRepository.addItemToList(3, 1) } throws
        ApiException(httpStatus = HttpStatus.I_AM_A_TEAPOT, cause = Exception())
      every { mockLrmItemRepository.findByIdOrNull(1) } returns null
      val exception = shouldThrow<ApiException> { spy.moveToList(1, 2, 3) }
      exception.cause.shouldBeInstanceOf<Exception>()
      exception.httpStatus.shouldBe(HttpStatus.INTERNAL_SERVER_ERROR)
      verify(exactly = 1) { spy.addToList(any(), any()) }
      verify(exactly = 0) { spy.removeFromList(any(), any()) }
    }

    it("anticipated api exception with no root cause") {
      val spy = spyk(CommonService(mockLrmItemRepository, mockLrmItemService, mockLrmListService))
      every { mockLrmItemRepository.addItemToList(3, 1) } throws
        ApiException(httpStatus = HttpStatus.I_AM_A_TEAPOT)
      every { mockLrmItemRepository.findByIdOrNull(1) } returns null
      val exception = shouldThrow<ApiException> { spy.moveToList(1, 2, 3) }
      exception.cause.shouldBeNull()
      exception.httpStatus.shouldBe(HttpStatus.I_AM_A_TEAPOT)
      verify(exactly = 1) { spy.addToList(any(), any()) }
      verify(exactly = 0) { spy.removeFromList(any(), any()) }
    }
  }

  describe("removeFromList()") {
    it("removed from list") {
      every { mockLrmItemRepository.removeItemFromList(1, 2) } returns 1
      every { mockLrmItemService.findById(1) } returns lrmItem()
      every { mockLrmListService.findById(2) } returns lrmList()
      commonService.removeFromList(1, 2)
      verify(exactly = 1) { mockLrmItemRepository.removeItemFromList(1, 2) }
      verify(exactly = 1) { mockLrmItemService.findById(1) }
      verify(exactly = 1) { mockLrmListService.findById(2) }
    }

    it("item not found") {
      every { mockLrmItemRepository.removeItemFromList(1, 2) } returns 0
      every { mockLrmItemService.findById(any()) } throws ItemNotFoundException(1)
      val exception = shouldThrow<ApiException> { commonService.removeFromList(1, 2) }
      exception.cause.shouldBeInstanceOf<ItemNotFoundException>()
      verify(exactly = 1) { mockLrmItemRepository.removeItemFromList(1, 2) }
      verify(exactly = 1) { mockLrmItemService.findById(1) }
      verify(exactly = 0) { mockLrmListService.findById(2) }
    }

    it("list not found") {
      every { mockLrmItemRepository.removeItemFromList(1, 2) } returns 0
      every { mockLrmItemService.findById(1) } returns lrmItem()
      every { mockLrmListService.findById(2) } throws ListNotFoundException(2)
      val exception = shouldThrow<ApiException> { commonService.removeFromList(1, 2) }
      exception.cause.shouldBeInstanceOf<ListNotFoundException>()
      verify(exactly = 1) { mockLrmItemRepository.removeItemFromList(1, 2) }
      verify(exactly = 1) { mockLrmItemService.findById(1) }
      verify(exactly = 1) { mockLrmListService.findById(2) }
    }

    it("item is not associated with the list") {
      every { mockLrmItemRepository.removeItemFromList(1, 2) } returns 0
      every { mockLrmItemService.findById(1) } returns lrmItem()
      every { mockLrmListService.findById(2) } returns lrmList()
      val exception = shouldThrow<ApiException> { commonService.removeFromList(1, 2) }
      exception.cause.shouldBeNull()
      exception.responseMessage.shouldBe("Item id 1 is not associated with list id 2.")
      verify(exactly = 1) { mockLrmItemRepository.removeItemFromList(1, 2) }
      verify(exactly = 1) { mockLrmItemService.findById(1) }
      verify(exactly = 1) { mockLrmListService.findById(2) }
    }

    it("item is associated with the list multiple times") {
      every { mockLrmItemRepository.removeItemFromList(1, 2) } returns 2
      val exception = shouldThrow<ApiException> { commonService.removeFromList(1, 2) }
      exception.cause.shouldBeNull()
      verify(exactly = 1) { mockLrmItemRepository.removeItemFromList(1, 2) }
    }

    it("item repository throws exception") {
      every { mockLrmItemRepository.removeItemFromList(1, 2) } throws Exception("Lorem Ipsum")
      val exception = shouldThrow<ApiException> { commonService.removeFromList(1, 2) }
      exception.cause.shouldBeInstanceOf<Exception>()
      exception.httpStatus.shouldBe(HttpStatus.INTERNAL_SERVER_ERROR)
      exception.message.shouldNotBeNull().shouldBeEqual("Item id 1 could not be removed from list id 2.")
      exception.responseMessage.shouldBeEqual("Item id 1 could not be removed from list id 2.")
      exception.title.shouldBeEqual("API Exception")
    }
  }
})
