package net.flyingfishflash.loremlist.unit.domain.lrmitem

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
import net.flyingfishflash.loremlist.domain.lrmitem.ItemNotFoundException
import net.flyingfishflash.loremlist.domain.lrmitem.LrmItem
import net.flyingfishflash.loremlist.domain.lrmitem.LrmItemRepository
import net.flyingfishflash.loremlist.domain.lrmitem.LrmItemService
import net.flyingfishflash.loremlist.domain.lrmitem.data.LrmItemRequest
import net.flyingfishflash.loremlist.domain.lrmlist.data.LrmListSuccinct
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.statements.StatementContext
import org.springframework.http.HttpStatus
import java.sql.SQLException
import java.util.UUID
import kotlinx.datetime.Clock.System.now

class LrmItemServiceTests : DescribeSpec({
  val mockAssociationService = mockk<AssociationService>()
  val mockLrmItemRepository = mockk<LrmItemRepository>()
  val lrmItemService = LrmItemService(mockAssociationService, mockLrmItemRepository)

  val now = now()
  val id0 = UUID.fromString("00000000-0000-4000-a000-000000000000")
  val id1 = UUID.fromString("00000000-0000-4000-a000-000000000001")
  val lrmItemRequest = LrmItemRequest("Lorem Item Name", "Lorem Item Description")

  fun lrmItem(): LrmItem = LrmItem(
    id = id0,
    name = lrmItemRequest.name,
    description = lrmItemRequest.description,
    quantity = 0,
    created = now,
    createdBy = "Lorem Ipsum Created By",
    updated = now,
    updatedBy = "Lorem Ipsum Updated By",
    lists = null
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
      lrmItemService.countByOwner(owner = "lorem ipsum").shouldBe(999)
      verify(exactly = 1) { mockLrmItemRepository.countByOwner(owner = ofType(String::class)) }
    }

    it("item repository throws exception") {
      every { mockLrmItemRepository.countByOwner(owner = ofType(String::class)) } throws Exception("Lorem Ipsum")
      val exception = shouldThrow<ApiException> { lrmItemService.countByOwner("lorem ipsum") }
      exception.httpStatus.shouldBe(HttpStatus.INTERNAL_SERVER_ERROR)
      verify(exactly = 1) { mockLrmItemRepository.countByOwner(owner = ofType(String::class)) }
    }
  }

  describe("create()") {
    it("item is created") {
      every { mockLrmItemRepository.insert(ofType(LrmItem::class)) } returns id1
      every { mockLrmItemRepository.findByOwnerAndIdOrNull(id = id1, owner = ofType(String::class)) } returns lrmItem()
      lrmItemService.create(lrmItemRequest, owner = "lorem ipsum")
      verify(exactly = 1) { mockLrmItemRepository.insert(lrmItem = ofType(LrmItem::class)) }
      verify(exactly = 1) { mockLrmItemRepository.findByOwnerAndIdOrNull(id = ofType(UUID::class), owner = ofType(String::class)) }
    }

    it("item repository throws exposed sql exception") {
      every { mockLrmItemRepository.insert(ofType(LrmItem::class)) } throws exposedSQLExceptionGeneric()
      val exception = shouldThrow<ApiException> { lrmItemService.create(lrmItemRequest, owner = "lorem ipsum") }
      exception.cause.shouldBeInstanceOf<ExposedSQLException>()
      exception.httpStatus.shouldBe(HttpStatus.INTERNAL_SERVER_ERROR)
      exception.message.shouldNotBeNull().shouldBeEqual("Item could not be created.")
      exception.responseMessage.shouldBeEqual("Item could not be created.")
      exception.title.shouldBeEqual(ApiException::class.java.simpleName)
      verify(exactly = 1) { mockLrmItemRepository.insert(lrmItem = ofType(LrmItem::class)) }
    }
  }

  describe("deleteAllByOwner()") {
    it("all items deleted") {
      every { mockLrmItemRepository.findByOwner(owner = ofType(String::class)) } returns listOf(lrmItemWithLists())
      every {
        mockAssociationService.deleteByItemOwnerAndItemId(itemId = ofType(UUID::class), itemOwner = ofType(String::class))
      } returns Pair(first = "item name", second = 999)
      every { mockLrmItemRepository.deleteById(ids = any()) } returns 999
      val lrmListDeleteResponse = lrmItemService.deleteByOwner(owner = "lorem ipsum")
      lrmListDeleteResponse.itemNames.size.shouldBe(1)
      lrmListDeleteResponse.associatedListNames.size.shouldBe(1)
      verify(exactly = 1) { mockLrmItemRepository.findByOwner(owner = ofType(String::class)) }
      verify(
        exactly = 1,
      ) { mockAssociationService.deleteByItemOwnerAndItemId(itemId = ofType(UUID::class), itemOwner = ofType(String::class)) }
      verify(exactly = 1) { mockLrmItemRepository.deleteById(ids = any()) }
    }

    it("no lists deleted (none present)") {
      every { mockLrmItemRepository.findByOwner(owner = ofType(String::class)) } returns emptyList()
      every {
        mockAssociationService.deleteByItemOwnerAndItemId(itemId = ofType(UUID::class), itemOwner = ofType(String::class))
      } returns Pair(first = "item name", second = 999)
      every { mockLrmItemRepository.deleteById(ids = any()) } returns 999
      val lrmListDeleteResponse = lrmItemService.deleteByOwner(owner = "lorem ipsum")
      lrmListDeleteResponse.itemNames.size.shouldBe(0)
      lrmListDeleteResponse.associatedListNames.size.shouldBe(0)
      verify(exactly = 1) { mockLrmItemRepository.findByOwner(owner = ofType(String::class)) }
      verify(
        exactly = 0,
      ) { mockAssociationService.deleteByItemOwnerAndItemId(itemId = ofType(UUID::class), itemOwner = ofType(String::class)) }
      verify(exactly = 0) { mockLrmItemRepository.deleteById(ids = any()) }
    }

    it("no lists deleted (api exception)") {
      every { mockLrmItemRepository.findByOwner(owner = ofType(String::class)) } throws (Exception("Lorem Ipsum"))
      val apiException = shouldThrow<ApiException> { lrmItemService.deleteByOwner(owner = "lorem ipsum") }
      apiException.message.shouldContain("No items were deleted")
      apiException.message.shouldContain("Items (including associated lists) could not be retrieved")
      verify(exactly = 1) { mockLrmItemRepository.findByOwner(owner = ofType(String::class)) }
    }
  }

  describe("deleteByIdAndOwner()") {
    it("item not found") {
      every { mockLrmItemRepository.findByOwnerAndIdOrNull(id = id1, owner = ofType(String::class)) } returns null
      val exception = shouldThrow<ApiException> {
        lrmItemService.deleteByOwnerAndId(id = id1, owner = "lorem ipsum", removeListAssociations = false)
      }
      exception.cause.shouldBeInstanceOf<ItemNotFoundException>()
      exception.responseMessage.shouldBe("Item id $id1 could not be deleted: Item could not be found.")
      verify(
        exactly = 1,
      ) { mockLrmItemRepository.findByOwnerAndIdOrNull(id = ofType(UUID::class), owner = ofType(String::class)) }
    }

    describe("associated lists") {
      it("item is deleted (removeListAssociations = true)") {
        every { mockLrmItemRepository.findByOwnerAndIdOrNull(id = id1, owner = ofType(String::class)) } returns lrmItem()
        every {
          mockLrmItemRepository.findByOwnerAndIdOrNull(
            id = id1,
            owner = ofType(String::class),
          )
        } returns lrmItemWithLists()
        every {
          mockAssociationService.deleteByItemOwnerAndItemId(itemId = id1, itemOwner = ofType(String::class))
        } returns Pair(lrmItem().name, 999)
        every { mockLrmItemRepository.deleteByOwnerAndId(id = id1, owner = ofType(String::class)) } returns 1
        lrmItemService.deleteByOwnerAndId(id = id1, owner = "lorem ipsum", removeListAssociations = true)
        verify(
          exactly = 1,
        ) { mockLrmItemRepository.findByOwnerAndIdOrNull(id = ofType(UUID::class), owner = ofType(String::class)) }
        verify(
          exactly = 1,
        ) { mockLrmItemRepository.findByOwnerAndIdOrNull(id = ofType(UUID::class), owner = ofType(String::class)) }
        verify(
          exactly = 1,
        ) { mockAssociationService.deleteByItemOwnerAndItemId(itemId = ofType(UUID::class), itemOwner = ofType(String::class)) }
        verify(exactly = 1) { mockLrmItemRepository.deleteByOwnerAndId(id = ofType(UUID::class), owner = ofType(String::class)) }
      }

      it("item is not deleted (removeListAssociations = false)") {
        every { mockLrmItemRepository.findByOwnerAndIdOrNull(id = id1, owner = ofType(String::class)) } returns lrmItem()
        every {
          mockLrmItemRepository.findByOwnerAndIdOrNull(
            id = id1,
            owner = ofType(String::class),
          )
        } returns lrmItemWithLists()
        val exception = shouldThrow<ApiException> {
          lrmItemService.deleteByOwnerAndId(id = id1, owner = "lorem ipsum", removeListAssociations = false)
        }
        exception.supplemental.shouldNotBeNull().size.shouldBe(2)
        exception.message
          .shouldContainIgnoringCase("$id1")
          .shouldContainIgnoringCase("could not be deleted")
          .shouldContainIgnoringCase("is associated with")
        exception.responseMessage
          .shouldContainIgnoringCase("$id1")
          .shouldContainIgnoringCase("could not be deleted")
          .shouldContainIgnoringCase("is associated with")
      }

      it("item repository returns > 1 deleted records") {
        every { mockLrmItemRepository.findByOwnerAndIdOrNull(id = id1, owner = ofType(String::class)) } returns lrmItem()
        every {
          mockLrmItemRepository.findByOwnerAndIdOrNull(
            id = id1,
            owner = ofType(String::class),
          )
        } returns lrmItemWithLists()
        every {
          mockAssociationService.deleteByItemOwnerAndItemId(itemId = id1, itemOwner = ofType(String::class))
        } returns Pair("Lorem Ipsum", 999)
        every { mockLrmItemRepository.deleteByOwnerAndId(id = id1, owner = ofType(String::class)) } returns 2
        val exception = shouldThrow<ApiException> {
          lrmItemService.deleteByOwnerAndId(id = id1, owner = "lorem ipsum", removeListAssociations = true)
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

    describe("no associated lists") {
      it("item is deleted") {
        every { mockLrmItemRepository.findByOwnerAndIdOrNull(id = id1, owner = ofType(String::class)) } returns lrmItem()
        every { mockLrmItemRepository.findByOwnerAndIdOrNull(id = id1, owner = ofType(String::class)) } returns lrmItem()
        every { mockLrmItemRepository.deleteByOwnerAndId(id = id1, owner = ofType(String::class)) } returns 1
        lrmItemService.deleteByOwnerAndId(id = id1, owner = "lorem ipsum", removeListAssociations = false)
        verify(
          exactly = 1,
        ) { mockLrmItemRepository.findByOwnerAndIdOrNull(id = ofType(UUID::class), owner = ofType(String::class)) }
        verify(exactly = 1) { mockLrmItemRepository.deleteByOwnerAndId(id = id1, owner = ofType(String::class)) }
      }

      it("item repository returns > 1 deleted records") {
        every { mockLrmItemRepository.findByOwnerAndIdOrNull(id = id1, owner = ofType(String::class)) } returns lrmItem()
        every { mockLrmItemRepository.findByOwnerAndIdOrNull(id = id1, owner = ofType(String::class)) } returns lrmItem()
        every {
          mockAssociationService.deleteByItemOwnerAndItemId(itemId = id1, itemOwner = ofType(String::class))
        } returns Pair("Lorem Ipsum", 999)
        every { mockLrmItemRepository.deleteByOwnerAndId(id = id1, owner = ofType(String::class)) } returns 2
        val exception = shouldThrow<ApiException> {
          lrmItemService.deleteByOwnerAndId(id = id1, owner = "lorem ipsum", removeListAssociations = false)
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
        verify(exactly = 1) {
          mockLrmItemRepository.findByOwnerAndIdOrNull(id = ofType(UUID::class), owner = ofType(String::class))
        }
      }
    }
  }

  describe("findAllByOwner()") {
    it("all are returned") {
      every { mockLrmItemRepository.findByOwner(owner = ofType(String::class)) } returns listOf(lrmItem())
      lrmItemService.findByOwnerExcludeLists(owner = "lorem ipsum")
      verify(exactly = 1) { mockLrmItemRepository.findByOwner(owner = ofType(String::class)) }
    }

    it("item repository throws exception") {
      every { mockLrmItemRepository.findByOwner(owner = ofType(String::class)) } throws Exception("Lorem Ipsum")
      val exception = shouldThrow<ApiException> { lrmItemService.findByOwnerExcludeLists(owner = "lorem ipsum") }
      exception.httpStatus.shouldBeEqual(HttpStatus.INTERNAL_SERVER_ERROR)
      exception.cause.shouldBeInstanceOf<Exception>()
      exception.message.shouldBe("Items could not be retrieved.")
      exception.responseMessage.shouldBe("Items could not be retrieved.")
    }
  }

  describe("findAllByOwnerIncludeLists()") {
    it("all and lists are returned") {
      every { mockLrmItemRepository.findByOwner(owner = ofType(String::class)) } returns listOf(lrmItem())
      lrmItemService.findByOwner(owner = "lorem ipsum")
      verify(exactly = 1) { mockLrmItemRepository.findByOwner(owner = ofType(String::class)) }
    }

    it("item repository throws exception") {
      every { mockLrmItemRepository.findByOwner(owner = ofType(String::class)) } throws Exception("Lorem Ipsum")
      val exception = shouldThrow<ApiException> { lrmItemService.findByOwner(owner = "lorem ipsum") }
      exception.httpStatus.shouldBeEqual(HttpStatus.INTERNAL_SERVER_ERROR)
      exception.cause.shouldBeInstanceOf<Exception>()
      exception.message.shouldBe("Items (including associated lists) could not be retrieved.")
      exception.responseMessage.shouldBe("Items (including associated lists) could not be retrieved.")
    }
  }

  describe("findByIdAndOwner()") {
    it("item is returned") {
      every { mockLrmItemRepository.findByOwnerAndIdOrNull(id = id1, owner = ofType(String::class)) } returns lrmItem()
      lrmItemService.findByOwnerAndIdExcludeLists(id = id1, owner = "lorem ipsum")
      verify(exactly = 1) { mockLrmItemRepository.findByOwnerAndIdOrNull(id = id1, owner = ofType(String::class)) }
    }

    it("item is not returned") {
      every { mockLrmItemRepository.findByOwnerAndIdOrNull(id = id1, owner = ofType(String::class)) } returns null
      shouldThrow<ItemNotFoundException> { lrmItemService.findByOwnerAndIdExcludeLists(id = id1, owner = "lorem ipsum") }
    }

    it("item repository throws exception") {
      every { mockLrmItemRepository.findByOwnerAndIdOrNull(id = id1, owner = ofType(String::class)) } throws Exception("Lorem Ipsum")
      val exception = shouldThrow<ApiException> { lrmItemService.findByOwnerAndIdExcludeLists(id = id1, owner = "lorem ipsum") }
      exception.httpStatus.shouldBeEqual(HttpStatus.INTERNAL_SERVER_ERROR)
      exception.cause.shouldBeInstanceOf<Exception>()
      exception.message
        .shouldContainIgnoringCase("$id1")
        .shouldContainIgnoringCase("could not be retrieved")
      exception.responseMessage
        .shouldContainIgnoringCase("$id1")
        .shouldContainIgnoringCase("could not be retrieved")
    }
  }

  describe("findByIdAndOwnerIncludeLists()") {
    it("item and lists are returned") {
      every { mockLrmItemRepository.findByOwnerAndIdOrNull(id = id1, owner = ofType(String::class)) } returns lrmItem()
      lrmItemService.findByOwnerAndId(id = id1, owner = "lorem ipsum")
      verify(exactly = 1) { mockLrmItemRepository.findByOwnerAndIdOrNull(id = id1, owner = ofType(String::class)) }
    }

    it("item is not returned") {
      every { mockLrmItemRepository.findByOwnerAndIdOrNull(id = id1, owner = ofType(String::class)) } returns null
      shouldThrow<ItemNotFoundException> { lrmItemService.findByOwnerAndId(id = id1, owner = "lorem ipsum") }
    }

    it("item repository throws exception") {
      every {
        mockLrmItemRepository.findByOwnerAndIdOrNull(
          id = id1,
          owner = ofType(String::class),
        )
      } throws Exception("Lorem Ipsum")
      val exception = shouldThrow<ApiException> { lrmItemService.findByOwnerAndId(id = id1, owner = "lorem ipsum") }
      exception.httpStatus.shouldBeEqual(HttpStatus.INTERNAL_SERVER_ERROR)
      exception.cause.shouldBeInstanceOf<Exception>()
      exception.message
        .shouldContainIgnoringCase("$id1")
        .shouldContainIgnoringCase("including associated lists")
        .shouldContainIgnoringCase("could not be retrieved")
      exception.responseMessage
        .shouldContainIgnoringCase("$id1")
        .shouldContainIgnoringCase("including associated lists")
        .shouldContainIgnoringCase("could not be retrieved")
    }
  }

  describe("findByOwnerWithNoLists()") {
    it("items are returned") {
      every { mockLrmItemRepository.findByOwnerAndHavingNoListAssociations(owner = ofType(String::class)) } returns listOf(lrmItem())
      lrmItemService.findByOwnerAndHavingNoListAssociations(owner = "lorem ipsum")
      verify(exactly = 1) { mockLrmItemRepository.findByOwnerAndHavingNoListAssociations(owner = ofType(String::class)) }
    }

    it("item repository throws exception") {
      every { mockLrmItemRepository.findByOwnerAndHavingNoListAssociations(owner = ofType(String::class)) } throws Exception("Lorem Ipsum")
      val exception = shouldThrow<ApiException> { lrmItemService.findByOwnerAndHavingNoListAssociations(owner = "lorem upsum") }
      exception.httpStatus.shouldBe(HttpStatus.INTERNAL_SERVER_ERROR)
      exception.message.shouldContainIgnoringCase("could not be retrieved")
    }
  }

  describe("patchByIdAndOwner()") {
    it("item is not found") {
      every { mockLrmItemRepository.findByOwnerAndIdOrNull(id = id1, owner = ofType(String::class)) } returns null
      shouldThrow<ItemNotFoundException> {
        lrmItemService.patchByOwnerAndId(
          id = id1,
          owner = "lorem ipsum",
          patchRequest = mapOf("name" to "lorum ipsum"),
        )
      }
    }

    it("update name") {
      val expectedName = "patched lorem item"
      every { mockLrmItemRepository.findByOwnerAndIdOrNull(id = id1, owner = ofType(String::class)) } returns lrmItem()
      every { mockLrmItemRepository.update(ofType(LrmItem::class)) } returns 1
      val patchedLrmItem = lrmItemService.patchByOwnerAndId(
        id = id1,
        owner = "lorem ipsum",
        patchRequest = mapOf("name" to expectedName),
      ).first
      patchedLrmItem.name.shouldBe(expectedName)
      verify(exactly = 2) { mockLrmItemRepository.findByOwnerAndIdOrNull(id = id1, owner = ofType(String::class)) }
      verify(exactly = 1) { mockLrmItemRepository.update(ofType(LrmItem::class)) }
    }

    it("update name, description, quantity to current values") {
      val expectedName = lrmItem().name
      val expectedDescription = lrmItem().description
      val expectedQuantity = lrmItem().quantity
      every { mockLrmItemRepository.findByOwnerAndIdOrNull(id = id1, owner = ofType(String::class)) } returns lrmItem()
      every { mockLrmItemRepository.update(ofType(LrmItem::class)) } returns 1
      val patchResponse = lrmItemService.patchByOwnerAndId(
        id = id1,
        owner = "lorem ipsum",
        patchRequest = mapOf("name" to expectedName, "description" to (expectedDescription ?: ""), "quantity" to expectedQuantity),
      )
      patchResponse.second.shouldBeFalse()
      patchResponse.first.name.shouldBe(expectedName)
      patchResponse.first.description.shouldBe(expectedDescription)
      patchResponse.first.quantity.shouldBe(expectedQuantity)
      verify(exactly = 1) { mockLrmItemRepository.findByOwnerAndIdOrNull(id = id1, owner = ofType(String::class)) }
      verify(exactly = 0) { mockLrmItemRepository.update(ofType(LrmItem::class)) }
    }

    it("update description") {
      val expectedDescription = "patched lorem list description"
      every { mockLrmItemRepository.findByOwnerAndIdOrNull(id = id1, owner = ofType(String::class)) } returns lrmItem()
      every { mockLrmItemRepository.update(ofType(LrmItem::class)) } returns 1
      val patchedLrmList = lrmItemService.patchByOwnerAndId(
        id = id1,
        owner = "lorem ipsum",
        patchRequest = mapOf("description" to expectedDescription),
      ).first
      patchedLrmList.description.shouldBe(expectedDescription)
      verify(exactly = 2) { mockLrmItemRepository.findByOwnerAndIdOrNull(id = id1, owner = ofType(String::class)) }
      verify(exactly = 1) { mockLrmItemRepository.update(ofType(LrmItem::class)) }
    }

    it("update quantity") {
      val expectedQuantity = 999
      every { mockLrmItemRepository.findByOwnerAndIdOrNull(id = id1, owner = ofType(String::class)) } returns lrmItem()
      every { mockLrmItemRepository.update(ofType(LrmItem::class)) } returns 1
      val patchedLrmList = lrmItemService.patchByOwnerAndId(
        id = id1,
        owner = "lorem ipsum",
        patchRequest = mapOf("quantity" to expectedQuantity),
      ).first
      patchedLrmList.quantity.shouldBe(expectedQuantity)
      verify(exactly = 2) { mockLrmItemRepository.findByOwnerAndIdOrNull(id = id1, owner = ofType(String::class)) }
      verify(exactly = 1) { mockLrmItemRepository.update(ofType(LrmItem::class)) }
    }

    it("update description to '  '") {
      every { mockLrmItemRepository.findByOwnerAndIdOrNull(id = id1, owner = ofType(String::class)) } returns lrmItem()
      shouldThrow<ConstraintViolationException> {
        lrmItemService.patchByOwnerAndId(id = id1, owner = "lorem ipsum", patchRequest = mapOf("description" to "  "))
      }
    }

    it("update an undefined list property") {
      every { mockLrmItemRepository.findByOwnerAndIdOrNull(id = id1, owner = ofType(String::class)) } returns lrmItem()
      shouldThrow<IllegalArgumentException> {
        lrmItemService.patchByOwnerAndId(id = id1, owner = "lorem ipsum", patchRequest = mapOf("undefined property" to "irrelevant value"))
      }
    }

    it("update no properties") {
      every { mockLrmItemRepository.findByOwnerAndIdOrNull(id = id1, owner = ofType(String::class)) } returns lrmItem()
      val patchReturn = lrmItemService.patchByOwnerAndId(id = id1, owner = "lorem ipsum", patchRequest = mapOf())
      patchReturn.first.shouldBeEqual(lrmItem())
      patchReturn.second.shouldBeFalse()
      verify(exactly = 1) { mockLrmItemRepository.findByOwnerAndIdOrNull(id = id1, owner = ofType(String::class)) }
      verify(exactly = 0) { mockLrmItemRepository.update(ofType(LrmItem::class)) }
    }

    it("item repository updates more than 1 record") {
      val expectedName = "patched lorem list"
      every { mockLrmItemRepository.findByOwnerAndIdOrNull(id = id1, owner = ofType(String::class)) } returns lrmItem()
      every { mockLrmItemRepository.update(ofType(LrmItem::class)) } returns 2
      val exception = shouldThrow<ApiException> {
        lrmItemService.patchByOwnerAndId(id = id1, owner = "lorem ipsum", patchRequest = mapOf("name" to expectedName)).first
      }
      exception.httpStatus.shouldBe(HttpStatus.INTERNAL_SERVER_ERROR)
      exception.cause.shouldBeNull()
      exception.message?.shouldBeEqual(
        "Item id ${lrmItem().id} could not be updated. " +
          "2 records would have been updated rather than 1.",
      )
      exception.responseMessage.shouldBeEqual(
        "Item id ${lrmItem().id} could not be updated. " +
          "2 records would have been updated rather than 1.",
      )
      verify(exactly = 1) { mockLrmItemRepository.findByOwnerAndIdOrNull(id = id1, owner = ofType(String::class)) }
      verify(exactly = 1) { mockLrmItemRepository.update(ofType(LrmItem::class)) }
    }

    it("item repository throws exposed sql exception") {
      val expectedName = "patched lorem list"
      every { mockLrmItemRepository.findByOwnerAndIdOrNull(id = id1, owner = ofType(String::class)) } returns lrmItem()
      every { mockLrmItemRepository.update(ofType(LrmItem::class)) } throws exposedSQLExceptionGeneric()
      val exception = shouldThrow<ApiException> {
        lrmItemService.patchByOwnerAndId(id = id1, owner = "lorem ipsum", patchRequest = mapOf("name" to expectedName)).first
      }
      exception.httpStatus.shouldBe(HttpStatus.INTERNAL_SERVER_ERROR)
      exception.cause.shouldBeInstanceOf<ExposedSQLException>()
      exception.message?.shouldBeEqual(
        "Item id ${lrmItem().id} could not be updated. " +
          "The item was found and patch request is valid but an exception was thrown by the item repository.",
      )
      exception.responseMessage.shouldBeEqual("Item id ${lrmItem().id} could not be updated.")
      verify(exactly = 1) { mockLrmItemRepository.findByOwnerAndIdOrNull(id = id1, owner = ofType(String::class)) }
      verify(exactly = 1) { mockLrmItemRepository.update(ofType(LrmItem::class)) }
    }
  }
})
