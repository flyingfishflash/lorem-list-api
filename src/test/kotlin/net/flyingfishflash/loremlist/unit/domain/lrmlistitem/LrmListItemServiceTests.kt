package net.flyingfishflash.loremlist.unit.domain.lrmlistitem

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import net.flyingfishflash.loremlist.domain.exceptions.DomainException
import net.flyingfishflash.loremlist.domain.lrmitem.ItemNotFoundException
import net.flyingfishflash.loremlist.domain.lrmitem.LrmItem
import net.flyingfishflash.loremlist.domain.lrmitem.LrmItemRepository
import net.flyingfishflash.loremlist.domain.lrmitem.LrmItemSuccinct
import net.flyingfishflash.loremlist.domain.lrmitem.data.LrmItemCreate
import net.flyingfishflash.loremlist.domain.lrmlist.ListNotFoundException
import net.flyingfishflash.loremlist.domain.lrmlist.LrmList
import net.flyingfishflash.loremlist.domain.lrmlist.LrmListRepository
import net.flyingfishflash.loremlist.domain.lrmlist.LrmListSuccinct
import net.flyingfishflash.loremlist.domain.lrmlistitem.ListItemNotFoundException
import net.flyingfishflash.loremlist.domain.lrmlistitem.LrmListItem
import net.flyingfishflash.loremlist.domain.lrmlistitem.LrmListItemRepository
import net.flyingfishflash.loremlist.domain.lrmlistitem.LrmListItemServiceDefault
import net.flyingfishflash.loremlist.persistence.SuccinctLrmComponentPair
import org.springframework.http.HttpStatus
import java.sql.SQLException
import java.util.*

