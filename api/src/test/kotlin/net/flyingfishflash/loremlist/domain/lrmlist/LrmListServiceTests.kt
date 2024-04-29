package net.flyingfishflash.loremlist.domain.lrmlist

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
import net.flyingfishflash.loremlist.domain.lrmlist.data.LrmList
import net.flyingfishflash.loremlist.domain.lrmlist.data.LrmListRepository
import net.flyingfishflash.loremlist.domain.lrmlist.data.dto.LrmListRequest
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.statements.StatementContext
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpStatus
import java.sql.SQLException
import java.util.UUID

/**
 * LrmListService Unit Tests
 */
class LrmListServiceTests : DescribeSpec({

  val lrmListRepository = mockk<LrmListRepository>()
  val lrmListService = LrmListService(lrmListRepository)

  val lrmListRequest = LrmListRequest("Lorem List Name", "Lorem List Description")

  val listUuid = UUID.randomUUID()
  fun lrmList(): LrmList = LrmList(id = 0, uuid = listUuid, name = lrmListRequest.name, description = lrmListRequest.description)

  fun exposedSQLExceptionGeneric(): ExposedSQLException = ExposedSQLException(
    cause = SQLException("Cause of ExposedSQLException"),
    transaction = mockk<Transaction>(relaxed = true),
    contexts = listOf(mockk<StatementContext>(relaxed = true)),
  )

  afterEach { clearAllMocks() }
  afterSpec { unmockkAll() }

  describe("create()") {
    it("list repository returns inserted list id") {
      every { lrmListRepository.insert(ofType(LrmListRequest::class)) } returns 1L
      every { lrmListRepository.findByIdOrNull(1L) } returns lrmList()
      lrmListService.create(lrmListRequest)
      verify(exactly = 1) { lrmListRepository.insert(ofType(LrmListRequest::class)) }
      verify(exactly = 1) { lrmListRepository.findByIdOrNull(any()) }
    }

    it("list repository throws exposed sql exception") {
      every { lrmListRepository.insert(ofType(LrmListRequest::class)) } throws exposedSQLExceptionGeneric()
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
      every { lrmListRepository.deleteById(1) } returns 0
      assertThrows<ListNotFoundException> {
        lrmListService.deleteSingleById(1)
      }.cause.shouldBeNull()
    }

    it("list repository returns > 1 deleted records") {
      every { lrmListRepository.deleteById(1) } returns 2
      assertThrows<ApiException> {
        lrmListService.deleteSingleById(1)
      }.cause.shouldBeNull()
    }

    it("list repository returns 1 deleted record") {
      every { lrmListRepository.findByIdOrNull(1) } returns lrmList()
      every { lrmListRepository.deleteById(1) } returns 1
      lrmListService.deleteSingleById(1)
      verify(exactly = 1) { lrmListRepository.deleteById(1) }
    }

    it("list repository throws exposed sql exception") {
      every { lrmListRepository.deleteById(1) } throws exposedSQLExceptionGeneric()
      val exception = shouldThrow<ApiException> { lrmListService.deleteSingleById(1) }
      exception.cause.shouldBeInstanceOf<ExposedSQLException>()
      exception.httpStatus.shouldBe(HttpStatus.INTERNAL_SERVER_ERROR)
      exception.message.shouldNotBeNull().shouldBeEqual("List 1 could not be deleted.")
      exception.responseMessage.shouldBeEqual("List 1 could not be deleted.")
      exception.title.shouldBeEqual("API Exception")
    }
  }

  describe("findAll()") {
    it("lists are returned") {
      every { lrmListRepository.findAll() } returns listOf(lrmList())
      lrmListService.findAll()
      verify(exactly = 1) { lrmListRepository.findAll() }
    }

    it("list repository throws exception") {
      every { lrmListRepository.findAll() } throws Exception("Lorem Ipsum")
      val exception = shouldThrow<ApiException> { lrmListService.findAll() }
      exception.httpStatus.shouldBeEqual(HttpStatus.INTERNAL_SERVER_ERROR)
      exception.cause.shouldBeInstanceOf<Exception>()
      exception.message.shouldBe("Lists could not be retrieved.")
      exception.responseMessage.shouldBe("Lists could not be retrieved.")
    }
  }

  describe("findAllIncludeItems()") {
    it("lists are returned") {
      every { lrmListRepository.findAllIncludeItems() } returns listOf(lrmList())
      lrmListService.findAllIncludeItems()
      verify(exactly = 1) { lrmListRepository.findAllIncludeItems() }
    }

    it("list repository throws exception") {
      every { lrmListRepository.findAllIncludeItems() } throws Exception("Lorem Ipsum")
      val exception = shouldThrow<ApiException> { lrmListService.findAllIncludeItems() }
      exception.httpStatus.shouldBeEqual(HttpStatus.INTERNAL_SERVER_ERROR)
      exception.cause.shouldBeInstanceOf<Exception>()
      exception.message.shouldBe("Lists (including associated items) could not be retrieved.")
      exception.responseMessage.shouldBe("Lists (including associated items) could not be retrieved.")
    }
  }

  describe("findById()") {
    it("list is found and returned") {
      every { lrmListRepository.findByIdOrNull(1) } returns lrmList()
      val result = lrmListService.findById(1)
      result.shouldBe(lrmList())
      verify(exactly = 1) { lrmListRepository.findByIdOrNull(1) }
    }

    it("list is not found") {
      every { lrmListRepository.findByIdOrNull(1) } returns null
      assertThrows<ListNotFoundException> {
        lrmListService.findById(1)
      }
    }

    it("list repository throws exception") {
      every { lrmListRepository.findByIdOrNull(1) } throws Exception("Lorem Ipsum")
      val exception = shouldThrow<ApiException> { lrmListService.findById(1) }
      exception.cause.shouldBeInstanceOf<Exception>()
      exception.httpStatus.shouldBe(HttpStatus.INTERNAL_SERVER_ERROR)
      exception.message.shouldNotBeNull().shouldBeEqual("List id 1 could not be retrieved.")
      exception.responseMessage.shouldBeEqual("List id 1 could not be retrieved.")
      exception.title.shouldBeEqual("API Exception")
    }
  }

  describe("findByIdIncludeItems()") {
    it("list is found and returned") {
      every { lrmListRepository.findByIdOrNullIncludeItems(1) } returns lrmList()
      val result = lrmListService.findByIdIncludeItems(1)
      result.shouldBe(lrmList())
      verify(exactly = 1) { lrmListRepository.findByIdOrNullIncludeItems(1) }
    }

    it("list is not found") {
      every { lrmListRepository.findByIdOrNullIncludeItems(1) } returns null
      assertThrows<ListNotFoundException> {
        lrmListService.findByIdIncludeItems(1)
      }
    }

    it("list repository throws exception") {
      every { lrmListRepository.findByIdOrNullIncludeItems(1) } throws Exception("Lorem Ipsum")
      val exception = shouldThrow<ApiException> { lrmListService.findByIdIncludeItems(1) }
      exception.cause.shouldBeInstanceOf<Exception>()
      exception.httpStatus.shouldBe(HttpStatus.INTERNAL_SERVER_ERROR)
      exception.message.shouldNotBeNull().shouldBeEqual("List id 1 (including associated items) could not be retrieved.")
      exception.responseMessage.shouldBeEqual("List id 1 (including associated items) could not be retrieved.")
      exception.title.shouldBeEqual("API Exception")
    }
  }

  describe("patch()") {
    it("list is not found") {
      every { lrmListRepository.findByIdOrNull(1) } returns null
      shouldThrow<ListNotFoundException> { lrmListService.patch(1, mapOf("name" to "lorum ipsum")) }
    }

    it("update name") {
      val expectedName = "patched lorem list"
      every { lrmListRepository.findByIdOrNull(1) } returns lrmList()
      every { lrmListRepository.update(ofType(LrmList::class)) } returns 1
      val patchedLrmList = lrmListService.patch(1, mapOf("name" to expectedName)).first
      patchedLrmList.name.shouldBe(expectedName)
      verify(exactly = 1) { lrmListRepository.findByIdOrNull(1) }
      verify(exactly = 1) { lrmListRepository.update(ofType(LrmList::class)) }
    }

    it("update name and description to current values") {
      val expectedName = lrmList().name
      val expectedDescription = lrmList().description
      every { lrmListRepository.findByIdOrNull(1) } returns lrmList()
      every { lrmListRepository.update(ofType(LrmList::class)) } returns 1
      val patchResponse = lrmListService.patch(1, mapOf("name" to expectedName, "description" to (expectedDescription ?: "")))
      patchResponse.second.shouldBeFalse()
      patchResponse.first.name.shouldBe(expectedName)
      patchResponse.first.description.shouldBe(expectedDescription)
      verify(exactly = 1) { lrmListRepository.findByIdOrNull(1) }
      verify(exactly = 0) { lrmListRepository.update(ofType(LrmList::class)) }
    }

    it("update description") {
      val expectedDescription = "patched lorem list description"
      every { lrmListRepository.findByIdOrNull(1) } returns lrmList()
      every { lrmListRepository.update(ofType(LrmList::class)) } returns 1
      val patchedLrmList = lrmListService.patch(1, mapOf("description" to expectedDescription)).first
      patchedLrmList.description.shouldBe(expectedDescription)
      verify(exactly = 1) { lrmListRepository.findByIdOrNull(1) }
      verify(exactly = 1) { lrmListRepository.update(ofType(LrmList::class)) }
    }

    it("update description to '  '") {
      every { lrmListRepository.findByIdOrNull(1) } returns lrmList()
      shouldThrow<ConstraintViolationException> { lrmListService.patch(1, mapOf("description" to "  ")) }
    }

    it("update an undefined list property") {
      every { lrmListRepository.findByIdOrNull(1) } returns lrmList()
      shouldThrow<IllegalArgumentException> {
        lrmListService.patch(1, mapOf("undefined property" to "irrelevant value"))
      }
    }

    it("update no properties") {
      every { lrmListRepository.findByIdOrNull(1) } returns lrmList()
      val patchReturn = lrmListService.patch(1, mapOf())
      patchReturn.first.shouldBeEqual(lrmList())
      patchReturn.second.shouldBeFalse()
      verify(exactly = 1) { lrmListRepository.findByIdOrNull(1) }
      verify(exactly = 0) { lrmListRepository.update(ofType(LrmList::class)) }
    }

    it("list repository updates more than 1 record") {
      val expectedName = "patched lorem list"
      every { lrmListRepository.findByIdOrNull(1) } returns lrmList()
      every { lrmListRepository.update(ofType(LrmList::class)) } returns 2
      val exception = shouldThrow<ApiException> { lrmListService.patch(1, mapOf("name" to expectedName)).first }
      exception.httpStatus.shouldBe(HttpStatus.INTERNAL_SERVER_ERROR)
      exception.cause.shouldBeNull()
      exception.detail.shouldBeEqual(
        "List id ${lrmList().id} could not be updated. 2 records would have been updated rather than 1.",
      )
      exception.responseMessage.shouldBeEqual(
        "List id ${lrmList().id} could not be updated. 2 records would have been updated rather than 1.",
      )
      verify(exactly = 1) { lrmListRepository.findByIdOrNull(1) }
      verify(exactly = 1) { lrmListRepository.update(ofType(LrmList::class)) }
    }

    it("list repository throws exposed sql exception") {
      val expectedName = "patched lorem list"
      every { lrmListRepository.findByIdOrNull(1) } returns lrmList()
      every { lrmListRepository.update(ofType(LrmList::class)) } throws exposedSQLExceptionGeneric()
      val exception = shouldThrow<ApiException> { lrmListService.patch(1, mapOf("name" to expectedName)).first }
      exception.httpStatus.shouldBe(HttpStatus.INTERNAL_SERVER_ERROR)
      exception.cause.shouldBeInstanceOf<ExposedSQLException>()
      exception.detail.shouldBeEqual(
        "List id ${lrmList().id} could not be updated. " +
          "The list was found and patch request is valid but an exception was thrown by the list repository.",
      )
      exception.responseMessage.shouldBeEqual("List id ${lrmList().id} could not be updated.")
      verify(exactly = 1) { lrmListRepository.findByIdOrNull(1) }
      verify(exactly = 1) { lrmListRepository.update(ofType(LrmList::class)) }
    }
  }
})
