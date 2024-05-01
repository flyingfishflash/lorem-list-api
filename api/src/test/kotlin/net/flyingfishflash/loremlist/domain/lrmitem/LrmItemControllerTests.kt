package net.flyingfishflash.loremlist.domain.lrmitem

import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.extensions.spring.SpringExtension
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.flyingfishflash.loremlist.core.exceptions.ApiException
import net.flyingfishflash.loremlist.core.response.structure.DispositionOfProblem
import net.flyingfishflash.loremlist.core.response.structure.DispositionOfSuccess
import net.flyingfishflash.loremlist.domain.common.CommonService
import net.flyingfishflash.loremlist.domain.lrmitem.data.LrmItem
import net.flyingfishflash.loremlist.domain.lrmitem.data.LrmItemMoveToListRequest
import net.flyingfishflash.loremlist.domain.lrmitem.data.LrmItemRequest
import net.flyingfishflash.loremlist.domain.lrmlist.ListNotFoundException
import net.flyingfishflash.loremlist.domain.lrmlist.data.dto.LrmListRequest
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
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
  lateinit var commonService: CommonService

  init {
    val id: Long = 1
    val lrmItemMoveToListRequest = LrmItemMoveToListRequest(2, 3)
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
            content = Json.encodeToString(LrmListRequest("", lrmItem().description))
            contentType = MediaType.APPLICATION_JSON
          }.andExpect {
            status { isBadRequest() }
            content { contentType(MediaType.APPLICATION_PROBLEM_JSON) }
            jsonPath("$.disposition") { value(DispositionOfProblem.FAILURE.nameAsLowercase()) }
            jsonPath("$.method") { value(HttpMethod.POST.name().lowercase()) }
            // TODO: Fix this response message
            jsonPath("$.message") { value("Invalid request content.") }
            jsonPath("$.instance") { value(instance) }
            jsonPath("$.size") { value(1) }
            jsonPath("$.content.title") { value("Bad Request") }
            jsonPath("$.content.status") { HttpStatus.BAD_REQUEST.value() }
          }
          verify(exactly = 0) { lrmItemService.create(ofType(LrmItemRequest::class)) }
        }

        it("requested item description is an empty string") {
          val instance = "/items"
          mockMvc.post(instance) {
            content = Json.encodeToString(LrmListRequest(lrmItem().name, ""))
            contentType = MediaType.APPLICATION_JSON
          }.andExpect {
            status { isBadRequest() }
            content { contentType(MediaType.APPLICATION_PROBLEM_JSON) }
            jsonPath("$.disposition") { value(DispositionOfProblem.FAILURE.nameAsLowercase()) }
            jsonPath("$.method") { value(HttpMethod.POST.name().lowercase()) }
            jsonPath("$.message") { value("Invalid request content.") }
            jsonPath("$.instance") { value(instance) }
            jsonPath("$.size") { value(1) }
            jsonPath("$.content.title") { value("Bad Request") }
            jsonPath("$.content.status") { HttpStatus.BAD_REQUEST.value() }
          }
          verify(exactly = 0) { lrmItemService.create(ofType(LrmItemRequest::class)) }
        }
      }
    }

    describe("/items/{id}") {
      describe("delete") {
        it("item is deleted") {
          every { lrmItemService.deleteSingleById(id) } just Runs
          val instance = "/items/$id"
          mockMvc.delete(instance).andExpect {
            status { isOk() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
            jsonPath("$.method") { value(HttpMethod.DELETE.name().lowercase()) }
            jsonPath("$.message") { value("deleted item id $id") }
            jsonPath("$.instance") { value(instance) }
            jsonPath("$.size") { value(1) }
            jsonPath("$.content.message") { value("content") }
          }
          verify(exactly = 1) { lrmItemService.deleteSingleById(id) }
        }

        it("item is not found") {
          every { lrmItemService.deleteSingleById(id) } throws ItemNotFoundException(id)
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
          verify(exactly = 1) { lrmItemService.deleteSingleById(id) }
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
        it("item is found and updated") { TODO() }

        it("item is found and not updated") { TODO() }

        it("item is not found") { TODO() }
      }
    }

    describe("/items/{id}/add-to-list") {
      describe("post") {
        it("item is added to list") {
          val lrmListName = "Lorem List Name"
          every { commonService.addToList(id, 2) } returns Pair(lrmItem().name, lrmListName)
          val instance = "/items/$id/add-to-list"
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
          verify(exactly = 1) { commonService.addToList(id, 2) }
        }

        it("item is not found") {
          every { commonService.addToList(id, 2) } throws ItemNotFoundException(id)
          val instance = "/items/$id/add-to-list"
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
          verify(exactly = 1) { commonService.addToList(id, 2) }
        }

        it("list is not found") {
          every { commonService.addToList(id, 2) } throws ListNotFoundException(id)
          val instance = "/items/$id/add-to-list"
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
          verify(exactly = 1) { commonService.addToList(id, 2) }
        }
      }
    }

    describe("/items/{id}/move-to-list") {
      describe("post") {
        it("item is moved from one list to another list") {
          val fromListName = "List A"
          val toListName = "List B"
          every { commonService.moveToList(id, 2, 3) } returns Triple(lrmItem().name, fromListName, toListName)
          val instance = "/items/$id/move-to-list"
          mockMvc.post(instance) {
            content = Json.encodeToString(lrmItemMoveToListRequest)
            contentType = MediaType.APPLICATION_JSON
          }.andExpect {
            status { isOk() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
            jsonPath("$.method") { value(HttpMethod.POST.name().lowercase()) }
            jsonPath("$.message") { value("Moved item '${lrmItem().name}' from list '$fromListName' to list '$toListName'.") }
            jsonPath("$.instance") { value(instance) }
            jsonPath("$.size") { value(1) }
            jsonPath("$.content.message") { value("Moved item '${lrmItem().name}' from list '$fromListName' to list '$toListName'.") }
          }
          verify(exactly = 1) { commonService.moveToList(id, 2, 3) }
        }

        it("item is not moved") {
          every { commonService.moveToList(id, 2, 3) } throws
            ApiException(httpStatus = HttpStatus.I_AM_A_TEAPOT, title = "Api Exception Title", responseMessage = "Api Exception Detail")
          val instance = "/items/$id/move-to-list"
          mockMvc.post(instance) {
            content = Json.encodeToString(lrmItemMoveToListRequest)
            contentType = MediaType.APPLICATION_JSON
          }.andExpect {
            status { isIAmATeapot() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.disposition") { value(DispositionOfProblem.FAILURE.nameAsLowercase()) }
            jsonPath("$.method") { value(HttpMethod.POST.name().lowercase()) }
            jsonPath("$.message") { value("Api Exception Detail") }
            jsonPath("$.instance") { value(instance) }
            jsonPath("$.size") { value(1) }
          }
          verify(exactly = 1) { commonService.moveToList(id, 2, 3) }
        }
      }
    }

    describe("/items/{id}/remove-from-list") {
      describe("post") {
        it("item is removed from list") {
          val lrmListName = "Lorem List Name"
          every { commonService.removeFromList(id, 2) } returns Pair(lrmItem().name, lrmListName)
          val instance = "/items/$id/remove-from-list"
          mockMvc.post(instance) {
            content = Json.encodeToString(2)
            contentType = MediaType.APPLICATION_JSON
          }.andExpect {
            status { isOk() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
            jsonPath("$.method") { value(HttpMethod.POST.name().lowercase()) }
            jsonPath("$.message") { value("Removed item '${lrmItem().name}' from list '$lrmListName'.") }
            jsonPath("$.instance") { value(instance) }
            jsonPath("$.size") { value(1) }
            jsonPath("$.content.message") { value("Removed item '${lrmItem().name}' from list '$lrmListName'.") }
          }
          verify(exactly = 1) { commonService.removeFromList(id, 2) }
        }

        it("item is not found") {
          every { commonService.removeFromList(id, 2) } throws ItemNotFoundException(id)
          val instance = "/items/$id/remove-from-list"
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
          verify(exactly = 1) { commonService.removeFromList(id, 2) }
        }

        it("list is not found") {
          every { commonService.removeFromList(id, 2) } throws ListNotFoundException(id)
          val instance = "/items/$id/remove-from-list"
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
          verify(exactly = 1) { commonService.removeFromList(id, 2) }
        }
      }
    }
  }
}
