package net.flyingfishflash.loremlist.unit.domain.lrmitem

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
import kotlinx.datetime.Clock.System.now
import net.flyingfishflash.loremlist.domain.ServiceResponse
import net.flyingfishflash.loremlist.domain.exceptions.DomainException
import net.flyingfishflash.loremlist.domain.lrmitem.ItemNotFoundException
import net.flyingfishflash.loremlist.domain.lrmitem.LrmItem
import net.flyingfishflash.loremlist.domain.lrmitem.LrmItemRepository
import net.flyingfishflash.loremlist.domain.lrmitem.LrmItemServiceDefault
import net.flyingfishflash.loremlist.domain.lrmitem.data.LrmItemCreate
import net.flyingfishflash.loremlist.domain.lrmlist.ListNotFoundException
import net.flyingfishflash.loremlist.domain.lrmlist.LrmList
import net.flyingfishflash.loremlist.domain.lrmlist.LrmListService
import net.flyingfishflash.loremlist.domain.lrmlist.LrmListSuccinct
import net.flyingfishflash.loremlist.domain.lrmlistitem.LrmListItemService
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.statements.StatementContext
import org.springframework.http.HttpStatus
import java.sql.SQLException
import java.util.UUID

class LrmItemServiceTests :
  DescribeSpec({
    val mockLrmItemRepository = mockk<LrmItemRepository>()
    val mockLrmListItemService = mockk<LrmListItemService>()
    val mockLrmListService = mockk<LrmListService>()
    val lrmItemService = LrmItemServiceDefault(mockLrmItemRepository, mockLrmListService, mockLrmListItemService)

    val now = now()
    val id0 = UUID.fromString("00000000-0000-4000-a000-000000000000")
    val id1 = UUID.fromString("00000000-0000-4000-a000-000000000001")
    val lrmItemCreate = LrmItemCreate(name = "Lorem Item Name", description = "Lorem Item Description", isSuppressed = false)
    val irrelevantMessage = "ksADs8y96KRa1Zo4ipMdr5t8faudmFj4c564S02MjsNG6TXEO7yctC08Bb53bCB7"

    fun lrmItem(): LrmItem = LrmItem(
      id = id0,
      name = lrmItemCreate.name,
      description = lrmItemCreate.description,
      owner = "Lorem Ipsum Owner",
      created = now,
      creator = "Lorem Ipsum Created By",
      updated = now,
      updater = "Lorem Ipsum Updated By",
    )

    fun lrmList(): LrmList = LrmList(
      id = id1,
      name = "Lorem List Name",
      description = "Lorem List Description",
      public = false,
      owner = "Lorem Ipsum Owner",
      created = now,
      creator = "Lorem Ipsum Created By",
      updated = now,
      updater = "Lorem Ipsum Updated By",
    )

    fun lrmItemWithLists() = lrmItem().copy(
      lists = setOf(LrmListSuccinct(id = id0, name = "Lorem List Name")),
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
        every { mockLrmItemRepository.countByOwner(owner = ofType(String::class)) } returns 999
        lrmItemService.countByOwner(owner = "lorem ipsum") shouldBe ServiceResponse(content = 999L, message = "999 items.")
        verify { mockLrmItemRepository.countByOwner(owner = ofType(String::class)) }
      }

      it("item repository throws exception") {
        every { mockLrmItemRepository.countByOwner(owner = ofType(String::class)) } throws Exception("Lorem Ipsum")
        val exception = shouldThrow<DomainException> { lrmItemService.countByOwner("lorem ipsum") }
        exception.httpStatus.shouldBe(HttpStatus.INTERNAL_SERVER_ERROR)
        verify { mockLrmItemRepository.countByOwner(owner = ofType(String::class)) }
      }
    }

    describe("deleteAllByOwner()") {
      it("all items deleted") {
        // the '999' values in the repository and association service are not used to calculate the number of items deleted
        // or the number of lists an item was associated with
        val mockAssociationServiceResponse = ServiceResponse(content = Pair(first = "item name", second = 999), message = irrelevantMessage)
        every { mockLrmItemRepository.findByOwner(owner = ofType(String::class)) } returns listOf(lrmItemWithLists())
        every { mockLrmListItemService.removeByOwnerAndItemId(itemId = ofType(UUID::class), owner = ofType(String::class)) } returns
          mockAssociationServiceResponse
        every { mockLrmItemRepository.deleteById(ids = any()) } returns 999
        val serviceResponse = lrmItemService.deleteByOwner(owner = "lorem ipsum")
        serviceResponse.content.itemNames.size.shouldBe(1)
        serviceResponse.content.associatedListNames.size.shouldBe(1)
        serviceResponse.message shouldBe "Deleted all (1) of your items, and removed them from 1 lists."
        verify { mockLrmItemRepository.findByOwner(owner = ofType(String::class)) }
        verify { mockLrmListItemService.removeByOwnerAndItemId(itemId = ofType(UUID::class), owner = ofType(String::class)) }
        verify { mockLrmItemRepository.deleteById(ids = any()) }
      }

      it("no lists deleted (none present)") {
        val mockAssociationServiceResponse = ServiceResponse(content = Pair(first = "item name", second = 999), message = irrelevantMessage)
        every { mockLrmItemRepository.findByOwner(owner = ofType(String::class)) } returns emptyList()
        every { mockLrmListItemService.removeByOwnerAndItemId(itemId = ofType(UUID::class), owner = ofType(String::class)) } returns
          mockAssociationServiceResponse
        every { mockLrmItemRepository.deleteById(ids = any()) } returns 999
        val serviceResponse = lrmItemService.deleteByOwner(owner = "lorem ipsum")
        serviceResponse.content.itemNames.size.shouldBe(0)
        serviceResponse.content.associatedListNames.size.shouldBe(0)
        serviceResponse.message shouldBe "Deleted all (0) of your items, and removed them from 0 lists."
        verify { mockLrmItemRepository.findByOwner(owner = ofType(String::class)) }
        verify(exactly = 0) { mockLrmListItemService.removeByOwnerAndItemId(itemId = ofType(UUID::class), owner = ofType(String::class)) }
        verify(exactly = 0) { mockLrmItemRepository.deleteById(ids = any()) }
      }

      it("no lists deleted (api exception)") {
        every { mockLrmItemRepository.findByOwner(owner = ofType(String::class)) } throws (Exception("Lorem Ipsum"))
        val apiException = shouldThrow<DomainException> { lrmItemService.deleteByOwner(owner = "lorem ipsum") }
        apiException.message.shouldContain("No items were deleted")
        apiException.message.shouldContain("Items could not be retrieved")
        verify { mockLrmItemRepository.findByOwner(owner = ofType(String::class)) }
      }
    }

    describe("deleteByIdAndOwner()") {
      it("item not found") {
        every { mockLrmItemRepository.findByOwnerAndIdOrNull(id = id1, owner = ofType(String::class)) } returns null
        val exception = shouldThrow<DomainException> { lrmItemService.deleteByOwnerAndId(id = id1, owner = "lorem ipsum", removeListAssociations = false) }
        exception.cause.shouldBeInstanceOf<ItemNotFoundException>()
        exception.responseMessage.shouldBe("Item id $id1 could not be deleted: Item could not be found.")
        verify { mockLrmItemRepository.findByOwnerAndIdOrNull(id = ofType(UUID::class), owner = ofType(String::class)) }
      }

      describe("associated lists") {
        it("item is deleted (removeListAssociations = true)") {
          val mockAssociationServiceResponse = ServiceResponse(content = Pair(first = lrmItem().name, second = 999), message = irrelevantMessage)
          every { mockLrmItemRepository.findByOwnerAndIdOrNull(id = id1, owner = ofType(String::class)) } returns lrmItemWithLists()
          every { mockLrmListItemService.removeByOwnerAndItemId(itemId = id1, owner = ofType(String::class)) } returns mockAssociationServiceResponse
          every { mockLrmItemRepository.deleteByOwnerAndId(id = id1, owner = ofType(String::class)) } returns 1
          val serviceResponse = lrmItemService.deleteByOwnerAndId(id = id1, owner = "lorem ipsum", removeListAssociations = true)
          serviceResponse.content.itemNames shouldBe listOf(lrmItemWithLists().name)
          serviceResponse.content.associatedListNames shouldBe listOf("Lorem List Name")
          serviceResponse.message shouldBe "Deleted item '${lrmItemWithLists().name}', and removed it from ${lrmItemWithLists().lists.size} list."
          verify { mockLrmItemRepository.findByOwnerAndIdOrNull(id = ofType(UUID::class), owner = ofType(String::class)) }
          verify { mockLrmListItemService.removeByOwnerAndItemId(itemId = ofType(UUID::class), owner = ofType(String::class)) }
          verify { mockLrmItemRepository.deleteByOwnerAndId(id = ofType(UUID::class), owner = ofType(String::class)) }
        }

        it("item is not deleted (removeListAssociations = false)") {
          every { mockLrmItemRepository.findByOwnerAndIdOrNull(id = id1, owner = ofType(String::class)) } returns lrmItem()
          every { mockLrmItemRepository.findByOwnerAndIdOrNull(id = id1, owner = ofType(String::class)) } returns lrmItemWithLists()
          val exception = shouldThrow<DomainException> { lrmItemService.deleteByOwnerAndId(id = id1, owner = "lorem ipsum", removeListAssociations = false) }
          exception.supplemental.shouldNotBeNull().size.shouldBe(2)
          exception.message.shouldContainIgnoringCase(lrmItemWithLists().name)
          exception.message.shouldContainIgnoringCase("could not be deleted")
          exception.message.shouldContainIgnoringCase("is associated with")
          exception.responseMessage.shouldContainIgnoringCase(lrmItemWithLists().name)
          exception.responseMessage.shouldContainIgnoringCase("could not be deleted")
          exception.responseMessage.shouldContainIgnoringCase("is associated with")
        }

        it("item repository returns > 1 deleted records") {
          val mockAssociationServiceResponse = ServiceResponse(content = Pair(first = "item name", second = 999), message = irrelevantMessage)
          every { mockLrmItemRepository.findByOwnerAndIdOrNull(id = id1, owner = ofType(String::class)) } returns lrmItem()
          every { mockLrmListItemService.removeByOwnerAndItemId(itemId = id1, owner = ofType(String::class)) } returns mockAssociationServiceResponse
          every { mockLrmItemRepository.deleteByOwnerAndId(id = id1, owner = ofType(String::class)) } returns 2
          val exception = shouldThrow<DomainException> { lrmItemService.deleteByOwnerAndId(id = id1, owner = "lorem ipsum", removeListAssociations = true) }
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

      describe("no associated lists") {
        it("item is deleted") {
          every { mockLrmItemRepository.findByOwnerAndIdOrNull(id = id1, owner = ofType(String::class)) } returns lrmItem()
          every { mockLrmItemRepository.deleteByOwnerAndId(id = id1, owner = ofType(String::class)) } returns 1
          val serviceResponse = lrmItemService.deleteByOwnerAndId(id = id1, owner = "lorem ipsum", removeListAssociations = false)
          serviceResponse.content.itemNames shouldBe listOf(lrmItem().name)
          serviceResponse.content.associatedListNames shouldBe emptyList()
          serviceResponse.message shouldBe "Deleted item '${lrmItem().name}', and removed it from ${lrmItem().lists.size} lists."
          verify { mockLrmItemRepository.findByOwnerAndIdOrNull(id = ofType(UUID::class), owner = ofType(String::class)) }
          verify { mockLrmItemRepository.deleteByOwnerAndId(id = id1, owner = ofType(String::class)) }
        }

        it("item repository returns > 1 deleted records") {
          val mockAssociationServiceResponse = ServiceResponse(content = Pair(first = "item name", second = 999), message = irrelevantMessage)
          every { mockLrmItemRepository.findByOwnerAndIdOrNull(id = id1, owner = ofType(String::class)) } returns lrmItem()
          every { mockLrmItemRepository.findByOwnerAndIdOrNull(id = id1, owner = ofType(String::class)) } returns lrmItem()
          every { mockLrmListItemService.removeByOwnerAndItemId(itemId = id1, owner = ofType(String::class)) } returns mockAssociationServiceResponse
          every { mockLrmItemRepository.deleteByOwnerAndId(id = id1, owner = ofType(String::class)) } returns 2
          val exception = shouldThrow<DomainException> { lrmItemService.deleteByOwnerAndId(id = id1, owner = "lorem ipsum", removeListAssociations = false) }
          exception.cause.shouldBeInstanceOf<DomainException>()
          exception.httpStatus.shouldBe(HttpStatus.INTERNAL_SERVER_ERROR)
          exception.message.shouldContainIgnoringCase("$id1")
          exception.message.shouldContainIgnoringCase("could not be deleted")
          exception.message.shouldContainIgnoringCase("more than one")
          exception.responseMessage.shouldContainIgnoringCase("$id1")
          exception.responseMessage.shouldContainIgnoringCase("could not be deleted")
          exception.responseMessage.shouldContainIgnoringCase("more than one")
          verify { mockLrmItemRepository.findByOwnerAndIdOrNull(id = ofType(UUID::class), owner = ofType(String::class)) }
        }
      }
    }

    describe("findAllByOwner()") {
      it("all are returned") {
        val owner = "lorem ipsum"
        every { mockLrmItemRepository.findByOwner(owner = ofType(String::class)) } returns listOf(lrmItem())
        val serviceResponse = lrmItemService.findByOwner(owner = owner)
        serviceResponse.content shouldBe listOf(lrmItem())
        serviceResponse.message shouldBe "Retrieved all items owned by $owner."
        verify { mockLrmItemRepository.findByOwner(owner = ofType(String::class)) }
      }

      it("item repository throws exception") {
        every { mockLrmItemRepository.findByOwner(owner = ofType(String::class)) } throws Exception("Lorem Ipsum")
        val exception = shouldThrow<DomainException> { lrmItemService.findByOwner(owner = "lorem ipsum") }
        exception.httpStatus.shouldBeEqual(HttpStatus.INTERNAL_SERVER_ERROR)
        exception.cause.shouldBeInstanceOf<Exception>()
        exception.message.shouldBe("Items could not be retrieved.")
        exception.responseMessage.shouldBe("Items could not be retrieved.")
      }
    }

    describe("findByIdAndOwner") {
      it("item and lists are returned") {
        every { mockLrmItemRepository.findByOwnerAndIdOrNull(id = id1, owner = ofType(String::class)) } returns lrmItemWithLists()
        val serviceResponse = lrmItemService.findByOwnerAndId(id = id1, owner = "lorem ipsum")
        serviceResponse.content shouldBe lrmItemWithLists()
        serviceResponse.message shouldBe "Retrieved item '${lrmItemWithLists().name}'"
        verify { mockLrmItemRepository.findByOwnerAndIdOrNull(id = id1, owner = ofType(String::class)) }
      }

      it("item is not returned") {
        every { mockLrmItemRepository.findByOwnerAndIdOrNull(id = id1, owner = ofType(String::class)) } returns null
        shouldThrow<ItemNotFoundException> { lrmItemService.findByOwnerAndId(id = id1, owner = "lorem ipsum") }
      }

      it("item repository throws exception") {
        every { mockLrmItemRepository.findByOwnerAndIdOrNull(id = id1, owner = ofType(String::class)) } throws Exception("Lorem Ipsum")
        val exception = shouldThrow<DomainException> { lrmItemService.findByOwnerAndId(id = id1, owner = "lorem ipsum") }
        exception.httpStatus.shouldBeEqual(HttpStatus.INTERNAL_SERVER_ERROR)
        exception.cause.shouldBeInstanceOf<Exception>()
        exception.message.shouldContainIgnoringCase("$id1")
        exception.message.shouldContainIgnoringCase("could not be retrieved")
        exception.responseMessage.shouldContainIgnoringCase("$id1")
        exception.responseMessage.shouldContainIgnoringCase("could not be retrieved")
      }
    }

    describe("findByOwnerWithNoLists()") {
      it("items are returned") {
        every { mockLrmItemRepository.findByOwnerAndHavingNoListAssociations(owner = ofType(String::class)) } returns listOf(lrmItem())
        val serviceResponse = lrmItemService.findByOwnerAndHavingNoListAssociations(owner = "lorem ipsum")
        serviceResponse.content shouldBe listOf(lrmItem())
        serviceResponse.message shouldBe "Retrieved 1 items that are not a part of a list."
        verify { mockLrmItemRepository.findByOwnerAndHavingNoListAssociations(owner = ofType(String::class)) }
      }

      it("item repository throws exception") {
        every { mockLrmItemRepository.findByOwnerAndHavingNoListAssociations(owner = ofType(String::class)) } throws Exception("Lorem Ipsum")
        val exception = shouldThrow<DomainException> { lrmItemService.findByOwnerAndHavingNoListAssociations(owner = "lorem ipsum") }
        exception.httpStatus.shouldBe(HttpStatus.INTERNAL_SERVER_ERROR)
        exception.message.shouldContainIgnoringCase("could not be retrieved")
      }
    }

    describe("findByOwnerAndHavingNoListAssociations()") {
      it("eligible items are returned") {
        every { mockLrmListService.findByOwnerAndId(owner = ofType(String::class), id = id1) } returns
          ServiceResponse(content = lrmList(), message = "Lorem Ipsum")
        every { mockLrmItemRepository.findByOwnerAndHavingNoListAssociations(owner = ofType(String::class), listId = id1) } returns listOf(lrmItem())
        val serviceResponse = lrmItemService.findByOwnerAndHavingNoListAssociations(owner = "lorem ipsum", listId = id1)
        serviceResponse.content shouldBe listOf(lrmItem())
        serviceResponse.message shouldContain "Retrieved 1 items eligible to be added to list"
        verify { mockLrmItemRepository.findByOwnerAndHavingNoListAssociations(owner = ofType(String::class), listId = id1) }
      }

      it("list service throws ListNotFound exception") {
        every { mockLrmListService.findByOwnerAndId(owner = ofType(String::class), id = id1) } throws ListNotFoundException()
        val exception = shouldThrow<DomainException> { lrmItemService.findByOwnerAndHavingNoListAssociations(owner = "lorem ipsum", listId = id1) }
        exception.httpStatus.shouldBe(HttpStatus.NOT_FOUND)
        exception.message.shouldContainIgnoringCase("could not be retrieved")
        exception.message.shouldContainIgnoringCase("list could not be found")
      }

      it("list service throws exception") {
        every { mockLrmListService.findByOwnerAndId(owner = ofType(String::class), id = id1) } throws Exception("Lorem Ipsum")
        val exception = shouldThrow<DomainException> { lrmItemService.findByOwnerAndHavingNoListAssociations(owner = "lorem ipsum", listId = id1) }
        exception.httpStatus.shouldBe(HttpStatus.INTERNAL_SERVER_ERROR)
        exception.message.shouldContainIgnoringCase("could not be retrieved")
        exception.message.shouldContainIgnoringCase("lorem ipsum")
      }

      it("item repository throws exception") {
        every { mockLrmItemRepository.findByOwnerAndHavingNoListAssociations(owner = ofType(String::class), listId = id0) } throws Exception("Lorem Ipsum")
        val exception = shouldThrow<DomainException> { lrmItemService.findByOwnerAndHavingNoListAssociations(owner = "lorem ipsum", listId = id0) }
        exception.httpStatus.shouldBe(HttpStatus.INTERNAL_SERVER_ERROR)
        exception.message.shouldContainIgnoringCase("could not be retrieved")
        exception.message.shouldContainIgnoringCase("lorem ipsum")
      }
    }

    describe("patchFields()") {
      it("more than one item name updated") {
        every { mockLrmItemRepository.updateName(ofType(LrmItem::class)) } returns 2
        val exception = shouldThrow<DomainException> { lrmItemService.patchName(lrmItem()) }
        exception.message.shouldContain(lrmItem().id.toString())
      }

      it("exactly one item name updated") {
        every { mockLrmItemRepository.updateName(ofType(LrmItem::class)) } returns 1
        lrmItemService.patchName(lrmItem())
        verify { mockLrmItemRepository.updateName(ofType(LrmItem::class)) }
      }

      it("less than one item name updated") {
        every { mockLrmItemRepository.updateName(ofType(LrmItem::class)) } returns 0
        val exception = shouldThrow<DomainException> { lrmItemService.patchName(lrmItem()) }
        exception.message.shouldContain("No item affected")
      }

      it("more than one item description updated") {
        every { mockLrmItemRepository.updateDescription(ofType(LrmItem::class)) } returns 2
        shouldThrow<DomainException> { lrmItemService.patchDescription(lrmItem()) }
      }

      it("exactly one item description updated") {
        every { mockLrmItemRepository.updateDescription(ofType(LrmItem::class)) } returns 1
        lrmItemService.patchDescription(lrmItem())
        verify { mockLrmItemRepository.updateDescription(ofType(LrmItem::class)) }
      }

      it("less than one item description updated") {
        every { mockLrmItemRepository.updateDescription(ofType(LrmItem::class)) } returns 0
        shouldThrow<DomainException> { lrmItemService.patchDescription(lrmItem()) }
      }

//    it("more than one item quantity updated") {
//      every { mockLrmItemRepository.updateQuantity(ofType(LrmItem::class)) } returns 2
//      shouldThrow<DomainException> { lrmItemService.patchQuantity(lrmItem()) }
//    }

//    it("exactly one item quantity updated") {
//      every { mockLrmItemRepository.updateQuantity(ofType(LrmItem::class)) } returns 1
//      lrmItemService.patchQuantity(lrmItem())
//      verify { mockLrmItemRepository.updateQuantity(ofType(LrmItem::class)) }
//    }

//    it("less than one item quantity updated") {
//      every { mockLrmItemRepository.updateQuantity(ofType(LrmItem::class)) } returns 0
//      shouldThrow<DomainException> { lrmItemService.patchQuantity(lrmItem()) }
//    }

//    it("update quantity to -1") {
//      val patchedLrmItem = lrmItem().copy(quantity = -1)
//      shouldThrow<ConstraintViolationException> { lrmItemService.patchName(patchedLrmItem) }
//    }

      it("item repository throws exposed sql exception") {
        every { mockLrmItemRepository.updateName(ofType(LrmItem::class)) } throws exposedSQLExceptionGeneric()
        val exception = shouldThrow<ExposedSQLException> { lrmItemService.patchName(lrmItem()) }
        exception.message?.shouldContain("ExposedSQLException")
        verify { mockLrmItemRepository.updateName(ofType(LrmItem::class)) }
      }
    }
  })