class LrmListItemServiceTests :
  DescribeSpec({
    val mockLrmListItemRepository = mockk<LrmListItemRepository>()
    val mockLrmItemRepository = mockk<LrmItemRepository>()
    val mockLrmListRepository = mockk<LrmListRepository>()
    val mockLrmList = mockk<LrmList>()
    val mockLrmItem1 = mockk<LrmItem>()
    val mockLrmItem2 = mockk<LrmItem>()
    val mockLrmListItem = mockk<LrmListItem>(relaxed = true)

    val lrmListItemService = LrmListItemServiceDefault(mockLrmItemRepository, mockLrmListRepository, mockLrmListItemRepository)
    val owner = "lorem ipsum"

    afterEach { clearAllMocks() }
    afterSpec { unmockkAll() }

    describe("countByOwnerAndListId()") {
      val listId = UUID.randomUUID()

      it("return expected count when list found") {
        every { mockLrmListRepository.findByOwnerAndIdOrNull(id = listId, owner = owner) } returns mockLrmList
        every { mockLrmListItemRepository.countByOwnerAndListId(listId = listId, listOwner = owner) } returns 1
        val response = lrmListItemService.countByOwnerAndListId(listId = listId, owner = owner)
        response.content shouldBe 1L
        response.message shouldBe "List is associated with 1 items."
        verify { mockLrmListItemRepository.countByOwnerAndListId(listId, owner) }
      }

      it("throw exception when list not found") {
        every { mockLrmListRepository.findByOwnerAndIdOrNull(id = listId, owner = owner) } returns null
        val exception = shouldThrow<DomainException> { lrmListItemService.countByOwnerAndListId(listId, owner) }
        exception.cause.shouldBeInstanceOf<ListNotFoundException>()
        exception.httpStatus shouldBe HttpStatus.NOT_FOUND
      }

      it("throw exception when list repository throws an exception") {
        every { mockLrmListRepository.findByOwnerAndIdOrNull(id = listId, owner = "lorem ipsum") } throws RuntimeException("Error")
        val exception = shouldThrow<DomainException> { lrmListItemService.countByOwnerAndListId(listId, "lorem ipsum") }
        exception.cause.shouldBeInstanceOf<RuntimeException>()
        exception.httpStatus shouldBe HttpStatus.INTERNAL_SERVER_ERROR
      }
    }

    describe("findByOwnerAndItemIdAndListId()") {
      val listId = UUID.randomUUID()
      val itemId = UUID.randomUUID()

      it("return correct list item when valid item is found") {
        every { mockLrmListItem.name } returns "Lorem List Item"
        every { mockLrmListRepository.findByOwnerAndIdOrNull(id = listId, owner = owner) } returns mockLrmList
        every { mockLrmListItemRepository.findByOwnerAndItemIdAndListIdOrNull(itemId = itemId, listId = listId, owner = owner) } returns mockLrmListItem
        val response = lrmListItemService.findByOwnerAndItemIdAndListId(itemId = itemId, listId = listId, owner = owner)
        response.content shouldBe mockLrmListItem
        response.message shouldBe "Retrieved list item 'Lorem List Item'"
      }

      it("throw ListNotFoundException when list is not found") {
        every { mockLrmListRepository.findByOwnerAndIdOrNull(id = listId, owner = owner) } returns null
        shouldThrow<ListNotFoundException> {
          lrmListItemService.findByOwnerAndItemIdAndListId(itemId = itemId, listId = listId, owner = owner)
        }
      }

      it("throw ItemNotFoundException when item is not found") {
        every { mockLrmListRepository.findByOwnerAndIdOrNull(id = listId, owner = owner) } returns mockLrmList
        every { mockLrmListItemRepository.findByOwnerAndItemIdAndListIdOrNull(itemId = itemId, listId = listId, owner = owner) } returns null
        shouldThrow<ListItemNotFoundException> {
          lrmListItemService.findByOwnerAndItemIdAndListId(itemId = itemId, listId = listId, owner = owner)
        }
      }
    }

    describe("add()") {
      val listId = UUID.randomUUID()
      val itemId1 = UUID.randomUUID()
      val itemId2 = UUID.randomUUID()

      it("catch/rethrow IllegalStateException when item ids are empty") {
        val exception = shouldThrow<DomainException> { lrmListItemService.add(id = listId, idCollection = emptyList(), componentsOwner = owner) }
        exception.cause.shouldBeInstanceOf<IllegalStateException>()
      }

      it("catch/rethrow ListNotFoundException when list not found") {
        every { mockLrmListRepository.findByOwnerAndIdOrNull(id = listId, owner = owner) } returns null
        val exception = shouldThrow<DomainException> { lrmListItemService.add(id = listId, idCollection = listOf(itemId1), componentsOwner = owner) }
        exception.cause.shouldBeInstanceOf<ListNotFoundException>()
      }

      it("catch/rethrow ItemNotFoundException when item not found") {
        every { mockLrmListRepository.findByOwnerAndIdOrNull(id = listId, owner = owner) } returns mockLrmList
        every { mockLrmItemRepository.notFoundByOwnerAndId(itemIdCollection = listOf(itemId1), owner = owner) } returns setOf(itemId1)
        val exception = shouldThrow<DomainException> { lrmListItemService.add(id = listId, idCollection = listOf(itemId1), componentsOwner = owner) }
        exception.cause.shouldBeInstanceOf<ItemNotFoundException>()
      }

      context("create associations successfully") {
        beforeEach {
          every { mockLrmListRepository.findByOwnerAndIdOrNull(id = listId, owner = owner) } returns mockLrmList
          every { mockLrmItemRepository.findByOwnerAndIdOrNull(id = itemId1, owner = owner) } returns mockLrmItem1
          every { mockLrmItem1.id } returns itemId1
          every { mockLrmItem1.name } returns "Item1"
          every { mockLrmItem2.id } returns itemId2
          every { mockLrmItem2.name } returns "Item2"
          every { mockLrmList.id } returns listId
          every { mockLrmList.name } returns "List1"
        }

        it("when a single item id is provided") {
          every { mockLrmItemRepository.notFoundByOwnerAndId(itemIdCollection = listOf(itemId1), owner = owner) } returns emptySet()
          every { mockLrmListItemRepository.create(any()) } returns
            listOf(
              SuccinctLrmComponentPair(list = LrmListSuccinct.fromLrmList(mockLrmList), item = LrmItemSuccinct.fromLrmItem(mockLrmItem1)),
            )
          val response = lrmListItemService.add(id = listId, idCollection = listOf(itemId1), componentsOwner = owner)
          response.message shouldContain "to item"
          response.content.items shouldBe setOf(LrmItemSuccinct.fromLrmItem(mockLrmItem1))
        }

        it("when multiple items id's are provided") {
          every { mockLrmItemRepository.notFoundByOwnerAndId(itemIdCollection = listOf(itemId1, itemId2), owner = owner) } returns emptySet()
          every { mockLrmListItemRepository.create(any()) } returns listOf(
            SuccinctLrmComponentPair(list = LrmListSuccinct.fromLrmList(mockLrmList), item = LrmItemSuccinct.fromLrmItem(mockLrmItem1)),
            SuccinctLrmComponentPair(list = LrmListSuccinct.fromLrmList(mockLrmList), item = LrmItemSuccinct.fromLrmItem(mockLrmItem2)),
          )
          val response = lrmListItemService.add(listId, listOf(itemId1, itemId2), owner)
          response.message shouldContain "2 items"
          response.content.items shouldBe setOf(LrmItemSuccinct.fromLrmItem(mockLrmItem1), LrmItemSuccinct.fromLrmItem(mockLrmItem2))
        }
      }

      it("throw DomainException when created association count not equal to requested association count") {
        every { mockLrmList.id } returns listId
        every { mockLrmList.name } returns "LrmList"
        every { mockLrmItem1.id } returns itemId1
        every { mockLrmItem1.name } returns "LrmItem"
        every { mockLrmListRepository.findByOwnerAndIdOrNull(id = listId, owner = owner) } returns mockLrmList
        every { mockLrmItemRepository.notFoundByOwnerAndId(itemIdCollection = listOf(itemId1, itemId2), owner = owner) } returns emptySet()
        every { mockLrmListItemRepository.create(any()) } returns
          listOf(
            SuccinctLrmComponentPair(list = LrmListSuccinct.fromLrmList(mockLrmList), item = LrmItemSuccinct.fromLrmItem(mockLrmItem1)),
          )
        val exception = shouldThrow<DomainException> { lrmListItemService.add(id = listId, idCollection = listOf(itemId1, itemId2), componentsOwner = owner) }
        exception.message.shouldContain("created = 1 / requested = 2")
      }

      context("catch/rethrow DomainException when repository throws SQLException") {
        beforeEach {
          every { mockLrmListRepository.findByOwnerAndIdOrNull(id = listId, owner = owner) } returns mockLrmList
          every { mockLrmItemRepository.notFoundByOwnerAndId(itemIdCollection = listOf(itemId1), owner = owner) } returns emptySet()
        }

        it("h2 -> Unique index or primary key violation") {
          every { mockLrmListItemRepository.create(any()) } throws SQLException("Unique index or primary key violation")
          val exception = shouldThrow<DomainException> { lrmListItemService.add(id = listId, idCollection = listOf(itemId1), componentsOwner = owner) }
          exception.cause.shouldBeInstanceOf<SQLException>()
          exception.responseMessage.shouldContain("It already exists.")
        }

        it("pg -> duplicate key value violates unique constraint") {
          every { mockLrmListItemRepository.create(any()) } throws SQLException("duplicate key value violates unique constraint")
          val exception = shouldThrow<DomainException> { lrmListItemService.add(id = listId, idCollection = listOf(itemId1), componentsOwner = "lorem ipsum") }
          exception.cause.shouldBeInstanceOf<SQLException>()
          exception.responseMessage.shouldContain("It already exists.")
        }

        it("other sql exception") {
          every { mockLrmListItemRepository.create(any()) } throws SQLException("other sql exception")
          val exception = shouldThrow<DomainException> { lrmListItemService.add(id = listId, idCollection = listOf(itemId1), componentsOwner = "lorem ipsum") }
          exception.cause.shouldBeInstanceOf<SQLException>()
          exception.responseMessage.shouldContain("Unanticipated SQL exception")
        }
      }

      it("catch/rethrow DomainException when repository throws RuntimeException") {
        every { mockLrmListItemRepository.create(any()) } throws RuntimeException("Repository Exception")
        val exception = shouldThrow<DomainException> { lrmListItemService.add(id = listId, idCollection = listOf(itemId1), componentsOwner = "lorem ipsum") }
        exception.cause.shouldBeInstanceOf<RuntimeException>()
        exception.responseMessage.shouldContain("Could not create a new association")
      }
    }

    describe("create()") {
      val listId = UUID.randomUUID()
      val itemId = UUID.randomUUID()
      val mockSuccinctLrmComponentPair = mockk<SuccinctLrmComponentPair>(relaxed = true)
      val lrmItemCreate = LrmItemCreate(name = "UZ4p3ClnTd", isSuppressed = false)
      val mockSuccinctItemName = "mock succinct item component name"
      val mockListName = "mock lorem list name"

      beforeEach {
        every { mockLrmItemRepository.insert(lrmItem = ofType<LrmItem>()) } returns itemId
      }

      it("create item and assign it to a list") {
        every { mockLrmItem1.id } returns itemId
        every { mockLrmList.name } returns mockListName
        every { mockLrmListItem.name } returns "Zmi22CTcJ4"
        every { mockLrmItemRepository.findByOwnerAndIdOrNull(id = itemId, owner = owner) } returns mockLrmItem1
        every { mockLrmListRepository.findByOwnerAndIdOrNull(id = listId, owner = owner) } returns mockLrmList
        every { mockLrmItemRepository.notFoundByOwnerAndId(itemIdCollection = listOf(itemId), owner = owner) } returns emptySet()
        every { mockLrmListItemRepository.create(associationCollection = setOf(Pair(listId, itemId))) } returns listOf(mockSuccinctLrmComponentPair)
        every { mockSuccinctLrmComponentPair.item.name } returns mockSuccinctItemName
        every { mockLrmListItemRepository.findByOwnerAndItemIdAndListIdOrNull(itemId = itemId, listId = listId, owner = owner) } returns mockLrmListItem
        every { mockLrmListItemRepository.updateQuantity(lrmListItem = ofType<LrmListItem>()) } returns 1
        every { mockLrmListItemRepository.updateIsItemSuppressed(lrmListItem = ofType<LrmListItem>()) } returns 1
        val response = lrmListItemService.create(listId = listId, lrmItemCreate = lrmItemCreate, creator = owner)
        response.message shouldBe "Created item '$mockSuccinctItemName' and assigned it to list '$mockListName'"
        response.content shouldBe mockLrmListItem
      }

      it("item cannot be created") {
        every { mockLrmItemRepository.findByOwnerAndIdOrNull(id = itemId, owner = owner) } returns null
        val exception = shouldThrow<DomainException> { lrmListItemService.create(listId = listId, lrmItemCreate = lrmItemCreate, creator = owner) }
        exception.cause.shouldBeInstanceOf<ItemNotFoundException>()
      }
    }

    describe("removeByOwnerAndListIdAndItemId()") {
      val itemId = UUID.randomUUID()
      val listId = UUID.randomUUID()
      val listItemId = UUID.randomUUID()

      beforeEach {
        every { mockLrmItem1.name } returns "Item1"
        every { mockLrmList.name } returns "List1"
      }

      it("remove an item successfully from the list") {
        every { mockLrmItemRepository.findByOwnerAndIdOrNull(id = itemId, owner = owner) } returns mockLrmItem1
        every { mockLrmListRepository.findByOwnerAndIdOrNull(id = listId, owner = owner) } returns mockLrmList
        every { mockLrmListItemRepository.findByOwnerAndItemIdAndListIdOrNull(itemId = itemId, listId = listId, owner = owner) } returns mockLrmListItem
        every { mockLrmListItemRepository.removeByOwnerAndListIdAndItemId(listId = listId, itemId = itemId, owner = owner) } returns 1
        val response = lrmListItemService.removeByOwnerAndListIdAndItemId(listId = listId, itemId = itemId, owner = owner)
        response.content shouldBe Pair("Item1", "List1")
        response.message shouldBe "Removed item 'Item1' from list 'List1'"
      }

      it("throw an exception when the item is not found") {
        every { mockLrmItemRepository.findByOwnerAndIdOrNull(id = itemId, owner = owner) } returns null
        val exception = shouldThrow<DomainException> { lrmListItemService.removeByOwnerAndListIdAndItemId(listId = listId, itemId = itemId, owner = owner) }
        exception.cause.shouldBeInstanceOf<ItemNotFoundException>()
      }

      it("throw an exception when the list is not found") {
        every { mockLrmItemRepository.findByOwnerAndIdOrNull(id = itemId, owner = owner) } returns mockLrmItem1
        every { mockLrmListRepository.findByOwnerAndIdOrNull(id = listId, owner = owner) } returns null
        val exception = shouldThrow<DomainException> { lrmListItemService.removeByOwnerAndListIdAndItemId(listId = listId, itemId = itemId, owner = owner) }
        exception.cause.shouldBeInstanceOf<ListNotFoundException>()
      }

      it("throw an exception when no association is found") {
        every { mockLrmItemRepository.findByOwnerAndIdOrNull(id = itemId, owner = owner) } returns mockLrmItem1
        every { mockLrmListRepository.findByOwnerAndIdOrNull(id = listId, owner = owner) } returns mockLrmList
        every { mockLrmListItemRepository.findByOwnerAndItemIdAndListIdOrNull(itemId = itemId, listId = listId, owner = owner) } returns null
        val exception = shouldThrow<DomainException> { lrmListItemService.removeByOwnerAndListIdAndItemId(listId = listId, itemId = itemId, owner = owner) }
        exception.cause.shouldBeInstanceOf<ListItemNotFoundException>()
      }

      it("throw an exception when a repository exception occurs") {
        every { mockLrmListItem.id } returns listItemId
        every { mockLrmItemRepository.findByOwnerAndIdOrNull(id = itemId, owner = owner) } returns mockLrmItem1
        every { mockLrmListRepository.findByOwnerAndIdOrNull(id = listId, owner = owner) } returns mockLrmList
        every { mockLrmListItemRepository.findByOwnerAndItemIdAndListIdOrNull(itemId = itemId, listId = listId, owner = owner) } returns mockLrmListItem
        every { mockLrmListItemRepository.removeByOwnerAndListIdAndItemId(listId = listId, itemId = itemId, owner = owner) } throws
          RuntimeException("Repository Exception")
        val exception = shouldThrow<DomainException> { lrmListItemService.removeByOwnerAndListIdAndItemId(listId = listId, itemId = itemId, owner = owner) }
        exception.cause.shouldBeInstanceOf<RuntimeException>()
        (exception.cause as RuntimeException).message.shouldBe("Repository Exception")
      }

      it("throw an exception when no records are deleted") {
        every { mockLrmListItem.id } returns listItemId
        every { mockLrmItemRepository.findByOwnerAndIdOrNull(id = itemId, owner = owner) } returns mockLrmItem1
        every { mockLrmListRepository.findByOwnerAndIdOrNull(id = listId, owner = owner) } returns mockLrmList
        every { mockLrmListItemRepository.findByOwnerAndItemIdAndListIdOrNull(itemId = itemId, listId = listId, owner = owner) } returns mockLrmListItem
        every { mockLrmListItemRepository.removeByOwnerAndListIdAndItemId(listId = listId, itemId = itemId, owner = owner) } returns 0
        shouldThrow<DomainException> { lrmListItemService.removeByOwnerAndListIdAndItemId(listId = listId, itemId = itemId, owner = owner) }
      }

      it("throw an exception when more than one record is deleted") {
        every { mockLrmItemRepository.findByOwnerAndIdOrNull(id = itemId, owner = owner) } returns mockLrmItem1
        every { mockLrmListRepository.findByOwnerAndIdOrNull(id = listId, owner = owner) } returns mockLrmList
        every { mockLrmListItemRepository.findByOwnerAndItemIdAndListIdOrNull(itemId = itemId, listId = listId, owner = owner) } returns mockLrmListItem
        every { mockLrmListItemRepository.removeByOwnerAndListIdAndItemId(listId = listId, itemId = itemId, owner = owner) } returns 2
        shouldThrow<DomainException> { lrmListItemService.removeByOwnerAndListIdAndItemId(listId = listId, itemId = itemId, owner = owner) }
      }
    }

    describe("removeByOwnerAndItemId()") {
      val itemId = UUID.randomUUID()

      it("remove items successfully from the list") {
        every { mockLrmItem1.name } returns "Item1"
        every { mockLrmItemRepository.findByOwnerAndIdOrNull(id = itemId, owner = owner) } returns mockLrmItem1
        every { mockLrmListItemRepository.removeByOwnerAndItemId(itemId = itemId, owner = owner) } returns 3
        val response = lrmListItemService.removeByOwnerAndItemId(itemId = itemId, owner = owner)
        response.content shouldBe Pair("Item1", 3)
        response.message shouldBe "Removed '${mockLrmItem1.name}' from 3 lists."
      }

      it("return correct response when one item is removed from multiple lists") {
        every { mockLrmItem1.name } returns "Item1"
        every { mockLrmItemRepository.findByOwnerAndIdOrNull(id = itemId, owner = owner) } returns mockLrmItem1
        every { mockLrmListItemRepository.removeByOwnerAndItemId(itemId = itemId, owner = owner) } returns 1
        val response = lrmListItemService.removeByOwnerAndItemId(itemId = itemId, owner = owner)
        response.content shouldBe Pair("Item1", 1)
        response.message shouldBe "Removed '${mockLrmItem1.name}' from 1 list."
      }

      it("throw an exception if the item is not found") {
        every { mockLrmItemRepository.findByOwnerAndIdOrNull(id = itemId, owner = owner) } returns null
        val exception = shouldThrow<DomainException> { lrmListItemService.removeByOwnerAndItemId(itemId = itemId, owner = owner) }
        exception.cause.shouldBeInstanceOf<ItemNotFoundException>()
      }

      it("throw a DomainException when an unknown error occurs") {
        every { mockLrmItemRepository.findByOwnerAndIdOrNull(id = itemId, owner = owner) } returns mockLrmItem1
        every { mockLrmListItemRepository.removeByOwnerAndListId(listId = itemId, owner = owner) } throws RuntimeException("Unknown error")
        shouldThrow<DomainException> { lrmListItemService.removeByOwnerAndItemId(itemId = itemId, owner = owner) }
      }
    }

    describe("removeByOwnerAndListId()") {
      val listId = UUID.randomUUID()

      it("remove items successfully from the list") {
        every { mockLrmList.name } returns "List1"
        every { mockLrmListRepository.findByOwnerAndIdOrNull(id = listId, owner = owner) } returns mockLrmList
        every { mockLrmListItemRepository.removeByOwnerAndListId(listId = listId, owner = owner) } returns 3
        val response = lrmListItemService.removeByOwnerAndListId(listId = listId, owner = owner)
        response.content shouldBe Pair("List1", 3)
        response.message shouldBe "Removed 3 items from list 'List1'"
      }

      it("return correct response when one item is removed") {
        every { mockLrmList.name } returns "List1"
        every { mockLrmListRepository.findByOwnerAndIdOrNull(id = listId, owner = owner) } returns mockLrmList
        every { mockLrmListItemRepository.removeByOwnerAndListId(listId = listId, owner = owner) } returns 1
        val response = lrmListItemService.removeByOwnerAndListId(listId = listId, owner = owner)
        response.content shouldBe Pair("List1", 1)
        response.message shouldBe "Removed 1 item from list 'List1'"
      }

      it("throw an exception if the list is not found") {
        every { mockLrmListRepository.findByOwnerAndIdOrNull(id = listId, owner = owner) } returns null
        val exception = shouldThrow<DomainException> { lrmListItemService.removeByOwnerAndListId(listId = listId, owner = owner) }
        exception.cause.shouldBeInstanceOf<ListNotFoundException>()
      }

      it("throw a DomainException when an unknown error occurs") {
        every { mockLrmListRepository.findByOwnerAndIdOrNull(id = listId, owner = owner) } returns mockLrmList
        every { mockLrmListItemRepository.removeByOwnerAndListId(listId = listId, owner = owner) } throws RuntimeException("Unknown error")
        shouldThrow<DomainException> { lrmListItemService.removeByOwnerAndListId(listId = listId, owner = owner) }
      }
    }

    describe("move()") {
      val itemId = UUID.randomUUID()
      val currentListId = UUID.randomUUID()
      val destinationListId = UUID.randomUUID()
      val mockCurrentList = mockk<LrmList>()
      val mockDestinationList = mockk<LrmList>()

      it("move the item from the current list to the destination list successfully") {
        every { mockLrmItem1.name } returns "Item1"
        every { mockCurrentList.name } returns "CurrentList"
        every { mockDestinationList.name } returns "DestinationList"
        every { mockLrmItemRepository.findByOwnerAndIdOrNull(id = itemId, owner = owner) } returns mockLrmItem1
        every { mockLrmListRepository.findByOwnerAndIdOrNull(id = currentListId, owner = owner) } returns mockCurrentList
        every { mockLrmListRepository.findByOwnerAndIdOrNull(id = destinationListId, owner = owner) } returns mockDestinationList
        every { mockLrmListItemRepository.findByOwnerAndItemIdAndListIdOrNull(itemId = itemId, listId = currentListId, owner = owner) } returns mockLrmListItem
        every { mockLrmListItemRepository.updateListId(lrmListItem = mockLrmListItem, destinationListId = destinationListId) } returns 1
        val response = lrmListItemService.move(itemId = itemId, currentListId = currentListId, destinationListId = destinationListId, owner = owner)
        response.content shouldBe Triple("Item1", "CurrentList", "DestinationList")
        response.message shouldBe "Moved item 'Item1' from list 'CurrentList' to list 'DestinationList'"
      }

      it("throw an exception when the item is not found") {
        every { mockLrmItemRepository.findByOwnerAndIdOrNull(id = itemId, owner = owner) } returns null
        val exception = shouldThrow<DomainException> {
          lrmListItemService.move(itemId = itemId, currentListId = currentListId, destinationListId = destinationListId, owner = owner)
        }
        exception.cause.shouldBeInstanceOf<ItemNotFoundException>()
      }

      it("throw an exception when the current list is not found") {
        every { mockLrmItemRepository.findByOwnerAndIdOrNull(id = itemId, owner = owner) } returns mockLrmItem1
        every { mockLrmListRepository.findByOwnerAndIdOrNull(id = currentListId, owner = owner) } returns null
        val exception = shouldThrow<DomainException> {
          lrmListItemService.move(itemId = itemId, currentListId = currentListId, destinationListId = destinationListId, owner = owner)
        }
        exception.cause.shouldBeInstanceOf<ListNotFoundException>()
      }

      it("throw an exception when the destination list is not found") {
        every { mockLrmItemRepository.findByOwnerAndIdOrNull(id = itemId, owner = owner) } returns mockLrmItem1
        every { mockLrmListRepository.findByOwnerAndIdOrNull(id = currentListId, owner = owner) } returns mockCurrentList
        every { mockLrmListRepository.findByOwnerAndIdOrNull(id = destinationListId, owner = owner) } returns null
        val exception = shouldThrow<DomainException> {
          lrmListItemService.move(itemId = itemId, currentListId = currentListId, destinationListId = destinationListId, owner = owner)
        }
        exception.cause.shouldBeInstanceOf<ListNotFoundException>()
      }

      it("throw an exception when no association is found between the item and the current list") {
        every { mockLrmItemRepository.findByOwnerAndIdOrNull(id = itemId, owner = owner) } returns mockLrmItem1
        every { mockLrmListRepository.findByOwnerAndIdOrNull(id = currentListId, owner = owner) } returns mockCurrentList
        every { mockLrmListRepository.findByOwnerAndIdOrNull(id = destinationListId, owner = owner) } returns mockDestinationList
        every { mockLrmListItemRepository.findByOwnerAndItemIdAndListIdOrNull(itemId = itemId, listId = currentListId, owner = owner) } returns null
        val exception = shouldThrow<DomainException> {
          lrmListItemService.move(itemId = itemId, currentListId = currentListId, destinationListId = destinationListId, owner = owner)
        }
        exception.cause.shouldBeInstanceOf<ListItemNotFoundException>()
      }

      it("throw an exception when an unexpected error occurs") {
        every { mockLrmItemRepository.findByOwnerAndIdOrNull(id = itemId, owner = owner) } returns mockLrmItem1
        every { mockLrmListRepository.findByOwnerAndIdOrNull(id = currentListId, owner = owner) } returns mockCurrentList
        every { mockLrmListRepository.findByOwnerAndIdOrNull(id = destinationListId, owner = owner) } returns mockDestinationList
        every { mockLrmListItemRepository.findByOwnerAndItemIdAndListIdOrNull(itemId = itemId, listId = currentListId, owner = owner) } returns mockLrmListItem
        every { mockLrmListItemRepository.updateListId(lrmListItem = mockLrmListItem, destinationListId = destinationListId) } throws
          RuntimeException("Unknown error")
        val exception = shouldThrow<DomainException> {
          lrmListItemService.move(itemId = itemId, currentListId = currentListId, destinationListId = destinationListId, owner = owner)
        }
        exception.cause.shouldBeInstanceOf<RuntimeException>()
      }
    }

    describe("countByOwnerAndItemId()") {
      val itemId = UUID.randomUUID()

      it("return the expected count of list associations when item is found") {
        val expectedCount = 5L
        every { mockLrmItemRepository.findByOwnerAndIdOrNull(id = itemId, owner = owner) } returns mockLrmItem1 // Assuming this returns a valid item
        every { mockLrmListItemRepository.countByOwnerAndItemId(itemId = itemId, itemOwner = owner) } returns expectedCount
        val response = lrmListItemService.countByOwnerAndItemId(itemId = itemId, owner = owner)
        response.content shouldBe expectedCount
        response.message shouldBe "Item is associated with $expectedCount lists."
      }

      it("throw exception when item not found") {
        every { mockLrmItemRepository.findByOwnerAndIdOrNull(id = itemId, owner = owner) } returns null
        val exception = shouldThrow<DomainException> { lrmListItemService.countByOwnerAndItemId(itemId = itemId, owner = owner) }
        exception.cause.shouldBeInstanceOf<ItemNotFoundException>()
        exception.httpStatus shouldBe HttpStatus.NOT_FOUND
      }

      it("throw exception when item repository throws an exception") {
        every { mockLrmListRepository.findByOwnerAndIdOrNull(id = itemId, owner = "lorem ipsum") } throws RuntimeException("Error")
        val exception = shouldThrow<DomainException> { lrmListItemService.countByOwnerAndListId(listId = itemId, owner = "lorem ipsum") }
        exception.cause.shouldBeInstanceOf<RuntimeException>()
        exception.httpStatus shouldBe HttpStatus.INTERNAL_SERVER_ERROR
      }
    }
  })
