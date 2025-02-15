package net.flyingfishflash.loremlist.unit.domain.lrmlist

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.equals.shouldBeEqual
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
import net.flyingfishflash.loremlist.domain.association.AssociationService
import net.flyingfishflash.loremlist.domain.lrmitem.LrmItem
import net.flyingfishflash.loremlist.domain.lrmlist.ListNotFoundException
import net.flyingfishflash.loremlist.domain.lrmlist.LrmList
import net.flyingfishflash.loremlist.domain.lrmlist.LrmListRepository
import net.flyingfishflash.loremlist.domain.lrmlist.LrmListServiceDefault
import net.flyingfishflash.loremlist.toJsonElement
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.statements.StatementContext
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpStatus
import java.sql.SQLException
import java.util.UUID
import kotlinx.datetime.Clock.System.now
import net.flyingfishflash.loremlist.domain.exceptions.DomainException
import net.flyingfishflash.loremlist.domain.ServiceResponse
import net.flyingfishflash.loremlist.domain.lrmlist.data.LrmListCreate
import net.flyingfishflash.loremlist.domain.lrmlist.data.LrmListDeleted

class LrmListServiceTests : DescribeSpec({

  val mockAssociationService = mockk<AssociationService>()
  val mockLrmListRepository = mockk<LrmListRepository>()
  val lrmListService = LrmListServiceDefault(mockAssociationService, mockLrmListRepository)

  val lrmListCreate = LrmListCreate(name = "Lorem List Name", description = "Lorem List Description", public = true)
  val id0 = UUID.fromString("00000000-0000-4000-a000-000000000000")
  val id1 = UUID.fromString("00000000-0000-4000-a000-000000000001")
  val now = now()
  val mockUserName = "mockUserName"
  val irrelevantMessage = "ksADs8y96KRa1Zo4ipMdr5t8faudmFj4c564S02MjsNG6TXEO7yctC08Bb53bCB7"

  fun lrmList(): LrmList = LrmList(
    id = id0,
    name = lrmListCreate.name,
    description = lrmListCreate.description,
    public = lrmListCreate.public,
    created = now,
    createdBy = "Lorem Ipsum Created By",
    updated = now,
    updatedBy = "Lorem Ipsum Updated By",
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
      val serviceResponse = lrmListService.countByOwner("lorem ipsum")
      serviceResponse.content shouldBe 999L
      serviceResponse.message shouldBe "999 lists."
      verify { mockLrmListRepository.countByOwner(owner = ofType(String::class)) }
    }

    it("list repository throws exception") {
      every { mockLrmListRepository.countByOwner(owner = ofType(String::class)) } throws Exception("Lorem Ipsum")
      val exception = shouldThrow<DomainException> { lrmListService.countByOwner("lorem ipsum") }
      exception.httpStatus.shouldBe(HttpStatus.INTERNAL_SERVER_ERROR)
      verify { mockLrmListRepository.countByOwner(owner = ofType(String::class)) }
    }
  }

  describe("create()") {
    it("list repository returns inserted list id") {
      every { mockLrmListRepository.insert(ofType(LrmList::class), ofType(String::class)) } returns id1
      every { mockLrmListRepository.findByOwnerAndIdOrNull(id = id1, owner = ofType(String::class)) } returns lrmList()
      val serviceResponse = lrmListService.create(lrmListCreate, mockUserName)
      serviceResponse.content shouldBe lrmList()
      serviceResponse.message shouldBe "Created list '${lrmList().name}'"
      verify { mockLrmListRepository.insert(ofType(LrmList::class), ofType(String::class)) }
      verify { mockLrmListRepository.findByOwnerAndIdOrNull(id = ofType(UUID::class), owner = ofType(String::class)) }
    }

    it("list repository throws exposed sql exception") {
      every { mockLrmListRepository.insert(ofType(LrmList::class), ofType(String::class)) } throws exposedSQLExceptionGeneric()
      val exception = shouldThrow<DomainException> { lrmListService.create(lrmListCreate, mockUserName) }
      exception.cause.shouldBeInstanceOf<ExposedSQLException>()
      exception.httpStatus.shouldBe(HttpStatus.INTERNAL_SERVER_ERROR)
      exception.message.shouldNotBeNull().shouldBeEqual("List could not be created.")
      exception.responseMessage.shouldBeEqual("List could not be created.")
      exception.title.shouldBeEqual(DomainException::class.java.simpleName)
    }
  }

  describe("deleteAllByOwner()") {
    it("all lists deleted") {
      val mockAssociationServiceResponse = ServiceResponse(Pair("Lorem Ipsum", 999), message = irrelevantMessage)
      every { mockLrmListRepository.findByOwner(ofType(String::class)) } returns listOf(lrmListWithItems())
      every { mockAssociationService.deleteByListOwnerAndListId(listId = ofType(UUID::class), listOwner = ofType(String::class)) } returns mockAssociationServiceResponse
      every { mockLrmListRepository.deleteById(ids = any()) } returns 999
      val serviceResponse = lrmListService.deleteByOwner(mockUserName)
      serviceResponse.content.listNames.size.shouldBe(1)
      serviceResponse.content.associatedItemNames.size.shouldBe(1)
      serviceResponse.message shouldBe "Deleted all (1) of your lists, and disassociated 1 items."
      verify { mockLrmListRepository.findByOwner(ofType(String::class)) }
      verify { mockAssociationService.deleteByListOwnerAndListId(listId = ofType(UUID::class), listOwner = ofType(String::class)) }
      verify { mockLrmListRepository.deleteById(ids = any()) }
    }

    it("no lists deleted (none present)") {
      every { mockLrmListRepository.findByOwner(ofType(String::class)) } returns emptyList()
      every { mockLrmListRepository.deleteById(ids = any()) } returns 0
      val serviceResponse = lrmListService.deleteByOwner(mockUserName)
      serviceResponse.content.listNames.size.shouldBe(0)
      serviceResponse.content.associatedItemNames.size.shouldBe(0)
      serviceResponse.message shouldBe "Deleted all (0) of your lists, and disassociated 0 items."
      verify { mockLrmListRepository.findByOwner(ofType(String::class)) }
      verify(exactly = 0) { mockAssociationService.deleteByListOwnerAndListId(listId = ofType(UUID::class), listOwner = ofType(String::class)) }
      verify(exactly = 0) { mockLrmListRepository.deleteById(ids = any()) }
    }

    it("no lists deleted (api exception)") {
      every { mockLrmListRepository.findByOwner(ofType(String::class)) } throws (Exception("Lorem Ipsum"))
      val apiException = shouldThrow<DomainException> { lrmListService.deleteByOwner(mockUserName) }
      apiException.message.shouldContain("No lists were deleted")
      apiException.message.shouldContain("could not be retrieved")
      verify { mockLrmListRepository.findByOwner(ofType(String::class)) }
      verify(exactly = 0) { mockAssociationService.deleteByListOwnerAndListId(listId = ofType(UUID::class), listOwner = ofType(String::class)) }
      verify(exactly = 0) { mockLrmListRepository.deleteById(ids = any()) }
    }
  }

  describe("deleteByIdAndOwner()") {
    it("list not found") {
      every { mockLrmListRepository.findByOwnerAndIdOrNull(id = ofType(UUID::class), owner = ofType(String::class)) } returns null
      val exception = shouldThrow<DomainException> { lrmListService.deleteByOwnerAndId(id = id1, owner = "lorem list", removeItemAssociations = false) }
      exception.cause.shouldBeInstanceOf<ListNotFoundException>()
      exception.responseMessage.shouldBe("List could not be deleted: List could not be found.")
      verify { mockLrmListRepository.findByOwnerAndIdOrNull(id = ofType(UUID::class), owner = ofType(String::class)) }
    }

    describe("associated items") {
      it("list is deleted (removeItemAssociations = true)") {
        val mockAssociationServiceResponse = ServiceResponse(content = Pair(lrmList().name, 999), message = irrelevantMessage)
        every { mockLrmListRepository.findByOwnerAndIdOrNull(id = id1, owner = ofType(String::class)) } returns lrmListWithItems()
        every { mockAssociationService.deleteByListOwnerAndListId(listId = ofType(UUID::class), listOwner = ofType(String::class)) } returns mockAssociationServiceResponse
        every { mockLrmListRepository.deleteByOwnerAndId(id = id1, owner = ofType(String::class)) } returns 1
        val serviceResponse = lrmListService.deleteByOwnerAndId(id = id1, owner = "lorem ipsum", removeItemAssociations = true)
        serviceResponse.content.listNames shouldBe listOf(lrmList().name)
        serviceResponse.content.associatedItemNames shouldBe lrmListWithItems().items.map { it.name }.toList()
        serviceResponse.message shouldBe "Deleted list 'Lorem List Name', and disassociated 1 item."
        verify { mockLrmListRepository.findByOwnerAndIdOrNull(id = ofType(UUID::class), owner = ofType(String::class)) }
        verify { mockAssociationService.deleteByListOwnerAndListId(listId = ofType(UUID::class), listOwner = ofType(String::class)) }
        verify { mockLrmListRepository.deleteByOwnerAndId(id = id1, owner = ofType(String::class)) }
      }

      it("list is not deleted (removeItemAssociations = false)") {
        every { mockLrmListRepository.findByOwnerAndIdOrNull(id = id1, owner = ofType(String::class)) } returns lrmList()
        every { mockLrmListRepository.findByOwnerAndIdOrNull(id = id1, owner = ofType(String::class)) } returns lrmListWithItems()
        val exception = shouldThrow<DomainException> { lrmListService.deleteByOwnerAndId(id = id1, owner = "lorem list", removeItemAssociations = false) }
        exception.supplemental.shouldNotBeNull().size.shouldBe(2)
        exception.supplemental.shouldNotBeNull()["listNames"].shouldBe(listOf(lrmListWithItems().name).toJsonElement())
        exception.supplemental.shouldNotBeNull()["associatedItemNames"].shouldBe(lrmListWithItems().items.map { it.name }.toJsonElement())
        exception.message.shouldContainIgnoringCase(lrmList().name)
        exception.message.shouldContainIgnoringCase("could not be deleted")
        exception.message.shouldContainIgnoringCase("includes")
        exception.message.shouldContainIgnoringCase(lrmList().name)
        exception.message.shouldContainIgnoringCase("could not be deleted")
        exception.message.shouldContainIgnoringCase("includes")
      }

      it("list repository returns > 1 deleted records") {
        val mockAssociationServiceResponse = ServiceResponse(content = Pair("Lorem Ipsum", 999), message = irrelevantMessage)
        every { mockLrmListRepository.findByOwnerAndIdOrNull(id = id1, owner = ofType(String::class)) } returns lrmListWithItems()
        every { mockAssociationService.deleteByListOwnerAndListId(listId = ofType(UUID::class), listOwner = ofType(String::class)) } returns mockAssociationServiceResponse
        every { mockLrmListRepository.deleteByOwnerAndId(id = id1, owner = ofType(String::class)) } returns 2
        val exception = shouldThrow<DomainException> { lrmListService.deleteByOwnerAndId(id = id1, owner = "lorem ipsum", removeItemAssociations = true) }
        exception.cause.shouldBeInstanceOf<DomainException>()
        exception.httpStatus.shouldBe(HttpStatus.INTERNAL_SERVER_ERROR)
        exception.message.shouldContainIgnoringCase("$id1")
        exception.message.shouldContainIgnoringCase("could not be deleted")
        exception.message.shouldContainIgnoringCase("more than one")
        exception.responseMessage.shouldContainIgnoringCase("$id1")
        exception.responseMessage.shouldContainIgnoringCase("could not be deleted")
        exception.responseMessage.shouldContainIgnoringCase("more than one")
      }
    }

    describe("no associated items") {
      it("list is deleted") {
        every { mockLrmListRepository.findByOwnerAndIdOrNull(id = id1, owner = ofType(String::class)) } returns lrmList()
        every { mockLrmListRepository.deleteByOwnerAndId(id = id1, owner = ofType(String::class)) } returns 1
        val serviceResponse = lrmListService.deleteByOwnerAndId(id = id1, owner = "lorem list", removeItemAssociations = false)
        serviceResponse.content shouldBe LrmListDeleted(listNames = listOf(lrmList().name), associatedItemNames = emptyList())
        serviceResponse.message shouldBe "Deleted list '${lrmList().name}', and disassociated 0 items."
        verify { mockLrmListRepository.findByOwnerAndIdOrNull(id = ofType(UUID::class), owner = ofType(String::class)) }
        verify { mockLrmListRepository.deleteByOwnerAndId(id = id1, owner = ofType(String::class)) }
      }

      it("list repository returns > 1 deleted records") {
        val mockAssociationServiceResponse = ServiceResponse(content = Pair("Lorem Ipsum", 999), message = irrelevantMessage)
        every { mockLrmListRepository.findByOwnerAndIdOrNull(id = id1, owner = ofType(String::class)) } returns lrmList()
        every { mockAssociationService.deleteByListOwnerAndListId(listId = id1, listOwner = ofType(String::class)) } returns mockAssociationServiceResponse
        every { mockLrmListRepository.deleteByOwnerAndId(id = id1, owner = ofType(String::class)) } returns 2
        val exception = shouldThrow<DomainException> { lrmListService.deleteByOwnerAndId(id = id1, owner = "lorem list", removeItemAssociations = false) }
        exception.cause.shouldBeInstanceOf<DomainException>()
        exception.httpStatus.shouldBe(HttpStatus.INTERNAL_SERVER_ERROR)
        exception.message.shouldContainIgnoringCase("$id1")
        exception.message.shouldContainIgnoringCase("could not be deleted")
        exception.message.shouldContainIgnoringCase("more than one")
        exception.responseMessage.shouldContainIgnoringCase("$id1")
        exception.responseMessage.shouldContainIgnoringCase("could not be deleted")
        exception.responseMessage.shouldContainIgnoringCase("more than one")
        verify { mockLrmListRepository.findByOwnerAndIdOrNull(id = ofType(UUID::class), owner = ofType(String::class)) }
      }
    }
  }

  describe("findAllByOwnerIncludeItems()") {
    it("lists are returned") {
      every { mockLrmListRepository.findByOwner(ofType(String::class)) } returns listOf(lrmList())
      val serviceResponse = lrmListService.findByOwner(mockUserName)
      serviceResponse.content shouldBe listOf(lrmList())
      serviceResponse.message shouldBe "Retrieved all lists owned by '$mockUserName'"
      verify { mockLrmListRepository.findByOwner(ofType(String::class)) }
    }

    it("list repository throws exception") {
      every { mockLrmListRepository.findByOwner(ofType(String::class)) } throws Exception("Lorem Ipsum")
      val exception = shouldThrow<DomainException> { lrmListService.findByOwner(mockUserName) }
      exception.httpStatus.shouldBeEqual(HttpStatus.INTERNAL_SERVER_ERROR)
      exception.cause.shouldBeInstanceOf<Exception>()
      exception.message.shouldBe("Lists (including associated items) could not be retrieved.")
      exception.responseMessage.shouldBe("Lists (including associated items) could not be retrieved.")
    }
  }

  describe("findByPublic()") {
    it("lists are returned") {
      every { mockLrmListRepository.findByPublic() } returns listOf(lrmList())
      val serviceResponse = lrmListService.findByPublic()
      serviceResponse.content shouldBe listOf(lrmList())
      serviceResponse.message shouldBe "Retrieved 1 public lists."
      verify { mockLrmListRepository.findByPublic() }
    }

    it("list repository throws exception") {
      every { mockLrmListRepository.findByPublic() } throws Exception("Lorem Ipsum")
      val exception = shouldThrow<DomainException> { lrmListService.findByPublic() }
      exception.httpStatus.shouldBeEqual(HttpStatus.INTERNAL_SERVER_ERROR)
      exception.cause.shouldBeInstanceOf<Exception>()
      exception.message.shouldBe("Public lists (including associated items) could not be retrieved.")
      exception.responseMessage.shouldBe("Public lists (including associated items) could not be retrieved.")
    }
  }

  describe("findByIdAndOwnerIncludeItems()") {
    it("list is found and returned") {
      every { mockLrmListRepository.findByOwnerAndIdOrNull(id = id1, owner = ofType(String::class)) } returns lrmList()
      val serviceResponse = lrmListService.findByOwnerAndId(id = id1, owner = "lorem ipsum")
      serviceResponse.content shouldBe lrmList()
      serviceResponse.message shouldBe "Retrieved list '${lrmList().name}'"
      verify { mockLrmListRepository.findByOwnerAndIdOrNull(id = id1, owner = ofType(String::class)) }
    }

    it("list is not found") {
      every { mockLrmListRepository.findByOwnerAndIdOrNull(id = id1, owner = ofType(String::class)) } returns null
      assertThrows<ListNotFoundException> { lrmListService.findByOwnerAndId(id = id1, owner = "lorem ipsum") }
    }

    it("list repository throws exception") {
      every { mockLrmListRepository.findByOwnerAndIdOrNull(id = id1, owner = ofType(String::class)) } throws Exception("Lorem Ipsum")
      val exception = shouldThrow<DomainException> { lrmListService.findByOwnerAndId(id = id1, owner = "lorem ipsum") }
      exception.cause.shouldBeInstanceOf<Exception>()
      exception.httpStatus.shouldBe(HttpStatus.INTERNAL_SERVER_ERROR)
      exception.message.shouldContainIgnoringCase("$id1")
      exception.message.shouldContainIgnoringCase("including associated items")
      exception.message.shouldContainIgnoringCase("could not be retrieved")
      exception.responseMessage.shouldContainIgnoringCase("$id1")
      exception.responseMessage.shouldContainIgnoringCase("including associated items")
      exception.responseMessage.shouldContainIgnoringCase("could not be retrieved")
      exception.title.shouldBeEqual(DomainException::class.java.simpleName)
    }
  }

  describe("findByOwnerWithNoItems()") {
    it("items are returned") {
      every { mockLrmListRepository.findByOwnerAndHavingNoItemAssociations(owner = ofType(String::class)) } returns listOf(lrmList())
      val serviceResponse = lrmListService.findByOwnerAndHavingNoItemAssociations(owner = "lorem ipsum")
      serviceResponse.content shouldBe listOf(lrmList())
      serviceResponse.message shouldBe "Retrieved 1 lists that have no items."
      verify { mockLrmListRepository.findByOwnerAndHavingNoItemAssociations(owner = ofType(String::class)) }
    }

    it("item repository throws exception") {
      every { mockLrmListRepository.findByOwnerAndHavingNoItemAssociations(owner = ofType(String::class)) } throws Exception("Lorem Ipsum")
      val exception = shouldThrow<DomainException> { lrmListService.findByOwnerAndHavingNoItemAssociations(owner = "lorem ipsum") }
      exception.httpStatus.shouldBe(HttpStatus.INTERNAL_SERVER_ERROR)
      exception.message.shouldContainIgnoringCase("could not be retrieved")
    }
  }

  describe("patchFields()") {
    it("more than one list name updated") {
      every { mockLrmListRepository.updateName(ofType(LrmList::class)) } returns 2
      val exception = shouldThrow<DomainException> { lrmListService.patchName(lrmList()) }
      exception.message.shouldContain(lrmList().id.toString())
    }

    it("exactly one list name updated") {
      every { mockLrmListRepository.updateName(ofType(LrmList::class)) } returns 1
      lrmListService.patchName(lrmList())
      verify { mockLrmListRepository.updateName(ofType(LrmList::class)) }
    }

    it("less than one list name updated") {
      every { mockLrmListRepository.updateName(ofType(LrmList::class)) } returns 0
      val exception = shouldThrow<DomainException> { lrmListService.patchName(lrmList()) }
      exception.message.shouldContain("No list affected")
    }

    it("more than one list description updated") {
      every { mockLrmListRepository.updateDescription(ofType(LrmList::class)) } returns 2
      val exception = shouldThrow<DomainException> { lrmListService.patchDescription(lrmList()) }
      exception.message.shouldContain(lrmList().id.toString())
    }

    it("exactly one list description updated") {
      every { mockLrmListRepository.updateDescription(ofType(LrmList::class)) } returns 1
      lrmListService.patchDescription(lrmList())
      verify { mockLrmListRepository.updateDescription(ofType(LrmList::class)) }
    }

    it("less than one list description updated") {
      every { mockLrmListRepository.updateDescription(ofType(LrmList::class)) } returns 0
      val exception = shouldThrow<DomainException> { lrmListService.patchDescription(lrmList()) }
      exception.message.shouldContain("No list affected")
    }

    it("more than one list is public indicator updated") {
      every { mockLrmListRepository.updateIsPublic(ofType(LrmList::class)) } returns 2
      val exception = shouldThrow<DomainException> { lrmListService.patchIsPublic(lrmList()) }
      exception.message.shouldContain(lrmList().id.toString())
    }

    it("exactly one list is public indicator updated") {
      every { mockLrmListRepository.updateIsPublic(ofType(LrmList::class)) } returns 1
      lrmListService.patchIsPublic(lrmList())
      verify { mockLrmListRepository.updateIsPublic(ofType(LrmList::class)) }
    }

    it("less than one list is public indicator updated") {
      every { mockLrmListRepository.updateIsPublic(ofType(LrmList::class)) } returns 0
      val exception = shouldThrow<DomainException> { lrmListService.patchIsPublic(lrmList()) }
      exception.message.shouldContain("No list affected")
    }

    it("update name to all spaces") {
      val patchedLrmList = lrmList().copy(name = "   ")
      shouldThrow<ConstraintViolationException> { lrmListService.patchName(patchedLrmList) }
    }

    it("list repository throws exposed sql exception") {
      every { mockLrmListRepository.updateName(ofType(LrmList::class)) } throws exposedSQLExceptionGeneric()
      val exception = shouldThrow<ExposedSQLException> { lrmListService.patchName(lrmList()) }
      exception.message?.shouldContain("ExposedSQLException")
      verify { mockLrmListRepository.updateName(ofType(LrmList::class)) }
    }
  }
})
