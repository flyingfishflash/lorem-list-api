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
import net.flyingfishflash.loremlist.domain.ServiceResponse
import net.flyingfishflash.loremlist.domain.lrmitem.LrmItem
import net.flyingfishflash.loremlist.domain.lrmitem.LrmItemServiceDefault
import net.flyingfishflash.loremlist.domain.lrmitem.data.LrmItemDeleted
import net.flyingfishflash.loremlist.domain.lrmlistitem.LrmListItemService
import java.util.*

class LrmItemApiServiceDefaultTests :
  DescribeSpec({
    val mockLrmItemService = mockk<LrmItemServiceDefault>(relaxed = true)
    val mockLrmListItemService = mockk<LrmListItemService>(relaxed = true)
    val lrmItemApiService = LrmItemApiServiceDefault(mockLrmItemService, mockLrmListItemService)

    val now = now()
    val id0 = UUID.fromString("00000000-0000-4000-a000-000000000000")
    val lrmItemCreateRequest = LrmItemCreateRequest(name = "Lorem Item Name", description = "Lorem Item Description", isSuppressed = false)
    val irrelevantMessage = "ksADs8y96KRa1Zo4ipMdr5t8faudmFj4c564S02MjsNG6TXEO7yctC08Bb53bCB7"

    fun lrmItem(): LrmItem = LrmItem(
      id = id0,
      name = lrmItemCreateRequest.name,
      description = lrmItemCreateRequest.description,
      owner = "Lorem Ipsum Owner",
      created = now,
      creator = "Lorem Ipsum Created By",
      updated = now,
      updater = "Lorem Ipsum Updated By",
    )

    afterEach { clearAllMocks() }
    afterSpec { unmockkAll() }

    describe("LrmItemApiServiceDefault") {
      it("count items by owner") {
        val owner = "test_owner"
        val expectedCount = 10L
        val serviceResponse = ServiceResponse(expectedCount, message = irrelevantMessage)
        val apiServiceResponse =
          ApiServiceResponse(content = ApiMessageNumeric(serviceResponse.content), message = serviceResponse.message)
        every { mockLrmItemService.countByOwner(owner) } returns serviceResponse
        lrmItemApiService.countByOwner(owner) shouldBe apiServiceResponse
        verify { mockLrmItemService.countByOwner(owner) }
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
//        val updatedItemQuantity = 10000
        val patchRequest = mapOf(
          "name" to updatedItemName,
          "description" to updatedItemDescription,
//          "quantity" to updatedItemQuantity
        )
        val originalLrmItem = lrmItem()
        val updatedLrmItem = lrmItem().copy(
          name = updatedItemName,
          description = updatedItemDescription,
          // quantity = updatedItemQuantity
        )
        every { mockLrmItemService.findByOwnerAndId(id0, owner) } returns ServiceResponse(
          originalLrmItem,
          irrelevantMessage,
        ) andThen
          ServiceResponse(updatedLrmItem, irrelevantMessage)
        every { mockLrmItemService.patchName(any()) } returns Unit
        every { mockLrmItemService.patchDescription(any()) } returns Unit
//        every { mockLrmItemService.patchQuantity(any()) } returns Unit
        val apiServiceResponse = lrmItemApiService.patchByOwnerAndId(id0, owner, patchRequest)
        apiServiceResponse.content shouldBe LrmItemResponse.fromLrmItem(updatedLrmItem)
        apiServiceResponse.message shouldBe "Item 'Updated Item Name' updated. Fields changed: name, description."
        verify { mockLrmItemService.patchName(any()) }
        verify { mockLrmItemService.patchDescription(any()) }
//        verify { mockLrmItemService.patchQuantity(any()) }
        verify { mockLrmItemService.findByOwnerAndId(id0, owner) }
      }

      it("not patch an item by owner and id") {
        val owner = "test_owner"
        val updatedItemName = lrmItem().name
        val updatedItemDescription = lrmItem().description.toString()
//        val updatedItemQuantity = lrmItem().quantity
        val patchRequest = mapOf(
          "name" to updatedItemName,
          "description" to updatedItemDescription,
//          "quantity" to updatedItemQuantity
        )
        val originalLrmItem = lrmItem()
        val updatedLrmItem = lrmItem().copy()
        every { mockLrmItemService.findByOwnerAndId(id0, owner) } returns ServiceResponse(
          originalLrmItem,
          irrelevantMessage,
        ) andThen
          ServiceResponse(updatedLrmItem, irrelevantMessage)
        every { mockLrmItemService.patchName(any()) } returns Unit
        every { mockLrmItemService.patchDescription(any()) } returns Unit
//        every { mockLrmItemService.patchQuantity(any()) } returns Unit
        val apiServiceResponse = lrmItemApiService.patchByOwnerAndId(id0, owner, patchRequest)
        apiServiceResponse.content shouldBe LrmItemResponse.fromLrmItem(updatedLrmItem)
        apiServiceResponse.message shouldBe "Item '${updatedLrmItem.name}' not updated."
        verify(exactly = 0) { mockLrmItemService.patchName(any()) }
        verify(exactly = 0) { mockLrmItemService.patchDescription(any()) }
//        verify(exactly = 0) { mockLrmItemService.patchQuantity(any()) }
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
          mockLrmListItemService.countByOwnerAndItemId(
            itemId = ofType(UUID::class),
            owner = ofType(String::class),
          )
        } returns serviceResponse
        val apiServiceResponse = lrmItemApiService.countListAssociationsByItemIdAndItemOwner(id0, "test_owner")
        apiServiceResponse.content.value shouldBe itemCount
        apiServiceResponse.message shouldBe irrelevantMessage
      }
    }
  })
