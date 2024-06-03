package net.flyingfishflash.loremlist.unit.domain.lrmitem

import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.extensions.spring.SpringExtension
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.flyingfishflash.loremlist.core.exceptions.ApiException
import net.flyingfishflash.loremlist.core.response.advice.ApiExceptionHandler
import net.flyingfishflash.loremlist.core.response.structure.DispositionOfProblem
import net.flyingfishflash.loremlist.core.response.structure.DispositionOfSuccess
import net.flyingfishflash.loremlist.domain.association.AssociationService
import net.flyingfishflash.loremlist.domain.association.data.ItemToListAssociationUpdateRequest
import net.flyingfishflash.loremlist.domain.lrmitem.ItemDeleteWithListAssociationException
import net.flyingfishflash.loremlist.domain.lrmitem.ItemNotFoundException
import net.flyingfishflash.loremlist.domain.lrmitem.LrmItem
import net.flyingfishflash.loremlist.domain.lrmitem.LrmItemController
import net.flyingfishflash.loremlist.domain.lrmitem.LrmItemService
import net.flyingfishflash.loremlist.domain.lrmitem.data.LrmItemDeleteResponse
import net.flyingfishflash.loremlist.domain.lrmitem.data.LrmItemRequest
import net.flyingfishflash.loremlist.domain.lrmlist.ListNotFoundException
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post

/**
 * LrmListController Unit Tests
 */
@WebMvcTest(controllers = [LrmItemController::class])
class LrmItemControllerTests(mockMvc: MockMvc) : DescribeSpec() {
  override fun extensions() = listOf(SpringExtension)

  @MockkBean
  lateinit var lrmItemService: LrmItemService

  @MockkBean
  lateinit var associationService: AssociationService

