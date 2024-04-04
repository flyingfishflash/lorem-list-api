package net.flyingfishflash.loremlist.domain.lrmitem

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.nulls.shouldBeNull
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
import net.flyingfishflash.loremlist.core.exceptions.AbstractApiException
import net.flyingfishflash.loremlist.core.exceptions.ApiException
import net.flyingfishflash.loremlist.domain.lrmitem.data.LrmItem
import net.flyingfishflash.loremlist.domain.lrmitem.data.LrmItemRepository
import net.flyingfishflash.loremlist.domain.lrmitem.data.LrmItemRequest
import net.flyingfishflash.loremlist.domain.lrmlist.ListNotFoundException
import net.flyingfishflash.loremlist.domain.lrmlist.data.LrmList
import net.flyingfishflash.loremlist.domain.lrmlist.data.LrmListRepository
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.statements.Statement
import org.jetbrains.exposed.sql.statements.StatementContext
import org.jetbrains.exposed.sql.statements.StatementType
import org.springframework.http.HttpStatus
import java.sql.SQLException
import java.sql.SQLIntegrityConstraintViolationException

class LrmItemServiceTests : DescribeSpec({
  val mockLrmItemRepository = mockk<LrmItemRepository>()
  val mockLrmListRepository = mockk<LrmListRepository>()
  val mockTransaction = mockk<Transaction>()
  val mockStatementContext = mockk<StatementContext>()
  val mockContexts = listOf(mockStatementContext)
  val lrmItemService = LrmItemService(mockLrmItemRepository, mockLrmListRepository)
  val mockSQLIntegrityConstraintViolationException = mockk<SQLIntegrityConstraintViolationException>()
  val exposedSQLExceptionConstraintViolation = ExposedSQLException(
    cause = mockSQLIntegrityConstraintViolationException,
    transaction = mockTransaction,
    contexts = mockContexts,
  )

  val lrmItemName = "Lorem Item Name"
  val lrmItemDescription = "Lorem Item Description"
  val lrmItemMockResponse = LrmItem(id = 0, name = lrmItemName, description = lrmItemDescription)
  val lrmItemRequest = LrmItemRequest(lrmItemName, lrmItemDescription)
  val id = 1L
  val lrmListName = "Lorem List Name"
  val lrmListDescription = "Lorem List Description"
  val lrmListMockResponse = LrmList(id = 0, name = lrmListName, description = lrmListDescription)

  describe("addToList()") {
    it("added to list") {
      every { mockLrmItemRepository.addItemToList(1L, 1L) } just Runs
      every { mockLrmItemRepository.findByIdOrNull(1L) } returns LrmItem(id = 1L, name = lrmItemName)
      every { mockLrmListRepository.findByIdOrNull(1L) } returns LrmList(id = 1L, name = lrmListName)
      val response = lrmItemService.addToList(itemId = 1, listId = 1)
      response.first.shouldBeEqual(lrmItemName)
      response.second.shouldBeEqual(lrmListName)
      verify(exactly = 1) { mockLrmItemRepository.addItemToList(1L, 1L) }
      verify(exactly = 1) { mockLrmItemRepository.findByIdOrNull(1L) }
      verify(exactly = 1) { mockLrmListRepository.findByIdOrNull(1L) }
    }

    it("item not found") {
      every { mockLrmItemRepository.addItemToList(1L, 1L) } throws exposedSQLExceptionConstraintViolation
      every { mockLrmItemRepository.findByIdOrNull(1L) } returns null
      shouldThrow<ItemAddToListException> { lrmItemService.addToList(itemId = 1, listId = 1) }
        .cause.shouldBeInstanceOf<ItemNotFoundException>()
      verify(exactly = 1) { mockLrmItemRepository.addItemToList(1L, 1L) }
    }

    it("list not found") {
      every { mockLrmItemRepository.addItemToList(1L, 1L) } throws exposedSQLExceptionConstraintViolation
      every { mockLrmItemRepository.findByIdOrNull(1L) } returns lrmItemMockResponse
      every { mockLrmListRepository.findByIdOrNull(1L) } returns null
      shouldThrow<ItemAddToListException> { lrmItemService.addToList(itemId = 1, listId = 1) }
        .cause.shouldBeInstanceOf<ListNotFoundException>()
      verify(exactly = 1) { mockLrmItemRepository.addItemToList(1L, 1L) }
    }

    it("already added to list") {
      every { mockLrmItemRepository.addItemToList(1L, 1L) } throws exposedSQLExceptionConstraintViolation
      every { mockLrmItemRepository.findByIdOrNull(1L) } returns lrmItemMockResponse
      every { mockLrmListRepository.findByIdOrNull(1L) } returns lrmListMockResponse
      every { mockContexts[0].statement } returns mockk<Statement<String>>()
      every { mockContexts[0].statement.type } returns mockk<StatementType>()
      every { mockSQLIntegrityConstraintViolationException.message } returns "Unique index or primary key violation"
      shouldThrow<ItemAddToListException> { lrmItemService.addToList(itemId = 1, listId = 1) }
        .responseMessage.shouldBe("Item id 1 could not be added to list id 1 because it's already been added.")
      verify(exactly = 1) { mockLrmItemRepository.addItemToList(1L, 1L) }
    }

    it("unanticipated sql integrity constraint violation") {
      every { mockLrmItemRepository.addItemToList(1L, 1L) } throws exposedSQLExceptionConstraintViolation
      every { mockLrmItemRepository.findByIdOrNull(1L) } returns lrmItemMockResponse
      every { mockLrmListRepository.findByIdOrNull(1L) } returns lrmListMockResponse
      every { mockSQLIntegrityConstraintViolationException.message } returns "unanticipated sql integrity constraint violation"
      shouldThrow<ItemAddToListException> { lrmItemService.addToList(itemId = 1, listId = 1) }
        .cause.shouldBeInstanceOf<SQLIntegrityConstraintViolationException>()
      verify(exactly = 1) { mockLrmItemRepository.addItemToList(1L, 1L) }
    }

    it("unanticipated exposed sql exception") {
      val exposedSQLExceptionGeneric = ExposedSQLException(
        cause = SQLException("Cause of ExposedSQLException"),
        transaction = mockTransaction,
        contexts = mockContexts,
      )
      every { mockLrmItemRepository.addItemToList(1L, 1L) } throws exposedSQLExceptionGeneric
      every { mockContexts[0].statement } returns mockk<Statement<String>>()
      every { mockContexts[0].statement.type } returns mockk<StatementType>()
      val exception = shouldThrow<ApiException> { lrmItemService.addToList(itemId = 1, listId = 1) }
      exception.httpStatus.shouldBe(HttpStatus.INTERNAL_SERVER_ERROR)
      exception.cause.shouldBeInstanceOf<SQLException>()
      exception.cause?.message.shouldContain("Cause of ExposedSQLException")
      verify(exactly = 1) { mockLrmItemRepository.addItemToList(1L, 1L) }
    }
  }

  describe("create()") {
    it("repository returns inserted item") {
      every { mockLrmItemRepository.insert(ofType(LrmItemRequest::class)) } returns lrmItemMockResponse
      lrmItemService.create(lrmItemRequest)
      verify(exactly = 1) { mockLrmItemRepository.insert(lrmItemRequest) }
    }
  }

  describe("deleteSingleById()") {
    it("repository returns 0 deleted records") {
      every { mockLrmItemRepository.deleteById(id) } returns 0
      shouldThrow<ItemDeleteException> { lrmItemService.deleteSingleById(id) }
        .cause.shouldBeInstanceOf<ItemNotFoundException>()
    }

    it("repository returns > 1 deleted records") {
      every { mockLrmItemRepository.deleteById(id) } returns 2
      shouldThrow<ItemDeleteException> { lrmItemService.deleteSingleById(id) }
        .cause.shouldBeNull()
    }

    it("repository returns 1 deleted record") {
      every { mockLrmItemRepository.findByIdOrNull(id) } returns lrmItemMockResponse
      every { mockLrmItemRepository.deleteById(id) } returns 1
      lrmItemService.deleteSingleById(id)
      verify(exactly = 1) { mockLrmItemRepository.deleteById(id) }
    }
  }

  describe("findAll()") {
    it("all are returned") {
      every { mockLrmItemRepository.findAll() } returns listOf(lrmItemMockResponse)
      lrmItemService.findAll()
      verify(exactly = 1) { mockLrmItemRepository.findAll() }
    }
  }

  describe("findAllAndLists()") {
    it("all and lists are returned") {
      every { mockLrmItemRepository.findAllAndLists() } returns listOf(lrmItemMockResponse)
      lrmItemService.findAllAndLists()
      verify(exactly = 1) { mockLrmItemRepository.findAllAndLists() }
    }
  }

  describe("findById()") {
    it("item is returned") {
      every { mockLrmItemRepository.findByIdOrNull(1L) } returns lrmItemMockResponse
      lrmItemService.findById(1L)
      verify(exactly = 1) { mockLrmItemRepository.findByIdOrNull(1L) }
    }
  }

  describe("findByIdAndLists()") {
    it("item and lists are returned") {
      every { mockLrmItemRepository.findByIdOrNullAndLists(1L) } returns lrmItemMockResponse
      lrmItemService.findByIdAndLists(1L)
      verify(exactly = 1) { mockLrmItemRepository.findByIdOrNullAndLists(1L) }
    }
  }

  describe("moveToList()") {
    it("item is moved from one list to another list") {
      val spy = spyk(LrmItemService(mockLrmItemRepository, mockLrmListRepository))
      every { mockLrmItemRepository.findByIdOrNull(1L) } returns lrmItemMockResponse
      every { mockLrmListRepository.findByIdOrNull(2L) } returns lrmListMockResponse
      every { mockLrmListRepository.findByIdOrNull(3L) } returns lrmListMockResponse
      every { spy.addToList(any(), any()) } returns Pair("1L", "3L")
      every { spy.removeFromList(any(), any()) } returns Pair("1L", "2L")
      spy.moveToList(1L, 2L, 3L)
      verify(exactly = 1) { spy.addToList(1L, 3L) }
      verify(exactly = 1) { spy.removeFromList(1L, 2L) }
    }

    it("anticipated api exception with cause of type abstract api exception") {
      val spy = spyk(LrmItemService(mockLrmItemRepository, mockLrmListRepository))
      every { mockLrmItemRepository.addItemToList(3L, 1L) } throws exposedSQLExceptionConstraintViolation
      every { mockLrmItemRepository.findByIdOrNull(1L) } returns null
      shouldThrow<ApiException> {
        spy.moveToList(1L, 2L, 3L)
      }.cause.shouldBeInstanceOf<AbstractApiException>()
      verify(exactly = 1) { spy.addToList(any(), any()) }
      verify(exactly = 0) { spy.removeFromList(any(), any()) }
    }
  }

  describe("removeFromList()") {
    it("removed from list") {
      every { mockLrmItemRepository.removeItemFromList(1L, 2L) } returns 1
      every { mockLrmItemRepository.findByIdOrNull(1L) } returns lrmItemMockResponse
      every { mockLrmListRepository.findByIdOrNull(2L) } returns lrmListMockResponse
      lrmItemService.removeFromList(1L, 2L)
      verify(exactly = 1) { mockLrmItemRepository.removeItemFromList(1L, 2L) }
      verify(exactly = 1) { mockLrmItemRepository.findByIdOrNull(1L) }
      verify(exactly = 1) { mockLrmListRepository.findByIdOrNull(2L) }
    }

    it("item not found") {
      every { mockLrmItemRepository.removeItemFromList(any(), any()) } returns 0
      every { mockLrmItemRepository.findByIdOrNull(any()) } returns null
      val exception = shouldThrow<ItemRemoveFromListException> { lrmItemService.removeFromList(1, 2) }
      exception.cause.shouldBeInstanceOf<ItemNotFoundException>()
      verify(exactly = 1) { mockLrmItemRepository.removeItemFromList(1, 2) }
      verify(exactly = 1) { mockLrmItemRepository.findByIdOrNull(1L) }
      verify(exactly = 0) { mockLrmListRepository.findByIdOrNull(2L) }
    }

    it("list not found") {
      every { mockLrmItemRepository.removeItemFromList(any(), any()) } returns 0
      every { mockLrmItemRepository.findByIdOrNull(any()) } returns lrmItemMockResponse
      every { mockLrmListRepository.findByIdOrNull(any()) } returns null
      val exception = shouldThrow<ItemRemoveFromListException> { lrmItemService.removeFromList(1, 2) }
      exception.cause.shouldBeInstanceOf<ListNotFoundException>()
      verify(exactly = 1) { mockLrmItemRepository.removeItemFromList(1, 2) }
      verify(exactly = 1) { mockLrmItemRepository.findByIdOrNull(1L) }
      verify(exactly = 1) { mockLrmListRepository.findByIdOrNull(2L) }
    }

    it("item is not associated with the list") {
      every { mockLrmItemRepository.removeItemFromList(1L, 2L) } returns 0
      every { mockLrmItemRepository.findByIdOrNull(1L) } returns lrmItemMockResponse
      every { mockLrmListRepository.findByIdOrNull(2L) } returns lrmListMockResponse
      val exception = shouldThrow<ItemRemoveFromListException> { lrmItemService.removeFromList(1, 2) }
      exception.cause.shouldBeNull()
      verify(exactly = 1) { mockLrmItemRepository.removeItemFromList(1, 2) }
      verify(exactly = 1) { mockLrmItemRepository.findByIdOrNull(1L) }
      verify(exactly = 1) { mockLrmListRepository.findByIdOrNull(2L) }
    }

    it("item is associated with the list multiple times") {
      every { mockLrmItemRepository.removeItemFromList(1L, 2L) } returns 2
      val exception = shouldThrow<ItemRemoveFromListException> { lrmItemService.removeFromList(1, 2) }
      exception.cause.shouldBeNull()
      verify(exactly = 1) { mockLrmItemRepository.removeItemFromList(1, 2) }
    }
  }

  afterTest {
    clearAllMocks()
  }

  afterSpec {
    unmockkAll()
  }
})
