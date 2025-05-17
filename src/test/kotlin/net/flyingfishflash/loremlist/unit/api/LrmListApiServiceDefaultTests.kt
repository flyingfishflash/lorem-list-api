package net.flyingfishflash.loremlist.unit.api

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.datetime.Clock.System.now
import net.flyingfishflash.loremlist.api.LrmListApiServiceDefault
import net.flyingfishflash.loremlist.api.data.request.LrmItemCreateRequest
import net.flyingfishflash.loremlist.api.data.response.ApiServiceResponse
import net.flyingfishflash.loremlist.api.data.response.AssociationsDeletedResponse
import net.flyingfishflash.loremlist.api.data.response.LrmListDeletedResponse
import net.flyingfishflash.loremlist.api.data.response.LrmListResponse
import net.flyingfishflash.loremlist.core.response.structure.ApiMessageNumeric
import net.flyingfishflash.loremlist.domain.ServiceResponse
import net.flyingfishflash.loremlist.domain.lrmitem.LrmItemSuccinct
import net.flyingfishflash.loremlist.domain.lrmitem.data.LrmItemCreate
import net.flyingfishflash.loremlist.domain.lrmlist.LrmList
import net.flyingfishflash.loremlist.domain.lrmlist.LrmListServiceDefault
import net.flyingfishflash.loremlist.domain.lrmlist.data.LrmListCreate
import net.flyingfishflash.loremlist.domain.lrmlist.data.LrmListDeleted
import net.flyingfishflash.loremlist.domain.lrmlistitem.LrmListItem
import net.flyingfishflash.loremlist.domain.lrmlistitem.LrmListItemService
import net.flyingfishflash.loremlist.domain.lrmlistitem.data.LrmListItemAdded
import java.util.UUID

