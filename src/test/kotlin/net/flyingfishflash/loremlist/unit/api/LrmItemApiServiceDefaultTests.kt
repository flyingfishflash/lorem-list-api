package net.flyingfishflash.loremlist.unit.api

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.datetime.Clock.System.now
import net.flyingfishflash.loremlist.api.LrmItemApiServiceDefault
import net.flyingfishflash.loremlist.api.data.request.LrmItemCreateRequest
import net.flyingfishflash.loremlist.api.data.response.ApiServiceResponse
import net.flyingfishflash.loremlist.api.data.response.LrmItemDeletedResponse
import net.flyingfishflash.loremlist.api.data.response.LrmItemResponse
import net.flyingfishflash.loremlist.core.response.structure.ApiMessageNumeric
import net.flyingfishflash.loremlist.domain.LrmComponentType
import net.flyingfishflash.loremlist.domain.ServiceResponse
import net.flyingfishflash.loremlist.domain.association.AssociationService
import net.flyingfishflash.loremlist.domain.association.data.AssociationCreated
import net.flyingfishflash.loremlist.domain.lrmitem.LrmItem
import net.flyingfishflash.loremlist.domain.lrmitem.LrmItemServiceDefault
import net.flyingfishflash.loremlist.domain.lrmitem.data.LrmItemCreate
import net.flyingfishflash.loremlist.domain.lrmitem.data.LrmItemDeleted
import net.flyingfishflash.loremlist.domain.lrmlist.LrmListSuccinct
import java.util.*

