package net.flyingfishflash.loremlist.unit.domain.association

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
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
  val itemUuid = UUID.randomUUID()
  val listUuid = UUID.randomUUID()
  fun lrmItem(): LrmItem = LrmItem(id = 0, uuid = itemUuid, name = lrmItemRequest.name, description = lrmItemRequest.description)
  fun lrmList(): LrmList = LrmList(id = 0, uuid = listUuid, name = "Lorem List Name", description = "Lorem List Description")
  fun association(): Association = Association(uuid = UUID.randomUUID(), itemId = 1, listId = 2)

  afterEach { clearAllMocks() }
  afterSpec { unmockkAll() }

  describe("addItemToList()") {
    it("item is added to list") {
      every { mockAssociationRepository.create(itemId = 1, listId = 1) } just Runs
      every { mockLrmItemRepository.findByIdOrNull(1) } returns lrmItem()
      every { mockLrmListRepository.findByIdOrNull(1) } returns lrmList()
      val response = associationService.addItemToList(itemId = 1, listId = 1)
      response.first.shouldBeEqual(lrmItem().name)
      response.second.shouldBeEqual(lrmList().name)
      verify(exactly = 1) { mockAssociationRepository.create(1, 1) }
      verify(exactly = 1) { mockLrmItemRepository.findByIdOrNull(1) }
      verify(exactly = 1) { mockLrmListRepository.findByIdOrNull(1) }
    }

    it("item not found") {
      every { mockLrmItemRepository.findByIdOrNull(1) } returns null
      val exception = shouldThrow<ApiException> { associationService.addItemToList(itemId = 1, listId = 1) }
      exception.cause.shouldBeInstanceOf<ItemNotFoundException>()
      exception.httpStatus.shouldBe(ItemNotFoundException.HTTP_STATUS)
      exception.responseMessage.shouldBeEqual("Item id 1 could not be added to list id 1: Item id 1 could not be found.")
      verify(exactly = 0) { mockAssociationRepository.create(1, 1) }
      verify(exactly = 1) { mockLrmItemRepository.findByIdOrNull(1) }
      verify(exactly = 0) { mockLrmListRepository.findByIdOrNull(1) }
    }

    it("list not found") {
      every { mockLrmItemRepository.findByIdOrNull(1) } returns lrmItem()
      every { mockLrmListRepository.findByIdOrNull(1) } returns null
      val exception = shouldThrow<ApiException> { associationService.addItemToList(itemId = 1, listId = 1) }
      exception.cause.shouldBeInstanceOf<ListNotFoundException>()
      exception.httpStatus.shouldBe(ListNotFoundException.HTTP_STATUS)
      exception.responseMessage.shouldBeEqual("Item id 1 could not be added to list id 1: List id 1 could not be found.")
      verify(exactly = 0) { mockAssociationRepository.create(1, 1) }
      verify(exactly = 1) { mockLrmItemRepository.findByIdOrNull(1) }
      verify(exactly = 1) { mockLrmListRepository.findByIdOrNull(1) }
    }

    it("already added to list (postgresql") {
      every { mockLrmItemRepository.findByIdOrNull(1) } returns lrmItem()
      every { mockLrmListRepository.findByIdOrNull(1) } returns lrmList()
      every {
        mockAssociationRepository.create(
          listId = 1,
          itemId = 1,
        )
      } throws SQLException("duplicate key value violates unique constraint")
      val exception = shouldThrow<ApiException> { associationService.addItemToList(itemId = 1, listId = 1) }
      exception.cause.shouldBeInstanceOf<SQLException>()
      exception.responseMessage.shouldBe("Item id 1 could not be added to list id 1: It's already been added.")
      verify(exactly = 1) { mockLrmItemRepository.findByIdOrNull(1) }
      verify(exactly = 1) { mockLrmListRepository.findByIdOrNull(1) }
      verify(exactly = 1) { mockAssociationRepository.create(1, 1) }
    }

    it("already added to list (h2)") {
      every { mockLrmItemRepository.findByIdOrNull(1) } returns lrmItem()
      every { mockLrmListRepository.findByIdOrNull(1) } returns lrmList()
      every { mockAssociationRepository.create(listId = 1, itemId = 1) } throws SQLException("Unique index or primary key violation")
      val exception = shouldThrow<ApiException> { associationService.addItemToList(itemId = 1, listId = 1) }
      exception.cause.shouldBeInstanceOf<SQLException>()
      exception.responseMessage.shouldBe("Item id 1 could not be added to list id 1: It's already been added.")
      verify(exactly = 1) { mockLrmItemRepository.findByIdOrNull(1) }
      verify(exactly = 1) { mockLrmListRepository.findByIdOrNull(1) }
      verify(exactly = 1) { mockAssociationRepository.create(1, 1) }
    }

    it("unanticipated sql exception (exception message is null)") {
      every { mockLrmItemRepository.findByIdOrNull(1) } returns lrmItem()
      every { mockLrmListRepository.findByIdOrNull(1) } returns lrmList()
      every { mockAssociationRepository.create(listId = 1, itemId = 1) } throws SQLException()
      val exception = shouldThrow<ApiException> { associationService.addItemToList(itemId = 1, listId = 1) }
      exception.cause.shouldBeInstanceOf<SQLException>()
      exception.responseMessage.shouldBe(
        "Item id 1 could not be added to list id 1: Unanticipated SQL exception.",
      )
      verify(exactly = 1) { mockLrmItemRepository.findByIdOrNull(1) }
      verify(exactly = 1) { mockLrmListRepository.findByIdOrNull(1) }
      verify(exactly = 1) { mockAssociationRepository.create(1, 1) }
    }

    it("unanticipated sql exception (exception message is not null)") {
      every { mockLrmItemRepository.findByIdOrNull(1) } returns lrmItem()
      every { mockLrmListRepository.findByIdOrNull(1) } returns lrmList()
      every { mockAssociationRepository.create(listId = 1, itemId = 1) } throws SQLException("Lorem Ipsum")
      val exception = shouldThrow<ApiException> { associationService.addItemToList(itemId = 1, listId = 1) }
      exception.cause.shouldBeInstanceOf<SQLException>()
      exception.responseMessage.shouldBe(
        "Item id 1 could not be added to list id 1: Unanticipated SQL exception.",
      )
      verify(exactly = 1) { mockLrmItemRepository.findByIdOrNull(1) }
      verify(exactly = 1) { mockLrmListRepository.findByIdOrNull(1) }
      verify(exactly = 1) { mockAssociationRepository.create(1, 1) }
    }

    it("unanticipated exception") {
      every { mockLrmItemRepository.findByIdOrNull(1) } returns lrmItem()
      every { mockLrmListRepository.findByIdOrNull(1) } returns lrmList()
      every { mockAssociationRepository.create(listId = 1, itemId = 1) } throws Exception("Lorem Ipsum")
      val exception = shouldThrow<ApiException> { associationService.addItemToList(itemId = 1, listId = 1) }
      exception.cause.shouldBeInstanceOf<Exception>()
      exception.responseMessage.shouldBe("Item id 1 could not be added to list id 1.")
      verify(exactly = 1) { mockLrmItemRepository.findByIdOrNull(1) }
      verify(exactly = 1) { mockLrmListRepository.findByIdOrNull(1) }
      verify(exactly = 1) { mockAssociationRepository.create(1, 1) }
    }
  }

  describe("countItemToList()") {
    it("count of list associations is returned") {
      every { mockLrmItemRepository.findByIdOrNull(1) } returns lrmItem()
      every { mockAssociationRepository.countItemToList(1) } returns 1
      associationService.countItemToList(1)
      verify(exactly = 1) { mockAssociationRepository.countItemToList(any()) }
    }

    it("item is not found") {
      every { mockLrmItemRepository.findByIdOrNull(1) } returns null
      val exception = shouldThrow<ApiException> { associationService.countItemToList(1) }
      exception.cause.shouldBeInstanceOf<ItemNotFoundException>()
      exception.httpStatus.shouldBe(HttpStatus.NOT_FOUND)
    }

    it("item repository throws exception") {
      every { mockLrmItemRepository.findByIdOrNull(1) } throws RuntimeException("Lorem Ipsum")
      val exception = shouldThrow<ApiException> { associationService.countItemToList(1) }
      exception.cause.shouldBeInstanceOf<RuntimeException>()
      exception.httpStatus.shouldBe(HttpStatus.INTERNAL_SERVER_ERROR)
    }
  }

  describe("countListToItem()") {
    it("count of item associations is returned") {
      every { mockLrmListRepository.findByIdOrNull(1) } returns lrmList()
      every { mockAssociationRepository.countListToItem(1) } returns 1
      associationService.countListToItem(1)
      verify(exactly = 1) { mockAssociationRepository.countListToItem(any()) }
    }

    it("list is not found") {
      every { mockLrmListRepository.findByIdOrNull(1) } returns null
      val exception = shouldThrow<ApiException> { associationService.countListToItem(1) }
      exception.cause.shouldBeInstanceOf<ListNotFoundException>()
      exception.httpStatus.shouldBe(HttpStatus.NOT_FOUND)
    }

    it("list repository throws exception") {
      every { mockLrmListRepository.findByIdOrNull(1) } throws RuntimeException("Lorem Ipsum")
      val exception = shouldThrow<ApiException> { associationService.countListToItem(1) }
      exception.cause.shouldBeInstanceOf<RuntimeException>()
      exception.httpStatus.shouldBe(HttpStatus.INTERNAL_SERVER_ERROR)
    }
  }

  describe("updateItemToList()") {
    it("item is moved from one list to another list") {
      val association = Association(UUID.randomUUID(), itemId = 1, listId = 2)
      val updatedAssociation = association.copy(listId = 3)
      every { mockLrmItemRepository.findByIdOrNull(1) } returns lrmItem()
      every { mockLrmListRepository.findByIdOrNull(2) } returns lrmList()
      every { mockLrmListRepository.findByIdOrNull(3) } returns lrmList()
      every { mockAssociationRepository.findByItemIdAndListIdOrNull(1, 2) } returns association
      every { mockAssociationRepository.update(updatedAssociation) } returns 1
      associationService.updateItemToList(1, 2, 3)
      verify(exactly = 1) { mockLrmItemRepository.findByIdOrNull(any()) }
      verify(exactly = 2) { mockLrmListRepository.findByIdOrNull(any()) }
      verify(exactly = 1) { mockAssociationRepository.findByItemIdAndListIdOrNull(any(), any()) }
      verify(exactly = 1) { mockAssociationRepository.update(any()) }
    }

    it("item is not found") {
      every { mockLrmItemRepository.findByIdOrNull(1) } returns null
      val exception = shouldThrow<ApiException> { associationService.updateItemToList(1, 2, 3) }
      exception.cause.shouldBeInstanceOf<ItemNotFoundException>()
      verify(exactly = 0) { mockAssociationRepository.update(any()) }
    }

    it("from list is not found") {
      every { mockLrmItemRepository.findByIdOrNull(1) } returns lrmItem()
      every { mockLrmListRepository.findByIdOrNull(2) } returns null
      val exception = shouldThrow<ApiException> { associationService.updateItemToList(1, 2, 3) }
      exception.cause.shouldBeInstanceOf<ListNotFoundException>()
      verify(exactly = 0) { mockAssociationRepository.update(any()) }
    }

    it("to list is not found") {
      every { mockLrmItemRepository.findByIdOrNull(1) } returns lrmItem()
      every { mockLrmListRepository.findByIdOrNull(2) } returns lrmList()
      every { mockLrmListRepository.findByIdOrNull(3) } returns null
      val exception = shouldThrow<ApiException> { associationService.updateItemToList(1, 2, 3) }
      exception.cause.shouldBeInstanceOf<ListNotFoundException>()
      verify(exactly = 0) { mockAssociationRepository.update(any()) }
    }

    it("association is not found") {
      every { mockLrmItemRepository.findByIdOrNull(1) } returns lrmItem()
      every { mockLrmListRepository.findByIdOrNull(2) } returns lrmList()
      every { mockLrmListRepository.findByIdOrNull(3) } returns lrmList()
      every { mockAssociationRepository.findByItemIdAndListIdOrNull(1, 2) } returns null
      val exception = shouldThrow<ApiException> { associationService.updateItemToList(1, 2, 3) }
      exception.cause.shouldBeInstanceOf<AssociationNotFoundException>()
      verify(exactly = 0) { mockAssociationRepository.update(any()) }
    }

    it("non-api exception is thrown") {
      every { mockLrmItemRepository.findByIdOrNull(1) } returns lrmItem()
      every { mockLrmListRepository.findByIdOrNull(2) } returns lrmList()
      every { mockLrmListRepository.findByIdOrNull(3) } returns lrmList()
      every { mockAssociationRepository.findByItemIdAndListIdOrNull(1, 2) } throws RuntimeException("Lorem Ipsum")
      val exception = shouldThrow<ApiException> { associationService.updateItemToList(1, 2, 3) }
      exception.cause.shouldBeInstanceOf<RuntimeException>()
      exception.httpStatus.shouldBe(HttpStatus.INTERNAL_SERVER_ERROR)
      exception.responseMessage.shouldBe("Item id 1 was not moved from list id 2 to list id 3.")
      verify(exactly = 1) { mockLrmItemRepository.findByIdOrNull(any()) }
      verify(exactly = 2) { mockLrmListRepository.findByIdOrNull(any()) }
    }
  }

  describe("removeFromAllLists()") {
    it("remove from all lists") {
      every { mockAssociationRepository.deleteAllItemToListForItem(1) } returns 999
      every { mockLrmItemRepository.findByIdOrNull(1) } returns lrmItem()
      associationService.deleteAllItemToListForItem(1)
      verify(exactly = 1) { mockAssociationRepository.deleteAllItemToListForItem(any()) }
      verify(exactly = 1) { mockLrmItemRepository.findByIdOrNull(any()) }
    }

    it("item not found") {
      every { mockAssociationRepository.deleteAllItemToListForItem(1) } returns 999
      every { mockLrmItemRepository.findByIdOrNull(1) } returns null
      val exception = shouldThrow<ApiException> { associationService.deleteAllItemToListForItem(1) }
      exception.cause.shouldBeInstanceOf<ItemNotFoundException>()
      exception.httpStatus.shouldBe(ItemNotFoundException.HTTP_STATUS)
      exception.responseMessage.shouldBe("Item id 1 could not be removed from any/all lists: Item id 1 could not be found.")
    }

    it("item repository throws exception") {
      every { mockAssociationRepository.deleteAllItemToListForItem(1) } throws RuntimeException()
      val exception = shouldThrow<ApiException> { associationService.deleteAllItemToListForItem(1) }
      exception.cause.shouldBeInstanceOf<RuntimeException>()
      exception.httpStatus.shouldBe(HttpStatus.INTERNAL_SERVER_ERROR)
      exception.responseMessage.shouldBe("Item id 1 could not be removed from any/all lists.")
    }
  }

  describe("removeFromList()") {
    it("removed from list") {
      every { mockLrmItemRepository.findByIdOrNull(1) } returns lrmItem()
      every { mockLrmListRepository.findByIdOrNull(2) } returns lrmList()
      every { mockAssociationRepository.findByItemIdAndListIdOrNull(1, 2) } returns association()
      every { mockAssociationRepository.delete(any()) } returns 1
      associationService.deleteItemToList(1, 2)
      verify(exactly = 1) { mockAssociationRepository.delete(uuid = any()) }
      verify(exactly = 1) { mockLrmItemRepository.findByIdOrNull(id = any()) }
      verify(exactly = 1) { mockLrmListRepository.findByIdOrNull(id = any()) }
      verify(exactly = 1) { mockAssociationRepository.findByItemIdAndListIdOrNull(itemId = any(), listId = any()) }
    }

    it("item not found") {
      every { mockAssociationRepository.delete(1, 2) } returns 0
      every { mockLrmItemRepository.findByIdOrNull(any()) } returns null
      val exception = shouldThrow<ApiException> { associationService.deleteItemToList(1, 2) }
      exception.cause.shouldBeInstanceOf<ItemNotFoundException>()
      verify(exactly = 1) { mockLrmItemRepository.findByIdOrNull(id = any()) }
    }

    it("list not found") {
      every { mockLrmItemRepository.findByIdOrNull(1) } returns lrmItem()
      every { mockLrmListRepository.findByIdOrNull(2) } returns null
      val exception = shouldThrow<ApiException> { associationService.deleteItemToList(1, 2) }
      exception.cause.shouldBeInstanceOf<ListNotFoundException>()
      verify(exactly = 1) { mockLrmItemRepository.findByIdOrNull(id = any()) }
      verify(exactly = 1) { mockLrmListRepository.findByIdOrNull(id = any()) }
    }

    it("association not found") {
      every { mockLrmItemRepository.findByIdOrNull(1) } returns lrmItem()
      every { mockLrmListRepository.findByIdOrNull(2) } returns lrmList()
      every { mockAssociationRepository.findByItemIdAndListIdOrNull(1, 2) } returns null
      val exception = shouldThrow<ApiException> { associationService.deleteItemToList(1, 2) }
      exception.cause.shouldBeInstanceOf<AssociationNotFoundException>()
      verify(exactly = 1) { mockLrmItemRepository.findByIdOrNull(id = any()) }
      verify(exactly = 1) { mockLrmListRepository.findByIdOrNull(id = any()) }
      verify(exactly = 1) { mockAssociationRepository.findByItemIdAndListIdOrNull(itemId = any(), listId = any()) }
    }

    it("association is found but zero records deleted") {
      every { mockLrmItemRepository.findByIdOrNull(1) } returns lrmItem()
      every { mockLrmListRepository.findByIdOrNull(2) } returns lrmList()
      every { mockAssociationRepository.findByItemIdAndListIdOrNull(1, 2) } returns association()
      every { mockAssociationRepository.delete(any()) } returns 0
      val exception = shouldThrow<ApiException> { associationService.deleteItemToList(1, 2) }
      exception.cause.shouldBeNull()
      exception.responseMessage.shouldBe(
        "Item id 1 could not be removed from list id 2: Item, list, and association were found, but 0 records were deleted.",
      )
      verify(exactly = 1) { mockAssociationRepository.delete(uuid = any()) }
      verify(exactly = 1) { mockLrmItemRepository.findByIdOrNull(1) }
      verify(exactly = 1) { mockLrmListRepository.findByIdOrNull(2) }
      verify(exactly = 1) { mockAssociationRepository.findByItemIdAndListIdOrNull(itemId = any(), listId = any()) }
    }

    it("item is associated with the list multiple times") {
      every { mockLrmItemRepository.findByIdOrNull(1) } returns lrmItem()
      every { mockLrmListRepository.findByIdOrNull(2) } returns lrmList()
      every { mockAssociationRepository.findByItemIdAndListIdOrNull(1, 2) } returns association()
      every { mockAssociationRepository.delete(uuid = any()) } returns 2
      val exception = shouldThrow<ApiException> { associationService.deleteItemToList(1, 2) }
      exception.cause.shouldBeNull()
      verify(exactly = 1) { mockLrmItemRepository.findByIdOrNull(1) }
      verify(exactly = 1) { mockLrmListRepository.findByIdOrNull(2) }
      verify(exactly = 1) { mockAssociationRepository.findByItemIdAndListIdOrNull(itemId = any(), listId = any()) }
      verify(exactly = 1) { mockAssociationRepository.delete(uuid = any()) }
    }

    it("item repository throws exception") {
      every { mockLrmItemRepository.findByIdOrNull(1) } returns lrmItem()
      every { mockLrmListRepository.findByIdOrNull(2) } returns lrmList()
      every { mockAssociationRepository.findByItemIdAndListIdOrNull(1, 2) } returns association()
      every { mockAssociationRepository.delete(1, 2) } throws Exception("Lorem Ipsum")
      val exception = shouldThrow<ApiException> { associationService.deleteItemToList(1, 2) }
      exception.cause.shouldBeInstanceOf<Exception>()
      exception.httpStatus.shouldBe(HttpStatus.INTERNAL_SERVER_ERROR)
      exception.message.shouldBe("Item id 1 could not be removed from list id 2.")
      exception.responseMessage.shouldBe("Item id 1 could not be removed from list id 2.")
      exception.title.shouldBe(ApiException::class.java.simpleName)
    }
  }
})