class LrmListApiServiceDefaultTests :
  DescribeSpec({
    val mockLrmListService = mockk<LrmListServiceDefault>()
    val mockLrmListItemService = mockk<LrmListItemService>()
    val mockLrmList = mockk<LrmList>(relaxed = true)
    val mockLrmListItem = mockk<LrmListItem>(relaxed = true)
    val lrmListApiService = LrmListApiServiceDefault(mockLrmListService, mockLrmListItemService)

    val id0 = UUID.fromString("00000000-0000-4000-a000-000000000000")
    val id1 = UUID.fromString("00000000-0000-4000-a000-000000000001")
    val serviceResponseMessage = "ksADs8y96KRa1Zo4ipMdr5t8faudmFj4c564S02MjsNG6TXEO7yctC08Bb53bCB7"
    val owner = "owner"
    val now = now()

    fun createLrmList(id: UUID, nameSuffix: String = "", items: Set<LrmListItem> = emptySet()) = LrmList(
      id = id,
      name = "Lorem List Name${if (nameSuffix.isNotEmpty()) " ($nameSuffix)" else ""}",
      description = "Lorem List Description",
      public = true,
      owner = "Lorem Ipsum Owner",
      created = now,
      creator = "Lorem Ipsum Created By",
      updated = now,
      updater = "Lorem Ipsum Updated By",
      items = items,
    )

    afterEach { clearAllMocks() }
    afterSpec { unmockkAll() }

    describe("LrmListApiServiceDefault") {
      context("list") {
        it("count lists by owner") {
          val serviceResponse = ServiceResponse(content = 10L, message = serviceResponseMessage)
          val apiServiceResponse = ApiServiceResponse(ApiMessageNumeric(value = serviceResponse.content), serviceResponse.message)
          every { mockLrmListService.countByOwner(owner) } returns serviceResponse
          lrmListApiService.countByOwner(owner) shouldBe apiServiceResponse
        }

        it("create a new list") {
          val serviceResponse = ServiceResponse(content = mockLrmList, message = serviceResponseMessage)
          val apiServiceResponse = ApiServiceResponse(content = LrmListResponse.fromLrmList(serviceResponse.content), message = serviceResponse.message)
          every { mockLrmListService.create(lrmListCreate = ofType<LrmListCreate>(), creator = owner) } returns serviceResponse
          lrmListApiService.create(mockk(relaxed = true), owner) shouldBe apiServiceResponse
        }

        it("delete lists by owner") {
          val serviceResponse = ServiceResponse(content = mockk<LrmListDeleted>(relaxed = true), message = serviceResponseMessage)
          val listDeletedResponse = LrmListDeletedResponse(
            listNames = serviceResponse.content.listNames,
            associatedItemNames = serviceResponse.content.associatedItemNames,
          )
          val apiServiceResponse = ApiServiceResponse(content = listDeletedResponse, message = serviceResponse.message)
          every { mockLrmListService.deleteByOwner(owner) } returns serviceResponse
          lrmListApiService.deleteByOwner(owner) shouldBe apiServiceResponse
        }

        it("delete list by owner and id") {
          val serviceResponse = ServiceResponse(content = mockk<LrmListDeleted>(relaxed = true), message = serviceResponseMessage)
          val listDeletedResponse = LrmListDeletedResponse(
            listNames = serviceResponse.content.listNames,
            associatedItemNames = serviceResponse.content.associatedItemNames,
          )
          val apiServiceResponse = ApiServiceResponse(content = listDeletedResponse, message = serviceResponse.message)
          every { mockLrmListService.deleteByOwnerAndId(id = id0, owner = owner, removeItemAssociations = false) } returns serviceResponse
          lrmListApiService.deleteByOwnerAndId(id = id0, owner = owner, removeItemAssociations = false) shouldBe apiServiceResponse
        }

        it("find lists by owner") {
          val serviceResponse = ServiceResponse(listOf(element = mockLrmList), message = serviceResponseMessage)
          val apiServiceResponseContent = serviceResponse.content.map { LrmListResponse.fromLrmList(it) }
          val apiServiceResponse = ApiServiceResponse(content = apiServiceResponseContent, message = serviceResponse.message)
          every { mockLrmListService.findByOwner(owner) } returns serviceResponse
          lrmListApiService.findByOwner(owner) shouldBe apiServiceResponse
        }

        it("find lists by owner and exclude items") {
          val serviceResponseContent = listOf(createLrmList(id = id0, items = setOf(mockLrmListItem)))
          val serviceResponse = ServiceResponse(serviceResponseContent, message = serviceResponseMessage)
          serviceResponse.content.size shouldBe 1
          serviceResponse.content[0].items shouldNotBe emptySet<LrmListItem>()
          val apiServiceResponseContent = serviceResponse.content.map { LrmListResponse.fromLrmList(it.copy(items = emptySet())) }
          val apiServiceResponse = ApiServiceResponse(content = apiServiceResponseContent, message = serviceResponse.message)
          every { mockLrmListService.findByOwner(owner) } returns serviceResponse
          lrmListApiService.findByOwnerExcludeItems(owner) shouldBe apiServiceResponse
        }

        it("find lists by owner and having no item associations") {
          val serviceResponse = ServiceResponse(listOf(element = mockLrmList), message = serviceResponseMessage)
          val apiServiceResponseContent = serviceResponse.content.map { LrmListResponse.fromLrmList(it) }
          val apiServiceResponse = ApiServiceResponse(content = apiServiceResponseContent, message = serviceResponse.message)
          every { mockLrmListService.findByOwnerAndHavingNoItemAssociations(owner) } returns serviceResponse
          lrmListApiService.findByOwnerAndHavingNoItemAssociations(owner) shouldBe apiServiceResponse
        }

        it("find list by owner and id") {
          val serviceResponse = ServiceResponse(content = mockLrmList, message = serviceResponseMessage)
          val apiServiceResponseContent = LrmListResponse.fromLrmList(serviceResponse.content)
          val apiServiceResponse = ApiServiceResponse(content = apiServiceResponseContent, message = serviceResponse.message)
          every { mockLrmListService.findByOwnerAndId(id = id0, owner = owner) } returns serviceResponse
          lrmListApiService.findByOwnerAndId(id = id0, owner = owner) shouldBe apiServiceResponse
        }

        it("find list by owner and id and exclude items") {
          val serviceResponseContent = createLrmList(id = id0, items = setOf(mockLrmListItem))
          val serviceResponse = ServiceResponse(content = serviceResponseContent, message = serviceResponseMessage)
          val apiServiceResponseContent = LrmListResponse.fromLrmList(serviceResponseContent.copy(items = emptySet()))
          val apiServiceResponse = ApiServiceResponse(content = apiServiceResponseContent, message = serviceResponse.message)
          every { mockLrmListService.findByOwnerAndId(owner = owner, id = id0) } returns serviceResponse
          lrmListApiService.findByOwnerAndIdExcludeItems(owner = owner, id = id0) shouldBe apiServiceResponse
        }

        it("find lists by public indicator") {
          val serviceResponse = ServiceResponse(content = listOf(element = mockLrmList), message = serviceResponseMessage)
          val apiServiceResponseContent = serviceResponse.content.map { LrmListResponse.fromLrmList(it) }
          val apiServiceResponse = ApiServiceResponse(content = apiServiceResponseContent, message = serviceResponse.message)
          every { mockLrmListService.findByPublic() } returns serviceResponse
          lrmListApiService.findByPublic() shouldBe apiServiceResponse
        }

        it("find lists by public indicator and exclude items") {
          val serviceResponseContent = listOf(createLrmList(id = id0, items = setOf(mockLrmListItem)))
          val serviceResponse = ServiceResponse(content = serviceResponseContent, message = serviceResponseMessage)
          val apiServiceResponseContent = serviceResponse.content.map { LrmListResponse.fromLrmList(it.copy(items = emptySet())) }
          val apiServiceResponse = ApiServiceResponse(content = apiServiceResponseContent, message = serviceResponse.message)
          every { mockLrmListService.findByPublic() } returns serviceResponse
          lrmListApiService.findByPublicExcludeItems() shouldBe apiServiceResponse
        }

        context("patch list by id and owner") {
          it("list is updated") {
            val patchRequest = mapOf("name" to "Updated Name", "description" to "Updated Description", "public" to true)
            val updatedList = mockLrmList.copy(name = "Updated Name", description = "Updated Description", public = true)
            every { mockLrmListService.findByOwnerAndId(id = id0, owner = owner) } returns
              ServiceResponse(mockLrmList, message = serviceResponseMessage) andThen
              ServiceResponse(updatedList, message = serviceResponseMessage)
            every { mockLrmListService.patchName(any()) } just Runs
            every { mockLrmListService.patchDescription(any()) } just Runs
            every { mockLrmListService.patchIsPublic(any()) } just Runs
            val result = lrmListApiService.patchByOwnerAndId(id = id0, owner = owner, patchRequest = patchRequest)
            result.content shouldBe LrmListResponse.fromLrmList(updatedList)
          }

          it("list is not updated when patch request contains up-to-date values") {
            val patchRequest = mapOf("name" to "Updated Name", "description" to "Updated Description", "public" to true)
            val serviceResponse = ServiceResponse(content = mockLrmList, message = serviceResponseMessage)
            every { mockLrmListService.findByOwnerAndId(id = id0, owner = owner) } returns serviceResponse
            every { mockLrmListService.patchName(any()) } just Runs
            every { mockLrmListService.patchDescription(any()) } just Runs
            every { mockLrmListService.patchIsPublic(any()) } just Runs
            val result = lrmListApiService.patchByOwnerAndId(id = id0, owner = owner, patchRequest = patchRequest)
            result.content shouldBe LrmListResponse.fromLrmList(mockLrmList)
            result.message shouldContain "not updated"
          }

          it("throw exception for unsupported patch field") {
            val patchRequest = mapOf("unsupportedField" to "value")
            every { mockLrmListService.findByOwnerAndId(id = id0, owner = owner) } returns
              ServiceResponse(content = mockLrmList, message = serviceResponseMessage)
            val exception = shouldThrow<IllegalArgumentException> { lrmListApiService.patchByOwnerAndId(id = id0, owner = owner, patchRequest = patchRequest) }
            exception.message shouldBe "Patch operation is not supported on field: unsupportedField"
          }
        }
      }

      context("list item") {
        it("count list items by list id and owner") {
          every { mockLrmListItemService.countByOwnerAndListId(listId = id0, owner = owner) } returns
            ServiceResponse(content = 99L, message = serviceResponseMessage)
          lrmListApiService.countListItems(id0, owner)
        }

        it("add list item") {
          val serviceResponseContent = LrmListItemAdded(
            listName = "Lorem Ipsum",
            items = listOf(LrmItemSuccinct(id = id0, name = "Lorem Item")),
          )
          val serviceResponse = ServiceResponse(content = serviceResponseContent, message = serviceResponseMessage)
          every { mockLrmListItemService.add(id = id0, idCollection = listOf(id0), componentsOwner = owner) } returns serviceResponse
          lrmListApiService.addListItem(listId = id0, itemIdCollection = setOf(id0), owner = owner)
        }

        it("create list item") {
          val mockLrmItemCreateRequest = mockk<LrmItemCreateRequest>(relaxed = true)

          val serviceResponse = ServiceResponse(content = mockLrmListItem, message = serviceResponseMessage)
          every { mockLrmListItemService.create(listId = id0, lrmItemCreate = ofType<LrmItemCreate>(), creator = owner) } returns serviceResponse
          lrmListApiService.createListItem(listId = id0, itemCreateRequest = mockLrmItemCreateRequest, creator = owner)
        }

        it("remove list item by item id and list id") {
          every { mockLrmListItemService.removeByOwnerAndListIdAndItemId(listId = id1, itemId = id0, owner = owner) } returns
            ServiceResponse(Pair(first = "Lorem", second = "Ipsum"), message = serviceResponseMessage)
          lrmListApiService.removeListItem(listId = id1, itemId = id0, componentsOwner = owner)
        }

        it("remove all list items") {
          val serviceResponse = ServiceResponse(content = Pair("", 9), message = serviceResponseMessage)
          val apiResponseContent = AssociationsDeletedResponse(
            itemName = serviceResponse.content.first,
            deletedAssociationsCount = serviceResponse.content.second,
          )
          val apiServiceResponse = ApiServiceResponse(content = apiResponseContent, message = serviceResponse.message)
          every { mockLrmListItemService.removeByOwnerAndListId(listId = id0, owner = owner) } returns serviceResponse
          lrmListApiService.removeAllListItems(listId = id0, listOwner = owner) shouldBe apiServiceResponse
        }

        it("move list item") {
          val listId2 = UUID.fromString("00000000-0000-4000-a000-000000000002")
          val serviceResponse = ServiceResponse(content = Triple("Item 1", "List A", "List B"), message = serviceResponseMessage)
          every { mockLrmListItemService.move(itemId = id0, currentListId = id1, destinationListId = listId2, owner = owner) } returns serviceResponse
          val result = lrmListApiService.moveListItem(itemId = id0, listId = id1, destinationListId = listId2, owner = owner)
          result.content.itemName shouldBe serviceResponse.content.first
          result.content.currentListName shouldBe serviceResponse.content.second
          result.content.newListName shouldBe serviceResponse.content.third
          result.message shouldBe serviceResponse.message
        }

        context("patch list item by list id, item id and owner") {
          it("list item is updated") {
            val mockPatchedLrmListItem = mockk<LrmListItem>(relaxed = true)
            val patchRequest = mapOf(
              "name" to "Updated Name",
              "description" to "Updated Description",
              "quantity" to 7,
              "isSuppressed" to true,
            )
            every { mockLrmListItem.name } returns "Item Name"
            every { mockPatchedLrmListItem.name } returns "Updated Item Name"
            every { mockLrmListItemService.findByOwnerAndItemIdAndListId(itemId = id0, listId = id1, owner = owner) } returns
              ServiceResponse(content = mockLrmListItem, message = serviceResponseMessage) andThen
              ServiceResponse(content = mockPatchedLrmListItem, message = serviceResponseMessage)
            every { mockLrmListItemService.patchName(any()) } just Runs
            every { mockLrmListItemService.patchDescription(any()) } just Runs
            every { mockLrmListItemService.patchQuantity(any()) } just Runs
            every { mockLrmListItemService.patchIsSuppressed(any()) } just Runs
            val response = lrmListApiService.patchListItem(listId = id1, itemId = id0, listOwner = owner, patchRequest = patchRequest)
            response.message shouldBe "Item 'Updated Item Name' updated. Fields changed: name, description, quantity, isSuppressed."
          }

          it("list item is not updated when patch request is empty") {
            val mockPatchRequest = mockk<Map<String, Any>>(relaxed = true)
            every { mockLrmListItem.name } returns "Item Name"
            every { mockLrmListItemService.findByOwnerAndItemIdAndListId(itemId = id0, listId = id1, owner = owner) } returns
              ServiceResponse(content = mockLrmListItem, message = serviceResponseMessage)
            val response = lrmListApiService.patchListItem(listId = id1, itemId = id0, listOwner = owner, patchRequest = mockPatchRequest)
            response.message shouldBe "Item 'Item Name' not updated."
          }

          it("throw exception for unsupported patch field") {
            val patchRequest = mapOf("unsupportedField" to "value")
            val serviceResponse = ServiceResponse(content = mockLrmListItem, message = serviceResponseMessage)
            every { mockLrmListItemService.findByOwnerAndItemIdAndListId(itemId = id0, listId = id1, owner) } returns serviceResponse
            val exception = shouldThrow<IllegalArgumentException> {
              lrmListApiService.patchListItem(listId = id1, itemId = id0, listOwner = owner, patchRequest = patchRequest)
            }
            exception.message shouldBe "Patch operation is not supported on field: unsupportedField"
          }
        }
      }
    }
  })
