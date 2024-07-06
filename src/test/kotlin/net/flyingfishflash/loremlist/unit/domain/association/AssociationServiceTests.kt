package net.flyingfishflash.loremlist.unit.domain.association

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
import io.mockk.unmockkAll
import io.mockk.verify
import net.flyingfishflash.loremlist.core.exceptions.ApiException
import net.flyingfishflash.loremlist.domain.association.Association
import net.flyingfishflash.loremlist.domain.association.AssociationNotFoundException
import net.flyingfishflash.loremlist.domain.association.AssociationRepository
import net.flyingfishflash.loremlist.domain.association.AssociationService
import net.flyingfishflash.loremlist.domain.lrmitem.ItemNotFoundException
import net.flyingfishflash.loremlist.domain.lrmitem.LrmItem
import net.flyingfishflash.loremlist.domain.lrmitem.LrmItemRepository
import net.flyingfishflash.loremlist.domain.lrmitem.data.LrmItemRequest
import net.flyingfishflash.loremlist.domain.lrmlist.ListNotFoundException
import net.flyingfishflash.loremlist.domain.lrmlist.LrmList
import net.flyingfishflash.loremlist.domain.lrmlist.LrmListRepository
import org.springframework.http.HttpStatus
import java.sql.SQLException
import java.util.UUID

