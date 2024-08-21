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
  val id1 = UUID.fromString("00000000-0000-0000-0000-000000000001")
  val id2 = UUID.fromString("00000000-0000-0000-0000-000000000002")
  val id3 = UUID.fromString("00000000-0000-0000-0000-000000000003")
  fun lrmItem1(): LrmItem = LrmItem(id = id1, name = lrmItemRequest.name, description = lrmItemRequest.description)
  fun lrmItem2(): LrmItem = LrmItem(id = id2, name = "Lorem Item Name (id2)", description = "Lorem Item Description")
  fun lrmItem3(): LrmItem = LrmItem(id = id3, name = "Lorem Item Name (id3)", description = "Lorem Item Description")
  fun lrmList1(): LrmList = LrmList(id = id1, name = "Lorem List Name (id1)", description = "Lorem List Description", public = true)
  fun lrmList2(): LrmList = LrmList(id = id2, name = "Lorem List Name (id2)", description = "Lorem List Description", public = true)
  fun lrmList3(): LrmList = LrmList(id = id3, name = "Lorem List Name (id3)", description = "Lorem List Description", public = true)
  fun associationItem1ListId2(): Association = Association(id = UUID.randomUUID(), itemId = lrmItem1().id, listId = id2)

  afterEach { clearAllMocks() }
  afterSpec { unmockkAll() }

  describe("createForItem()") {
    it("associate item with list(s)") {
      val listIds = listOf(lrmList2().id, lrmList3().id)
      every {
        mockLrmItemRepository.findByOwnerAndIdOrNull(
          id = lrmItem1().id,
          owner = ofType(String::class),
        )
      } returns lrmItem1()
      every {
        mockLrmListRepository.notFoundByOwnerAndId(
          listIdCollection = listIds,
          owner = ofType(String::class),
        )
      } returns emptySet()
      every {
        mockAssociationRepository.create(any())
      } returns listOf(
        SuccinctLrmComponentPair(lrmList2().succinct(), lrmItem1().succinct()),
        SuccinctLrmComponentPair(lrmList3().succinct(), lrmItem1().succinct()),
      )
      val response = associationService.create(
        id = lrmItem1().id,
        idCollection = listIds,
        type = LrmComponentType.Item,
        componentOwner = "lorem ipsum",
      )
      response.componentName.shouldBe(lrmItem1().name)
      response.associatedComponents.shouldBe(setOf(lrmList2().succinct(), lrmList3().succinct()))
    }

    it("item not found") {
      every { mockLrmItemRepository.findByOwnerAndIdOrNull(id = ofType(UUID::class), owner = ofType(String::class)) } returns null
      val exception =
        shouldThrow<ApiException> {
          associationService.create(
            id = UUID.randomUUID(),
            idCollection = listOf(UUID.randomUUID()),
            type = LrmComponentType.Item,
            componentOwner = "lorem ipsum",
          )
        }
      exception.cause.shouldBeInstanceOf<ItemNotFoundException>()
      exception.httpStatus.shouldBe(ItemNotFoundException.HTTP_STATUS)
    }

    it("list not found") {
      every { mockLrmItemRepository.findByOwnerAndIdOrNull(id = ofType(UUID::class), owner = ofType(String::class)) } returns lrmItem1()
      every {
        mockLrmListRepository.notFoundByOwnerAndId(listIdCollection = any(), owner = ofType(String::class))
      } returns setOf(UUID.randomUUID())
      val exception =
        shouldThrow<ApiException> {
          associationService.create(
            id = UUID.randomUUID(),
            idCollection = listOf(UUID.randomUUID()),
            type = LrmComponentType.Item,
            componentOwner = "lorem ipsum",
          )
        }
      exception.cause.shouldBeInstanceOf<ListNotFoundException>()
      exception.httpStatus.shouldBe(ListNotFoundException.HTTP_STATUS)
    }

    it("created association count not equal to requested association count") {
      val listIds = listOf(lrmList2().id, lrmList3().id)
      every {
        mockLrmItemRepository.findByOwnerAndIdOrNull(
          id = lrmItem1().id,
          owner = ofType(String::class),
        )
      } returns lrmItem1()
      every {
        mockLrmListRepository.notFoundByOwnerAndId(
          listIdCollection = listIds,
          owner = ofType(String::class),
        )
      } returns emptySet()
      every {
        mockAssociationRepository.create(any())
      } returns listOf(SuccinctLrmComponentPair(lrmList2().succinct(), lrmItem1().succinct()))
      val exception = shouldThrow<ApiException> {
        associationService.create(
          id = lrmItem1().id,
          idCollection = listIds,
          LrmComponentType.Item,
          componentOwner = "lorem ipsum",
        )
      }
      exception.message.shouldContain("created = 1 / requested = 2")
    }

    it("already associated with list (postgresql)") {
      val listIds = listOf(lrmList2().id)
      every {
        mockLrmItemRepository.findByOwnerAndIdOrNull(
          id = lrmItem1().id,
          owner = ofType(String::class),
        )
      } returns lrmItem1()
      every {
        mockLrmListRepository.notFoundByOwnerAndId(
          listIdCollection = listIds,
          owner = ofType(String::class),
        )
      } returns emptySet()
      every { mockAssociationRepository.create(any()) } throws SQLException("duplicate key value violates unique constraint")
      val exception =
        shouldThrow<ApiException> {
          associationService.create(
            id = lrmItem1().id,
            idCollection = listIds,
            LrmComponentType.Item,
            componentOwner = "lorem ipsum",
          )
        }
      exception.cause.shouldBeInstanceOf<SQLException>()
      exception.responseMessage.shouldContain("It already exists.")
    }

    it("already associated with list (h2)") {
      val listIds = listOf(lrmList2().id)
      every {
        mockLrmItemRepository.findByOwnerAndIdOrNull(
          id = lrmItem1().id,
          owner = ofType(String::class),
        )
      } returns lrmItem1()
      every {
        mockLrmListRepository.notFoundByOwnerAndId(
          listIdCollection = listIds,
          owner = ofType(String::class),
        )
      } returns emptySet()
      every { mockAssociationRepository.create(any()) } throws SQLException("Unique index or primary key violation")
      val exception =
        shouldThrow<ApiException> {
          associationService.create(
            id = lrmItem1().id,
            idCollection = listIds,
            LrmComponentType.Item,
            componentOwner = "lorem ipsum",
          )
        }
      exception.cause.shouldBeInstanceOf<SQLException>()
      exception.responseMessage.shouldContain("It already exists.")
    }

    it("unanticipated sql exception (sql exception message is null)") {
      val listIds = listOf(lrmList2().id, lrmList3().id)
      every {
        mockLrmItemRepository.findByOwnerAndIdOrNull(
          id = lrmItem1().id,
          owner = ofType(String::class),
        )
      } returns lrmItem1()
      every {
        mockLrmListRepository.notFoundByOwnerAndId(
          listIdCollection = listIds,
          owner = ofType(String::class),
        )
      } returns emptySet()
      every { mockAssociationRepository.create(any()) } throws SQLException()
      val exception =
        shouldThrow<ApiException> {
          associationService.create(
            id = lrmItem1().id,
            idCollection = listIds,
            LrmComponentType.Item,
            componentOwner = "lorem ipsum",
          )
        }
      exception.cause.shouldBeInstanceOf<SQLException>()
      exception.responseMessage.shouldContain("Unanticipated SQL exception")
    }

    it("unanticipated sql exception (sql exception message is not null)") {
      val listIds = listOf(lrmList2().id, lrmList3().id)
      every {
        mockLrmItemRepository.findByOwnerAndIdOrNull(
          id = lrmItem1().id,
          owner = ofType(String::class),
        )
      } returns lrmItem1()
      every {
        mockLrmListRepository.notFoundByOwnerAndId(
          listIdCollection = listIds,
          owner = ofType(String::class),
        )
      } returns emptySet()
      every { mockAssociationRepository.create(any()) } throws SQLException("Lorem Ipsum")
      val exception =
        shouldThrow<ApiException> {
          associationService.create(
            id = lrmItem1().id,
            idCollection = listIds,
            LrmComponentType.Item,
            componentOwner = "lorem ipsum",
          )
        }
      exception.cause.shouldBeInstanceOf<SQLException>()
      exception.responseMessage.shouldContain("Unanticipated SQL exception")
    }

    it("unanticipated exception") {
      val listIds = listOf(lrmList2().id, lrmList3().id)
      every {
        mockLrmItemRepository.findByOwnerAndIdOrNull(
          id = lrmItem1().id,
          owner = ofType(String::class),
        )
      } returns lrmItem1()
      every {
        mockLrmListRepository.notFoundByOwnerAndId(
          listIdCollection = listIds,
          owner = ofType(String::class),
        )
      } returns emptySet()
      every { mockAssociationRepository.create(any()) } throws Exception("Lorem Ipsum")
      val exception =
        shouldThrow<ApiException> {
          associationService.create(
            id = lrmItem1().id,
            idCollection = listIds,
            LrmComponentType.Item,
            componentOwner = "lorem ipsum",
          )
        }
      exception.cause.shouldBeInstanceOf<Exception>()
    }
  }

  describe("createForList()") {
    it("associate list with item(s)") {
      val itemIds = listOf(lrmItem2().id, lrmItem3().id)
      every {
        mockLrmListRepository.findByOwnerAndIdOrNull(
          id = lrmList1().id,
          owner = "lorem ipsum",
        )
      } returns lrmList1()
      every { mockLrmItemRepository.notFoundById(itemIds) } returns emptySet()
      every { mockAssociationRepository.create(any()) } returns listOf(
        SuccinctLrmComponentPair(lrmList1().succinct(), lrmItem2().succinct()),
        SuccinctLrmComponentPair(lrmList1().succinct(), lrmItem3().succinct()),
      )
      every {
        mockLrmItemRepository.findByOwnerAndIdOrNull(
          id = lrmItem2().id,
          owner = ofType(String::class),
        )
      } returns lrmItem2()
      every {
        mockLrmItemRepository.findByOwnerAndIdOrNull(
          id = lrmItem3().id,
          owner = ofType(String::class),
        )
      } returns lrmItem3()
      val response = associationService.create(
        id = lrmList1().id,
        idCollection = itemIds,
        LrmComponentType.List,
        componentOwner = "lorem ipsum",
      )
      response.componentName.shouldBe(lrmList1().name)
      response.associatedComponents.shouldBe(setOf(lrmItem2().succinct(), lrmItem3().succinct()))
    }

    it("list not found") {
      every { mockLrmListRepository.findByOwnerAndIdOrNull(ofType(UUID::class), ofType(String::class)) } returns null
      val exception =
        shouldThrow<ApiException> {
          associationService.create(
            id = UUID.randomUUID(),
            idCollection = listOf(UUID.randomUUID()),
            type = LrmComponentType.List,
            componentOwner = "lorem ipsum",
          )
        }
      exception.cause.shouldBeInstanceOf<ListNotFoundException>()
      exception.httpStatus.shouldBe(ListNotFoundException.HTTP_STATUS)
    }

    it("item not found") {
      every { mockLrmListRepository.findByOwnerAndIdOrNull(ofType(UUID::class), ofType(String::class)) } returns lrmList1()
      every { mockLrmItemRepository.notFoundById(any()) } returns setOf(UUID.randomUUID())
      val exception =
        shouldThrow<ApiException> {
          associationService.create(
            id = UUID.randomUUID(),
            idCollection = listOf(UUID.randomUUID()),
            type = LrmComponentType.List,
            componentOwner = "lorem ipsum",
          )
        }
      exception.cause.shouldBeInstanceOf<ItemNotFoundException>()
      exception.httpStatus.shouldBe(ItemNotFoundException.HTTP_STATUS)
    }

    it("created association count not equal to requested association count") {
      val itemIds = listOf(lrmItem2().id, lrmItem3().id)
      every { mockLrmListRepository.findByOwnerAndIdOrNull(lrmList1().id, ofType(String::class)) } returns lrmList1()
      every { mockLrmItemRepository.notFoundById(itemIds) } returns emptySet()
      every { mockAssociationRepository.create(any()) } returns listOf(
        SuccinctLrmComponentPair(lrmList1().succinct(), lrmItem2().succinct()),
      )
      val exception = shouldThrow<ApiException> {
        associationService.create(
          id = lrmList1().id,
          idCollection = itemIds,
          LrmComponentType.List,
          componentOwner = "lorem ipsum",
        )
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

  describe("countItemToListByIdAndItemOwner()") {
    it("count of list associations is returned") {
      every {
        mockLrmItemRepository.findByOwnerAndIdOrNull(
          id = lrmItem1().id,
          owner = ofType(String::class),
        )
      } returns lrmItem1()
      every { mockAssociationRepository.countItemToListByIdAndItemOwner(itemId = id1, itemOwner = ofType(String::class)) } returns 1
      associationService.countByIdAndItemOwnerForItem(
        itemId = id1,
        itemOwner = "lorem ipsum item owner",
      )

      verify(exactly = 1) {
        mockAssociationRepository.countItemToListByIdAndItemOwner(itemId = ofType(UUID::class), itemOwner = ofType(String::class))
      }
    }

    it("item is not found") {
      every { mockLrmItemRepository.findByOwnerAndIdOrNull(id = id1, owner = ofType(String::class)) } returns null
      val exception =
        shouldThrow<ApiException> {
          associationService.countByIdAndItemOwnerForItem(
            itemId = id1,
            itemOwner = "lorem ipsum item owner",
          )
        }
      exception.cause.shouldBeInstanceOf<ItemNotFoundException>()
      exception.httpStatus.shouldBe(HttpStatus.NOT_FOUND)
    }

    it("item repository throws exception") {
      every { mockLrmItemRepository.findByOwnerAndIdOrNull(id = id1, owner = ofType(String::class)) } throws RuntimeException("Lorem Ipsum")
      val exception =
        shouldThrow<ApiException> {
          associationService.countByIdAndItemOwnerForItem(itemId = id1, itemOwner = "lorem ipsum")
        }
      exception.cause.shouldBeInstanceOf<RuntimeException>()
      exception.httpStatus.shouldBe(HttpStatus.INTERNAL_SERVER_ERROR)
    }
  }

  describe("countListToItemByIdAndListOwner()") {
    it("count of item associations is returned") {
      every { mockLrmListRepository.findByOwnerAndIdOrNull(id = id1, owner = ofType(String::class)) } returns lrmList2()
      every { mockAssociationRepository.countListToItemByIdandListOwner(listId = id1, listOwner = ofType(String::class)) } returns 1
      associationService.countByOwnerForList(listId = id1, listOwner = "lorem ipsum")
      verify(exactly = 1) {
        mockAssociationRepository.countListToItemByIdandListOwner(listId = ofType(UUID::class), listOwner = ofType(String::class))
      }
    }

    it("list is not found") {
      every { mockLrmListRepository.findByOwnerAndIdOrNull(id1, ofType(String::class)) } returns null
      val exception = shouldThrow<ApiException> { associationService.countByOwnerForList(listId = id1, listOwner = "lorem ipsum") }
      exception.cause.shouldBeInstanceOf<ListNotFoundException>()
      exception.httpStatus.shouldBe(HttpStatus.NOT_FOUND)
    }

    it("list repository throws exception") {
      every { mockLrmListRepository.findByOwnerAndIdOrNull(id = id1, ofType(String::class)) } throws RuntimeException("Lorem Ipsum")
      val exception = shouldThrow<ApiException> { associationService.countByOwnerForList(listId = id1, listOwner = "lorem ipsum") }
      exception.cause.shouldBeInstanceOf<RuntimeException>()
      exception.httpStatus.shouldBe(HttpStatus.INTERNAL_SERVER_ERROR)
    }
  }

  describe("updateItemToList()") {
    it("item is moved from one list to another list") {
      val association = Association(UUID.randomUUID(), itemId = id1, listId = id2)
      val updatedAssociation = association.copy(listId = id3)
      every { mockLrmItemRepository.findByOwnerAndIdOrNull(id = id1, owner = ofType(String::class)) } returns lrmItem1()
      every { mockLrmListRepository.findByOwnerAndIdOrNull(id2, ofType(String::class)) } returns lrmList2()
      every { mockLrmListRepository.findByOwnerAndIdOrNull(id3, ofType(String::class)) } returns lrmList2()
      every { mockAssociationRepository.findByItemIdAndListIdOrNull(id1, id2) } returns association
      every { mockAssociationRepository.update(updatedAssociation) } returns 1
      associationService.updateList(id1, id2, id3, listOwner = "lorem ipsum")
      verify(exactly = 1) { mockLrmItemRepository.findByOwnerAndIdOrNull(id = ofType(UUID::class), owner = ofType(String::class)) }
      verify(exactly = 2) { mockLrmListRepository.findByOwnerAndIdOrNull(id = ofType(UUID::class), owner = ofType(String::class)) }
      verify(exactly = 1) {
        mockAssociationRepository.findByItemIdAndListIdOrNull(itemId = ofType(UUID::class), listId = ofType(UUID::class))
      }
      verify(exactly = 1) { mockAssociationRepository.update(any()) }
    }

    it("item is not found") {
      every { mockLrmItemRepository.findByOwnerAndIdOrNull(id = id1, owner = ofType(String::class)) } returns null
      val exception = shouldThrow<ApiException> { associationService.updateList(id1, id2, id3, listOwner = "lorem ipsum") }
      exception.cause.shouldBeInstanceOf<ItemNotFoundException>()
      verify(exactly = 0) { mockAssociationRepository.update(any()) }
    }

    it("from list is not found") {
      every { mockLrmItemRepository.findByOwnerAndIdOrNull(id = id1, owner = ofType(String::class)) } returns lrmItem1()
      every { mockLrmListRepository.findByOwnerAndIdOrNull(id = id2, owner = ofType(String::class)) } returns null
      val exception = shouldThrow<ApiException> { associationService.updateList(id1, id2, id3, listOwner = "lorem ipsum") }
      exception.cause.shouldBeInstanceOf<ListNotFoundException>()
      verify(exactly = 0) { mockAssociationRepository.update(any()) }
    }

    it("to list is not found") {
      every { mockLrmItemRepository.findByOwnerAndIdOrNull(id = id1, owner = ofType(String::class)) } returns lrmItem1()
      every { mockLrmListRepository.findByOwnerAndIdOrNull(id = id2, owner = ofType(String::class)) } returns lrmList2()
      every { mockLrmListRepository.findByOwnerAndIdOrNull(id = id3, owner = ofType(String::class)) } returns null
      val exception = shouldThrow<ApiException> { associationService.updateList(id1, id2, id3, listOwner = "lorem ipsum") }
      exception.cause.shouldBeInstanceOf<ListNotFoundException>()
      verify(exactly = 0) { mockAssociationRepository.update(any()) }
    }

    it("association is not found") {
      every { mockLrmItemRepository.findByOwnerAndIdOrNull(id = id1, owner = ofType(String::class)) } returns lrmItem1()
      every { mockLrmListRepository.findByOwnerAndIdOrNull(id = id2, owner = ofType(String::class)) } returns lrmList2()
      every { mockLrmListRepository.findByOwnerAndIdOrNull(id = id3, owner = ofType(String::class)) } returns lrmList2()
      every { mockAssociationRepository.findByItemIdAndListIdOrNull(id1, id2) } returns null
      val exception = shouldThrow<ApiException> { associationService.updateList(id1, id2, id3, listOwner = "lorem ipsum") }
      exception.cause.shouldBeInstanceOf<AssociationNotFoundException>()
      verify(exactly = 0) { mockAssociationRepository.update(any()) }
    }

    it("non-api exception is thrown") {
      every { mockLrmItemRepository.findByOwnerAndIdOrNull(id = id1, owner = ofType(String::class)) } returns lrmItem1()
      every { mockLrmListRepository.findByOwnerAndIdOrNull(id = id2, owner = ofType(String::class)) } returns lrmList2()
      every { mockLrmListRepository.findByOwnerAndIdOrNull(id = id3, owner = ofType(String::class)) } returns lrmList2()
      every { mockAssociationRepository.findByItemIdAndListIdOrNull(id1, id2) } throws RuntimeException("Lorem Ipsum")
      val exception = shouldThrow<ApiException> { associationService.updateList(id1, id2, id3, listOwner = "lorem ipsum") }
      exception.cause.shouldBeInstanceOf<RuntimeException>()
      exception.httpStatus.shouldBe(HttpStatus.INTERNAL_SERVER_ERROR)
      exception.responseMessage.shouldBe("Item id $id1 was not moved from list id $id2 to list id $id3.")
      verify(exactly = 1) { mockLrmItemRepository.findByOwnerAndIdOrNull(id = ofType(UUID::class), owner = ofType(String::class)) }
      verify(exactly = 2) { mockLrmListRepository.findByOwnerAndIdOrNull(id = ofType(UUID::class), owner = ofType(String::class)) }
    }
  }

  describe("deleteAll()") {
    it("delete all associations") {
      every { mockAssociationRepository.delete() } returns 999
      associationService.delete().shouldBe(999)
    }

    it("association repository throws exception") {
      every { mockAssociationRepository.delete() } throws RuntimeException("Lorem Ipsum")
      shouldThrow<ApiException> { associationService.delete() }
    }
  }

  describe("deleteAllForItem()") {
    it("remove all associations by item") {
      every { mockAssociationRepository.deleteByItemId(id1) } returns 999
      every { mockLrmItemRepository.findByOwnerAndIdOrNull(id = id1, owner = ofType(String::class)) } returns lrmItem1()
      associationService.deleteByItemOwnerAndItemId(itemId = id1, itemOwner = "lorem ipsum")
      verify(exactly = 1) { mockAssociationRepository.deleteByItemId(any()) }
      verify(exactly = 1) { mockLrmItemRepository.findByOwnerAndIdOrNull(id = ofType(UUID::class), owner = ofType(String::class)) }
    }

    it("item not found") {
      every { mockAssociationRepository.deleteByItemId(id1) } returns 999
      every { mockLrmItemRepository.findByOwnerAndIdOrNull(id = id1, owner = ofType(String::class)) } returns null
      val exception = shouldThrow<ApiException> { associationService.deleteByItemOwnerAndItemId(itemId = id1, itemOwner = "lorem ipsum") }
      exception.cause.shouldBeInstanceOf<ItemNotFoundException>()
      exception.httpStatus.shouldBe(ItemNotFoundException.HTTP_STATUS)
      exception.responseMessage.shouldContain("Item could not be found.")
    }

    it("item repository throws exception") {
      every { mockAssociationRepository.deleteByItemId(id1) } throws RuntimeException()
      val exception = shouldThrow<ApiException> { associationService.deleteByItemOwnerAndItemId(itemId = id1, itemOwner = "lorem ipsum") }
      exception.cause.shouldBeInstanceOf<RuntimeException>()
      exception.httpStatus.shouldBe(HttpStatus.INTERNAL_SERVER_ERROR)
      exception.responseMessage.shouldBe("Item id $id1 could not be removed from any/all lists.")
    }
  }

  describe("deleteAllForList()") {
    it("remove all associations by list") {
      every { mockAssociationRepository.deleteByListId(id1) } returns 999
      every { mockLrmListRepository.findByOwnerAndIdOrNull(id = id1, owner = ofType(String::class)) } returns lrmList2()
      associationService.deleteByListOwnerAndListId(listId = id1, listOwner = "lorem ipsum")
      verify(exactly = 1) { mockAssociationRepository.deleteByListId(any()) }
      verify(exactly = 1) { mockLrmListRepository.findByOwnerAndIdOrNull(id = ofType(UUID::class), owner = ofType(String::class)) }
    }

    it("list not found") {
      every { mockAssociationRepository.deleteByListId(listId = id1) } returns 999
      every { mockLrmListRepository.findByOwnerAndIdOrNull(id = id1, owner = ofType(String::class)) } returns null
      val exception = shouldThrow<ApiException> { associationService.deleteByListOwnerAndListId(listId = id1, listOwner = "lorem ipsum") }
      exception.cause.shouldBeInstanceOf<ListNotFoundException>()
      exception.httpStatus.shouldBe(ListNotFoundException.HTTP_STATUS)
      exception.responseMessage.shouldContain("List could not be found.")
    }

    it("list repository throws exception") {
      every { mockAssociationRepository.deleteByListId(listId = id1) } throws RuntimeException()
      val exception = shouldThrow<ApiException> { associationService.deleteByListOwnerAndListId(listId = id1, listOwner = "lorem ipsum") }
      exception.cause.shouldBeInstanceOf<RuntimeException>()
      exception.httpStatus.shouldBe(HttpStatus.INTERNAL_SERVER_ERROR)
      exception.responseMessage.shouldBe("Could not remove any/all items from List id $id1.")
    }
  }

  describe("removeFromList()") {
    it("removed from list") {
      every { mockLrmItemRepository.findByOwnerAndIdOrNull(id = id1, owner = ofType(String::class)) } returns lrmItem1()
      every { mockLrmListRepository.findByOwnerAndIdOrNull(id = id2, owner = ofType(String::class)) } returns lrmList2()
      every { mockAssociationRepository.findByItemIdAndListIdOrNull(itemId = id1, listId = id2) } returns associationItem1ListId2()
      every { mockAssociationRepository.deleteById(id = ofType(UUID::class)) } returns 1
      associationService.deleteByItemIdAndListId(itemId = id1, listId = id2, componentsOwner = "lorem ipsum")
      verify(exactly = 1) { mockAssociationRepository.deleteById(id = any()) }
      verify(exactly = 1) { mockLrmItemRepository.findByOwnerAndIdOrNull(id = ofType(UUID::class), owner = ofType(String::class)) }
      verify(exactly = 1) { mockLrmListRepository.findByOwnerAndIdOrNull(id = ofType(UUID::class), owner = ofType(String::class)) }
      verify(exactly = 1) { mockAssociationRepository.findByItemIdAndListIdOrNull(itemId = any(), listId = any()) }
    }

    it("item not found") {
      every { mockAssociationRepository.deleteByItemIdAndListId(itemId = id1, listId = id2) } returns 0
      every { mockLrmItemRepository.findByOwnerAndIdOrNull(id = ofType(UUID::class), owner = ofType(String::class)) } returns null
      val exception = shouldThrow<ApiException> {
        associationService.deleteByItemIdAndListId(itemId = id1, listId = id2, componentsOwner = "lorem ipsum")
      }
      exception.cause.shouldBeInstanceOf<ItemNotFoundException>()
      verify(exactly = 1) { mockLrmItemRepository.findByOwnerAndIdOrNull(id = ofType(UUID::class), owner = ofType(String::class)) }
    }

    it("list not found") {
      every { mockLrmItemRepository.findByOwnerAndIdOrNull(id = id1, owner = ofType(String::class)) } returns lrmItem1()
      every { mockLrmListRepository.findByOwnerAndIdOrNull(id = id2, owner = ofType(String::class)) } returns null
      val exception =
        shouldThrow<ApiException> {
          associationService.deleteByItemIdAndListId(
            itemId = id1,
            listId = id2,
            componentsOwner = "lorem ipsum",
          )
        }
      exception.cause.shouldBeInstanceOf<ListNotFoundException>()
      verify(exactly = 1) { mockLrmItemRepository.findByOwnerAndIdOrNull(id = ofType(UUID::class), owner = ofType(String::class)) }
      verify(exactly = 1) { mockLrmListRepository.findByOwnerAndIdOrNull(id = ofType(UUID::class), owner = ofType(String::class)) }
    }

    it("association not found") {
      every { mockLrmItemRepository.findByOwnerAndIdOrNull(id = id1, owner = ofType(String::class)) } returns lrmItem1()
      every { mockLrmListRepository.findByOwnerAndIdOrNull(id = id2, owner = ofType(String::class)) } returns lrmList2()
      every { mockAssociationRepository.findByItemIdAndListIdOrNull(itemId = id1, listId = id2) } returns null
      val exception =
        shouldThrow<ApiException> {
          associationService.deleteByItemIdAndListId(
            itemId = id1,
            listId = id2,
            componentsOwner = "lorem ipsum",
          )
        }
      exception.cause.shouldBeInstanceOf<AssociationNotFoundException>()
      verify(exactly = 1) { mockLrmItemRepository.findByOwnerAndIdOrNull(id = ofType(UUID::class), owner = ofType(String::class)) }
      verify(exactly = 1) { mockLrmListRepository.findByOwnerAndIdOrNull(id = ofType(UUID::class), owner = ofType(String::class)) }
      verify(exactly = 1) { mockAssociationRepository.findByItemIdAndListIdOrNull(itemId = any(), listId = any()) }
    }

    it("association is found but zero records deleted") {
      every { mockLrmItemRepository.findByOwnerAndIdOrNull(id = id1, owner = ofType(String::class)) } returns lrmItem1()
      every { mockLrmListRepository.findByOwnerAndIdOrNull(id = id2, owner = ofType(String::class)) } returns lrmList2()
      every { mockAssociationRepository.findByItemIdAndListIdOrNull(itemId = id1, listId = id2) } returns associationItem1ListId2()
      every { mockAssociationRepository.deleteById(any()) } returns 0
      val exception = shouldThrow<ApiException> {
        associationService.deleteByItemIdAndListId(itemId = id1, listId = id2, componentsOwner = "lorem ipsum")
      }
      exception.cause.shouldBeNull()
      exception.responseMessage.shouldBe(
        "Item id $id1 could not be removed from list id $id2: Item, list, and association were found, but 0 records were deleted.",
      )
      verify(exactly = 1) { mockAssociationRepository.deleteById(id = any()) }
      verify(exactly = 1) { mockLrmItemRepository.findByOwnerAndIdOrNull(id = id1, owner = ofType(String::class)) }
      verify(exactly = 1) { mockLrmListRepository.findByOwnerAndIdOrNull(id = id2, owner = ofType(String::class)) }
      verify(exactly = 1) { mockAssociationRepository.findByItemIdAndListIdOrNull(itemId = any(), listId = any()) }
    }

    it("item is associated with the list multiple times") {
      every { mockLrmItemRepository.findByOwnerAndIdOrNull(id = id1, owner = ofType(String::class)) } returns lrmItem1()
      every { mockLrmListRepository.findByOwnerAndIdOrNull(id = id2, owner = ofType(String::class)) } returns lrmList2()
      every { mockAssociationRepository.findByItemIdAndListIdOrNull(itemId = id1, listId = id2) } returns associationItem1ListId2()
      every { mockAssociationRepository.deleteById(id = any()) } returns 2
      val exception = shouldThrow<ApiException> { associationService.deleteByItemIdAndListId(id1, id2, componentsOwner = "lorem ipsum") }
      exception.cause.shouldBeNull()
      verify(exactly = 1) { mockLrmItemRepository.findByOwnerAndIdOrNull(id = id1, owner = ofType(String::class)) }
      verify(exactly = 1) { mockLrmListRepository.findByOwnerAndIdOrNull(id = id2, owner = ofType(String::class)) }
      verify(exactly = 1) { mockAssociationRepository.findByItemIdAndListIdOrNull(itemId = any(), listId = any()) }
      verify(exactly = 1) { mockAssociationRepository.deleteById(id = any()) }
    }

    it("item repository throws exception") {
      every { mockLrmItemRepository.findByOwnerAndIdOrNull(id = id1, owner = ofType(String::class)) } returns lrmItem1()
      every { mockLrmListRepository.findByOwnerAndIdOrNull(id = id2, owner = ofType(String::class)) } returns lrmList2()
      every { mockAssociationRepository.findByItemIdAndListIdOrNull(itemId = id1, listId = id2) } returns associationItem1ListId2()
      every { mockAssociationRepository.deleteByItemIdAndListId(id1, id2) } throws Exception("Lorem Ipsum")
      val exception =
        shouldThrow<ApiException> {
          associationService.deleteByItemIdAndListId(
            itemId = id1,
            listId = id2,
            componentsOwner = "lorem ipsum",
          )
        }
      exception.cause.shouldBeInstanceOf<Exception>()
      exception.httpStatus.shouldBe(HttpStatus.INTERNAL_SERVER_ERROR)
      exception.message.shouldBe("Item id $id1 could not be removed from list id $id2.")
      exception.responseMessage.shouldBe("Item id $id1 could not be removed from list id $id2.")
      exception.title.shouldBe(ApiException::class.java.simpleName)
    }
  }
})
