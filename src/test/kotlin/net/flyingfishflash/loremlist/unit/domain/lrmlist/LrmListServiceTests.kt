package net.flyingfishflash.loremlist.unit.domain.lrmlist

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldContainIgnoringCase
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import jakarta.validation.ConstraintViolationException
import net.flyingfishflash.loremlist.core.exceptions.ApiException
import net.flyingfishflash.loremlist.domain.association.AssociationService
import net.flyingfishflash.loremlist.domain.lrmitem.LrmItem
import net.flyingfishflash.loremlist.domain.lrmlist.ListNotFoundException
import net.flyingfishflash.loremlist.domain.lrmlist.LrmList
import net.flyingfishflash.loremlist.domain.lrmlist.LrmListRepository
import net.flyingfishflash.loremlist.domain.lrmlist.LrmListService
import net.flyingfishflash.loremlist.domain.lrmlist.data.LrmListRequest
import net.flyingfishflash.loremlist.toJsonElement
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.statements.StatementContext
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpStatus
import java.sql.SQLException
import java.util.UUID

class LrmListServiceTests : DescribeSpec({

  val mockAssociationService = mockk<AssociationService>()
  val mockLrmListRepository = mockk<LrmListRepository>()
  val lrmListService = LrmListService(mockAssociationService, mockLrmListRepository)

  val lrmListRequest = LrmListRequest("Lorem List Name", "Lorem List Description")

  val uuid0 = UUID.fromString("00000000-0000-4000-a000-000000000000")
  val uuid1 = UUID.fromString("00000000-0000-4000-a000-000000000001")

  fun lrmList(): LrmList = LrmList(uuid = uuid0, name = lrmListRequest.name, description = lrmListRequest.description)
  fun lrmListWithItems() = lrmList().copy(
    items = setOf(LrmItem(uuid = uuid0, name = "Lorem Item Name")),
  )
  fun exposedSQLExceptionGeneric(): ExposedSQLException = ExposedSQLException(
    cause = SQLException("Cause of ExposedSQLException"),
    transaction = mockk<Transaction>(relaxed = true),
    contexts = listOf(mockk<StatementContext>(relaxed = true)),
  )

  afterEach { clearAllMocks() }
  afterSpec { unmockkAll() }

  describe("count()") {
    it("count is returned") {
      every { mockLrmListRepository.count() } returns 999
      lrmListService.count().shouldBe(999)
      verify(exactly = 1) { mockLrmListRepository.count() }
    }

    it("list repository throws exception") {
      every { mockLrmListRepository.count() } throws Exception("Lorem Ipsum")
      val exception = shouldThrow<ApiException> { lrmListService.count() }
      exception.httpStatus.shouldBe(HttpStatus.INTERNAL_SERVER_ERROR)
      verify(exactly = 1) { mockLrmListRepository.count() }
    }
  }

  describe("create()") {
    it("list repository returns inserted list id") {
      every { mockLrmListRepository.insert(ofType(LrmListRequest::class)) } returns uuid1
      every { mockLrmListRepository.findByIdOrNull(uuid1) } returns lrmList()
      lrmListService.create(lrmListRequest)
      verify(exactly = 1) { mockLrmListRepository.insert(ofType(LrmListRequest::class)) }
      verify(exactly = 1) { mockLrmListRepository.findByIdOrNull(any()) }
    }

    it("list repository throws exposed sql exception") {
      every { mockLrmListRepository.insert(ofType(LrmListRequest::class)) } throws exposedSQLExceptionGeneric()
      val exception = shouldThrow<ApiException> { lrmListService.create(lrmListRequest) }
      exception.cause.shouldBeInstanceOf<ExposedSQLException>()
      exception.httpStatus.shouldBe(HttpStatus.INTERNAL_SERVER_ERROR)
      exception.message.shouldNotBeNull().shouldBeEqual("List could not be created.")
      exception.responseMessage.shouldBeEqual("List could not be created.")
      exception.title.shouldBeEqual(ApiException::class.java.simpleName)
    }
  }

  describe("deleteAll()") {
    it("all lists deleted") {
      every { mockLrmListRepository.findAll() } returns listOf(lrmList())
      every { mockLrmListRepository.findAllIncludeItems() } returns listOf(lrmListWithItems())
      every { mockAssociationService.deleteAll() } returns 999
      every { mockLrmListRepository.deleteAll() } returns 999
      val lrmListDeleteResponse = lrmListService.deleteAll()
      lrmListDeleteResponse.listNames.size.shouldBe(1)
      lrmListDeleteResponse.associatedItemNames.size.shouldBe(1)
      verify(exactly = 1) { mockLrmListRepository.findAll() }
      verify(exactly = 1) { mockLrmListRepository.findAllIncludeItems() }
      verify(exactly = 1) { mockAssociationService.deleteAll() }
      verify(exactly = 1) { mockLrmListRepository.deleteAll() }
    }

    it("no lists deleted (none present)") {
      every { mockLrmListRepository.findAll() } returns emptyList()
      every { mockLrmListRepository.findAllIncludeItems() } returns emptyList()
      every { mockAssociationService.deleteAll() } returns 999
      every { mockLrmListRepository.deleteAll() } returns 999
      val lrmListDeleteResponse = lrmListService.deleteAll()
      lrmListDeleteResponse.listNames.size.shouldBe(0)
      lrmListDeleteResponse.associatedItemNames.size.shouldBe(0)
      verify(exactly = 1) { mockLrmListRepository.findAll() }
      verify(exactly = 1) { mockLrmListRepository.findAllIncludeItems() }
      verify(exactly = 0) { mockAssociationService.deleteAll() }
      verify(exactly = 0) { mockLrmListRepository.deleteAll() }
    }

    it("no lists deleted (api exception)") {
      every { mockLrmListRepository.findAll() } throws (Exception("Lorem Ipsum"))
      val apiException = shouldThrow<ApiException> { lrmListService.deleteAll() }
      apiException.message.shouldContain("No lists were deleted")
      apiException.message.shouldContain("Lists could not be retrieved")
      verify(exactly = 1) { mockLrmListRepository.findAll() }
      verify(exactly = 0) { mockLrmListRepository.findAllIncludeItems() }
      verify(exactly = 0) { mockAssociationService.deleteAll() }
      verify(exactly = 0) { mockLrmListRepository.deleteAll() }
    }
  }

  describe("deleteById()") {
    it("list not found") {
      every { mockLrmListRepository.findByIdOrNull(uuid1) } returns null
      val exception = shouldThrow<ApiException> { lrmListService.deleteById(uuid1, removeItemAssociations = false) }
      exception.cause.shouldBeInstanceOf<ListNotFoundException>()
      exception.responseMessage.shouldBe("List id $uuid1 could not be deleted: List id $uuid1 could not be found.")
      verify(exactly = 1) { mockLrmListRepository.findByIdOrNull(any()) }
    }

    describe("associated items") {
      it("list is deleted (removeItemAssociations = true)") {
        every { mockLrmListRepository.findByIdOrNull(uuid1) } returns lrmList()
        every { mockLrmListRepository.findByIdOrNullIncludeItems(uuid1) } returns lrmListWithItems()
        every { mockAssociationService.deleteAllOfList(uuid1) } returns Pair(lrmList().name, 999)
        every { mockLrmListRepository.deleteById(uuid1) } returns 1
        lrmListService.deleteById(uuid1, removeItemAssociations = true)
        verify(exactly = 1) { mockLrmListRepository.findByIdOrNull(any()) }
        verify(exactly = 1) { mockLrmListRepository.findByIdOrNullIncludeItems(any()) }
        verify(exactly = 1) { mockAssociationService.deleteAllOfList(any()) }
        verify(exactly = 1) { mockLrmListRepository.deleteById(any()) }
      }

      it("list is not deleted (removeItemAssociations = false)") {
        every { mockLrmListRepository.findByIdOrNull(uuid1) } returns lrmList()
        every { mockLrmListRepository.findByIdOrNullIncludeItems(uuid1) } returns lrmListWithItems()
        val exception = shouldThrow<ApiException> {
          lrmListService.deleteById(uuid1, removeItemAssociations = false)
        }
        exception.supplemental.shouldNotBeNull().size.shouldBe(2)
        exception.supplemental.shouldNotBeNull()["listNames"]
          .shouldBe(listOf(lrmListWithItems().name).toJsonElement())
        exception.supplemental.shouldNotBeNull()["associatedItemNames"]
          .shouldBe(lrmListWithItems().items?.map { it.name }.toJsonElement())
        exception.message
          .shouldContainIgnoringCase("$uuid1")
          .shouldContainIgnoringCase("could not be deleted")
          .shouldContainIgnoringCase("is associated with")
        exception.responseMessage
          .shouldContainIgnoringCase("$uuid1")
          .shouldContainIgnoringCase("could not be deleted")
          .shouldContainIgnoringCase("is associated with")
      }

      it("list repository returns > 1 deleted records") {
        every { mockLrmListRepository.findByIdOrNull(uuid1) } returns lrmList()
        every { mockLrmListRepository.findByIdOrNullIncludeItems(uuid1) } returns lrmListWithItems()
        every { mockAssociationService.deleteAllOfList(uuid1) } returns Pair("Lorem Ipsum", 999)
        every { mockLrmListRepository.deleteById(uuid1) } returns 2
        val exception = shouldThrow<ApiException> { lrmListService.deleteById(uuid1, removeItemAssociations = true) }
        exception.cause.shouldBeInstanceOf<ApiException>()
        exception.httpStatus.shouldBe(HttpStatus.INTERNAL_SERVER_ERROR)
        exception.message
          .shouldContainIgnoringCase("$uuid1")
          .shouldContainIgnoringCase("could not be deleted")
          .shouldContainIgnoringCase("more than one")
        exception.responseMessage
          .shouldContainIgnoringCase("$uuid1")
          .shouldContainIgnoringCase("could not be deleted")
          .shouldContainIgnoringCase("more than one")
      }
    }

    describe("no associated items") {
      it("list is deleted") {
        every { mockLrmListRepository.findByIdOrNull(uuid1) } returns lrmList()
        every { mockLrmListRepository.findByIdOrNullIncludeItems(uuid1) } returns lrmList()
        every { mockLrmListRepository.deleteById(uuid1) } returns 1
        lrmListService.deleteById(uuid1, removeItemAssociations = false)
        verify(exactly = 1) { mockLrmListRepository.findByIdOrNullIncludeItems(any()) }
        verify(exactly = 1) { mockLrmListRepository.deleteById(uuid1) }
      }

      it("list repository returns > 1 deleted records") {
        every { mockLrmListRepository.findByIdOrNull(uuid1) } returns lrmList()
        every { mockLrmListRepository.findByIdOrNullIncludeItems(uuid1) } returns lrmList()
        every { mockAssociationService.deleteAllOfList(uuid1) } returns Pair("Lorem Ipsum", 999)
        every { mockLrmListRepository.deleteById(uuid1) } returns 2
        val exception = shouldThrow<ApiException> { lrmListService.deleteById(uuid1, removeItemAssociations = false) }
        exception.cause.shouldBeInstanceOf<ApiException>()
        exception.httpStatus.shouldBe(HttpStatus.INTERNAL_SERVER_ERROR)
        exception.message
          .shouldContainIgnoringCase("$uuid1")
          .shouldContainIgnoringCase("could not be deleted")
          .shouldContainIgnoringCase("more than one")
        exception.responseMessage
          .shouldContainIgnoringCase("$uuid1")
          .shouldContainIgnoringCase("could not be deleted")
          .shouldContainIgnoringCase("more than one")
        verify(exactly = 1) { mockLrmListRepository.findByIdOrNullIncludeItems(any()) }
      }
    }
  }

  describe("findAll()") {
    it("lists are returned") {
      every { mockLrmListRepository.findAll() } returns listOf(lrmList())
      lrmListService.findAll()
      verify(exactly = 1) { mockLrmListRepository.findAll() }
    }

    it("list repository throws exception") {
      every { mockLrmListRepository.findAll() } throws Exception("Lorem Ipsum")
      val exception = shouldThrow<ApiException> { lrmListService.findAll() }
      exception.httpStatus.shouldBeEqual(HttpStatus.INTERNAL_SERVER_ERROR)
      exception.cause.shouldBeInstanceOf<Exception>()
      exception.message.shouldBe("Lists could not be retrieved.")
      exception.responseMessage.shouldBe("Lists could not be retrieved.")
    }
  }

  describe("findAllIncludeItems()") {
    it("lists are returned") {
      every { mockLrmListRepository.findAllIncludeItems() } returns listOf(lrmList())
      lrmListService.findAllIncludeItems()
      verify(exactly = 1) { mockLrmListRepository.findAllIncludeItems() }
    }

    it("list repository throws exception") {
      every { mockLrmListRepository.findAllIncludeItems() } throws Exception("Lorem Ipsum")
      val exception = shouldThrow<ApiException> { lrmListService.findAllIncludeItems() }
      exception.httpStatus.shouldBeEqual(HttpStatus.INTERNAL_SERVER_ERROR)
      exception.cause.shouldBeInstanceOf<Exception>()
      exception.message.shouldBe("Lists (including associated items) could not be retrieved.")
      exception.responseMessage.shouldBe("Lists (including associated items) could not be retrieved.")
    }
  }

  describe("findById()") {
    it("list is found and returned") {
      every { mockLrmListRepository.findByIdOrNull(uuid1) } returns lrmList()
      val result = lrmListService.findById(uuid1)
      result.shouldBe(lrmList())
      verify(exactly = 1) { mockLrmListRepository.findByIdOrNull(any()) }
    }

    it("list is not found") {
      every { mockLrmListRepository.findByIdOrNull(uuid1) } returns null
      assertThrows<ListNotFoundException> {
        lrmListService.findById(uuid1)
      }
    }

    it("list repository throws exception") {
      every { mockLrmListRepository.findByIdOrNull(uuid1) } throws Exception("Lorem Ipsum")
      val exception = shouldThrow<ApiException> { lrmListService.findById(uuid1) }
      exception.cause.shouldBeInstanceOf<Exception>()
      exception.httpStatus.shouldBe(HttpStatus.INTERNAL_SERVER_ERROR)
      exception.message
        .shouldContainIgnoringCase("$uuid1")
        .shouldContainIgnoringCase("could not be retrieved")
      exception.responseMessage
        .shouldContainIgnoringCase("$uuid1")
        .shouldContainIgnoringCase("could not be retrieved")
      exception.title.shouldBeEqual(ApiException::class.java.simpleName)
    }
  }

  describe("findByIdIncludeItems()") {
    it("list is found and returned") {
      every { mockLrmListRepository.findByIdOrNullIncludeItems(uuid1) } returns lrmList()
      val result = lrmListService.findByIdIncludeItems(uuid1)
      result.shouldBe(lrmList())
      verify(exactly = 1) { mockLrmListRepository.findByIdOrNullIncludeItems(uuid1) }
    }

    it("list is not found") {
      every { mockLrmListRepository.findByIdOrNullIncludeItems(uuid1) } returns null
      assertThrows<ListNotFoundException> {
        lrmListService.findByIdIncludeItems(uuid1)
      }
    }

    it("list repository throws exception") {
      every { mockLrmListRepository.findByIdOrNullIncludeItems(uuid1) } throws Exception("Lorem Ipsum")
      val exception = shouldThrow<ApiException> { lrmListService.findByIdIncludeItems(uuid1) }
      exception.cause.shouldBeInstanceOf<Exception>()
      exception.httpStatus.shouldBe(HttpStatus.INTERNAL_SERVER_ERROR)
      exception.message
        .shouldContainIgnoringCase("$uuid1")
        .shouldContainIgnoringCase("including associated items")
        .shouldContainIgnoringCase("could not be retrieved")

      exception.responseMessage
        .shouldContainIgnoringCase("$uuid1")
        .shouldContainIgnoringCase("including associated items")
        .shouldContainIgnoringCase("could not be retrieved")
      exception.title.shouldBeEqual(ApiException::class.java.simpleName)
    }
  }

  describe("patch()") {
    it("list is not found") {
      every { mockLrmListRepository.findByIdOrNull(uuid1) } returns null
      shouldThrow<ListNotFoundException> { lrmListService.patch(uuid1, mapOf("name" to "lorum ipsum")) }
    }

    it("update name") {
      val expectedName = "patched lorem list"
      every { mockLrmListRepository.findByIdOrNull(uuid1) } returns lrmList()
      every { mockLrmListRepository.update(ofType(LrmList::class)) } returns 1
      val patchedLrmList = lrmListService.patch(uuid1, mapOf("name" to expectedName)).first
      patchedLrmList.name.shouldBe(expectedName)
      verify(exactly = 2) { mockLrmListRepository.findByIdOrNull(uuid1) }
      verify(exactly = 1) { mockLrmListRepository.update(ofType(LrmList::class)) }
    }

    it("update name and description to current values") {
      val expectedName = lrmList().name
      val expectedDescription = lrmList().description
      every { mockLrmListRepository.findByIdOrNull(uuid1) } returns lrmList()
      every { mockLrmListRepository.update(ofType(LrmList::class)) } returns 1
      val patchResponse = lrmListService.patch(uuid1, mapOf("name" to expectedName, "description" to (expectedDescription ?: "")))
      patchResponse.second.shouldBeFalse()
      patchResponse.first.name.shouldBe(expectedName)
      patchResponse.first.description.shouldBe(expectedDescription)
      verify(exactly = 1) { mockLrmListRepository.findByIdOrNull(uuid1) }
      verify(exactly = 0) { mockLrmListRepository.update(ofType(LrmList::class)) }
    }

    it("update description") {
      val expectedDescription = "patched lorem list description"
      every { mockLrmListRepository.findByIdOrNull(uuid1) } returns lrmList()
      every { mockLrmListRepository.update(ofType(LrmList::class)) } returns 1
      val patchedLrmList = lrmListService.patch(uuid1, mapOf("description" to expectedDescription)).first
      patchedLrmList.description.shouldBe(expectedDescription)
      verify(exactly = 2) { mockLrmListRepository.findByIdOrNull(any()) }
      verify(exactly = 1) { mockLrmListRepository.update(ofType(LrmList::class)) }
    }

    it("update description to '  '") {
      every { mockLrmListRepository.findByIdOrNull(uuid1) } returns lrmList()
      shouldThrow<ConstraintViolationException> { lrmListService.patch(uuid1, mapOf("description" to "  ")) }
    }

    it("update an undefined list property") {
      every { mockLrmListRepository.findByIdOrNull(uuid1) } returns lrmList()
      shouldThrow<IllegalArgumentException> {
        lrmListService.patch(uuid1, mapOf("undefined property" to "irrelevant value"))
      }
    }

    it("update no properties") {
      every { mockLrmListRepository.findByIdOrNull(uuid1) } returns lrmList()
      val patchReturn = lrmListService.patch(uuid1, mapOf())
      patchReturn.first.shouldBeEqual(lrmList())
      patchReturn.second.shouldBeFalse()
      verify(exactly = 1) { mockLrmListRepository.findByIdOrNull(any()) }
      verify(exactly = 0) { mockLrmListRepository.update(ofType(LrmList::class)) }
    }

    it("list repository updates more than 1 record") {
      val expectedName = "patched lorem list"
      every { mockLrmListRepository.findByIdOrNull(uuid1) } returns lrmList()
      every { mockLrmListRepository.update(ofType(LrmList::class)) } returns 2
      val exception = shouldThrow<ApiException> { lrmListService.patch(uuid1, mapOf("name" to expectedName)).first }
      exception.httpStatus.shouldBe(HttpStatus.INTERNAL_SERVER_ERROR)
      exception.cause.shouldBeNull()
      exception.message?.shouldBeEqual(
        "List id ${lrmList().uuid} could not be updated. 2 records would have been updated rather than 1.",
      )
      exception.responseMessage.shouldBeEqual(
        "List id ${lrmList().uuid} could not be updated. 2 records would have been updated rather than 1.",
      )
      verify(exactly = 1) { mockLrmListRepository.findByIdOrNull(uuid1) }
      verify(exactly = 1) { mockLrmListRepository.update(ofType(LrmList::class)) }
    }

    it("list repository throws exposed sql exception") {
      val expectedName = "patched lorem list"
      every { mockLrmListRepository.findByIdOrNull(uuid1) } returns lrmList()
      every { mockLrmListRepository.update(ofType(LrmList::class)) } throws exposedSQLExceptionGeneric()
      val exception = shouldThrow<ApiException> { lrmListService.patch(uuid1, mapOf("name" to expectedName)).first }
      exception.httpStatus.shouldBe(HttpStatus.INTERNAL_SERVER_ERROR)
      exception.cause.shouldBeInstanceOf<ExposedSQLException>()
      exception.message?.shouldBeEqual(
        "List id ${lrmList().uuid} could not be updated. " +
          "The list was found and patch request is valid but an exception was thrown by the list repository.",
      )
      exception.responseMessage.shouldBeEqual("List id ${lrmList().uuid} could not be updated.")
      verify(exactly = 1) { mockLrmListRepository.findByIdOrNull(uuid1) }
      verify(exactly = 1) { mockLrmListRepository.update(ofType(LrmList::class)) }
    }
  }
})
