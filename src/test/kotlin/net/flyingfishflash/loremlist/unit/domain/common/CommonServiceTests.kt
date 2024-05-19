package net.flyingfishflash.loremlist.unit.domain.common

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
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
import net.flyingfishflash.loremlist.core.exceptions.AbstractApiException
import net.flyingfishflash.loremlist.core.exceptions.ApiException
import net.flyingfishflash.loremlist.domain.common.CommonRepository
import net.flyingfishflash.loremlist.domain.common.CommonService
import net.flyingfishflash.loremlist.domain.lrmitem.ItemNotFoundException
import net.flyingfishflash.loremlist.domain.lrmitem.LrmItem
import net.flyingfishflash.loremlist.domain.lrmitem.LrmItemRepository
import net.flyingfishflash.loremlist.domain.lrmitem.LrmItemService
import net.flyingfishflash.loremlist.domain.lrmitem.data.LrmItemRequest
import net.flyingfishflash.loremlist.domain.lrmlist.ListNotFoundException
import net.flyingfishflash.loremlist.domain.lrmlist.LrmList
import net.flyingfishflash.loremlist.domain.lrmlist.LrmListService
import org.springframework.http.HttpStatus
import java.sql.SQLException
import java.util.UUID

class CommonServiceTests : DescribeSpec({
  val mockCommonRepository = mockk<CommonRepository>()
  val mockLrmItemRepository = mockk<LrmItemRepository>()
  val mockLrmItemService = mockk<LrmItemService>()
  val mockLrmListService = mockk<LrmListService>()
  val commonService = CommonService(mockCommonRepository, mockLrmItemRepository, mockLrmItemService, mockLrmListService)

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
      every { mockLrmItemService.findById(1) } returns lrmItem()
      every { mockLrmListService.findById(1) } returns lrmList()
      val response = commonService.addToList(itemId = 1, listId = 1)
      response.first.shouldBeEqual(lrmItem().name)
      response.second.shouldBeEqual(lrmList().name)
      verify(exactly = 1) { mockLrmItemRepository.addItemToList(1, 1) }
      verify(exactly = 2) { mockLrmItemService.findById(1) }
      verify(exactly = 2) { mockLrmListService.findById(1) }
    }

    it("item not found") {
      every { mockLrmItemService.findById(1) } throws ItemNotFoundException(1)
      val exception = shouldThrow<ApiException> { commonService.addToList(itemId = 1, listId = 1) }
      exception.cause.shouldBeInstanceOf<ItemNotFoundException>()
      verify(exactly = 0) { mockLrmItemRepository.addItemToList(1, 1) }
      verify(exactly = 1) { mockLrmItemService.findById(1) }
      verify(exactly = 0) { mockLrmListService.findById(1) }
    }

    it("list not found") {
      every { mockLrmItemService.findById(1) } returns lrmItem()
      every { mockLrmListService.findById(1) } throws ListNotFoundException(1)
      shouldThrow<ApiException> { commonService.addToList(itemId = 1, listId = 1) }
        .cause.shouldBeInstanceOf<ListNotFoundException>()
      verify(exactly = 0) { mockLrmItemRepository.addItemToList(1, 1) }
      verify(exactly = 1) { mockLrmItemService.findById(1) }
      verify(exactly = 1) { mockLrmListService.findById(1) }
    }

    it("already added to list (postgresql") {
      every { mockLrmItemService.findById(1) } returns lrmItem()
      every { mockLrmListService.findById(1) } returns lrmList()
      every { mockLrmItemRepository.addItemToList(1, 1) } throws SQLException("duplicate key value violates unique constraint")
      val exception = shouldThrow<ApiException> { commonService.addToList(itemId = 1, listId = 1) }
      exception.cause.shouldBeInstanceOf<SQLException>()
      exception.responseMessage.shouldBe("Item id 1 could not be added to list id 1 because it's already been added.")
      verify(exactly = 1) { mockLrmItemService.findById(1) }
      verify(exactly = 1) { mockLrmListService.findById(1) }
      verify(exactly = 1) { mockLrmItemRepository.addItemToList(1, 1) }
    }

    it("already added to list (h2)") {
      every { mockLrmItemService.findById(1) } returns lrmItem()
      every { mockLrmListService.findById(1) } returns lrmList()
      every { mockLrmItemRepository.addItemToList(1, 1) } throws SQLException("Unique index or primary key violation")
      val exception = shouldThrow<ApiException> { commonService.addToList(itemId = 1, listId = 1) }
      exception.cause.shouldBeInstanceOf<SQLException>()
      exception.responseMessage.shouldBe("Item id 1 could not be added to list id 1 because it's already been added.")
      verify(exactly = 1) { mockLrmItemService.findById(1) }
      verify(exactly = 1) { mockLrmListService.findById(1) }
      verify(exactly = 1) { mockLrmItemRepository.addItemToList(1, 1) }
    }

    it("unanticipated sql exception (exception message is null)") {
      every { mockLrmItemService.findById(1) } returns lrmItem()
      every { mockLrmListService.findById(1) } returns lrmList()
      every { mockLrmItemRepository.addItemToList(1, 1) } throws SQLException()
      val exception = shouldThrow<ApiException> { commonService.addToList(itemId = 1, listId = 1) }
      exception.cause.shouldBeInstanceOf<SQLException>()
      exception.responseMessage.shouldBe(
        "Item id 1 could not be added to list id 1 because of an unanticipated sql exception.",
      )
      verify(exactly = 1) { mockLrmItemService.findById(1) }
      verify(exactly = 1) { mockLrmListService.findById(1) }
      verify(exactly = 1) { mockLrmItemRepository.addItemToList(1, 1) }
    }

    it("unanticipated sql exception (exception message is not null)") {
      every { mockLrmItemService.findById(1) } returns lrmItem()
      every { mockLrmListService.findById(1) } returns lrmList()
      every { mockLrmItemRepository.addItemToList(1, 1) } throws SQLException("Lorem Ipsum")
      val exception = shouldThrow<ApiException> { commonService.addToList(itemId = 1, listId = 1) }
      exception.cause.shouldBeInstanceOf<SQLException>()
      exception.responseMessage.shouldBe(
        "Item id 1 could not be added to list id 1 because of an unanticipated sql exception.",
      )
      verify(exactly = 1) { mockLrmItemService.findById(1) }
      verify(exactly = 1) { mockLrmListService.findById(1) }
      verify(exactly = 1) { mockLrmItemRepository.addItemToList(1, 1) }
    }

    it("unanticipated exception") {
      every { mockLrmItemService.findById(1) } returns lrmItem()
      every { mockLrmListService.findById(1) } returns lrmList()
      every { mockLrmItemRepository.addItemToList(1, 1) } throws Exception("Lorem Ipsum")
      val exception = shouldThrow<ApiException> { commonService.addToList(itemId = 1, listId = 1) }
      exception.cause.shouldBeInstanceOf<Exception>()
      exception.responseMessage.shouldBe("Item id 1 could not be added to list id 1 because of an unanticipated exception.")
      verify(exactly = 1) { mockLrmItemService.findById(1) }
      verify(exactly = 1) { mockLrmListService.findById(1) }
      verify(exactly = 1) { mockLrmItemRepository.addItemToList(1, 1) }
    }
  }

  describe("countItemToListAssociations()") {
    it("count of list associations is returned") {
      every { mockLrmItemService.findById(1) } returns lrmItem()
      every { mockCommonRepository.countItemToListAssociations(1) } returns 1
      commonService.countItemToListAssociations(1)
      verify(exactly = 1) { mockCommonRepository.countItemToListAssociations(any()) }
    }

    it("item is not found") {
      every { mockLrmItemService.findById(1) } throws ItemNotFoundException(1)
      val exception = shouldThrow<ApiException> { commonService.countItemToListAssociations(1) }
      exception.cause.shouldBeInstanceOf<ItemNotFoundException>()
      exception.httpStatus.shouldBe(HttpStatus.NOT_FOUND)
    }

    it("item repository throws exception") {
      every { mockLrmItemService.findById(1) } throws RuntimeException("Lorem Ipsum")
      val exception = shouldThrow<ApiException> { commonService.countItemToListAssociations(1) }
      exception.cause.shouldBeInstanceOf<RuntimeException>()
      exception.httpStatus.shouldBe(HttpStatus.INTERNAL_SERVER_ERROR)
    }
  }

  describe("countListToItemAssociations()") {
    it("count of item associations is returned") {
      every { mockLrmListService.findById(1) } returns lrmList()
      every { mockCommonRepository.countListToItemAssociations(1) } returns 1
      commonService.countListToItemAssociations(1)
      verify(exactly = 1) { mockCommonRepository.countListToItemAssociations(any()) }
    }

    it("list is not found") {
      every { mockLrmListService.findById(1) } throws ListNotFoundException(1)
      val exception = shouldThrow<ApiException> { commonService.countListToItemAssociations(1) }
      exception.cause.shouldBeInstanceOf<ListNotFoundException>()
      exception.httpStatus.shouldBe(HttpStatus.NOT_FOUND)
    }

    it("list repository throws exception") {
      every { mockLrmListService.findById(1) } throws RuntimeException("Lorem Ipsum")
      val exception = shouldThrow<ApiException> { commonService.countListToItemAssociations(1) }
      exception.cause.shouldBeInstanceOf<RuntimeException>()
      exception.httpStatus.shouldBe(HttpStatus.INTERNAL_SERVER_ERROR)
    }
  }

  describe("moveToList()") {
    it("item is moved from one list to another list") {
      val spy = spyk(CommonService(mockCommonRepository, mockLrmItemRepository, mockLrmItemService, mockLrmListService))
      every { mockLrmItemService.findById(1) } returns lrmItem()
      every { mockLrmListService.findById(2) } returns lrmList()
      every { mockLrmListService.findById(3) } returns lrmList()
      every { spy.addToList(any(), any()) } returns Pair("1", "3")
      every { spy.removeFromList(any(), any()) } returns Pair("1", "2")
      spy.moveToList(1, 2, 3)
      verify(exactly = 1) { spy.addToList(1, 3) }
      verify(exactly = 1) { spy.removeFromList(1, 2) }
    }

    it("anticipated api exception with cause of type abstract api exception") {
      val spy = spyk(CommonService(mockCommonRepository, mockLrmItemRepository, mockLrmItemService, mockLrmListService))
      every { spy.addToList(any(), any()) } throws ApiException(cause = ApiException(), httpStatus = HttpStatus.I_AM_A_TEAPOT)
      val exception = shouldThrow<ApiException> { spy.moveToList(1, 2, 3) }
      val causeHttpStatus = exception.cause.shouldBeInstanceOf<AbstractApiException>().httpStatus
      exception.httpStatus.shouldBe(causeHttpStatus)
      verify(exactly = 1) { spy.addToList(any(), any()) }
      verify(exactly = 0) { spy.removeFromList(any(), any()) }
    }

    it("anticipated api exception with root cause of type exception") {
      val spy = spyk(CommonService(mockCommonRepository, mockLrmItemRepository, mockLrmItemService, mockLrmListService))
      every { spy.addToList(any(), any()) } throws ApiException(cause = Exception())
      val exception = shouldThrow<ApiException> { spy.moveToList(1, 2, 3) }
      exception.cause.shouldBeInstanceOf<Exception>().message.shouldBeNull()
      exception.httpStatus.shouldBe(HttpStatus.INTERNAL_SERVER_ERROR)
      verify(exactly = 1) { spy.addToList(any(), any()) }
      verify(exactly = 0) { spy.removeFromList(any(), any()) }
    }

    it("anticipated api exception with root cause not of type api exception") {
      val spy = spyk(CommonService(mockCommonRepository, mockLrmItemRepository, mockLrmItemService, mockLrmListService))
      every { spy.addToList(any(), any()) } throws Exception("Lorem Ipsum")
      val exception = shouldThrow<ApiException> { spy.moveToList(1, 2, 3) }
      exception.cause.shouldBeInstanceOf<Exception>().message.shouldBe("Lorem Ipsum")
      exception.httpStatus.shouldBe(HttpStatus.INTERNAL_SERVER_ERROR)
      verify(exactly = 1) { spy.addToList(any(), any()) }
      verify(exactly = 0) { spy.removeFromList(any(), any()) }
    }

    it("anticipated api exception with root cause not of type api exception (no message)") {
      val spy = spyk(CommonService(mockCommonRepository, mockLrmItemRepository, mockLrmItemService, mockLrmListService))
      every { spy.addToList(any(), any()) } throws Exception()
      val exception = shouldThrow<ApiException> { spy.moveToList(1, 2, 3) }
      exception.cause.shouldBeInstanceOf<Exception>().message.shouldBeNull()
      exception.httpStatus.shouldBe(HttpStatus.INTERNAL_SERVER_ERROR)
      exception.responseMessage.shouldBe("Item was not moved: exception cause detail not available")
      verify(exactly = 1) { spy.addToList(any(), any()) }
      verify(exactly = 0) { spy.removeFromList(any(), any()) }
    }
  }

  describe("removeFromAllLists()") {
    it("remove from all lists") {
      every { mockCommonRepository.deleteAllItemToListAssociations(1) } returns 999
      every { mockLrmItemService.findById(1) } returns lrmItem()
      commonService.removeFromAllLists(1)
      verify(exactly = 1) { mockCommonRepository.deleteAllItemToListAssociations(any()) }
      verify(exactly = 1) { mockLrmItemService.findById(any()) }
    }

    it("item not found") {
      every { mockCommonRepository.deleteAllItemToListAssociations(1) } returns 999
      every { mockLrmItemService.findById(1) } throws ItemNotFoundException(999)
      val exception = shouldThrow<ApiException> { commonService.removeFromAllLists(1) }
      exception.cause.shouldBeInstanceOf<ItemNotFoundException>()
      exception.httpStatus.shouldBe(ItemNotFoundException.HTTP_STATUS)
      exception.responseMessage.shouldBe("Item id 1 could not be removed from any/all lists because the item could not be found.")
    }

    it("item repository throws exception") {
      every { mockCommonRepository.deleteAllItemToListAssociations(1) } throws RuntimeException()
      val exception = shouldThrow<ApiException> { commonService.removeFromAllLists(1) }
      exception.cause.shouldBeInstanceOf<RuntimeException>()
      exception.httpStatus.shouldBe(HttpStatus.INTERNAL_SERVER_ERROR)
      exception.responseMessage.shouldBe("Item id 1 could not be removed from any/all lists due to an exception.")
    }
  }

  describe("removeFromList()") {
    it("removed from list") {
      every { mockLrmItemRepository.removeItemFromList(1, 2) } returns 1
      every { mockLrmItemService.findById(1) } returns lrmItem()
      every { mockLrmListService.findById(2) } returns lrmList()
      commonService.removeFromList(1, 2)
      verify(exactly = 1) { mockLrmItemRepository.removeItemFromList(1, 2) }
      verify(exactly = 1) { mockLrmItemService.findById(1) }
      verify(exactly = 1) { mockLrmListService.findById(2) }
    }

    it("item not found") {
      every { mockLrmItemRepository.removeItemFromList(1, 2) } returns 0
      every { mockLrmItemService.findById(any()) } throws ItemNotFoundException(1)
      val exception = shouldThrow<ApiException> { commonService.removeFromList(1, 2) }
      exception.cause.shouldBeInstanceOf<ItemNotFoundException>()
      verify(exactly = 1) { mockLrmItemRepository.removeItemFromList(1, 2) }
      verify(exactly = 1) { mockLrmItemService.findById(1) }
      verify(exactly = 0) { mockLrmListService.findById(2) }
    }

    it("list not found") {
      every { mockLrmItemRepository.removeItemFromList(1, 2) } returns 0
      every { mockLrmItemService.findById(1) } returns lrmItem()
      every { mockLrmListService.findById(2) } throws ListNotFoundException(2)
      val exception = shouldThrow<ApiException> { commonService.removeFromList(1, 2) }
      exception.cause.shouldBeInstanceOf<ListNotFoundException>()
      verify(exactly = 1) { mockLrmItemRepository.removeItemFromList(1, 2) }
      verify(exactly = 1) { mockLrmItemService.findById(1) }
      verify(exactly = 1) { mockLrmListService.findById(2) }
    }

    it("item is not associated with the list") {
      every { mockLrmItemRepository.removeItemFromList(1, 2) } returns 0
      every { mockLrmItemService.findById(1) } returns lrmItem()
      every { mockLrmListService.findById(2) } returns lrmList()
      val exception = shouldThrow<ApiException> { commonService.removeFromList(1, 2) }
      exception.cause.shouldBeNull()
      exception.responseMessage.shouldBe("Item id 1 is not associated with list id 2.")
      verify(exactly = 1) { mockLrmItemRepository.removeItemFromList(1, 2) }
      verify(exactly = 1) { mockLrmItemService.findById(1) }
      verify(exactly = 1) { mockLrmListService.findById(2) }
    }

    it("item is associated with the list multiple times") {
      every { mockLrmItemRepository.removeItemFromList(1, 2) } returns 2
      val exception = shouldThrow<ApiException> { commonService.removeFromList(1, 2) }
      exception.cause.shouldBeNull()
      verify(exactly = 1) { mockLrmItemRepository.removeItemFromList(1, 2) }
    }

    it("item repository throws exception") {
      every { mockLrmItemRepository.removeItemFromList(1, 2) } throws Exception("Lorem Ipsum")
      val exception = shouldThrow<ApiException> { commonService.removeFromList(1, 2) }
      exception.cause.shouldBeInstanceOf<Exception>()
      exception.httpStatus.shouldBe(HttpStatus.INTERNAL_SERVER_ERROR)
      exception.message.shouldNotBeNull().shouldBeEqual("Item id 1 could not be removed from list id 2.")
      exception.responseMessage.shouldBeEqual("Item id 1 could not be removed from list id 2.")
      exception.title.shouldBeEqual("API Exception")
    }
  }
})
