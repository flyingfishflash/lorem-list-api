package net.flyingfishflash.loremlist.domain.lrmitem

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
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
import org.junit.jupiter.api.assertThrows
import java.sql.SQLException
import java.sql.SQLIntegrityConstraintViolationException

class LrmItemServiceTests : DescribeSpec({
  val lrmItemRepository = mockk<LrmItemRepository>()
  val lrmListRepository = mockk<LrmListRepository>()
  val lrmItemService = LrmItemService(lrmItemRepository, lrmListRepository)
  val mockTransaction = mockk<Transaction>()
  val mockStatementContext = mockk<StatementContext>()
  val mockContexts = listOf(mockStatementContext)
  val mockSQLIntegrityConstraintViolationException = mockk<SQLIntegrityConstraintViolationException>()

  val lrmItemName = "Lorem Item Name"
  val lrmItemDescription = "Lorem Item Description"
  val lrmItemMockResponse = LrmItem(id = 0, name = lrmItemName, description = lrmItemDescription)
  val lrmItemRequest = LrmItemRequest(lrmItemName, lrmItemDescription)
  val id = 1L
  val lrmListName = "Lorem List Name"
  val lrmListDescription = "Lorem List Description"
  val lrmListMockResponse = LrmList(id = 0, name = lrmListName, description = lrmListDescription)

  describe("addToList()") {
    it("item added to list") {
      every { lrmItemRepository.addItemToList(1L, 1L) } just runs
      lrmItemService.addToList(itemId = 1, listId = 1)
      verify(exactly = 1) { lrmItemRepository.addItemToList(1L, 1L) }
    }

    it("item not found") {
      val exposedSQLException = ExposedSQLException(
        cause = mockSQLIntegrityConstraintViolationException,
        transaction = mockTransaction,
        contexts = mockContexts,
      )
      every { lrmItemRepository.addItemToList(1L, 1L) } throws exposedSQLException
      every { lrmItemRepository.findByIdOrNull(1L) } returns null
      assertThrows<ItemAddToListException> {
        lrmItemService.addToList(itemId = 1, listId = 1)
      }.cause.shouldBeInstanceOf<ItemNotFoundException>()
      verify(exactly = 1) { lrmItemRepository.addItemToList(1L, 1L) }
    }

    it("list not found") {
      val exposedSQLException = ExposedSQLException(
        cause = mockSQLIntegrityConstraintViolationException,
        transaction = mockTransaction,
        contexts = mockContexts,
      )
      every { lrmItemRepository.addItemToList(1L, 1L) } throws exposedSQLException
      every { lrmItemRepository.findByIdOrNull(1L) } returns lrmItemMockResponse
      every { lrmListRepository.findByIdOrNull(1L) } returns null
      assertThrows<ItemAddToListException> {
        lrmItemService.addToList(itemId = 1, listId = 1)
      }.cause.shouldBeInstanceOf<ListNotFoundException>()
      verify(exactly = 1) { lrmItemRepository.addItemToList(1L, 1L) }
    }

    it("item already added to list") {
      val exposedSQLException = ExposedSQLException(
        cause = mockSQLIntegrityConstraintViolationException,
        transaction = mockTransaction,
        contexts = mockContexts,
      )
      every { lrmItemRepository.addItemToList(1L, 1L) } throws exposedSQLException
      every { lrmItemRepository.findByIdOrNull(1L) } returns lrmItemMockResponse
      every { lrmListRepository.findByIdOrNull(1L) } returns lrmListMockResponse
      every { mockContexts[0].statement } returns mockk<Statement<String>>()
      every { mockContexts[0].statement.type } returns mockk<StatementType>()
      assertThrows<ApiException> {
        lrmItemService.addToList(itemId = 1, listId = 1)
      }.cause.shouldBeInstanceOf<ExposedSQLException>()
      verify(exactly = 1) { lrmItemRepository.addItemToList(1L, 1L) }
    }

    it("other exposed sql exception caught") {
      val exposedSQLException = ExposedSQLException(
        cause = SQLException("Cause of ExposedSQLException"),
        transaction = mockTransaction,
        contexts = mockContexts,
      )
      every { lrmItemRepository.addItemToList(1L, 1L) } throws exposedSQLException
      every { mockContexts[0].statement } returns mockk<Statement<String>>()
      every { mockContexts[0].statement.type } returns mockk<StatementType>()
      assertThrows<ApiException> {
        lrmItemService.addToList(itemId = 1, listId = 1)
      }.cause.shouldBeInstanceOf<SQLException>()
        .cause?.message.shouldContain("Cause of ExposedSQLException")
      verify(exactly = 1) { lrmItemRepository.addItemToList(1L, 1L) }
    }
  }

  describe("create()") {
    it("repository returns inserted item") {
      every { lrmItemRepository.insert(ofType(LrmItemRequest::class)) } returns lrmItemMockResponse
      lrmItemService.create(lrmItemRequest)
      verify(exactly = 1) { lrmItemRepository.insert(lrmItemRequest) }
    }
  }

  describe("deleteSingleById()") {
    it("item repository returns 0 deleted records") {
      every { lrmItemRepository.deleteById(id) } returns 0
      assertThrows<ItemDeleteException> {
        lrmItemService.deleteSingleById(id)
      }.cause.shouldBeInstanceOf<ItemNotFoundException>()
    }

    it("item repository returns > 1 deleted records") {
      every { lrmItemRepository.deleteById(id) } returns 2
      assertThrows<ItemDeleteException> {
        lrmItemService.deleteSingleById(id)
      }.cause.shouldBeNull()
    }

    it("item repository returns 1 deleted record") {
      every { lrmItemRepository.findByIdOrNull(id) } returns lrmItemMockResponse
      every { lrmItemRepository.deleteById(id) } returns 1
      lrmItemService.deleteSingleById(id)
      verify(exactly = 1) { lrmItemRepository.deleteById(id) }
    }
  }

  describe("findAll()") {
    it("items are returned") {
      every { lrmItemRepository.findAll() } returns listOf(lrmItemMockResponse)
      lrmItemService.findAll()
      verify(exactly = 1) { lrmItemRepository.findAll() }
    }
  }

  describe("findAllAndLists()") {
    it("items are returned") {
      every { lrmItemRepository.findAllAndLists() } returns listOf(lrmItemMockResponse)
      lrmItemService.findAllAndLists()
      verify(exactly = 1) { lrmItemRepository.findAllAndLists() }
    }
  }

  afterTest {
    clearMocks(lrmItemRepository)
  }
})