  init {
    val id: Long = 1
    val itemToListAssociationUpdateRequest = ItemToListAssociationUpdateRequest(2, 3)
    val lrmItemRequest = LrmItemRequest("Lorem Item Name", "Lorem Item Description")
    fun lrmItem(): LrmItem = LrmItem(id = 0, name = lrmItemRequest.name, description = lrmItemRequest.description)
    fun lrmItemWithEmptyLists() = lrmItem().copy(lists = setOf())

    afterEach { clearAllMocks() }
    afterSpec { unmockkAll() }

    describe("/items") {
      describe("get") {
        it("items are found") {
          val mockReturn = listOf(lrmItem())
          every { lrmItemService.findAll() } returns mockReturn
          val instance = "/items"
          mockMvc.get(instance) {
            contentType = MediaType.APPLICATION_JSON
          }.andExpect {
            status { isOk() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
            jsonPath("$.method") { value(HttpMethod.GET.name().lowercase()) }
            jsonPath("$.message") { value("retrieved all items") }
            jsonPath("$.instance") { value(instance) }
            jsonPath("$.size") { value(mockReturn.size) }
            jsonPath("$.content") { exists() }
            jsonPath("$.content") { isArray() }
            jsonPath("$.content.[0].name") { value(mockReturn[0].name) }
            jsonPath("$.content.[0].description") { value(mockReturn[0].description) }
            jsonPath("$.content.[0].items") {
              doesNotExist()
            }
          }
          verify(exactly = 1) { lrmItemService.findAll() }
        }

        it("items are found ?includeLists=false") {
          val mockReturn = listOf(lrmItem())
          every { lrmItemService.findAll() } returns mockReturn
          val instance = "/items?includeLists=false"
          mockMvc.get(instance) {
            contentType = MediaType.APPLICATION_JSON
          }.andExpect {
            status { isOk() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
            jsonPath("$.method") { value(HttpMethod.GET.name().lowercase()) }
            jsonPath("$.message") { value("retrieved all items") }
            jsonPath("$.instance") { value(instance.substringBeforeLast("?").removeSuffix(instance)) }
            jsonPath("$.size") { value(mockReturn.size) }
            jsonPath("$.content") { isArray() }
            jsonPath("$.content.[0].name") { value(mockReturn[0].name) }
            jsonPath("$.content.[0].description") { value(mockReturn[0].description) }
            jsonPath("$.content.[0].lists") {
              doesNotExist()
            }
          }
          verify(exactly = 1) { lrmItemService.findAll() }
        }

        it("items are found ?includeLists=true") {
          val mockReturn = listOf(lrmItemWithEmptyLists())
          every { lrmItemService.findAllIncludeLists() } returns mockReturn
          val instance = "/items?includeLists=true"
          mockMvc.get(instance) {
            contentType = MediaType.APPLICATION_JSON
          }.andExpect {
            status { isOk() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
            jsonPath("$.method") { value(HttpMethod.GET.name().lowercase()) }
            jsonPath("$.message") { value("retrieved all items and the lists each item is associated with.") }
            jsonPath("$.instance") { value(instance.substringBeforeLast("?").removeSuffix(instance)) }
            jsonPath("$.size") { value(mockReturn.size) }
            jsonPath("$.content") { isArray() }
            jsonPath("$.content.[0].name") { value(mockReturn[0].name) }
            jsonPath("$.content.[0].description") { value(mockReturn[0].description) }
            jsonPath("$.content.[0].lists") {
              isArray()
              isEmpty()
            }
          }
          verify(exactly = 1) { lrmItemService.findAllIncludeLists() }
        }
      }

      describe("post") {
        it("item is created") {
          println(Json.encodeToString(lrmItemRequest))
          every { lrmItemService.create(lrmItemRequest) } returns lrmItem()
          val instance = "/items"
          mockMvc.post(instance) {
            content = Json.encodeToString(lrmItemRequest)
            contentType = MediaType.APPLICATION_JSON
          }.andExpect {
            status { isOk() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
            jsonPath("$.method") { value(HttpMethod.POST.name().lowercase()) }
            jsonPath("$.message") { value("created new item") }
            jsonPath("$.instance") { value(instance) }
            jsonPath("$.size") { value(1) }
            jsonPath("$.content.description") { value(lrmItem().description) }
            jsonPath("$.content.name") { value(lrmItem().name) }
          }
          verify(exactly = 1) { lrmItemService.create(lrmItemRequest) }
        }

        it("item is not created") {
          println(Json.encodeToString(lrmItemRequest))
          every { lrmItemService.create(lrmItemRequest) } throws ApiException(HttpStatus.INTERNAL_SERVER_ERROR)
          val instance = "/items"
          mockMvc.post(instance) {
            content = Json.encodeToString(lrmItemRequest)
            contentType = MediaType.APPLICATION_JSON
          }.andExpect {
            status { isInternalServerError() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.disposition") { value(DispositionOfProblem.ERROR.nameAsLowercase()) }
            jsonPath("$.method") { value(HttpMethod.POST.name().lowercase()) }
            jsonPath("$.message") { value("API Exception") }
            jsonPath("$.instance") { value(instance) }
            jsonPath("$.size") { value(1) }
            jsonPath("$.content.detail") { value("API Exception") }
          }
          verify(exactly = 1) { lrmItemService.create(lrmItemRequest) }
        }

        it("requested item name is an empty string") {
          val instance = "/items"
          mockMvc.post(instance) {
            content = Json.encodeToString(LrmItemRequest("", lrmItem().description))
            contentType = MediaType.APPLICATION_JSON
          }.andExpect {
            status { isBadRequest() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.disposition") { value(DispositionOfProblem.FAILURE.nameAsLowercase()) }
            jsonPath("$.method") { value(HttpMethod.POST.name().lowercase()) }
            jsonPath("$.message") { value("The following fields contained invalid content: name.") }
            jsonPath("$.instance") { value(instance) }
            jsonPath("$.size") { value(1) }
            jsonPath("$.content.title") { value(ApiExceptionHandler.VALIDATION_FAILURE) }
            jsonPath("$.content.status") { HttpStatus.BAD_REQUEST.value() }
            jsonPath("$.content.extensions.validationErrors.length()") { value(2) }
          }
          verify(exactly = 0) { lrmItemService.create(ofType(LrmItemRequest::class)) }
        }

        it("requested item description is an empty string") {
          val instance = "/items"
          mockMvc.post(instance) {
            content = Json.encodeToString(LrmItemRequest(lrmItem().name, ""))
            contentType = MediaType.APPLICATION_JSON
          }.andExpect {
            status { isBadRequest() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.disposition") { value(DispositionOfProblem.FAILURE.nameAsLowercase()) }
            jsonPath("$.method") { value(HttpMethod.POST.name().lowercase()) }
            jsonPath("$.message") { value("The following fields contained invalid content: description.") }
            jsonPath("$.instance") { value(instance) }
            jsonPath("$.size") { value(1) }
            jsonPath("$.content.title") { value(ApiExceptionHandler.VALIDATION_FAILURE) }
            jsonPath("$.content.status") { HttpStatus.BAD_REQUEST.value() }
            jsonPath("$.content.extensions.validationErrors.length()") { value(2) }
          }
          verify(exactly = 0) { lrmItemService.create(ofType(LrmItemRequest::class)) }
        }

        it("requested item quantity is less than 0") {
          val instance = "/items"
          mockMvc.post(instance) {
            content = Json.encodeToString(LrmItemRequest(lrmItem().name, lrmItem().description, -1))
            contentType = MediaType.APPLICATION_JSON
          }.andExpect {
            status { isBadRequest() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.disposition") { value(DispositionOfProblem.FAILURE.nameAsLowercase()) }
            jsonPath("$.method") { value(HttpMethod.POST.name().lowercase()) }
            jsonPath("$.message") { value("The following fields contained invalid content: quantity.") }
            jsonPath("$.instance") { value(instance) }
            jsonPath("$.size") { value(1) }
            jsonPath("$.content.title") { value(ApiExceptionHandler.VALIDATION_FAILURE) }
            jsonPath("$.content.status") { HttpStatus.BAD_REQUEST.value() }
            jsonPath("$.content.extensions.validationErrors.length()") { value(1) }
            jsonPath("$.content.extensions.validationErrors.[0]") { value("Item quantity must be zero or greater.") }
          }
          verify(exactly = 0) { lrmItemService.create(ofType(LrmItemRequest::class)) }
        }
      }
    }

    describe("/items/{id}") {
      describe("delete") {
        it("item is deleted") {
          // non-sensical conditioning of the delete response:
          // if the count of item to list associations is 0, then associatedListNames should be an empty list
          every { lrmItemService.deleteSingleById(id, removeListAssociations = false) } returns
            LrmItemDeleteResponse(0, listOf("Lorem Ipsum"))
          val instance = "/items/$id"
          mockMvc.delete(instance).andExpect {
            status { isOk() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
            jsonPath("$.method") { value(HttpMethod.DELETE.name().lowercase()) }
            jsonPath("$.message") { value("Deleted item id $id.") }
            jsonPath("$.instance") { value(instance) }
            jsonPath("$.size") { value(1) }
            jsonPath("$.content.countItemToListAssociations") { value(0) }
            jsonPath("$.content.associatedListNames.length()") { value(1) }
            jsonPath("$.content.associatedListNames.[0]") { value("Lorem Ipsum") }
          }
          verify(exactly = 1) { lrmItemService.deleteSingleById(any(), any()) }
        }

        it("item is not found") {
          every { lrmItemService.deleteSingleById(id, removeListAssociations = false) } throws ItemNotFoundException(id)
          val instance = "/items/$id"
          mockMvc.delete(instance).andExpect {
            status { isNotFound() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.disposition") { value(DispositionOfProblem.FAILURE.nameAsLowercase()) }
            jsonPath("$.method") { value(HttpMethod.DELETE.name().lowercase()) }
            jsonPath("$.message") { value("Item id $id could not be found.") }
            jsonPath("$.instance") { value(instance) }
            jsonPath("$.size") { value(1) }
            jsonPath("$.content.title") { value(ItemNotFoundException.TITLE) }
            jsonPath("$.content.status") { HttpStatus.NOT_FOUND.value() }
            jsonPath("$.content.detail") { value("Item id $id could not be found.") }
          }
          verify(exactly = 1) { lrmItemService.deleteSingleById(any(), any()) }
        }

        it("item is not deleted due to list associations") {
          every {
            lrmItemService.deleteSingleById(id, removeListAssociations = false)
          } throws ItemDeleteWithListAssociationException(1, LrmItemDeleteResponse(1, listOf("Lorem Ipsum")))
          val instance = "/items/$id"
          mockMvc.delete(instance).andExpect {
            status { ItemDeleteWithListAssociationException.HTTP_STATUS }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.disposition") { value(DispositionOfProblem.FAILURE.nameAsLowercase()) }
            jsonPath("$.method") { value(HttpMethod.DELETE.name().lowercase()) }
            jsonPath("$.message") {
              value(
                "Item id $id could not be deleted because it's associated with 1 list(s). " +
                  "First remove the item from each list.",
              )
            }
            jsonPath("$.instance") { value(instance) }
            jsonPath("$.size") { value(1) }
            jsonPath("$.content.title") { value(ItemDeleteWithListAssociationException.TITLE) }
            jsonPath("$.content.status") { ItemDeleteWithListAssociationException.HTTP_STATUS.value() }
            jsonPath("$.content.detail") {
              value(
                "Item id $id could not be deleted because it's associated with 1 list(s). " +
                  "First remove the item from each list.",
              )
            }
          }
          verify(exactly = 1) { lrmItemService.deleteSingleById(any(), any()) }
        }
      }

      describe("get") {
        it("item is found") {
          every { lrmItemService.findById(id) } returns lrmItem()
          val instance = "/items/$id"
          mockMvc.get(instance).andExpect {
            status { isOk() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
            jsonPath("$.method") { value(HttpMethod.GET.name().lowercase()) }
            jsonPath("$.message") { value("retrieved item id $id") }
            jsonPath("$.instance") { value(instance) }
            jsonPath("$.size") { value(1) }
            jsonPath("$.content.description") { value(lrmItem().description) }
            jsonPath("$.content.name") { value(lrmItem().name) }
            jsonPath("$.content.lists") { doesNotExist() }
          }
          verify(exactly = 1) { lrmItemService.findById(id) }
        }

        it("item is found ?includeLists=false") {
          every { lrmItemService.findById(id) } returns lrmItem()
          val instance = "/items/$id?includeLists=false"
          mockMvc.get(instance).andExpect {
            status { isOk() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
            jsonPath("$.method") { value(HttpMethod.GET.name().lowercase()) }
            jsonPath("$.message") { value("retrieved item id $id") }
            jsonPath("$.instance") { value(instance.substringBeforeLast("?").removeSuffix(instance)) }
            jsonPath("$.size") { value(1) }
            jsonPath("$.content.description") { value(lrmItem().description) }
            jsonPath("$.content.name") { value(lrmItem().name) }
            jsonPath("$.content.lists") { doesNotExist() }
          }
          verify(exactly = 1) { lrmItemService.findById(id) }
        }

        it("item is found ?includeLists=true") {
          every { lrmItemService.findByIdIncludeLists(id) } returns lrmItemWithEmptyLists()
          val instance = "/items/$id?includeLists=true"
          mockMvc.get(instance).andExpect {
            status { isOk() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
            jsonPath("$.method") { value(HttpMethod.GET.name().lowercase()) }
            jsonPath("$.message") { value("retrieved item id $id and it's associated lists") }
            jsonPath("$.instance") { value(instance.substringBeforeLast("?").removeSuffix(instance)) }
            jsonPath("$.size") { value(1) }
            jsonPath("$.content.description") { value(lrmItemWithEmptyLists().description) }
            jsonPath("$.content.name") { value(lrmItemWithEmptyLists().name) }
            jsonPath("$.content.lists") {
              isArray()
              isEmpty()
            }
          }
          verify(exactly = 1) { lrmItemService.findByIdIncludeLists(id) }
        }

        it("item is not found") {
          every { lrmItemService.findById(id) } throws ItemNotFoundException(id)
          val instance = "/items/$id"
          mockMvc.get(instance).andExpect {
            status { isNotFound() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.disposition") { value(DispositionOfProblem.FAILURE.nameAsLowercase()) }
            jsonPath("$.method") { value(HttpMethod.GET.name().lowercase()) }
            jsonPath("$.message") { value("Item id $id could not be found.") }
            jsonPath("$.instance") { value(instance) }
            jsonPath("$.size") { value(1) }
            jsonPath("$.content.title") { value(ItemNotFoundException.TITLE) }
            jsonPath("$.content.status") { HttpStatus.NOT_FOUND.value() }
            jsonPath("$.content.detail") { value("Item id $id could not be found.") }
          }
          verify(exactly = 1) { lrmItemService.findById(id) }
        }
      }

      describe("patch") {
        it("item is found and updated") {
          every { lrmItemService.patch(id, any()) } returns Pair(lrmItem(), true)
          val instance = "/items/$id"
          mockMvc.patch(instance) {
            content = Json.encodeToString(mapOf("name" to lrmItem().name))
            contentType = MediaType.APPLICATION_JSON
          }.andExpect {
            status { isOk() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
            jsonPath("$.method") { value(HttpMethod.PATCH.name().lowercase()) }
            jsonPath("$.message") { value("patched") }
            jsonPath("$.instance") { value(instance) }
            jsonPath("$.size") { value(1) }
            jsonPath("$.content.description") { value(lrmItem().description) }
            jsonPath("$.content.name") { value(lrmItem().name) }
            jsonPath("$.content.items") {
              doesNotExist()
            }
          }
          verify(exactly = 1) { lrmItemService.patch(id, mapOf("name" to lrmItem().name)) }
        }

        it("item is found and not updated") {
          every { lrmItemService.patch(id, any()) } returns Pair(lrmItem(), false)
          val instance = "/items/$id"
          mockMvc.patch(instance) {
            content = Json.encodeToString(mapOf("name" to lrmItem().name))
            contentType = MediaType.APPLICATION_JSON
          }.andExpect {
            status { isNoContent() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
            jsonPath("$.method") { value(HttpMethod.PATCH.name().lowercase()) }
            jsonPath("$.message") { value("not patched") }
            jsonPath("$.instance") { value(instance) }
            jsonPath("$.size") { value(1) }
            jsonPath("$.content.description") { value(lrmItem().description) }
            jsonPath("$.content.name") { value(lrmItem().name) }
            jsonPath("$.content.items") { doesNotExist() }
          }
          verify(exactly = 1) { lrmItemService.patch(id, mapOf("name" to lrmItem().name)) }
        }

        it("item is not found") {
          every { lrmItemService.patch(id, any()) } throws ListNotFoundException(id)
          val instance = "/items/$id"
          mockMvc.patch(instance) {
            content = Json.encodeToString(mapOf("name" to lrmItem().name))
            contentType = MediaType.APPLICATION_JSON
          }.andExpect {
            status { isNotFound() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.disposition") { value(DispositionOfProblem.FAILURE.nameAsLowercase()) }
            jsonPath("$.method") { value(HttpMethod.PATCH.name().lowercase()) }
            jsonPath("$.message") { value("List id $id could not be found.") }
            jsonPath("$.instance") { value(instance) }
            jsonPath("$.size") { value(1) }
            jsonPath("$.content.title") { value(ListNotFoundException.TITLE) }
            jsonPath("$.content.status") { HttpStatus.NOT_FOUND.value() }
            jsonPath("$.content.detail") { value("List id $id could not be found.") }
          }
          verify(exactly = 1) { lrmItemService.patch(id, mapOf("name" to lrmItem().name)) }
        }
      }
    }

    describe("/items/{id}/list-associations") {
      describe("delete") {
        it("item is removed from list") {
          val lrmListName = "Lorem List Name"
          every { associationService.deleteItemToList(id, 2) } returns Pair(lrmItem().name, lrmListName)
          val instance = "/items/$id/list-associations"
          mockMvc.delete(instance) {
            content = Json.encodeToString(2)
            contentType = MediaType.APPLICATION_JSON
          }.andExpect {
            status { isOk() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
            jsonPath("$.method") { value(HttpMethod.DELETE.name().lowercase()) }
            jsonPath("$.message") { value("Removed item '${lrmItem().name}' from list '$lrmListName'.") }
            jsonPath("$.instance") { value(instance) }
            jsonPath("$.size") { value(1) }
            jsonPath("$.content.message") { value("Removed item '${lrmItem().name}' from list '$lrmListName'.") }
          }
          verify(exactly = 1) { associationService.deleteItemToList(id, 2) }
        }

        it("item is not found") {
          every { associationService.deleteItemToList(id, 2) } throws ItemNotFoundException(id)
          val instance = "/items/$id/list-associations"
          mockMvc.delete(instance) {
            content = Json.encodeToString(2)
            contentType = MediaType.APPLICATION_JSON
          }.andExpect {
            status { isNotFound() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.disposition") { value(DispositionOfProblem.FAILURE.nameAsLowercase()) }
            jsonPath("$.method") { value(HttpMethod.DELETE.name().lowercase()) }
            jsonPath("$.message") { value("Item id $id could not be found.") }
            jsonPath("$.instance") { value(instance) }
            jsonPath("$.size") { value(1) }
            jsonPath("$.content.title") { value(ItemNotFoundException.TITLE) }
            jsonPath("$.content.status") { HttpStatus.NOT_FOUND.value() }
            jsonPath("$.content.detail") { value("Item id $id could not be found.") }
          }
          verify(exactly = 1) { associationService.deleteItemToList(id, 2) }
        }

        it("list is not found") {
          every { associationService.deleteItemToList(id, 2) } throws ListNotFoundException(id)
          val instance = "/items/$id/list-associations"
          mockMvc.delete(instance) {
            content = Json.encodeToString(2)
            contentType = MediaType.APPLICATION_JSON
          }.andExpect {
            status { isNotFound() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.disposition") { value(DispositionOfProblem.FAILURE.nameAsLowercase()) }
            jsonPath("$.method") { value(HttpMethod.DELETE.name().lowercase()) }
            jsonPath("$.message") { value("List id $id could not be found.") }
            jsonPath("$.instance") { value(instance) }
            jsonPath("$.size") { value(1) }
            jsonPath("$.content.title") { value(ListNotFoundException.TITLE) }
            jsonPath("$.content.status") { HttpStatus.NOT_FOUND.value() }
            jsonPath("$.content.detail") { value("List id $id could not be found.") }
          }
          verify(exactly = 1) { associationService.deleteItemToList(id, 2) }
        }
      }

      describe("patch") {
        it("item is moved from one list to another list") {
          val fromListName = "List A"
          val toListName = "List B"
          every { associationService.updateItemToList(id, 2, 3) } returns Triple(lrmItem().name, fromListName, toListName)
          val instance = "/items/$id/list-associations"
          mockMvc.patch(instance) {
            content = Json.encodeToString(itemToListAssociationUpdateRequest)
            contentType = MediaType.APPLICATION_JSON
          }.andExpect {
            status { isOk() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
            jsonPath("$.method") { value(HttpMethod.PATCH.name().lowercase()) }
            jsonPath("$.message") { value("Moved item '${lrmItem().name}' from list '$fromListName' to list '$toListName'.") }
            jsonPath("$.instance") { value(instance) }
            jsonPath("$.size") { value(1) }
            jsonPath("$.content.message") { value("Moved item '${lrmItem().name}' from list '$fromListName' to list '$toListName'.") }
          }
          verify(exactly = 1) { associationService.updateItemToList(id, 2, 3) }
        }

        it("item is not moved") {
          every { associationService.updateItemToList(id, 2, 3) } throws
            ApiException(httpStatus = HttpStatus.I_AM_A_TEAPOT, title = "Api Exception Title", responseMessage = "Api Exception Detail")
          val instance = "/items/$id/list-associations"
          mockMvc.patch(instance) {
            content = Json.encodeToString(itemToListAssociationUpdateRequest)
            contentType = MediaType.APPLICATION_JSON
          }.andExpect {
            status { isIAmATeapot() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.disposition") { value(DispositionOfProblem.FAILURE.nameAsLowercase()) }
            jsonPath("$.method") { value(HttpMethod.PATCH.name().lowercase()) }
            jsonPath("$.message") { value("Api Exception Detail") }
            jsonPath("$.instance") { value(instance) }
            jsonPath("$.size") { value(1) }
          }
          verify(exactly = 1) { associationService.updateItemToList(id, 2, 3) }
        }
      }

      describe("post") {
        it("item is added to list") {
          val lrmListName = "Lorem List Name"
          every { associationService.addItemToList(id, 2) } returns Pair(lrmItem().name, lrmListName)
          val instance = "/items/$id/list-associations"
          mockMvc.post(instance) {
            content = Json.encodeToString(2)
            contentType = MediaType.APPLICATION_JSON
          }.andExpect {
            status { isOk() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
            jsonPath("$.method") { value(HttpMethod.POST.name().lowercase()) }
            jsonPath("$.message") { value("Assigned item '${lrmItem().name}' to list '$lrmListName'.") }
            jsonPath("$.instance") { value(instance) }
            jsonPath("$.size") { value(1) }
            jsonPath("$.content.message") { value("Assigned item '${lrmItem().name}' to list '$lrmListName'.") }
          }
          verify(exactly = 1) { associationService.addItemToList(id, 2) }
        }

        it("item is not found") {
          every { associationService.addItemToList(id, 2) } throws ItemNotFoundException(id)
          val instance = "/items/$id/list-associations"
          mockMvc.post(instance) {
            content = Json.encodeToString(2)
            contentType = MediaType.APPLICATION_JSON
          }.andExpect {
            status { isNotFound() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.disposition") { value(DispositionOfProblem.FAILURE.nameAsLowercase()) }
            jsonPath("$.method") { value(HttpMethod.POST.name().lowercase()) }
            jsonPath("$.message") { value("Item id $id could not be found.") }
            jsonPath("$.instance") { value(instance) }
            jsonPath("$.size") { value(1) }
            jsonPath("$.content.title") { value(ItemNotFoundException.TITLE) }
            jsonPath("$.content.status") { HttpStatus.NOT_FOUND.value() }
            jsonPath("$.content.detail") { value("Item id $id could not be found.") }
          }
          verify(exactly = 1) { associationService.addItemToList(id, 2) }
        }

        it("list is not found") {
          every { associationService.addItemToList(id, 2) } throws ListNotFoundException(id)
          val instance = "/items/$id/list-associations"
          mockMvc.post(instance) {
            content = Json.encodeToString(2)
            contentType = MediaType.APPLICATION_JSON
          }.andExpect {
            status { isNotFound() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.disposition") { value(DispositionOfProblem.FAILURE.nameAsLowercase()) }
            jsonPath("$.method") { value(HttpMethod.POST.name().lowercase()) }
            jsonPath("$.message") { value("List id $id could not be found.") }
            jsonPath("$.instance") { value(instance) }
            jsonPath("$.size") { value(1) }
            jsonPath("$.content.title") { value(ListNotFoundException.TITLE) }
            jsonPath("$.content.status") { HttpStatus.NOT_FOUND.value() }
            jsonPath("$.content.detail") { value("List id $id could not be found.") }
          }
          verify(exactly = 1) { associationService.addItemToList(id, 2) }
        }
      }
    }

    describe("/items/{id}/list-associations/count") {
      describe("get") {
        it("count of list associations is returned") {
          every { associationService.countItemToList(1) } returns 999
          val instance = "/items/$id/list-associations/count"
          mockMvc.get(instance).andExpect {
            status { isOk() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
            jsonPath("$.method") { value(HttpMethod.GET.name().lowercase()) }
            jsonPath("$.message") { value("item is associated with 999 lists.") }
            jsonPath("$.instance") { value(instance) }
            jsonPath("$.size") { value(1) }
            jsonPath("$.content.length()") { value(1) }
            jsonPath("$.content.value") { value(999) }
          }
          verify(exactly = 1) { associationService.countItemToList(any()) }
        }

        it("item is not found") {
          every { associationService.countItemToList(1) } throws ApiException(httpStatus = HttpStatus.NOT_FOUND)
          val instance = "/items/$id/list-associations/count"
          mockMvc.get(instance).andExpect {
            status { isNotFound() }
            jsonPath("$.disposition") { value(DispositionOfProblem.FAILURE.nameAsLowercase()) }
            jsonPath("$.method") { value(HttpMethod.GET.name().lowercase()) }
            jsonPath("$.instance") { value(instance) }
            jsonPath("$.size") { value(1) }
            jsonPath("$.content.status") { HttpStatus.NOT_FOUND.value() }
          }
          verify(exactly = 1) { associationService.countItemToList(any()) }
        }
      }
    }

    describe("/items/{id}/list-associations/delete-all") {
      it("item is removed from all lists") {
        every { associationService.deleteAllItemToListForItem(1) } returns Pair(lrmItem().name, 999)
        val instance = "/items/$id/list-associations/delete-all"
        mockMvc.delete(instance) {
          contentType = MediaType.APPLICATION_JSON
        }.andExpect {
          status { isOk() }
          content { contentType(MediaType.APPLICATION_JSON) }
          jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
          jsonPath("$.method") { value(HttpMethod.DELETE.name().lowercase()) }
          jsonPath("$.message") { value("Removed item '${lrmItem().name}' from all associated lists (999).") }
          jsonPath("$.instance") { value(instance) }
          jsonPath("$.size") { value(1) }
          jsonPath("$.content.value") { value(999) }
        }
        verify(exactly = 1) { associationService.deleteAllItemToListForItem(any()) }
      }
    }

    describe("/items/count") {
      describe("get") {
        it("count of items is returned") {
          every { lrmItemService.count() } returns 999
          val instance = "/items/count"
          mockMvc.get(instance).andExpect {
            status { isOk() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
            jsonPath("$.method") { value(HttpMethod.GET.name().lowercase()) }
            jsonPath("$.message") { value("999 items.") }
            jsonPath("$.instance") { value(instance) }
            jsonPath("$.size") { value(1) }
            jsonPath("$.content.length()") { value(1) }
            jsonPath("$.content.value") { value(999) }
          }
        }
      }
    }
  }
}
