package net.flyingfishflash.loremlist.unit.domain.association

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import net.flyingfishflash.loremlist.core.exceptions.ApiException
import net.flyingfishflash.loremlist.domain.LrmComponentType
import net.flyingfishflash.loremlist.domain.association.Association
import net.flyingfishflash.loremlist.domain.association.AssociationNotFoundException
import net.flyingfishflash.loremlist.domain.association.AssociationRepository
import net.flyingfishflash.loremlist.domain.association.AssociationService
import net.flyingfishflash.loremlist.domain.association.SuccinctLrmComponentPair
import net.flyingfishflash.loremlist.domain.lrmitem.ItemNotFoundException
import net.flyingfishflash.loremlist.domain.lrmitem.LrmItem
import net.flyingfishflash.loremlist.domain.lrmitem.LrmItemRepository
import net.flyingfishflash.loremlist.domain.lrmitem.data.LrmItemRequest
import net.flyingfishflash.loremlist.domain.lrmitem.succinct
import net.flyingfishflash.loremlist.domain.lrmlist.ListNotFoundException
import net.flyingfishflash.loremlist.domain.lrmlist.LrmList
import net.flyingfishflash.loremlist.domain.lrmlist.LrmListRepository
import net.flyingfishflash.loremlist.domain.lrmlist.succinct
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
  fun lrmItem1(): LrmItem = LrmItem(uuid = uuid1, name = lrmItemRequest.name, description = lrmItemRequest.description)
  fun lrmItem2(): LrmItem = LrmItem(uuid = uuid2, name = "Lorem Item Name (uuid2)", description = "Lorem Item Description")
  fun lrmItem3(): LrmItem = LrmItem(uuid = uuid3, name = "Lorem Item Name (uuid3)", description = "Lorem Item Description")
  fun lrmList1(): LrmList = LrmList(uuid = uuid1, name = "Lorem List Name (uuid1)", description = "Lorem List Description")
  fun lrmList2(): LrmList = LrmList(uuid = uuid2, name = "Lorem List Name (uuid2)", description = "Lorem List Description")
  fun lrmList3(): LrmList = LrmList(uuid = uuid3, name = "Lorem List Name (uuid3)", description = "Lorem List Description")
  fun associationItem1ListUuid2(): Association = Association(uuid = UUID.randomUUID(), itemUuid = lrmItem1().uuid, listUuid = uuid2)

  afterEach { clearAllMocks() }
  afterSpec { unmockkAll() }

  describe("createForItem()") {
    it("associate item with list(s)") {
      val listUuids = listOf(lrmList2().uuid, lrmList3().uuid)
      every { mockLrmItemRepository.findByIdOrNull(lrmItem1().uuid) } returns lrmItem1()
      every { mockLrmListRepository.notFoundByIdCollection(listUuids) } returns emptySet()
      every {
        mockAssociationRepository.create(any())
      } returns listOf(
        SuccinctLrmComponentPair(lrmList2().succinct(), lrmItem1().succinct()),
        SuccinctLrmComponentPair(lrmList3().succinct(), lrmItem1().succinct()),
      )
      val response = associationService.create(uuid = lrmItem1().uuid, uuidCollection = listUuids, LrmComponentType.Item)
      response.componentName.shouldBe(lrmItem1().name)
      response.associatedComponents.shouldBe(setOf(lrmList2().succinct(), lrmList3().succinct()))
    }

    it("item not found") {
      every { mockLrmItemRepository.findByIdOrNull(any()) } returns null
      val exception =
        shouldThrow<ApiException> {
          associationService.create(uuid = UUID.randomUUID(), uuidCollection = listOf(UUID.randomUUID()), type = LrmComponentType.Item)
        }
      exception.cause.shouldBeInstanceOf<ItemNotFoundException>()
      exception.httpStatus.shouldBe(ItemNotFoundException.HTTP_STATUS)
    }

    it("list not found") {
      every { mockLrmItemRepository.findByIdOrNull(any()) } returns lrmItem1()
      every { mockLrmListRepository.notFoundByIdCollection(any()) } returns setOf(UUID.randomUUID())
      val exception =
        shouldThrow<ApiException> {
          associationService.create(uuid = UUID.randomUUID(), uuidCollection = listOf(UUID.randomUUID()), type = LrmComponentType.Item)
        }
      exception.cause.shouldBeInstanceOf<ListNotFoundException>()
      exception.httpStatus.shouldBe(ListNotFoundException.HTTP_STATUS)
    }

    it("created association count not equal to requested association count") {
      val listUuids = listOf(lrmList2().uuid, lrmList3().uuid)
      every { mockLrmItemRepository.findByIdOrNull(lrmItem1().uuid) } returns lrmItem1()
      every { mockLrmListRepository.notFoundByIdCollection(listUuids) } returns emptySet()
      every {
        mockAssociationRepository.create(any())
      } returns listOf(SuccinctLrmComponentPair(lrmList2().succinct(), lrmItem1().succinct()))
      val exception = shouldThrow<ApiException> {
        associationService.create(uuid = lrmItem1().uuid, uuidCollection = listUuids, LrmComponentType.Item)
      }
      exception.message.shouldContain("created = 1 / requested = 2")
    }

    it("already associated with list (postgresql)") {
      val listUuids = listOf(lrmList2().uuid)
      every { mockLrmItemRepository.findByIdOrNull(lrmItem1().uuid) } returns lrmItem1()
      every { mockLrmListRepository.notFoundByIdCollection(listUuids) } returns emptySet()
      every { mockAssociationRepository.create(any()) } throws SQLException("duplicate key value violates unique constraint")
      val exception =
        shouldThrow<ApiException> {
          associationService.create(uuid = lrmItem1().uuid, uuidCollection = listUuids, LrmComponentType.Item)
        }
      exception.cause.shouldBeInstanceOf<SQLException>()
      exception.responseMessage.shouldContain("It already exists.")
    }

    it("already associated with list (h2)") {
      val listUuids = listOf(lrmList2().uuid)
      every { mockLrmItemRepository.findByIdOrNull(lrmItem1().uuid) } returns lrmItem1()
      every { mockLrmListRepository.notFoundByIdCollection(listUuids) } returns emptySet()
      every { mockAssociationRepository.create(any()) } throws SQLException("Unique index or primary key violation")
      val exception =
        shouldThrow<ApiException> {
          associationService.create(uuid = lrmItem1().uuid, uuidCollection = listUuids, LrmComponentType.Item)
        }
      exception.cause.shouldBeInstanceOf<SQLException>()
      exception.responseMessage.shouldContain("It already exists.")
    }

    it("unanticipated sql exception (sql exception message is null)") {
      val listUuids = listOf(lrmList2().uuid, lrmList3().uuid)
      every { mockLrmItemRepository.findByIdOrNull(lrmItem1().uuid) } returns lrmItem1()
      every { mockLrmListRepository.notFoundByIdCollection(listUuids) } returns emptySet()
      every { mockAssociationRepository.create(any()) } throws SQLException()
      val exception =
        shouldThrow<ApiException> {
          associationService.create(uuid = lrmItem1().uuid, uuidCollection = listUuids, LrmComponentType.Item)
        }
      exception.cause.shouldBeInstanceOf<SQLException>()
      exception.responseMessage.shouldContain("Unanticipated SQL exception")
    }

    it("unanticipated sql exception (sql exception message is not null)") {
      val listUuids = listOf(lrmList2().uuid, lrmList3().uuid)
      every { mockLrmItemRepository.findByIdOrNull(lrmItem1().uuid) } returns lrmItem1()
      every { mockLrmListRepository.notFoundByIdCollection(listUuids) } returns emptySet()
      every { mockAssociationRepository.create(any()) } throws SQLException("Lorem Ipsum")
      val exception =
        shouldThrow<ApiException> {
          associationService.create(uuid = lrmItem1().uuid, uuidCollection = listUuids, LrmComponentType.Item)
        }
      exception.cause.shouldBeInstanceOf<SQLException>()
      exception.responseMessage.shouldContain("Unanticipated SQL exception")
    }

    it("unanticipated exception") {
      val listUuids = listOf(lrmList2().uuid, lrmList3().uuid)
      every { mockLrmItemRepository.findByIdOrNull(lrmItem1().uuid) } returns lrmItem1()
      every { mockLrmListRepository.notFoundByIdCollection(listUuids) } returns emptySet()
      every { mockAssociationRepository.create(any()) } throws Exception("Lorem Ipsum")
      val exception =
        shouldThrow<ApiException> {
          associationService.create(uuid = lrmItem1().uuid, uuidCollection = listUuids, LrmComponentType.Item)
        }
      exception.cause.shouldBeInstanceOf<Exception>()
    }
  }

  describe("createForList()") {
    it("associate list with item(s)") {
      val itemUuids = listOf(lrmItem2().uuid, lrmItem3().uuid)
      every { mockLrmListRepository.findByIdOrNull(lrmList1().uuid) } returns lrmList1()
      every { mockLrmItemRepository.notFoundByIdCollection(itemUuids) } returns emptySet()
      every { mockAssociationRepository.create(any()) } returns listOf(
        SuccinctLrmComponentPair(lrmList1().succinct(), lrmItem2().succinct()),
        SuccinctLrmComponentPair(lrmList1().succinct(), lrmItem3().succinct()),
      )
      every { mockLrmItemRepository.findByIdOrNull(lrmItem2().uuid) } returns lrmItem2()
      every { mockLrmItemRepository.findByIdOrNull(lrmItem3().uuid) } returns lrmItem3()
      val response = associationService.create(uuid = lrmList1().uuid, uuidCollection = itemUuids, LrmComponentType.List)
      response.componentName.shouldBe(lrmList1().name)
      response.associatedComponents.shouldBe(setOf(lrmItem2().succinct(), lrmItem3().succinct()))
    }

    it("list not found") {
      every { mockLrmListRepository.findByIdOrNull(any()) } returns null
      val exception =
        shouldThrow<ApiException> {
          associationService.create(uuid = UUID.randomUUID(), uuidCollection = listOf(UUID.randomUUID()), type = LrmComponentType.List)
        }
      exception.cause.shouldBeInstanceOf<ListNotFoundException>()
      exception.httpStatus.shouldBe(ListNotFoundException.HTTP_STATUS)
    }

    it("item not found") {
      every { mockLrmListRepository.findByIdOrNull(any()) } returns lrmList1()
      every { mockLrmItemRepository.notFoundByIdCollection(any()) } returns setOf(UUID.randomUUID())
      val exception =
        shouldThrow<ApiException> {
          associationService.create(uuid = UUID.randomUUID(), uuidCollection = listOf(UUID.randomUUID()), type = LrmComponentType.List)
        }
      exception.cause.shouldBeInstanceOf<ItemNotFoundException>()
      exception.httpStatus.shouldBe(ItemNotFoundException.HTTP_STATUS)
    }

    it("created association count not equal to requested association count") {
      val itemUuids = listOf(lrmItem2().uuid, lrmItem3().uuid)
      every { mockLrmListRepository.findByIdOrNull(lrmList1().uuid) } returns lrmList1()
      every { mockLrmItemRepository.notFoundByIdCollection(itemUuids) } returns emptySet()
      every { mockAssociationRepository.create(any()) } returns listOf(
        SuccinctLrmComponentPair(lrmList1().succinct(), lrmItem2().succinct()),
      )
      val exception = shouldThrow<ApiException> {
        associationService.create(uuid = lrmList1().uuid, uuidCollection = itemUuids, LrmComponentType.List)
      }
      exception.message.shouldContain("created = 1 / requested = 2")
    }
  }

  describe("countAll()") {
    it("count of all associations is returned") {
      every { mockAssociationRepository.count() } returns 999
      associationService.countAll().shouldBe(999)
    }

    it("association repository throws exception") {
      every { mockAssociationRepository.count() } throws RuntimeException("Lorem Ipsum")
      shouldThrow<ApiException> { associationService.countAll() }
    }
  }

  describe("countItemToList()") {
    it("count of list associations is returned") {
      every { mockLrmItemRepository.findByIdOrNull(uuid1) } returns lrmItem1()
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
      every { mockLrmListRepository.findByIdOrNull(uuid1) } returns lrmList2()
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
      every { mockLrmItemRepository.findByIdOrNull(uuid1) } returns lrmItem1()
      every { mockLrmListRepository.findByIdOrNull(uuid2) } returns lrmList2()
      every { mockLrmListRepository.findByIdOrNull(uuid3) } returns lrmList2()
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
      every { mockLrmItemRepository.findByIdOrNull(uuid1) } returns lrmItem1()
      every { mockLrmListRepository.findByIdOrNull(uuid2) } returns null
      val exception = shouldThrow<ApiException> { associationService.updateList(uuid1, uuid2, uuid3) }
      exception.cause.shouldBeInstanceOf<ListNotFoundException>()
      verify(exactly = 0) { mockAssociationRepository.update(any()) }
    }

    it("to list is not found") {
      every { mockLrmItemRepository.findByIdOrNull(uuid1) } returns lrmItem1()
      every { mockLrmListRepository.findByIdOrNull(uuid2) } returns lrmList2()
      every { mockLrmListRepository.findByIdOrNull(uuid3) } returns null
      val exception = shouldThrow<ApiException> { associationService.updateList(uuid1, uuid2, uuid3) }
      exception.cause.shouldBeInstanceOf<ListNotFoundException>()
      verify(exactly = 0) { mockAssociationRepository.update(any()) }
    }

    it("association is not found") {
      every { mockLrmItemRepository.findByIdOrNull(uuid1) } returns lrmItem1()
      every { mockLrmListRepository.findByIdOrNull(uuid2) } returns lrmList2()
      every { mockLrmListRepository.findByIdOrNull(uuid3) } returns lrmList2()
      every { mockAssociationRepository.findByItemIdAndListIdOrNull(uuid1, uuid2) } returns null
      val exception = shouldThrow<ApiException> { associationService.updateList(uuid1, uuid2, uuid3) }
      exception.cause.shouldBeInstanceOf<AssociationNotFoundException>()
      verify(exactly = 0) { mockAssociationRepository.update(any()) }
    }

    it("non-api exception is thrown") {
      every { mockLrmItemRepository.findByIdOrNull(uuid1) } returns lrmItem1()
      every { mockLrmListRepository.findByIdOrNull(uuid2) } returns lrmList2()
      every { mockLrmListRepository.findByIdOrNull(uuid3) } returns lrmList2()
      every { mockAssociationRepository.findByItemIdAndListIdOrNull(uuid1, uuid2) } throws RuntimeException("Lorem Ipsum")
      val exception = shouldThrow<ApiException> { associationService.updateList(uuid1, uuid2, uuid3) }
      exception.cause.shouldBeInstanceOf<RuntimeException>()
      exception.httpStatus.shouldBe(HttpStatus.INTERNAL_SERVER_ERROR)
      exception.responseMessage.shouldBe("Item id $uuid1 was not moved from list id $uuid2 to list id $uuid3.")
      verify(exactly = 1) { mockLrmItemRepository.findByIdOrNull(any()) }
      verify(exactly = 2) { mockLrmListRepository.findByIdOrNull(any()) }
    }
  }

  describe("deleteAll()") {
    it("delete all associations") {
      every { mockAssociationRepository.deleteAll() } returns 999
      associationService.deleteAll().shouldBe(999)
    }

    it("association repository throws exception") {
      every { mockAssociationRepository.deleteAll() } throws RuntimeException("Lorem Ipsum")
      shouldThrow<ApiException> { associationService.deleteAll() }
    }
  }

  describe("deleteAllForItem()") {
    it("remove all associations by item") {
      every { mockAssociationRepository.deleteAllOfItem(uuid1) } returns 999
      every { mockLrmItemRepository.findByIdOrNull(uuid1) } returns lrmItem1()
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
      exception.responseMessage.shouldContain("Item could not be found.")
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
      every { mockLrmListRepository.findByIdOrNull(uuid1) } returns lrmList2()
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
      exception.responseMessage.shouldContain("List could not be found.")
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
      every { mockLrmItemRepository.findByIdOrNull(uuid1) } returns lrmItem1()
      every { mockLrmListRepository.findByIdOrNull(uuid2) } returns lrmList2()
      every { mockAssociationRepository.findByItemIdAndListIdOrNull(uuid1, uuid2) } returns associationItem1ListUuid2()
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
      every { mockLrmItemRepository.findByIdOrNull(uuid1) } returns lrmItem1()
      every { mockLrmListRepository.findByIdOrNull(uuid2) } returns null
      val exception = shouldThrow<ApiException> { associationService.deleteByItemIdAndListId(uuid1, uuid2) }
      exception.cause.shouldBeInstanceOf<ListNotFoundException>()
      verify(exactly = 1) { mockLrmItemRepository.findByIdOrNull(uuid = any()) }
      verify(exactly = 1) { mockLrmListRepository.findByIdOrNull(uuid = any()) }
    }

    it("association not found") {
      every { mockLrmItemRepository.findByIdOrNull(uuid1) } returns lrmItem1()
      every { mockLrmListRepository.findByIdOrNull(uuid2) } returns lrmList2()
      every { mockAssociationRepository.findByItemIdAndListIdOrNull(uuid1, uuid2) } returns null
      val exception = shouldThrow<ApiException> { associationService.deleteByItemIdAndListId(uuid1, uuid2) }
      exception.cause.shouldBeInstanceOf<AssociationNotFoundException>()
      verify(exactly = 1) { mockLrmItemRepository.findByIdOrNull(uuid = any()) }
      verify(exactly = 1) { mockLrmListRepository.findByIdOrNull(uuid = any()) }
      verify(exactly = 1) { mockAssociationRepository.findByItemIdAndListIdOrNull(itemUuid = any(), listUuid = any()) }
    }

    it("association is found but zero records deleted") {
      every { mockLrmItemRepository.findByIdOrNull(uuid1) } returns lrmItem1()
      every { mockLrmListRepository.findByIdOrNull(uuid2) } returns lrmList2()
      every { mockAssociationRepository.findByItemIdAndListIdOrNull(uuid1, uuid2) } returns associationItem1ListUuid2()
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
      every { mockLrmItemRepository.findByIdOrNull(uuid1) } returns lrmItem1()
      every { mockLrmListRepository.findByIdOrNull(uuid2) } returns lrmList2()
      every { mockAssociationRepository.findByItemIdAndListIdOrNull(uuid1, uuid2) } returns associationItem1ListUuid2()
      every { mockAssociationRepository.delete(uuid = any()) } returns 2
      val exception = shouldThrow<ApiException> { associationService.deleteByItemIdAndListId(uuid1, uuid2) }
      exception.cause.shouldBeNull()
      verify(exactly = 1) { mockLrmItemRepository.findByIdOrNull(uuid1) }
      verify(exactly = 1) { mockLrmListRepository.findByIdOrNull(uuid2) }
      verify(exactly = 1) { mockAssociationRepository.findByItemIdAndListIdOrNull(itemUuid = any(), listUuid = any()) }
      verify(exactly = 1) { mockAssociationRepository.delete(uuid = any()) }
    }

    it("item repository throws exception") {
      every { mockLrmItemRepository.findByIdOrNull(uuid1) } returns lrmItem1()
      every { mockLrmListRepository.findByIdOrNull(uuid2) } returns lrmList2()
      every { mockAssociationRepository.findByItemIdAndListIdOrNull(uuid1, uuid2) } returns associationItem1ListUuid2()
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