class AssociationServiceTests : DescribeSpec({
  val mockAssociationRepository = mockk<AssociationRepository>()
  val mockLrmItemRepository = mockk<LrmItemRepository>()
  val mockLrmListRepository = mockk<LrmListRepository>()
  val associationService = AssociationService(mockAssociationRepository, mockLrmItemRepository, mockLrmListRepository)

  val lrmItemRequest = LrmItemRequest("Lorem Item Name", "Lorem Item Description")
  val uuid1 = UUID.fromString("00000000-0000-0000-0000-000000000001")
  val uuid2 = UUID.fromString("00000000-0000-0000-0000-000000000002")
  val uuid3 = UUID.fromString("00000000-0000-0000-0000-000000000003")
  fun lrmItem(): LrmItem = LrmItem(uuid = uuid1, name = lrmItemRequest.name, description = lrmItemRequest.description)
  fun lrmList(): LrmList = LrmList(uuid = uuid2, name = "Lorem List Name", description = "Lorem List Description")
  fun association(): Association = Association(uuid = UUID.randomUUID(), itemUuid = uuid1, listUuid = uuid2)

  afterEach { clearAllMocks() }
  afterSpec { unmockkAll() }

  describe("addItemToList()") {
    it("item is added to list") {
      every { mockAssociationRepository.create(itemUuid = uuid1, listUuid = uuid2) } just Runs
      every { mockLrmItemRepository.findByIdOrNull(uuid1) } returns lrmItem()
      every { mockLrmListRepository.findByIdOrNull(uuid2) } returns lrmList()
      val response = associationService.create(itemUuid = uuid1, listUuid = uuid2)
      response.first.shouldBeEqual(lrmItem().name)
      response.second.shouldBeEqual(lrmList().name)
      verify(exactly = 1) { mockAssociationRepository.create(itemUuid = uuid1, listUuid = uuid2) }
      verify(exactly = 1) { mockLrmItemRepository.findByIdOrNull(uuid1) }
      verify(exactly = 1) { mockLrmListRepository.findByIdOrNull(uuid2) }
    }

    it("item not found") {
      every { mockLrmItemRepository.findByIdOrNull(uuid1) } returns null
      val exception = shouldThrow<ApiException> { associationService.create(itemUuid = uuid1, listUuid = uuid2) }
      exception.cause.shouldBeInstanceOf<ItemNotFoundException>()
      exception.httpStatus.shouldBe(ItemNotFoundException.HTTP_STATUS)
      exception.responseMessage.shouldBeEqual("Item id $uuid1 could not be added to list id $uuid2: Item id $uuid1 could not be found.")
      verify(exactly = 0) { mockAssociationRepository.create(uuid1, uuid1) }
      verify(exactly = 1) { mockLrmItemRepository.findByIdOrNull(uuid1) }
      verify(exactly = 0) { mockLrmListRepository.findByIdOrNull(uuid1) }
    }

    it("list not found") {
      every { mockLrmItemRepository.findByIdOrNull(uuid1) } returns lrmItem()
      every { mockLrmListRepository.findByIdOrNull(uuid2) } returns null
      val exception = shouldThrow<ApiException> { associationService.create(itemUuid = uuid1, listUuid = uuid2) }
      exception.cause.shouldBeInstanceOf<ListNotFoundException>()
      exception.httpStatus.shouldBe(ListNotFoundException.HTTP_STATUS)
      exception.responseMessage.shouldBeEqual("Item id $uuid1 could not be added to list id $uuid2: List id $uuid2 could not be found.")
      verify(exactly = 0) { mockAssociationRepository.create(itemUuid = uuid1, listUuid = uuid2) }
      verify(exactly = 1) { mockLrmItemRepository.findByIdOrNull(uuid1) }
      verify(exactly = 1) { mockLrmListRepository.findByIdOrNull(uuid2) }
    }

    it("already added to list (postgresql") {
      every { mockLrmItemRepository.findByIdOrNull(uuid1) } returns lrmItem()
      every { mockLrmListRepository.findByIdOrNull(uuid2) } returns lrmList()
      every {
        mockAssociationRepository.create(
          itemUuid = uuid1,
          listUuid = uuid2,
        )
      } throws SQLException("duplicate key value violates unique constraint")
      val exception = shouldThrow<ApiException> { associationService.create(itemUuid = uuid1, listUuid = uuid2) }
      exception.cause.shouldBeInstanceOf<SQLException>()
      exception.responseMessage.shouldBe("Item id $uuid1 could not be added to list id $uuid2: It's already been added.")
      verify(exactly = 1) { mockLrmItemRepository.findByIdOrNull(uuid1) }
      verify(exactly = 1) { mockLrmListRepository.findByIdOrNull(uuid2) }
      verify(exactly = 1) { mockAssociationRepository.create(itemUuid = uuid1, listUuid = uuid2) }
    }

    it("already added to list (h2)") {
      every { mockLrmItemRepository.findByIdOrNull(uuid1) } returns lrmItem()
      every { mockLrmListRepository.findByIdOrNull(uuid2) } returns lrmList()
      every {
        mockAssociationRepository.create(
          itemUuid = uuid1,
          listUuid = uuid2,
        )
      } throws SQLException("Unique index or primary key violation")
      val exception = shouldThrow<ApiException> { associationService.create(itemUuid = uuid1, listUuid = uuid2) }
      exception.cause.shouldBeInstanceOf<SQLException>()
      exception.responseMessage.shouldBe("Item id $uuid1 could not be added to list id $uuid2: It's already been added.")
      verify(exactly = 1) { mockLrmItemRepository.findByIdOrNull(uuid1) }
      verify(exactly = 1) { mockLrmListRepository.findByIdOrNull(uuid2) }
      verify(exactly = 1) { mockAssociationRepository.create(itemUuid = uuid1, listUuid = uuid2) }
    }

    it("unanticipated sql exception (exception message is null)") {
      every { mockLrmItemRepository.findByIdOrNull(uuid1) } returns lrmItem()
      every { mockLrmListRepository.findByIdOrNull(uuid2) } returns lrmList()
      every { mockAssociationRepository.create(itemUuid = uuid1, listUuid = uuid2) } throws SQLException()
      val exception = shouldThrow<ApiException> { associationService.create(itemUuid = uuid1, listUuid = uuid2) }
      exception.cause.shouldBeInstanceOf<SQLException>()
      exception.responseMessage.shouldBe(
        "Item id $uuid1 could not be added to list id $uuid2: Unanticipated SQL exception.",
      )
      verify(exactly = 1) { mockLrmItemRepository.findByIdOrNull(uuid1) }
      verify(exactly = 1) { mockLrmListRepository.findByIdOrNull(uuid2) }
      verify(exactly = 1) { mockAssociationRepository.create(itemUuid = uuid1, listUuid = uuid2) }
    }

    it("unanticipated sql exception (exception message is not null)") {
      every { mockLrmItemRepository.findByIdOrNull(uuid1) } returns lrmItem()
      every { mockLrmListRepository.findByIdOrNull(uuid2) } returns lrmList()
      every { mockAssociationRepository.create(itemUuid = uuid1, listUuid = uuid2) } throws SQLException("Lorem Ipsum")
      val exception = shouldThrow<ApiException> { associationService.create(itemUuid = uuid1, listUuid = uuid2) }
      exception.cause.shouldBeInstanceOf<SQLException>()
      exception.responseMessage.shouldBe(
        "Item id $uuid1 could not be added to list id $uuid2: Unanticipated SQL exception.",
      )
      verify(exactly = 1) { mockLrmItemRepository.findByIdOrNull(uuid1) }
      verify(exactly = 1) { mockLrmListRepository.findByIdOrNull(uuid2) }
      verify(exactly = 1) { mockAssociationRepository.create(itemUuid = uuid1, listUuid = uuid2) }
    }

    it("unanticipated exception") {
      every { mockLrmItemRepository.findByIdOrNull(uuid1) } returns lrmItem()
      every { mockLrmListRepository.findByIdOrNull(uuid2) } returns lrmList()
      every { mockAssociationRepository.create(itemUuid = uuid1, listUuid = uuid2) } throws Exception("Lorem Ipsum")
      val exception = shouldThrow<ApiException> { associationService.create(itemUuid = uuid1, listUuid = uuid2) }
      exception.cause.shouldBeInstanceOf<Exception>()
      exception.responseMessage.shouldBe("Item id $uuid1 could not be added to list id $uuid2.")
      verify(exactly = 1) { mockLrmItemRepository.findByIdOrNull(uuid1) }
      verify(exactly = 1) { mockLrmListRepository.findByIdOrNull(uuid2) }
      verify(exactly = 1) { mockAssociationRepository.create(itemUuid = uuid1, listUuid = uuid2) }
    }
  }

  describe("countItemToList()") {
    it("count of list associations is returned") {
      every { mockLrmItemRepository.findByIdOrNull(uuid1) } returns lrmItem()
      every { mockAssociationRepository.countItemToList(uuid1) } returns 1
      associationService.countForItemId(uuid1)
      verify(exactly = 1) { mockAssociationRepository.countItemToList(any()) }
    }

    it("item is not found") {
      every { mockLrmItemRepository.findByIdOrNull(uuid1) } returns null
      val exception = shouldThrow<ApiException> { associationService.countForItemId(uuid1) }
      exception.cause.shouldBeInstanceOf<ItemNotFoundException>()
      exception.httpStatus.shouldBe(HttpStatus.NOT_FOUND)
    }

    it("item repository throws exception") {
      every { mockLrmItemRepository.findByIdOrNull(uuid1) } throws RuntimeException("Lorem Ipsum")
      val exception = shouldThrow<ApiException> { associationService.countForItemId(uuid1) }
      exception.cause.shouldBeInstanceOf<RuntimeException>()
      exception.httpStatus.shouldBe(HttpStatus.INTERNAL_SERVER_ERROR)
    }
  }

  describe("countListToItem()") {
    it("count of item associations is returned") {
      every { mockLrmListRepository.findByIdOrNull(uuid1) } returns lrmList()
      every { mockAssociationRepository.countListToItem(uuid1) } returns 1
      associationService.countForListId(uuid1)
      verify(exactly = 1) { mockAssociationRepository.countListToItem(any()) }
    }

    it("list is not found") {
      every { mockLrmListRepository.findByIdOrNull(uuid1) } returns null
      val exception = shouldThrow<ApiException> { associationService.countForListId(uuid1) }
      exception.cause.shouldBeInstanceOf<ListNotFoundException>()
      exception.httpStatus.shouldBe(HttpStatus.NOT_FOUND)
    }

    it("list repository throws exception") {
      every { mockLrmListRepository.findByIdOrNull(uuid1) } throws RuntimeException("Lorem Ipsum")
      val exception = shouldThrow<ApiException> { associationService.countForListId(uuid1) }
      exception.cause.shouldBeInstanceOf<RuntimeException>()
      exception.httpStatus.shouldBe(HttpStatus.INTERNAL_SERVER_ERROR)
    }
  }

  describe("updateItemToList()") {
    it("item is moved from one list to another list") {
      val association = Association(UUID.randomUUID(), itemUuid = uuid1, listUuid = uuid2)
      val updatedAssociation = association.copy(listUuid = uuid3)
      every { mockLrmItemRepository.findByIdOrNull(uuid1) } returns lrmItem()
      every { mockLrmListRepository.findByIdOrNull(uuid2) } returns lrmList()
      every { mockLrmListRepository.findByIdOrNull(uuid3) } returns lrmList()
      every { mockAssociationRepository.findByItemIdAndListIdOrNull(uuid1, uuid2) } returns association
      every { mockAssociationRepository.update(updatedAssociation) } returns 1
      associationService.updateList(uuid1, uuid2, uuid3)
      verify(exactly = 1) { mockLrmItemRepository.findByIdOrNull(any()) }
      verify(exactly = 2) { mockLrmListRepository.findByIdOrNull(any()) }
      verify(exactly = 1) { mockAssociationRepository.findByItemIdAndListIdOrNull(any(), any()) }
      verify(exactly = 1) { mockAssociationRepository.update(any()) }
    }

    it("item is not found") {
      every { mockLrmItemRepository.findByIdOrNull(uuid1) } returns null
      val exception = shouldThrow<ApiException> { associationService.updateList(uuid1, uuid2, uuid3) }
      exception.cause.shouldBeInstanceOf<ItemNotFoundException>()
      verify(exactly = 0) { mockAssociationRepository.update(any()) }
    }

    it("from list is not found") {
      every { mockLrmItemRepository.findByIdOrNull(uuid1) } returns lrmItem()
      every { mockLrmListRepository.findByIdOrNull(uuid2) } returns null
      val exception = shouldThrow<ApiException> { associationService.updateList(uuid1, uuid2, uuid3) }
      exception.cause.shouldBeInstanceOf<ListNotFoundException>()
      verify(exactly = 0) { mockAssociationRepository.update(any()) }
    }

    it("to list is not found") {
      every { mockLrmItemRepository.findByIdOrNull(uuid1) } returns lrmItem()
      every { mockLrmListRepository.findByIdOrNull(uuid2) } returns lrmList()
      every { mockLrmListRepository.findByIdOrNull(uuid3) } returns null
      val exception = shouldThrow<ApiException> { associationService.updateList(uuid1, uuid2, uuid3) }
      exception.cause.shouldBeInstanceOf<ListNotFoundException>()
      verify(exactly = 0) { mockAssociationRepository.update(any()) }
    }

    it("association is not found") {
      every { mockLrmItemRepository.findByIdOrNull(uuid1) } returns lrmItem()
      every { mockLrmListRepository.findByIdOrNull(uuid2) } returns lrmList()
      every { mockLrmListRepository.findByIdOrNull(uuid3) } returns lrmList()
      every { mockAssociationRepository.findByItemIdAndListIdOrNull(uuid1, uuid2) } returns null
      val exception = shouldThrow<ApiException> { associationService.updateList(uuid1, uuid2, uuid3) }
      exception.cause.shouldBeInstanceOf<AssociationNotFoundException>()
      verify(exactly = 0) { mockAssociationRepository.update(any()) }
    }

    it("non-api exception is thrown") {
      every { mockLrmItemRepository.findByIdOrNull(uuid1) } returns lrmItem()
      every { mockLrmListRepository.findByIdOrNull(uuid2) } returns lrmList()
      every { mockLrmListRepository.findByIdOrNull(uuid3) } returns lrmList()
      every { mockAssociationRepository.findByItemIdAndListIdOrNull(uuid1, uuid2) } throws RuntimeException("Lorem Ipsum")
      val exception = shouldThrow<ApiException> { associationService.updateList(uuid1, uuid2, uuid3) }
      exception.cause.shouldBeInstanceOf<RuntimeException>()
      exception.httpStatus.shouldBe(HttpStatus.INTERNAL_SERVER_ERROR)
      exception.responseMessage.shouldBe("Item id $uuid1 was not moved from list id $uuid2 to list id $uuid3.")
      verify(exactly = 1) { mockLrmItemRepository.findByIdOrNull(any()) }
      verify(exactly = 2) { mockLrmListRepository.findByIdOrNull(any()) }
    }
  }

  describe("deleteAllForItem()") {
    it("remove all associations by item") {
      every { mockAssociationRepository.deleteAllOfItem(uuid1) } returns 999
      every { mockLrmItemRepository.findByIdOrNull(uuid1) } returns lrmItem()
      associationService.deleteAllOfItem(uuid1)
      verify(exactly = 1) { mockAssociationRepository.deleteAllOfItem(any()) }
      verify(exactly = 1) { mockLrmItemRepository.findByIdOrNull(any()) }
    }

    it("item not found") {
      every { mockAssociationRepository.deleteAllOfItem(uuid1) } returns 999
      every { mockLrmItemRepository.findByIdOrNull(uuid1) } returns null
      val exception = shouldThrow<ApiException> { associationService.deleteAllOfItem(uuid1) }
      exception.cause.shouldBeInstanceOf<ItemNotFoundException>()
      exception.httpStatus.shouldBe(ItemNotFoundException.HTTP_STATUS)
      exception.responseMessage.shouldContain("Item id $uuid1 could not be found.")
    }

    it("item repository throws exception") {
      every { mockAssociationRepository.deleteAllOfItem(uuid1) } throws RuntimeException()
      val exception = shouldThrow<ApiException> { associationService.deleteAllOfItem(uuid1) }
      exception.cause.shouldBeInstanceOf<RuntimeException>()
      exception.httpStatus.shouldBe(HttpStatus.INTERNAL_SERVER_ERROR)
      exception.responseMessage.shouldBe("Item id $uuid1 could not be removed from any/all lists.")
    }
  }

  describe("deleteAllForList()") {
    it("remove all associations by list") {
      every { mockAssociationRepository.deleteAllOfList(uuid1) } returns 999
      every { mockLrmListRepository.findByIdOrNull(uuid1) } returns lrmList()
      associationService.deleteAllOfList(uuid1)
      verify(exactly = 1) { mockAssociationRepository.deleteAllOfList(any()) }
      verify(exactly = 1) { mockLrmListRepository.findByIdOrNull(any()) }
    }

    it("list not found") {
      every { mockAssociationRepository.deleteAllOfList(uuid1) } returns 999
      every { mockLrmListRepository.findByIdOrNull(uuid1) } returns null
      val exception = shouldThrow<ApiException> { associationService.deleteAllOfList(uuid1) }
      exception.cause.shouldBeInstanceOf<ListNotFoundException>()
      exception.httpStatus.shouldBe(ListNotFoundException.HTTP_STATUS)
      exception.responseMessage.shouldContain("List id $uuid1 could not be found.")
    }

    it("list repository throws exception") {
      every { mockAssociationRepository.deleteAllOfList(uuid1) } throws RuntimeException()
      val exception = shouldThrow<ApiException> { associationService.deleteAllOfList(uuid1) }
      exception.cause.shouldBeInstanceOf<RuntimeException>()
      exception.httpStatus.shouldBe(HttpStatus.INTERNAL_SERVER_ERROR)
      exception.responseMessage.shouldBe("Could not remove any/all items from List id $uuid1.")
    }
  }

  describe("removeFromList()") {
    it("removed from list") {
      every { mockLrmItemRepository.findByIdOrNull(uuid1) } returns lrmItem()
      every { mockLrmListRepository.findByIdOrNull(uuid2) } returns lrmList()
      every { mockAssociationRepository.findByItemIdAndListIdOrNull(uuid1, uuid2) } returns association()
      every { mockAssociationRepository.delete(any()) } returns 1
      associationService.deleteByItemIdAndListId(uuid1, uuid2)
      verify(exactly = 1) { mockAssociationRepository.delete(uuid = any()) }
      verify(exactly = 1) { mockLrmItemRepository.findByIdOrNull(uuid = any()) }
      verify(exactly = 1) { mockLrmListRepository.findByIdOrNull(uuid = any()) }
      verify(exactly = 1) { mockAssociationRepository.findByItemIdAndListIdOrNull(itemUuid = any(), listUuid = any()) }
    }

    it("item not found") {
      every { mockAssociationRepository.delete(uuid1, uuid2) } returns 0
      every { mockLrmItemRepository.findByIdOrNull(any()) } returns null
      val exception = shouldThrow<ApiException> { associationService.deleteByItemIdAndListId(uuid1, uuid2) }
      exception.cause.shouldBeInstanceOf<ItemNotFoundException>()
      verify(exactly = 1) { mockLrmItemRepository.findByIdOrNull(uuid = any()) }
    }

    it("list not found") {
      every { mockLrmItemRepository.findByIdOrNull(uuid1) } returns lrmItem()
      every { mockLrmListRepository.findByIdOrNull(uuid2) } returns null
      val exception = shouldThrow<ApiException> { associationService.deleteByItemIdAndListId(uuid1, uuid2) }
      exception.cause.shouldBeInstanceOf<ListNotFoundException>()
      verify(exactly = 1) { mockLrmItemRepository.findByIdOrNull(uuid = any()) }
      verify(exactly = 1) { mockLrmListRepository.findByIdOrNull(uuid = any()) }
    }

    it("association not found") {
      every { mockLrmItemRepository.findByIdOrNull(uuid1) } returns lrmItem()
      every { mockLrmListRepository.findByIdOrNull(uuid2) } returns lrmList()
      every { mockAssociationRepository.findByItemIdAndListIdOrNull(uuid1, uuid2) } returns null
      val exception = shouldThrow<ApiException> { associationService.deleteByItemIdAndListId(uuid1, uuid2) }
      exception.cause.shouldBeInstanceOf<AssociationNotFoundException>()
      verify(exactly = 1) { mockLrmItemRepository.findByIdOrNull(uuid = any()) }
      verify(exactly = 1) { mockLrmListRepository.findByIdOrNull(uuid = any()) }
      verify(exactly = 1) { mockAssociationRepository.findByItemIdAndListIdOrNull(itemUuid = any(), listUuid = any()) }
    }

    it("association is found but zero records deleted") {
      every { mockLrmItemRepository.findByIdOrNull(uuid1) } returns lrmItem()
      every { mockLrmListRepository.findByIdOrNull(uuid2) } returns lrmList()
      every { mockAssociationRepository.findByItemIdAndListIdOrNull(uuid1, uuid2) } returns association()
      every { mockAssociationRepository.delete(any()) } returns 0
      val exception = shouldThrow<ApiException> { associationService.deleteByItemIdAndListId(uuid1, uuid2) }
      exception.cause.shouldBeNull()
      exception.responseMessage.shouldBe(
        "Item id $uuid1 could not be removed from list id $uuid2: Item, list, and association were found, but 0 records were deleted.",
      )
      verify(exactly = 1) { mockAssociationRepository.delete(uuid = any()) }
      verify(exactly = 1) { mockLrmItemRepository.findByIdOrNull(uuid1) }
      verify(exactly = 1) { mockLrmListRepository.findByIdOrNull(uuid2) }
      verify(exactly = 1) { mockAssociationRepository.findByItemIdAndListIdOrNull(itemUuid = any(), listUuid = any()) }
    }

    it("item is associated with the list multiple times") {
      every { mockLrmItemRepository.findByIdOrNull(uuid1) } returns lrmItem()
      every { mockLrmListRepository.findByIdOrNull(uuid2) } returns lrmList()
      every { mockAssociationRepository.findByItemIdAndListIdOrNull(uuid1, uuid2) } returns association()
      every { mockAssociationRepository.delete(uuid = any()) } returns 2
      val exception = shouldThrow<ApiException> { associationService.deleteByItemIdAndListId(uuid1, uuid2) }
      exception.cause.shouldBeNull()
      verify(exactly = 1) { mockLrmItemRepository.findByIdOrNull(uuid1) }
      verify(exactly = 1) { mockLrmListRepository.findByIdOrNull(uuid2) }
      verify(exactly = 1) { mockAssociationRepository.findByItemIdAndListIdOrNull(itemUuid = any(), listUuid = any()) }
      verify(exactly = 1) { mockAssociationRepository.delete(uuid = any()) }
    }

    it("item repository throws exception") {
      every { mockLrmItemRepository.findByIdOrNull(uuid1) } returns lrmItem()
      every { mockLrmListRepository.findByIdOrNull(uuid2) } returns lrmList()
      every { mockAssociationRepository.findByItemIdAndListIdOrNull(uuid1, uuid2) } returns association()
      every { mockAssociationRepository.delete(uuid1, uuid2) } throws Exception("Lorem Ipsum")
      val exception = shouldThrow<ApiException> { associationService.deleteByItemIdAndListId(uuid1, uuid2) }
      exception.cause.shouldBeInstanceOf<Exception>()
      exception.httpStatus.shouldBe(HttpStatus.INTERNAL_SERVER_ERROR)
      exception.message.shouldBe("Item id $uuid1 could not be removed from list id $uuid2.")
      exception.responseMessage.shouldBe("Item id $uuid1 could not be removed from list id $uuid2.")
      exception.title.shouldBe(ApiException::class.java.simpleName)
    }
  }
})