class LrmItemApiServiceDefaultTests :
  DescribeSpec({
    val mockLrmItemService = mockk<LrmItemServiceDefault>(relaxed = true)
    val mockAssociationService = mockk<AssociationService>(relaxed = true)
    val lrmItemApiService = LrmItemApiServiceDefault(mockLrmItemService, mockAssociationService)

    val now = now()
    val id0 = UUID.fromString("00000000-0000-4000-a000-000000000000")
    val lrmItemCreateRequest = LrmItemCreateRequest("Lorem Item Name", "Lorem Item Description")
    val irrelevantMessage = "ksADs8y96KRa1Zo4ipMdr5t8faudmFj4c564S02MjsNG6TXEO7yctC08Bb53bCB7"

    fun lrmItem(): LrmItem = LrmItem(
      id = id0,
      name = lrmItemCreateRequest.name,
      description = lrmItemCreateRequest.description,
      quantity = 0,
      created = now,
      createdBy = "Lorem Ipsum Created By",
      updated = now,
      updatedBy = "Lorem Ipsum Updated By",
    )

    afterEach { clearAllMocks() }
    afterSpec { unmockkAll() }

    describe("LrmItemApiServiceDefault") {
      it("count items by owner") {
        val owner = "test_owner"
        val expectedCount = 10L
        val serviceResponse = ServiceResponse(expectedCount, message = irrelevantMessage)
        val apiServiceResponse = ApiServiceResponse(content = ApiMessageNumeric(serviceResponse.content), message = serviceResponse.message)
        every { mockLrmItemService.countByOwner(owner) } returns serviceResponse
        lrmItemApiService.countByOwner(owner) shouldBe apiServiceResponse
        verify { mockLrmItemService.countByOwner(owner) }
      }

      it("create a new item") {
        val owner = "test_owner"
        val serviceResponse = ServiceResponse(content = lrmItem(), message = irrelevantMessage)
        val apiServiceResponse = ApiServiceResponse(LrmItemResponse.fromLrmItem(serviceResponse.content), serviceResponse.message)
        every { mockLrmItemService.create(ofType(LrmItemCreate::class), owner) } returns serviceResponse
        lrmItemApiService.create(mockk<LrmItemCreateRequest>(relaxed = true), owner) shouldBe apiServiceResponse
        verify { mockLrmItemService.create(ofType(LrmItemCreate::class), owner) }
      }

      it("delete items by owner") {
        // TODO: evaluate the veracity of this test
        val owner = "test_owner"
        val serviceResponse = ServiceResponse(
          content = LrmItemDeleted(listOf("test_item_name"), listOf("test_list_name")),
          message = irrelevantMessage,
        )
        val apiServiceResponse = ApiServiceResponse(
          content = LrmItemDeletedResponse(
            itemNames = serviceResponse.content.itemNames,
            associatedListNames = serviceResponse.content.associatedListNames,
          ),
          message = serviceResponse.message,
        )
        every { mockLrmItemService.deleteByOwner(owner) } returns serviceResponse
        lrmItemApiService.deleteByOwner(owner) shouldBe apiServiceResponse
        verify { mockLrmItemService.deleteByOwner(owner) }
      }

      it("delete item by owner and id") {
        val id = UUID.randomUUID()
        val owner = "test_owner"
        val listName = "Lorem List Name"
        val removeListAssociations = true
        val serviceResponse = ServiceResponse(
          content = LrmItemDeleted(listOf(lrmItem().name), listOf(listName)),
          message = irrelevantMessage,
        )
        val apiServiceResponse = ApiServiceResponse(
          content = LrmItemDeletedResponse(
            itemNames = serviceResponse.content.itemNames,
            associatedListNames = serviceResponse.content.associatedListNames,
          ),
          message = serviceResponse.message,
        )
        every { mockLrmItemService.deleteByOwnerAndId(id, owner, removeListAssociations) } returns serviceResponse
        lrmItemApiService.deleteByOwnerAndId(id, owner, removeListAssociations) shouldBe apiServiceResponse
        verify { mockLrmItemService.deleteByOwnerAndId(id, owner, removeListAssociations) }
      }

      it("find items by owner") {
        val owner = "test_owner"
        val serviceResponse = ServiceResponse(content = listOf(lrmItem()), message = irrelevantMessage)
        val apiServiceResponse = ApiServiceResponse(
          content = serviceResponse.content.map { LrmItemResponse.fromLrmItem(it) },
          message = serviceResponse.message,
        )
        every { mockLrmItemService.findByOwner(owner) } returns serviceResponse
        lrmItemApiService.findByOwner(owner) shouldBe apiServiceResponse
        verify { mockLrmItemService.findByOwner(owner) }
      }

      it("find item by owner and id") {
        val owner = "test_owner"
        val serviceResponse = ServiceResponse(content = lrmItem(), message = irrelevantMessage)
        val apiServiceResponse = ApiServiceResponse(
          content = LrmItemResponse.fromLrmItem(serviceResponse.content),
          message = serviceResponse.message,
        )
        every { mockLrmItemService.findByOwnerAndId(id0, owner) } returns serviceResponse
        lrmItemApiService.findByOwnerAndId(id0, owner) shouldBe apiServiceResponse
        verify { mockLrmItemService.findByOwnerAndId(id0, owner) }
      }

      it("find items by owner having no list associations") {
        val owner = "test_owner"
        val serviceResponse = ServiceResponse(content = listOf(lrmItem()), message = irrelevantMessage)
        val apiServiceResponse = ApiServiceResponse(
          content = serviceResponse.content.map { LrmItemResponse.fromLrmItem(lrmItem()) },
          message = serviceResponse.message,
        )
        every { mockLrmItemService.findByOwnerAndHavingNoListAssociations(owner) } returns serviceResponse
        lrmItemApiService.findByOwnerAndHavingNoListAssociations(owner) shouldBe apiServiceResponse
        verify { mockLrmItemService.findByOwnerAndHavingNoListAssociations(owner) }
      }

      it("patch an item by owner and id") {
        val owner = "test_owner"
        val updatedItemName = "Updated Item Name"
        val updatedItemDescription = "Updated Item Description"
        val updatedItemQuantity = 10000
        val patchRequest = mapOf("name" to updatedItemName, "description" to updatedItemDescription, "quantity" to updatedItemQuantity)
        val originalLrmItem = lrmItem()
        val updatedLrmItem = lrmItem().copy(name = updatedItemName, description = updatedItemDescription, quantity = updatedItemQuantity)
        every { mockLrmItemService.findByOwnerAndId(id0, owner) } returns ServiceResponse(originalLrmItem, irrelevantMessage) andThen
          ServiceResponse(updatedLrmItem, irrelevantMessage)
        every { mockLrmItemService.patchName(any()) } returns Unit
        every { mockLrmItemService.patchDescription(any()) } returns Unit
        every { mockLrmItemService.patchQuantity(any()) } returns Unit
        val apiServiceResponse = lrmItemApiService.patchByOwnerAndId(id0, owner, patchRequest)
        apiServiceResponse.content shouldBe LrmItemResponse.fromLrmItem(updatedLrmItem)
        apiServiceResponse.message shouldBe "Item 'Updated Item Name' updated. Fields changed: name, description, quantity."
        verify { mockLrmItemService.patchName(any()) }
        verify { mockLrmItemService.patchDescription(any()) }
        verify { mockLrmItemService.patchQuantity(any()) }
        verify { mockLrmItemService.findByOwnerAndId(id0, owner) }
      }

      it("not patch an item by owner and id") {
        val owner = "test_owner"
        val updatedItemName = lrmItem().name
        val updatedItemDescription = lrmItem().description.toString()
        val updatedItemQuantity = lrmItem().quantity
        val patchRequest = mapOf("name" to updatedItemName, "description" to updatedItemDescription, "quantity" to updatedItemQuantity)
        val originalLrmItem = lrmItem()
        val updatedLrmItem = lrmItem().copy()
        every { mockLrmItemService.findByOwnerAndId(id0, owner) } returns ServiceResponse(originalLrmItem, irrelevantMessage) andThen
          ServiceResponse(updatedLrmItem, irrelevantMessage)
        every { mockLrmItemService.patchName(any()) } returns Unit
        every { mockLrmItemService.patchDescription(any()) } returns Unit
        every { mockLrmItemService.patchQuantity(any()) } returns Unit
        val apiServiceResponse = lrmItemApiService.patchByOwnerAndId(id0, owner, patchRequest)
        apiServiceResponse.content shouldBe LrmItemResponse.fromLrmItem(updatedLrmItem)
        apiServiceResponse.message shouldBe "Item '${updatedLrmItem.name}' not updated."
        verify(exactly = 0) { mockLrmItemService.patchName(any()) }
        verify(exactly = 0) { mockLrmItemService.patchDescription(any()) }
        verify(exactly = 0) { mockLrmItemService.patchQuantity(any()) }
        verify(exactly = 2) { mockLrmItemService.findByOwnerAndId(id0, owner) }
      }

      it("throw exception for unsupported patch field") {
        val owner = "test_owner"
        val patchRequest = mapOf("unsupportedField" to "value")
        every { mockLrmItemService.findByOwnerAndId(id0, owner) } returns ServiceResponse(lrmItem(), irrelevantMessage)
        val exception = shouldThrow<IllegalArgumentException> {
          lrmItemApiService.patchByOwnerAndId(id0, owner, patchRequest)
        }
        exception.message shouldBe "Patch operation is not supported on field: unsupportedField"
      }

      it("count list associations by item id and item owner") {
        val itemCount = 999L
        val serviceResponse = ServiceResponse(content = itemCount, message = irrelevantMessage)
        every {
          mockAssociationService.countByIdAndItemOwnerForItem(itemId = ofType(UUID::class), itemOwner = ofType(String::class))
        } returns serviceResponse
        val apiServiceResponse = lrmItemApiService.countListAssociationsByItemIdAndItemOwner(id0, "test_owner")
        apiServiceResponse.content.value shouldBe itemCount
        apiServiceResponse.message shouldBe irrelevantMessage
      }

      it("create list associations (single)") {
        val serviceResponse = ServiceResponse(
          AssociationCreated(
            componentName = lrmItem().name,
            associatedComponents = listOf(LrmListSuccinct(id = UUID.randomUUID(), "J8hVJW9PxHXWHVtO")),
          ),
          message = irrelevantMessage,
        )

        every {
          mockAssociationService.create(
            id = ofType<UUID>(),
            idCollection = ofType<List<UUID>>(),
            type = LrmComponentType.Item,
            componentsOwner = ofType<String>(),
          )
        } returns serviceResponse

        val apiServiceResponse = lrmItemApiService.createListAssociations(
          itemId = UUID.randomUUID(),
          listIdCollection = setOf(UUID.randomUUID()),
          owner = "test_owner",
        )

        apiServiceResponse.content.componentName shouldBe serviceResponse.content.componentName
        apiServiceResponse.content.associatedComponents shouldBe serviceResponse.content.associatedComponents
        apiServiceResponse.message shouldBe irrelevantMessage
      }

      it("create list associations (multiple)") {
        val serviceResponse = ServiceResponse(
          AssociationCreated(
            componentName = lrmItem().name,
            associatedComponents = listOf(
              LrmListSuccinct(id = UUID.randomUUID(), "JQq4Uc1IYphEltHB"),
              LrmListSuccinct(id = UUID.randomUUID(), "J8hVJW9PxHXWHVtO"),
            ),
          ),
          message = irrelevantMessage,
        )

        every {
          mockAssociationService.create(
            id = ofType<UUID>(),
            idCollection = ofType<List<UUID>>(),
            type = LrmComponentType.Item,
            componentsOwner = ofType<String>(),
          )
        } returns serviceResponse

        val apiServiceResponse = lrmItemApiService.createListAssociations(
          itemId = UUID.randomUUID(),
          listIdCollection = setOf(UUID.randomUUID()),
          owner = "test_owner",
        )

        apiServiceResponse.content.componentName shouldBe serviceResponse.content.componentName
        apiServiceResponse.content.associatedComponents shouldBe serviceResponse.content.associatedComponents
        apiServiceResponse.message shouldBe irrelevantMessage
      }

      it("delete list association by item id and list id and item owner") {
        val itemId = UUID.randomUUID()
        val listId = UUID.randomUUID()
        val itemOwner = "59kl3ulHWzunEvyP"
        val serviceResponse = ServiceResponse(Pair("Item 1", "List A"), message = irrelevantMessage)
        every { mockAssociationService.deleteByItemIdAndListId(ofType<UUID>(), ofType<UUID>(), ofType<String>()) } returns serviceResponse
        val apiServiceResponse = lrmItemApiService.deleteListAssociationByItemIdAndListIdAndItemOwner(itemId, listId, itemOwner)
        apiServiceResponse.content.itemName shouldBe serviceResponse.content.first
        apiServiceResponse.content.listName shouldBe serviceResponse.content.second
        apiServiceResponse.message shouldBe irrelevantMessage
      }

      it("delete list associations by item id and item owner") {
        val itemId = UUID.randomUUID()
        val itemOwner = "b4QXzRAxsMHi7oHd"
        val serviceResponse = ServiceResponse(content = Pair(lrmItem().name, 3), message = irrelevantMessage)
        every { mockAssociationService.deleteByItemOwnerAndItemId(itemId, itemOwner) } returns serviceResponse
        val apiServiceResponse = lrmItemApiService.deleteListAssociationsByItemIdAndItemOwner(itemId, itemOwner)
        apiServiceResponse.content.itemName shouldBe serviceResponse.content.first
        apiServiceResponse.content.deletedAssociationsCount shouldBe serviceResponse.content.second
//        apiServiceResponse.message shouldBe "Removed item 'Lorem Item Name' from all associated lists (3)."
      }

      it("update list association") {
        val itemId = UUID.randomUUID()
        val currentListId = UUID.randomUUID()
        val destinationListId = UUID.randomUUID()
        val owner = "owner123"
        val serviceResponse = ServiceResponse(content = Triple("Item 1", "List A", "List B"), message = irrelevantMessage)
        every {
          mockAssociationService.updateList(
            itemId = itemId,
            currentListId = currentListId,
            destinationListId = destinationListId,
            componentsOwner = owner,
          )
        } returns serviceResponse
        val apiServiceResponse = lrmItemApiService.updateListAssociation(itemId, currentListId, destinationListId, owner)
        apiServiceResponse.content.itemName shouldBe "Item 1"
        apiServiceResponse.content.currentListName shouldBe "List A"
        apiServiceResponse.content.newListName shouldBe "List B"
        apiServiceResponse.message shouldBe irrelevantMessage
      }
    }
  })
