package net.flyingfishflash.loremlist.domain.lrmlist

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.validation.ConstraintViolationException
import net.flyingfishflash.loremlist.core.exceptions.ApiException
import net.flyingfishflash.loremlist.domain.lrmlist.data.LrmList
import net.flyingfishflash.loremlist.domain.lrmlist.data.LrmListRepository
import net.flyingfishflash.loremlist.domain.lrmlist.data.dto.LrmListRequest
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.statements.StatementContext
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpStatus
import java.sql.SQLException

/**
 * LrmListService Unit Tests
 */
class LrmListServiceTests : DescribeSpec({

  val lrmListRepository = mockk<LrmListRepository>()
  val lrmListService = LrmListService(lrmListRepository)
  val mockTransaction = mockk<Transaction>()
  val mockStatementContext = mockk<StatementContext>()
  val mockContexts = listOf(mockStatementContext)
  val exposedSQLExceptionGeneric = ExposedSQLException(
    cause = SQLException("Cause of ExposedSQLException"),
    transaction = mockTransaction,
    contexts = mockContexts,
  )
  val lrmListName = "Lorem List Name"
  val lrmListDescription = "Lorem List Description"
  val lrmListMockResponse = LrmList(id = 0, name = lrmListName, description = lrmListDescription)
  val lrmListRequest = LrmListRequest(lrmListName, lrmListDescription)
  val id = 1L

  describe("create()") {
    it("repository returns inserted list id") {
      every { lrmListRepository.insert(ofType(LrmListRequest::class)) } returns id
      every { lrmListRepository.findByIdOrNull(id) } returns lrmListMockResponse
      lrmListService.create(lrmListRequest)
      verify(exactly = 1) { lrmListRepository.insert(ofType(LrmListRequest::class)) }
      verify(exactly = 1) { lrmListRepository.findByIdOrNull(any()) }
    }

    it("repository throws exposed sql exception") {
      every { lrmListRepository.insert(ofType(LrmListRequest::class)) } throws exposedSQLExceptionGeneric
      val exception = shouldThrow<ApiException> { lrmListService.create(lrmListRequest) }
      exception.cause.shouldBeInstanceOf<ExposedSQLException>()
      exception.httpStatus.shouldBe(HttpStatus.INTERNAL_SERVER_ERROR)
      exception.message.shouldNotBeNull().shouldBeEqual("List could not be created.")
      exception.responseMessage.shouldBeEqual("List could not be created.")
      exception.title.shouldBeEqual("API Exception")
    }
  }

  describe("delete()") {
    it("list repository returns 0 deleted records") {
      every { lrmListRepository.deleteById(id) } returns 0
      assertThrows<ListDeleteException> {
        lrmListService.deleteSingleById(id)
      }.cause.shouldBeInstanceOf<ListNotFoundException>()
    }

    it("list repository returns > 1 deleted records") {
      every { lrmListRepository.deleteById(id) } returns 2
      assertThrows<ListDeleteException> {
        lrmListService.deleteSingleById(id)
      }.cause.shouldBeNull()
    }

    it("list repository returns 1 deleted record") {
      every { lrmListRepository.findByIdOrNull(id) } returns lrmListMockResponse
      every { lrmListRepository.deleteById(id) } returns 1
      lrmListService.deleteSingleById(id)
      verify(exactly = 1) { lrmListRepository.deleteById(id) }
    }
  }

  describe("findAll()") {
    it("lists are returned") {
      every { lrmListRepository.findAll() } returns listOf(lrmListMockResponse)
      lrmListService.findAll()
      verify(exactly = 1) { lrmListRepository.findAll() }
    }
  }

  describe("findAllListsAndItems()") {
    it("lists are returned") {
      every { lrmListRepository.findAllIncludeItems() } returns listOf(lrmListMockResponse)
      lrmListService.findAllIncludeItems()
      verify(exactly = 1) { lrmListRepository.findAllIncludeItems() }
    }
  }

  describe("findByIdOrListNotFoundExceptionListAndItems()") {
    it("list is found and returned") {
      every { lrmListRepository.findByIdOrNullIncludeItems(id) } returns lrmListMockResponse
      val result = lrmListService.findByIdOrListNotFoundExceptionIncludeItems(id)
      result.shouldBe(lrmListMockResponse)
      verify(exactly = 1) { lrmListRepository.findByIdOrNullIncludeItems(id) }
    }

    it("list is not found") {
      every { lrmListRepository.findByIdOrNullIncludeItems(id) } returns null
      assertThrows<ListNotFoundException> {
        lrmListService.findByIdOrListNotFoundExceptionIncludeItems(id)
      }
    }
  }

  describe("findByIdOrListNotFoundException()") {
    it("list is found and returned") {
      every { lrmListRepository.findByIdOrNull(id) } returns lrmListMockResponse
      val result = lrmListService.findByIdOrListNotFoundException(id)
      result.shouldBe(lrmListMockResponse)
      verify(exactly = 1) { lrmListRepository.findByIdOrNull(id) }
    }

    it("list is not found") {
      every { lrmListRepository.findByIdOrNull(id) } returns null
      assertThrows<ListNotFoundException> {
        lrmListService.findByIdOrListNotFoundException(id)
      }
    }
  }

  describe("patch()") {
    it("list is not found") {
      every { lrmListRepository.findByIdOrNull(id) } returns null
      assertThrows<ListNotFoundException> {
        lrmListService.patch(id, mapOf("name" to "lorum ipsum"))
      }
    }

    it("update name") {
      val expectedName = "patched lorem list"
      every { lrmListRepository.findByIdOrNull(id) } returns lrmListMockResponse
      every { lrmListRepository.update(ofType(LrmList::class)) } returns lrmListMockResponse
      val patchedLrmList = lrmListService.patch(id, mapOf("name" to expectedName)).first
      patchedLrmList.name.shouldBe(expectedName)
      verify(exactly = 1) { lrmListRepository.findByIdOrNull(id) }
      verify(exactly = 1) { lrmListRepository.update(ofType(LrmList::class)) }
    }

    it("update description") {
      val expectedDescription = "patched lorem list description"
      every { lrmListRepository.findByIdOrNull(id) } returns lrmListMockResponse
      every { lrmListRepository.update(ofType(LrmList::class)) } returns lrmListMockResponse
      val patchedLrmList = lrmListService.patch(id, mapOf("description" to expectedDescription)).first
      patchedLrmList.description.shouldBe(expectedDescription)
      verify(exactly = 1) { lrmListRepository.findByIdOrNull(id) }
      verify(exactly = 1) { lrmListRepository.update(ofType(LrmList::class)) }
    }

    it("update an undefined list property") {
      every { lrmListRepository.findByIdOrNull(id) } returns lrmListMockResponse
      assertThrows<IllegalArgumentException> {
        lrmListService.patch(id, mapOf("undefined property" to "irrelevant value"))
      }
    }

    it("update description to '  '") {
      every { lrmListRepository.findByIdOrNull(id) } returns lrmListMockResponse
      assertThrows<ConstraintViolationException> {
        lrmListService.patch(id, mapOf("description" to "  "))
      }
    }
  }

  afterTest {
    clearMocks(lrmListRepository)
  }
})
