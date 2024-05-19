package net.flyingfishflash.loremlist.unit.domain.common

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
import io.mockk.spyk
import io.mockk.unmockkAll
import io.mockk.verify
import net.flyingfishflash.loremlist.core.exceptions.ApiException
import net.flyingfishflash.loremlist.domain.common.CommonRepository
import net.flyingfishflash.loremlist.domain.common.CommonService
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

class CommonServiceTests : DescribeSpec({
  val mockCommonRepository = mockk<CommonRepository>()
  val mockLrmItemRepository = mockk<LrmItemRepository>()
  val mockLrmListRepository = mockk<LrmListRepository>()
  val commonService = CommonService(mockCommonRepository, mockLrmItemRepository, mockLrmListRepository)

  val lrmItemRequest = LrmItemRequest("Lorem Item Name", "Lorem Item Description")
  val itemUuid = UUID.randomUUID()
  val listUuid = UUID.randomUUID()
  fun lrmItem(): LrmItem = LrmItem(id = 0, uuid = itemUuid, name = lrmItemRequest.name, description = lrmItemRequest.description)
  fun lrmList(): LrmList = LrmList(id = 0, uuid = listUuid, name = "Lorem List Name", description = "Lorem List Description")

  afterEach { clearAllMocks() }
  afterSpec { unmockkAll() }

  describe("addToList()") {
    it("item is added to list") {
      every { mockLrmItemRepository.addItemToList(1, 1) } just Runs
      every { mockLrmItemRepository.findByIdOrNull(1) } returns lrmItem()
      every { mockLrmListRepository.findByIdOrNull(1) } returns lrmList()
      val response = commonService.addToList(itemId = 1, listId = 1)
      response.first.shouldBeEqual(lrmItem().name)
      response.second.shouldBeEqual(lrmList().name)
      verify(exactly = 1) { mockLrmItemRepository.addItemToList(1, 1) }
      verify(exactly = 1) { mockLrmItemRepository.findByIdOrNull(1) }
      verify(exactly = 1) { mockLrmListRepository.findByIdOrNull(1) }
    }

    it("item not found") {
      every { mockLrmItemRepository.findByIdOrNull(1) } returns null
      val exception = shouldThrow<ApiException> { commonService.addToList(itemId = 1, listId = 1) }
      exception.cause.shouldBeInstanceOf<ItemNotFoundException>()
      exception.httpStatus.shouldBe(ItemNotFoundException.HTTP_STATUS)
      exception.responseMessage.shouldBeEqual("Item id 1 could not be added to list id 1: Item id 1 could not be found.")
      verify(exactly = 0) { mockLrmItemRepository.addItemToList(1, 1) }
      verify(exactly = 1) { mockLrmItemRepository.findByIdOrNull(1) }
      verify(exactly = 0) { mockLrmListRepository.findByIdOrNull(1) }
    }

    it("list not found") {
      every { mockLrmItemRepository.findByIdOrNull(1) } returns lrmItem()
      every { mockLrmListRepository.findByIdOrNull(1) } returns null
      val exception = shouldThrow<ApiException> { commonService.addToList(itemId = 1, listId = 1) }
      exception.cause.shouldBeInstanceOf<ListNotFoundException>()
      exception.httpStatus.shouldBe(ListNotFoundException.HTTP_STATUS)
      exception.responseMessage.shouldBeEqual("Item id 1 could not be added to list id 1: List id 1 could not be found.")
      verify(exactly = 0) { mockLrmItemRepository.addItemToList(1, 1) }
      verify(exactly = 1) { mockLrmItemRepository.findByIdOrNull(1) }
      verify(exactly = 1) { mockLrmListRepository.findByIdOrNull(1) }
    }

    it("already added to list (postgresql") {
      every { mockLrmItemRepository.findByIdOrNull(1) } returns lrmItem()
      every { mockLrmListRepository.findByIdOrNull(1) } returns lrmList()
      every { mockLrmItemRepository.addItemToList(1, 1) } throws SQLException("duplicate key value violates unique constraint")
      val exception = shouldThrow<ApiException> { commonService.addToList(itemId = 1, listId = 1) }
      exception.cause.shouldBeInstanceOf<SQLException>()
      exception.responseMessage.shouldBe("Item id 1 could not be added to list id 1: It's already been added.")
      verify(exactly = 1) { mockLrmItemRepository.findByIdOrNull(1) }
      verify(exactly = 1) { mockLrmListRepository.findByIdOrNull(1) }
      verify(exactly = 1) { mockLrmItemRepository.addItemToList(1, 1) }
    }

    it("already added to list (h2)") {
      every { mockLrmItemRepository.findByIdOrNull(1) } returns lrmItem()
      every { mockLrmListRepository.findByIdOrNull(1) } returns lrmList()
      every { mockLrmItemRepository.addItemToList(1, 1) } throws SQLException("Unique index or primary key violation")
      val exception = shouldThrow<ApiException> { commonService.addToList(itemId = 1, listId = 1) }
      exception.cause.shouldBeInstanceOf<SQLException>()
      exception.responseMessage.shouldBe("Item id 1 could not be added to list id 1: It's already been added.")
      verify(exactly = 1) { mockLrmItemRepository.findByIdOrNull(1) }
      verify(exactly = 1) { mockLrmListRepository.findByIdOrNull(1) }
      verify(exactly = 1) { mockLrmItemRepository.addItemToList(1, 1) }
    }

    it("unanticipated sql exception (exception message is null)") {
      every { mockLrmItemRepository.findByIdOrNull(1) } returns lrmItem()
      every { mockLrmListRepository.findByIdOrNull(1) } returns lrmList()
      every { mockLrmItemRepository.addItemToList(1, 1) } throws SQLException()
      val exception = shouldThrow<ApiException> { commonService.addToList(itemId = 1, listId = 1) }
      exception.cause.shouldBeInstanceOf<SQLException>()
      exception.responseMessage.shouldBe(
        "Item id 1 could not be added to list id 1: Unanticipated SQL exception.",
      )
      verify(exactly = 1) { mockLrmItemRepository.findByIdOrNull(1) }
      verify(exactly = 1) { mockLrmListRepository.findByIdOrNull(1) }
      verify(exactly = 1) { mockLrmItemRepository.addItemToList(1, 1) }
    }

    it("unanticipated sql exception (exception message is not null)") {
      every { mockLrmItemRepository.findByIdOrNull(1) } returns lrmItem()
      every { mockLrmListRepository.findByIdOrNull(1) } returns lrmList()
      every { mockLrmItemRepository.addItemToList(1, 1) } throws SQLException("Lorem Ipsum")
      val exception = shouldThrow<ApiException> { commonService.addToList(itemId = 1, listId = 1) }
      exception.cause.shouldBeInstanceOf<SQLException>()
      exception.responseMessage.shouldBe(
        "Item id 1 could not be added to list id 1: Unanticipated SQL exception.",
      )
      verify(exactly = 1) { mockLrmItemRepository.findByIdOrNull(1) }
      verify(exactly = 1) { mockLrmListRepository.findByIdOrNull(1) }
      verify(exactly = 1) { mockLrmItemRepository.addItemToList(1, 1) }
    }

    it("unanticipated exception") {
      every { mockLrmItemRepository.findByIdOrNull(1) } returns lrmItem()
      every { mockLrmListRepository.findByIdOrNull(1) } returns lrmList()
      every { mockLrmItemRepository.addItemToList(1, 1) } throws Exception("Lorem Ipsum")
      val exception = shouldThrow<ApiException> { commonService.addToList(itemId = 1, listId = 1) }
      exception.cause.shouldBeInstanceOf<Exception>()
      exception.responseMessage.shouldBe("Item id 1 could not be added to list id 1.")
      verify(exactly = 1) { mockLrmItemRepository.findByIdOrNull(1) }
      verify(exactly = 1) { mockLrmListRepository.findByIdOrNull(1) }
      verify(exactly = 1) { mockLrmItemRepository.addItemToList(1, 1) }
    }
  }

  describe("countItemToListAssociations()") {
    it("count of list associations is returned") {
      every { mockLrmItemRepository.findByIdOrNull(1) } returns lrmItem()
      every { mockCommonRepository.countItemToListAssociations(1) } returns 1
      commonService.countItemToListAssociations(1)
      verify(exactly = 1) { mockCommonRepository.countItemToListAssociations(any()) }
    }

    it("item is not found") {
      every { mockLrmItemRepository.findByIdOrNull(1) } returns null
      val exception = shouldThrow<ApiException> { commonService.countItemToListAssociations(1) }
      exception.cause.shouldBeInstanceOf<ItemNotFoundException>()
      exception.httpStatus.shouldBe(HttpStatus.NOT_FOUND)
    }

    it("item repository throws exception") {
      every { mockLrmItemRepository.findByIdOrNull(1) } throws RuntimeException("Lorem Ipsum")
      val exception = shouldThrow<ApiException> { commonService.countItemToListAssociations(1) }
      exception.cause.shouldBeInstanceOf<RuntimeException>()
      exception.httpStatus.shouldBe(HttpStatus.INTERNAL_SERVER_ERROR)
    }
  }

  describe("countListToItemAssociations()") {
    it("count of item associations is returned") {
      every { mockLrmListRepository.findByIdOrNull(1) } returns lrmList()
      every { mockCommonRepository.countListToItemAssociations(1) } returns 1
      commonService.countListToItemAssociations(1)
      verify(exactly = 1) { mockCommonRepository.countListToItemAssociations(any()) }
    }

    it("list is not found") {
      every { mockLrmListRepository.findByIdOrNull(1) } returns null
      val exception = shouldThrow<ApiException> { commonService.countListToItemAssociations(1) }
      exception.cause.shouldBeInstanceOf<ListNotFoundException>()
      exception.httpStatus.shouldBe(HttpStatus.NOT_FOUND)
    }

    it("list repository throws exception") {
      every { mockLrmListRepository.findByIdOrNull(1) } throws RuntimeException("Lorem Ipsum")
      val exception = shouldThrow<ApiException> { commonService.countListToItemAssociations(1) }
      exception.cause.shouldBeInstanceOf<RuntimeException>()
      exception.httpStatus.shouldBe(HttpStatus.INTERNAL_SERVER_ERROR)
    }
  }

  describe("moveToList()") {
    it("item is moved from one list to another list") {
      val spy = spyk(CommonService(mockCommonRepository, mockLrmItemRepository, mockLrmListRepository))
      every { mockLrmItemRepository.findByIdOrNull(1) } returns lrmItem()
      every { mockLrmListRepository.findByIdOrNull(2) } returns lrmList()
      every { mockLrmListRepository.findByIdOrNull(3) } returns lrmList()
      every { spy.addToList(any(), any()) } returns Pair("1", "3")
      every { spy.removeFromList(any(), any()) } returns Pair("1", "2")
      spy.moveToList(1, 2, 3)
      verify(exactly = 1) { spy.addToList(1, 3) }
      verify(exactly = 1) { spy.removeFromList(1, 2) }
    }

    it("item is not found") {
      val spy = spyk(CommonService(mockCommonRepository, mockLrmItemRepository, mockLrmListRepository))
      every { mockLrmItemRepository.findByIdOrNull(1) } returns null
      val exception = shouldThrow<ApiException> { spy.moveToList(1, 2, 3) }
      exception.cause.shouldBeInstanceOf<ItemNotFoundException>()
      verify(exactly = 0) { spy.addToList(1, 3) }
      verify(exactly = 0) { spy.removeFromList(1, 2) }
    }

    it("from list is not found") {
      val spy = spyk(CommonService(mockCommonRepository, mockLrmItemRepository, mockLrmListRepository))
      every { mockLrmItemRepository.findByIdOrNull(1) } returns lrmItem()
      every { mockLrmListRepository.findByIdOrNull(2) } returns null
      val exception = shouldThrow<ApiException> { spy.moveToList(1, 2, 3) }
      exception.cause.shouldBeInstanceOf<ListNotFoundException>()
      verify(exactly = 0) { spy.addToList(1, 3) }
      verify(exactly = 0) { spy.removeFromList(1, 2) }
    }

    it("to list is not found") {
      val spy = spyk(CommonService(mockCommonRepository, mockLrmItemRepository, mockLrmListRepository))
      every { mockLrmItemRepository.findByIdOrNull(1) } returns lrmItem()
      every { mockLrmListRepository.findByIdOrNull(2) } returns lrmList()
      every { mockLrmListRepository.findByIdOrNull(3) } returns null
      val exception = shouldThrow<ApiException> { spy.moveToList(1, 2, 3) }
      exception.cause.shouldBeInstanceOf<ListNotFoundException>()
      verify(exactly = 0) { spy.addToList(1, 3) }
      verify(exactly = 0) { spy.removeFromList(1, 2) }
    }

    it("non-api exception is thrown") {
      val spy = spyk(CommonService(mockCommonRepository, mockLrmItemRepository, mockLrmListRepository))
      every { mockLrmItemRepository.findByIdOrNull(1) } returns lrmItem()
      every { mockLrmListRepository.findByIdOrNull(2) } returns lrmList()
      every { mockLrmListRepository.findByIdOrNull(3) } returns lrmList()
      every { spy.addToList(any(), any()) } throws RuntimeException("Lorem Ipsum")
      every { spy.removeFromList(any(), any()) } returns Pair("1", "2")
      val exception = shouldThrow<ApiException> { spy.moveToList(1, 2, 3) }
      exception.cause.shouldBeInstanceOf<RuntimeException>()
      exception.httpStatus.shouldBe(HttpStatus.INTERNAL_SERVER_ERROR)
      exception.responseMessage.shouldBe("Item id 1 was not moved from list id 2 to list id 3.")
      verify(exactly = 1) { spy.addToList(1, 3) }
      verify(exactly = 0) { spy.removeFromList(1, 2) }
      verify(exactly = 1) { mockLrmItemRepository.findByIdOrNull(any()) }
      verify(exactly = 2) { mockLrmListRepository.findByIdOrNull(any()) }
    }
  }

  describe("removeFromAllLists()") {
    it("remove from all lists") {
      every { mockCommonRepository.deleteAllItemToListAssociations(1) } returns 999
      every { mockLrmItemRepository.findByIdOrNull(1) } returns lrmItem()
      commonService.removeFromAllLists(1)
      verify(exactly = 1) { mockCommonRepository.deleteAllItemToListAssociations(any()) }
      verify(exactly = 1) { mockLrmItemRepository.findByIdOrNull(any()) }
    }

    it("item not found") {
      every { mockCommonRepository.deleteAllItemToListAssociations(1) } returns 999
      every { mockLrmItemRepository.findByIdOrNull(1) } returns null
      val exception = shouldThrow<ApiException> { commonService.removeFromAllLists(1) }
      exception.cause.shouldBeInstanceOf<ItemNotFoundException>()
      exception.httpStatus.shouldBe(ItemNotFoundException.HTTP_STATUS)
      exception.responseMessage.shouldBe("Item id 1 could not be removed from any/all lists: Item id 1 could not be found.")
    }

    it("item repository throws exception") {
      every { mockCommonRepository.deleteAllItemToListAssociations(1) } throws RuntimeException()
      val exception = shouldThrow<ApiException> { commonService.removeFromAllLists(1) }
      exception.cause.shouldBeInstanceOf<RuntimeException>()
      exception.httpStatus.shouldBe(HttpStatus.INTERNAL_SERVER_ERROR)
      exception.responseMessage.shouldBe("Item id 1 could not be removed from any/all lists.")
    }
  }

  describe("removeFromList()") {
    it("removed from list") {
      every { mockLrmItemRepository.removeItemFromList(1, 2) } returns 1
      every { mockLrmItemRepository.findByIdOrNull(1) } returns lrmItem()
      every { mockLrmListRepository.findByIdOrNull(2) } returns lrmList()
      commonService.removeFromList(1, 2)
      verify(exactly = 1) { mockLrmItemRepository.removeItemFromList(1, 2) }
      verify(exactly = 1) { mockLrmItemRepository.findByIdOrNull(1) }
      verify(exactly = 1) { mockLrmListRepository.findByIdOrNull(2) }
    }

    it("item not found") {
      every { mockLrmItemRepository.removeItemFromList(1, 2) } returns 0
      every { mockLrmItemRepository.findByIdOrNull(any()) } returns null
      val exception = shouldThrow<ApiException> { commonService.removeFromList(1, 2) }
      exception.cause.shouldBeInstanceOf<ItemNotFoundException>()
      verify(exactly = 1) { mockLrmItemRepository.findByIdOrNull(1) }
      verify(exactly = 0) { mockLrmListRepository.findByIdOrNull(2) }
    }

    it("list not found") {
      every { mockLrmItemRepository.removeItemFromList(1, 2) } returns 0
      every { mockLrmItemRepository.findByIdOrNull(1) } returns lrmItem()
      every { mockLrmListRepository.findByIdOrNull(2) } returns null
      val exception = shouldThrow<ApiException> { commonService.removeFromList(1, 2) }
      exception.cause.shouldBeInstanceOf<ListNotFoundException>()
      verify(exactly = 1) { mockLrmItemRepository.findByIdOrNull(1) }
      verify(exactly = 1) { mockLrmListRepository.findByIdOrNull(2) }
    }

    it("item is not associated with the list") {
      every { mockLrmItemRepository.removeItemFromList(1, 2) } returns 0
      every { mockLrmItemRepository.findByIdOrNull(1) } returns lrmItem()
      every { mockLrmListRepository.findByIdOrNull(2) } returns lrmList()
      val exception = shouldThrow<ApiException> { commonService.removeFromList(1, 2) }
      exception.cause.shouldBeNull()
      exception.responseMessage.shouldBe("Item id 1 could not be removed from list id 2: Item id 1 is not associated with list id 2.")
      verify(exactly = 1) { mockLrmItemRepository.removeItemFromList(1, 2) }
      verify(exactly = 1) { mockLrmItemRepository.findByIdOrNull(1) }
      verify(exactly = 1) { mockLrmListRepository.findByIdOrNull(2) }
    }

    it("item is associated with the list multiple times") {
      every { mockLrmItemRepository.findByIdOrNull(1) } returns lrmItem()
      every { mockLrmListRepository.findByIdOrNull(2) } returns lrmList()
      every { mockLrmItemRepository.removeItemFromList(1, 2) } returns 2
      val exception = shouldThrow<ApiException> { commonService.removeFromList(1, 2) }
      exception.cause.shouldBeNull()
      verify(exactly = 1) { mockLrmItemRepository.removeItemFromList(1, 2) }
    }

    it("item repository throws exception") {
      every { mockLrmItemRepository.findByIdOrNull(1) } returns lrmItem()
      every { mockLrmListRepository.findByIdOrNull(2) } returns lrmList()
      every { mockLrmItemRepository.removeItemFromList(1, 2) } throws Exception("Lorem Ipsum")
      val exception = shouldThrow<ApiException> { commonService.removeFromList(1, 2) }
      exception.cause.shouldBeInstanceOf<Exception>()
      exception.httpStatus.shouldBe(HttpStatus.INTERNAL_SERVER_ERROR)
      exception.message.shouldBe("Item id 1 could not be removed from list id 2.")
      exception.responseMessage.shouldBe("Item id 1 could not be removed from list id 2.")
      exception.title.shouldBe("API Exception")
    }
  }
})
