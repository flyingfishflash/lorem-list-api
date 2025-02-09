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
import net.flyingfishflash.loremlist.domain.lrmlist.data.LrmListCreateRequest
import net.flyingfishflash.loremlist.toJsonElement
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.statements.StatementContext
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpStatus
import java.sql.SQLException
import java.util.UUID
import kotlinx.datetime.Clock.System.now

class LrmListServiceTests : DescribeSpec({

  val mockAssociationService = mockk<AssociationService>()
  val mockLrmListRepository = mockk<LrmListRepository>()
  val lrmListService = LrmListService(mockAssociationService, mockLrmListRepository)

  val lrmListCreateRequest = LrmListCreateRequest(name = "Lorem List Name", description = "Lorem List Description", public = true)

  val id0 = UUID.fromString("00000000-0000-4000-a000-000000000000")
  val id1 = UUID.fromString("00000000-0000-4000-a000-000000000001")

  val now = now()
  val mockUserName = "mockUserName"

  fun lrmList(): LrmList = LrmList(
    id = id0,
    name = lrmListCreateRequest.name,
    description = lrmListCreateRequest.description,
    public = lrmListCreateRequest.public,
    created = now,
    createdBy = "Lorem Ipsum Created By",
    updated = now,
    updatedBy = "Lorem Ipsum Updated By",
    items = null,
  )

  fun lrmListWithItems() = lrmList().copy(
    items = setOf(LrmItem(
      id = id0,
      name = "Lorem Item Name",
      description = "Lorem Ipsum Description",
      quantity = 0,
      created = now,
      createdBy = "Lorem Ipsum Created By",
      updated = now,
      updatedBy = "Lorem Ipsum Updated By",
      lists = null
    )),
  )

  fun exposedSQLExceptionGeneric(): ExposedSQLException = ExposedSQLException(
    cause = SQLException("Cause of ExposedSQLException"),
    transaction = mockk<Transaction>(relaxed = true),
    contexts = listOf(mockk<StatementContext>(relaxed = true)),
  )

  afterEach { clearAllMocks() }
  afterSpec { unmockkAll() }

  describe("countByOwner()") {
    it("count is returned") {
      every { mockLrmListRepository.countByOwner(owner = ofType(String::class)) } returns 999
      lrmListService.countByOwner("lorem ipsum").shouldBe(999)
      verify(exactly = 1) { mockLrmListRepository.countByOwner(owner = ofType(String::class)) }
    }

    it("list repository throws exception") {
      every { mockLrmListRepository.countByOwner(owner = ofType(String::class)) } throws Exception("Lorem Ipsum")
      val exception = shouldThrow<ApiException> { lrmListService.countByOwner("lorem ipsum") }
      exception.httpStatus.shouldBe(HttpStatus.INTERNAL_SERVER_ERROR)
      verify(exactly = 1) { mockLrmListRepository.countByOwner(owner = ofType(String::class)) }
    }
  }

  describe("create()") {
    it("list repository returns inserted list id") {
      every { mockLrmListRepository.insert(ofType(LrmList::class), ofType(String::class)) } returns id1
      every { mockLrmListRepository.findByOwnerAndIdOrNull(id = id1, owner = ofType(String::class)) } returns lrmList()
      lrmListService.create(lrmListCreateRequest, mockUserName)
      verify(exactly = 1) { mockLrmListRepository.insert(ofType(LrmList::class), ofType(String::class)) }
      verify(exactly = 1) { mockLrmListRepository.findByOwnerAndIdOrNull(id = ofType(UUID::class), owner = ofType(String::class)) }
    }

    it("list repository throws exposed sql exception") {
      every { mockLrmListRepository.insert(ofType(LrmList::class), ofType(String::class)) } throws exposedSQLExceptionGeneric()
      val exception = shouldThrow<ApiException> { lrmListService.create(lrmListCreateRequest, mockUserName) }
      exception.cause.shouldBeInstanceOf<ExposedSQLException>()
      exception.httpStatus.shouldBe(HttpStatus.INTERNAL_SERVER_ERROR)
      exception.message.shouldNotBeNull().shouldBeEqual("List could not be created.")
      exception.responseMessage.shouldBeEqual("List could not be created.")
      exception.title.shouldBeEqual(ApiException::class.java.simpleName)
    }
  }

  describe("deleteAllByOwner()") {
    it("all lists deleted") {
      every { mockLrmListRepository.findByOwnerIncludeItems(ofType(String::class)) } returns listOf(lrmListWithItems())
      every {
        mockAssociationService.deleteByListOwnerAndListId(listId = ofType(UUID::class), listOwner = ofType(String::class))
      } returns Pair("Lorem Ipsum", 999)
      every { mockLrmListRepository.deleteById(ids = any()) } returns 999
      val lrmListDeleteResponse = lrmListService.deleteByOwner(mockUserName)
      lrmListDeleteResponse.listNames.size.shouldBe(1)
      lrmListDeleteResponse.associatedItemNames.size.shouldBe(1)
      verify(exactly = 1) { mockLrmListRepository.findByOwnerIncludeItems(ofType(String::class)) }
      verify(
        exactly = 1,
      ) { mockAssociationService.deleteByListOwnerAndListId(listId = ofType(UUID::class), listOwner = ofType(String::class)) }
      verify(exactly = 1) { mockLrmListRepository.deleteById(ids = any()) }
    }

    it("no lists deleted (none present)") {
      every { mockLrmListRepository.findByOwnerIncludeItems(ofType(String::class)) } returns emptyList()
      every { mockLrmListRepository.deleteById(ids = any()) } returns 0
      val lrmListDeleteResponse = lrmListService.deleteByOwner(mockUserName)
      lrmListDeleteResponse.listNames.size.shouldBe(0)
      lrmListDeleteResponse.associatedItemNames.size.shouldBe(0)
      verify(exactly = 1) { mockLrmListRepository.findByOwnerIncludeItems(ofType(String::class)) }
      verify(
        exactly = 0,
      ) { mockAssociationService.deleteByListOwnerAndListId(listId = ofType(UUID::class), listOwner = ofType(String::class)) }
      verify(exactly = 0) { mockLrmListRepository.deleteById(ids = any()) }
    }

    it("no lists deleted (api exception)") {
      every { mockLrmListRepository.findByOwnerIncludeItems(ofType(String::class)) } throws (Exception("Lorem Ipsum"))
      val apiException = shouldThrow<ApiException> { lrmListService.deleteByOwner(mockUserName) }
      apiException.message.shouldContain("No lists were deleted")
      apiException.message.shouldContain("could not be retrieved")
      verify(exactly = 1) { mockLrmListRepository.findByOwnerIncludeItems(ofType(String::class)) }
      verify(
        exactly = 0,
      ) { mockAssociationService.deleteByListOwnerAndListId(listId = ofType(UUID::class), listOwner = ofType(String::class)) }
      verify(exactly = 0) { mockLrmListRepository.deleteById(ids = any()) }
    }
  }

  describe("deleteByIdAndOwner()") {
    it("list not found") {
      every {
        mockLrmListRepository.findByOwnerAndIdOrNullIncludeItems(
          id = ofType(UUID::class),
          owner = ofType(String::class),
        )
      } returns null
      val exception =
        shouldThrow<ApiException> { lrmListService.deleteByOwnerAndId(id = id1, owner = "lorem list", removeItemAssociations = false) }
      exception.cause.shouldBeInstanceOf<ListNotFoundException>()
      exception.responseMessage.shouldBe("List could not be deleted: List could not be found.")
      verify(
        exactly = 1,
      ) { mockLrmListRepository.findByOwnerAndIdOrNullIncludeItems(id = ofType(UUID::class), owner = ofType(String::class)) }
    }

    describe("associated items") {
      it("list is deleted (removeItemAssociations = true)") {
        every {
          mockLrmListRepository.findByOwnerAndIdOrNullIncludeItems(
            id = id1,
            owner = ofType(String::class),
          )
        } returns lrmListWithItems()
        every {
          mockAssociationService.deleteByListOwnerAndListId(listId = ofType(UUID::class), listOwner = ofType(String::class))
        } returns Pair(lrmList().name, 999)
        every { mockLrmListRepository.deleteByOwnerAndId(id = id1, owner = ofType(String::class)) } returns 1
        lrmListService.deleteByOwnerAndId(id = id1, owner = "lorem ipsum", removeItemAssociations = true)
        verify(exactly = 1) {
          mockLrmListRepository.findByOwnerAndIdOrNullIncludeItems(id = ofType(UUID::class), owner = ofType(String::class))
        }
        verify(
          exactly = 1,
        ) { mockAssociationService.deleteByListOwnerAndListId(listId = ofType(UUID::class), listOwner = ofType(String::class)) }
        verify(exactly = 1) { mockLrmListRepository.deleteByOwnerAndId(id = id1, owner = ofType(String::class)) }
      }

      it("list is not deleted (removeItemAssociations = false)") {
        every { mockLrmListRepository.findByOwnerAndIdOrNull(id = id1, owner = ofType(String::class)) } returns lrmList()
        every {
          mockLrmListRepository.findByOwnerAndIdOrNullIncludeItems(
            id = id1,
            owner = ofType(String::class),
          )
        } returns lrmListWithItems()
        val exception = shouldThrow<ApiException> {
          lrmListService.deleteByOwnerAndId(id = id1, owner = "lorem list", removeItemAssociations = false)
        }
        exception.supplemental.shouldNotBeNull().size.shouldBe(2)
        exception.supplemental.shouldNotBeNull()["listNames"]
          .shouldBe(listOf(lrmListWithItems().name).toJsonElement())
        exception.supplemental.shouldNotBeNull()["associatedItemNames"]
          .shouldBe(lrmListWithItems().items?.map { it.name }.toJsonElement())
        exception.message
          .shouldContainIgnoringCase(lrmList().name)
          .shouldContainIgnoringCase("could not be deleted")
          .shouldContainIgnoringCase("includes")
        exception.responseMessage
          .shouldContainIgnoringCase(lrmList().name)
          .shouldContainIgnoringCase("could not be deleted")
          .shouldContainIgnoringCase("includes")
      }

      it("list repository returns > 1 deleted records") {
        every {
          mockLrmListRepository.findByOwnerAndIdOrNullIncludeItems(
            id = id1,
            owner = ofType(String::class),
          )
        } returns lrmListWithItems()
        every {
          mockAssociationService.deleteByListOwnerAndListId(listId = ofType(UUID::class), listOwner = ofType(String::class))
        } returns Pair("Lorem Ipsum", 999)
        every { mockLrmListRepository.deleteByOwnerAndId(id = id1, owner = ofType(String::class)) } returns 2
        val exception = shouldThrow<ApiException> {
          lrmListService.deleteByOwnerAndId(id = id1, owner = "lorem ipsum", removeItemAssociations = true)
        }
        exception.cause.shouldBeInstanceOf<ApiException>()
        exception.httpStatus.shouldBe(HttpStatus.INTERNAL_SERVER_ERROR)
        exception.message
          .shouldContainIgnoringCase("$id1")
          .shouldContainIgnoringCase("could not be deleted")
          .shouldContainIgnoringCase("more than one")
        exception.responseMessage
          .shouldContainIgnoringCase("$id1")
          .shouldContainIgnoringCase("could not be deleted")
          .shouldContainIgnoringCase("more than one")
      }
    }

    describe("no associated items") {
      it("list is deleted") {
        every { mockLrmListRepository.findByOwnerAndIdOrNull(id = id1, owner = ofType(String::class)) } returns lrmList()
        every { mockLrmListRepository.findByOwnerAndIdOrNullIncludeItems(id = id1, owner = ofType(String::class)) } returns lrmList()
        every { mockLrmListRepository.deleteByOwnerAndId(id = id1, owner = ofType(String::class)) } returns 1
        lrmListService.deleteByOwnerAndId(id = id1, owner = "lorem list", removeItemAssociations = false)
        verify(
          exactly = 1,
        ) { mockLrmListRepository.findByOwnerAndIdOrNullIncludeItems(id = ofType(UUID::class), owner = ofType(String::class)) }
        verify(exactly = 1) { mockLrmListRepository.deleteByOwnerAndId(id = id1, owner = ofType(String::class)) }
      }

      it("list repository returns > 1 deleted records") {
        every { mockLrmListRepository.findByOwnerAndIdOrNull(id = id1, owner = ofType(String::class)) } returns lrmList()
        every { mockLrmListRepository.findByOwnerAndIdOrNullIncludeItems(id = id1, owner = ofType(String::class)) } returns lrmList()
        every {
          mockAssociationService.deleteByListOwnerAndListId(listId = id1, listOwner = ofType(String::class))
        } returns Pair("Lorem Ipsum", 999)
        every { mockLrmListRepository.deleteByOwnerAndId(id = id1, owner = ofType(String::class)) } returns 2
        val exception =
          shouldThrow<ApiException> { lrmListService.deleteByOwnerAndId(id = id1, owner = "lorem list", removeItemAssociations = false) }
        exception.cause.shouldBeInstanceOf<ApiException>()
        exception.httpStatus.shouldBe(HttpStatus.INTERNAL_SERVER_ERROR)
        exception.message
          .shouldContainIgnoringCase("$id1")
          .shouldContainIgnoringCase("could not be deleted")
          .shouldContainIgnoringCase("more than one")
        exception.responseMessage
          .shouldContainIgnoringCase("$id1")
          .shouldContainIgnoringCase("could not be deleted")
          .shouldContainIgnoringCase("more than one")
        verify(
          exactly = 1,
        ) { mockLrmListRepository.findByOwnerAndIdOrNullIncludeItems(id = ofType(UUID::class), owner = ofType(String::class)) }
      }
    }
  }

  describe("findAllByOwner()") {
    it("lists are returned") {
      every { mockLrmListRepository.findByOwner(ofType(String::class)) } returns listOf(lrmList())
      lrmListService.findByOwner(mockUserName)
      verify(exactly = 1) { mockLrmListRepository.findByOwner(ofType(String::class)) }
    }

    it("list repository throws exception") {
      every { mockLrmListRepository.findByOwner(ofType(String::class)) } throws Exception("Lorem Ipsum")
      val exception = shouldThrow<ApiException> { lrmListService.findByOwner(mockUserName) }
      exception.httpStatus.shouldBeEqual(HttpStatus.INTERNAL_SERVER_ERROR)
      exception.cause.shouldBeInstanceOf<Exception>()
      exception.message.shouldBe("Lists could not be retrieved.")
      exception.responseMessage.shouldBe("Lists could not be retrieved.")
    }
  }

  describe("findAllByOwnerIncludeItems()") {
    it("lists are returned") {
      every { mockLrmListRepository.findByOwnerIncludeItems(ofType(String::class)) } returns listOf(lrmList())
      lrmListService.findByOwnerIncludeItems(mockUserName)
      verify(exactly = 1) { mockLrmListRepository.findByOwnerIncludeItems(ofType(String::class)) }
    }

    it("list repository throws exception") {
      every { mockLrmListRepository.findByOwnerIncludeItems(ofType(String::class)) } throws Exception("Lorem Ipsum")
      val exception = shouldThrow<ApiException> { lrmListService.findByOwnerIncludeItems(mockUserName) }
      exception.httpStatus.shouldBeEqual(HttpStatus.INTERNAL_SERVER_ERROR)
      exception.cause.shouldBeInstanceOf<Exception>()
      exception.message.shouldBe("Lists (including associated items) could not be retrieved.")
      exception.responseMessage.shouldBe("Lists (including associated items) could not be retrieved.")
    }
  }

  describe("findAllPublic()") {
    it("lists are returned") {
      every { mockLrmListRepository.findByPublic() } returns listOf(lrmList())
      lrmListService.findByPublic()
      verify(exactly = 1) { mockLrmListRepository.findByPublic() }
    }

    it("list repository throws exception") {
      every { mockLrmListRepository.findByPublic() } throws Exception("Lorem Ipsum")
      val exception = shouldThrow<ApiException> { lrmListService.findByPublic() }
      exception.httpStatus.shouldBeEqual(HttpStatus.INTERNAL_SERVER_ERROR)
      exception.cause.shouldBeInstanceOf<Exception>()
      exception.message.shouldBe("Public lists could not be retrieved.")
      exception.responseMessage.shouldBe("Public lists could not be retrieved.")
    }
  }

  describe("findAllPublicIncludeItems()") {
    it("lists are returned") {
      every { mockLrmListRepository.findByPublicIncludeItems() } returns listOf(lrmList())
      lrmListService.findByPublicIncludeItems()
      verify(exactly = 1) { mockLrmListRepository.findByPublicIncludeItems() }
    }

    it("list repository throws exception") {
      every { mockLrmListRepository.findByPublicIncludeItems() } throws Exception("Lorem Ipsum")
      val exception = shouldThrow<ApiException> { lrmListService.findByPublicIncludeItems() }
      exception.httpStatus.shouldBeEqual(HttpStatus.INTERNAL_SERVER_ERROR)
      exception.cause.shouldBeInstanceOf<Exception>()
      exception.message.shouldBe("Public lists (including associated items) could not be retrieved.")
      exception.responseMessage.shouldBe("Public lists (including associated items) could not be retrieved.")
    }
  }

  describe("findByIdAndOwnerOrNull()") {
    it("list is found and returned") {
      every { mockLrmListRepository.findByOwnerAndIdOrNull(id = id1, owner = ofType(String::class)) } returns lrmList()
      val result = lrmListService.findByOwnerAndId(id = id1, owner = "lorem ipsum")
      result.shouldBe(lrmList())
      verify(exactly = 1) { mockLrmListRepository.findByOwnerAndIdOrNull(id = ofType(UUID::class), owner = ofType(String::class)) }
    }

    it("list is not found") {
      every { mockLrmListRepository.findByOwnerAndIdOrNull(id = id1, owner = ofType(String::class)) } returns null
      assertThrows<ListNotFoundException> {
        lrmListService.findByOwnerAndId(id = id1, owner = "lorem ipsum")
      }
    }

    it("list repository throws exception") {
      every { mockLrmListRepository.findByOwnerAndIdOrNull(id = id1, owner = ofType(String::class)) } throws Exception("Lorem Ipsum")
      val exception = shouldThrow<ApiException> { lrmListService.findByOwnerAndId(id = id1, owner = "lorem ipsum") }
      exception.cause.shouldBeInstanceOf<Exception>()
      exception.httpStatus.shouldBe(HttpStatus.INTERNAL_SERVER_ERROR)
      exception.message
        .shouldContainIgnoringCase("$id1")
        .shouldContainIgnoringCase("could not be retrieved")
      exception.responseMessage
        .shouldContainIgnoringCase("$id1")
        .shouldContainIgnoringCase("could not be retrieved")
      exception.title.shouldBeEqual(ApiException::class.java.simpleName)
    }
  }

  describe("findByIdAndOwnerIncludeItems()") {
    it("list is found and returned") {
      every { mockLrmListRepository.findByOwnerAndIdOrNullIncludeItems(id = id1, owner = ofType(String::class)) } returns lrmList()
      val result = lrmListService.findByOwnerAndIdIncludeItems(id = id1, owner = "lorem ipsum")
      result.shouldBe(lrmList())
      verify(exactly = 1) { mockLrmListRepository.findByOwnerAndIdOrNullIncludeItems(id = id1, owner = ofType(String::class)) }
    }

    it("list is not found") {
      every { mockLrmListRepository.findByOwnerAndIdOrNullIncludeItems(id = id1, owner = ofType(String::class)) } returns null
      assertThrows<ListNotFoundException> {
        lrmListService.findByOwnerAndIdIncludeItems(id = id1, owner = "lorem ipsum")
      }
    }

    it("list repository throws exception") {
      every {
        mockLrmListRepository.findByOwnerAndIdOrNullIncludeItems(
          id = id1,
          owner = ofType(String::class),
        )
      } throws Exception("Lorem Ipsum")
      val exception = shouldThrow<ApiException> { lrmListService.findByOwnerAndIdIncludeItems(id = id1, owner = "lorem ipsum") }
      exception.cause.shouldBeInstanceOf<Exception>()
      exception.httpStatus.shouldBe(HttpStatus.INTERNAL_SERVER_ERROR)
      exception.message
        .shouldContainIgnoringCase("$id1")
        .shouldContainIgnoringCase("including associated items")
        .shouldContainIgnoringCase("could not be retrieved")

      exception.responseMessage
        .shouldContainIgnoringCase("$id1")
        .shouldContainIgnoringCase("including associated items")
        .shouldContainIgnoringCase("could not be retrieved")
      exception.title.shouldBeEqual(ApiException::class.java.simpleName)
    }
  }

  describe("findByOwnerWithNoItems()") {
    it("items are returned") {
      every { mockLrmListRepository.findByOwnerAndHavingNoItemAssociations(owner = ofType(String::class)) } returns listOf(lrmList())
      lrmListService.findByOwnerAndHavingNoItemAssociations(owner = "lorem ipsum")
      verify(exactly = 1) { mockLrmListRepository.findByOwnerAndHavingNoItemAssociations(owner = ofType(String::class)) }
    }

    it("item repository throws exception") {
      every { mockLrmListRepository.findByOwnerAndHavingNoItemAssociations(owner = ofType(String::class)) } throws Exception("Lorem Ipsum")
      val exception = shouldThrow<ApiException> { lrmListService.findByOwnerAndHavingNoItemAssociations(owner = "lorem ipsum") }
      exception.httpStatus.shouldBe(HttpStatus.INTERNAL_SERVER_ERROR)
      exception.message.shouldContainIgnoringCase("could not be retrieved")
    }
  }

  describe("patchByIdAndOwner()") {
    it("list is not found") {
      every { mockLrmListRepository.findByOwnerAndIdOrNull(id = id1, owner = ofType(String::class)) } returns null
      shouldThrow<ListNotFoundException> {
        lrmListService.patchByOwnerAndId(id = id1, owner = "lorem ipsum", patchRequest = mapOf("name" to "lorum ipsum"))
      }
    }

    it("update name") {
      val expectedName = "patched lorem list"
      every { mockLrmListRepository.findByOwnerAndIdOrNull(id = id1, owner = ofType(String::class)) } returns lrmList()
      every { mockLrmListRepository.update(ofType(LrmList::class)) } returns 1
      val patchedLrmList = lrmListService.patchByOwnerAndId(
        id = id1,
        owner = "lorem ipsum",
        patchRequest = mapOf("name" to expectedName),
      ).first
      patchedLrmList.name.shouldBe(expectedName)
      verify(exactly = 2) { mockLrmListRepository.findByOwnerAndIdOrNull(id = id1, owner = ofType(String::class)) }
      verify(exactly = 1) { mockLrmListRepository.update(ofType(LrmList::class)) }
    }

    it("update name and description to current values") {
      val expectedName = lrmList().name
      val expectedDescription = lrmList().description
      every { mockLrmListRepository.findByOwnerAndIdOrNull(id = id1, owner = ofType(String::class)) } returns lrmList()
      every { mockLrmListRepository.update(ofType(LrmList::class)) } returns 1
      val patchResponse = lrmListService.patchByOwnerAndId(
        id = id1,
        owner = "lorem ipsum",
        patchRequest = mapOf("name" to expectedName, "description" to (expectedDescription ?: "")),
      )
      patchResponse.second.shouldBeFalse()
      patchResponse.first.name.shouldBe(expectedName)
      patchResponse.first.description.shouldBe(expectedDescription)
      verify(exactly = 1) { mockLrmListRepository.findByOwnerAndIdOrNull(id = id1, owner = ofType(String::class)) }
      verify(exactly = 0) { mockLrmListRepository.update(ofType(LrmList::class)) }
    }

    it("update description") {
      val expectedDescription = "patched lorem list description"
      every { mockLrmListRepository.findByOwnerAndIdOrNull(id = id1, owner = ofType(String::class)) } returns lrmList()
      every { mockLrmListRepository.update(ofType(LrmList::class)) } returns 1
      val patchedLrmList = lrmListService.patchByOwnerAndId(
        id = id1,
        owner = "lorem ipsum",
        patchRequest = mapOf("description" to expectedDescription),
      ).first
      patchedLrmList.description.shouldBe(expectedDescription)
      verify(exactly = 2) { mockLrmListRepository.findByOwnerAndIdOrNull(id = ofType(UUID::class), owner = ofType(String::class)) }
      verify(exactly = 1) { mockLrmListRepository.update(ofType(LrmList::class)) }
    }

    it("update description to '  '") {
      every { mockLrmListRepository.findByOwnerAndIdOrNull(id = id1, owner = ofType(String::class)) } returns lrmList()
      shouldThrow<ConstraintViolationException> {
        lrmListService.patchByOwnerAndId(id = id1, owner = "lorem ipsum", patchRequest = mapOf("description" to "  "))
      }
    }

    it("update public") {
      val expectedPublic = false
      every { mockLrmListRepository.findByOwnerAndIdOrNull(id = id1, owner = ofType(String::class)) } returns lrmList()
      every { mockLrmListRepository.update(ofType(LrmList::class)) } returns 1
      val patchedLrmList = lrmListService.patchByOwnerAndId(
        id = id1,
        owner = "lorem ipsum",
        patchRequest = mapOf("public" to expectedPublic),
      ).first
      patchedLrmList.public.shouldBe(expectedPublic)
      verify(exactly = 2) { mockLrmListRepository.findByOwnerAndIdOrNull(id = id1, owner = ofType(String::class)) }
      verify(exactly = 1) { mockLrmListRepository.update(ofType(LrmList::class)) }
    }

    it("update an undefined list property") {
      every { mockLrmListRepository.findByOwnerAndIdOrNull(id = id1, owner = ofType(String::class)) } returns lrmList()
      shouldThrow<IllegalArgumentException> {
        lrmListService.patchByOwnerAndId(id = id1, owner = "lorem ipsum", patchRequest = mapOf("undefined property" to "irrelevant value"))
      }
    }

    it("update no properties") {
      every { mockLrmListRepository.findByOwnerAndIdOrNull(id = id1, owner = ofType(String::class)) } returns lrmList()
      val patchReturn = lrmListService.patchByOwnerAndId(id = id1, owner = "lorem ipsum", patchRequest = mapOf())
      patchReturn.first.shouldBeEqual(lrmList())
      patchReturn.second.shouldBeFalse()
      verify(exactly = 1) { mockLrmListRepository.findByOwnerAndIdOrNull(id = ofType(UUID::class), owner = ofType(String::class)) }
      verify(exactly = 0) { mockLrmListRepository.update(ofType(LrmList::class)) }
    }

    it("list repository updates more than 1 record") {
      val expectedName = "patched lorem list"
      every { mockLrmListRepository.findByOwnerAndIdOrNull(id = id1, owner = ofType(String::class)) } returns lrmList()
      every { mockLrmListRepository.update(ofType(LrmList::class)) } returns 2
      val exception =
        shouldThrow<ApiException> {
          lrmListService.patchByOwnerAndId(id = id1, owner = "lorem ipsum", patchRequest = mapOf("name" to expectedName)).first
        }
      exception.httpStatus.shouldBe(HttpStatus.INTERNAL_SERVER_ERROR)
      exception.cause.shouldBeNull()
      exception.message?.shouldBeEqual(
        "List id ${lrmList().id} could not be updated. 2 records would have been updated rather than 1.",
      )
      exception.responseMessage.shouldBeEqual(
        "List id ${lrmList().id} could not be updated. 2 records would have been updated rather than 1.",
      )
      verify(exactly = 1) { mockLrmListRepository.findByOwnerAndIdOrNull(id = id1, owner = ofType(String::class)) }
      verify(exactly = 1) { mockLrmListRepository.update(ofType(LrmList::class)) }
    }

    it("list repository throws exposed sql exception") {
      val expectedName = "patched lorem list"
      every { mockLrmListRepository.findByOwnerAndIdOrNull(id = id1, owner = ofType(String::class)) } returns lrmList()
      every { mockLrmListRepository.update(ofType(LrmList::class)) } throws exposedSQLExceptionGeneric()
      val exception =
        shouldThrow<ApiException> {
          lrmListService.patchByOwnerAndId(id = id1, owner = "lorem ipsum", patchRequest = mapOf("name" to expectedName)).first
        }
      exception.httpStatus.shouldBe(HttpStatus.INTERNAL_SERVER_ERROR)
      exception.cause.shouldBeInstanceOf<ExposedSQLException>()
      exception.message?.shouldBeEqual(
        "List id ${lrmList().id} could not be updated. " +
          "The list was found and patch request is valid but an exception was thrown by the list repository.",
      )
      exception.responseMessage.shouldBeEqual("List id ${lrmList().id} could not be updated.")
      verify(exactly = 1) { mockLrmListRepository.findByOwnerAndIdOrNull(id = id1, owner = ofType(String::class)) }
      verify(exactly = 1) { mockLrmListRepository.update(ofType(LrmList::class)) }
    }
  }
})
