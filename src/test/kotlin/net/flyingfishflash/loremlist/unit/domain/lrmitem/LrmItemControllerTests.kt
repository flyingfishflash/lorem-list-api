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
import net.flyingfishflash.loremlist.core.response.advice.ApiExceptionHandler.Companion.VALIDATION_FAILURE_MESSAGE
import net.flyingfishflash.loremlist.core.response.structure.DispositionOfProblem
import net.flyingfishflash.loremlist.core.response.structure.DispositionOfSuccess
import net.flyingfishflash.loremlist.domain.association.AssociationService
import net.flyingfishflash.loremlist.domain.association.data.ItemToListAssociationUpdateRequest
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
import org.springframework.web.bind.MethodArgumentNotValidException
import java.util.*

/**
 * LrmListController Unit Tests
 */
@WebMvcTest(controllers = [LrmItemController::class])
class LrmItemControllerTests(mockMvc: MockMvc) : DescribeSpec() {
  override fun extensions() = listOf(SpringExtension)

  @MockkBean
  lateinit var mockLrmItemService: LrmItemService

  @MockkBean
  lateinit var mockAssociationService: AssociationService

  init {
    val uuid0 = UUID.fromString("00000000-0000-4000-a000-000000000000")
    val uuid1 = UUID.fromString("00000000-0000-4000-a000-000000000001")
    val uuid2 = UUID.fromString("00000000-0000-4000-a000-000000000002")
    val uuid3 = UUID.fromString("00000000-0000-4000-a000-000000000003")
    val itemToListAssociationUpdateRequest = ItemToListAssociationUpdateRequest(uuid2, uuid3)
    val lrmItemRequest = LrmItemRequest("Lorem Item Name", "Lorem Item Description")
    fun lrmItem(): LrmItem = LrmItem(uuid = uuid0, name = lrmItemRequest.name, description = lrmItemRequest.description)
    fun lrmItemWithEmptyLists() = lrmItem().copy(lists = setOf())

    afterEach { clearAllMocks() }
    afterSpec { unmockkAll() }

    describe("/items") {
      describe("delete") {
        it("items are deleted") {
          val mockReturn =
            LrmItemDeleteResponse(itemNames = listOf("Deleted Lorem Item Name"), associatedListNames = listOf("Associated Lorem List Name"))
          every { mockLrmItemService.deleteAll() } returns mockReturn
          val instance = "/items"
          mockMvc.delete(instance) {
            contentType = MediaType.APPLICATION_JSON
          }.andExpect {
            status { isOk() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
            jsonPath("$.method") { value(HttpMethod.DELETE.name().lowercase()) }
            jsonPath("$.message") { value("Deleted all items and disassociated all lists.") }
            jsonPath("$.instance") { value(instance) }
            jsonPath("$.size") { value(1) }
            jsonPath("$.content.itemNames[0]") { value(mockReturn.itemNames[0]) }
            jsonPath("$.content.associatedListNames[0]") { value(mockReturn.associatedListNames[0]) }
          }
        }
      }

      describe("get") {
        it("items are found") {
          val mockReturn = listOf(lrmItem())
          every { mockLrmItemService.findAll() } returns mockReturn
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
          verify(exactly = 1) { mockLrmItemService.findAll() }
        }

        it("items are found ?includeLists=false") {
          val mockReturn = listOf(lrmItem())
          every { mockLrmItemService.findAll() } returns mockReturn
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
          verify(exactly = 1) { mockLrmItemService.findAll() }
        }

        it("items are found ?includeLists=true") {
          val mockReturn = listOf(lrmItemWithEmptyLists())
          every { mockLrmItemService.findAllIncludeLists() } returns mockReturn
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
          verify(exactly = 1) { mockLrmItemService.findAllIncludeLists() }
        }
      }

      describe("post") {
        it("item is created") {
          println(Json.encodeToString(lrmItemRequest))
          every { mockLrmItemService.create(lrmItemRequest) } returns lrmItem()
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
          verify(exactly = 1) { mockLrmItemService.create(lrmItemRequest) }
        }

        it("item is not created") {
          println(Json.encodeToString(lrmItemRequest))
          every { mockLrmItemService.create(lrmItemRequest) } throws ApiException()
          val instance = "/items"
          mockMvc.post(instance) {
            content = Json.encodeToString(lrmItemRequest)
            contentType = MediaType.APPLICATION_JSON
          }.andExpect {
            status { isInternalServerError() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.disposition") { value(DispositionOfProblem.ERROR.nameAsLowercase()) }
            jsonPath("$.method") { value(HttpMethod.POST.name().lowercase()) }
            jsonPath("$.message") { value(ApiException.DEFAULT_TITLE) }
            jsonPath("$.instance") { value(instance) }
            jsonPath("$.size") { value(1) }
            jsonPath("$.content.detail") { value(ApiException.DEFAULT_TITLE) }
          }
          verify(exactly = 1) { mockLrmItemService.create(lrmItemRequest) }
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
            jsonPath("$.message") { value("$VALIDATION_FAILURE_MESSAGE name.") }
            jsonPath("$.instance") { value(instance) }
            jsonPath("$.size") { value(1) }
            jsonPath("$.content.title") { value(MethodArgumentNotValidException::class.java.simpleName) }
            jsonPath("$.content.status") { HttpStatus.BAD_REQUEST.value() }
            jsonPath("$.content.validationErrors.length()") { value(2) }
          }
          verify(exactly = 0) { mockLrmItemService.create(ofType(LrmItemRequest::class)) }
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
            jsonPath("$.message") { value("$VALIDATION_FAILURE_MESSAGE description.") }
            jsonPath("$.instance") { value(instance) }
            jsonPath("$.size") { value(1) }
            jsonPath("$.content.title") { value(MethodArgumentNotValidException::class.java.simpleName) }
            jsonPath("$.content.status") { HttpStatus.BAD_REQUEST.value() }
            jsonPath("$.content.validationErrors.length()") { value(2) }
          }
          verify(exactly = 0) { mockLrmItemService.create(ofType(LrmItemRequest::class)) }
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
            jsonPath("$.message") { value("$VALIDATION_FAILURE_MESSAGE quantity.") }
            jsonPath("$.instance") { value(instance) }
            jsonPath("$.size") { value(1) }
            jsonPath("$.content.title") { value(MethodArgumentNotValidException::class.java.simpleName) }
            jsonPath("$.content.status") { HttpStatus.BAD_REQUEST.value() }
            jsonPath("$.content.validationErrors.length()") { value(1) }
            jsonPath("$.content.validationErrors.[0]") { value("Item quantity must be zero or greater.") }
          }
          verify(exactly = 0) { mockLrmItemService.create(ofType(LrmItemRequest::class)) }
        }
      }
    }

    describe("/items/{id}") {
      describe("delete") {
        it("item is deleted") {
          // nonsensical conditioning of the delete response:
          // if the count of item to list associations is 0, then associatedListNames should be an empty list
          every { mockLrmItemService.deleteById(uuid1, removeListAssociations = false) } returns
            LrmItemDeleteResponse(itemNames = listOf("dolor sit amet"), associatedListNames = listOf("Lorem Ipsum"))
          val instance = "/items/$uuid1"
          mockMvc.delete(instance).andExpect {
            status { isOk() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
            jsonPath("$.method") { value(HttpMethod.DELETE.name().lowercase()) }
            jsonPath("$.message") { value("Deleted item id $uuid1.") }
            jsonPath("$.instance") { value(instance) }
            jsonPath("$.size") { value(1) }
            jsonPath("$.content.itemNames.length()") { value(1) }
            jsonPath("$.content.itemNames.[0]") { value("dolor sit amet") }
            jsonPath("$.content.associatedListNames.[0]") { value("Lorem Ipsum") }
          }
          verify(exactly = 1) { mockLrmItemService.deleteById(any(), any()) }
        }

        it("item is not found") {
          every { mockLrmItemService.deleteById(uuid1, removeListAssociations = false) } throws ItemNotFoundException(uuid1)
          val instance = "/items/$uuid1"
          mockMvc.delete(instance).andExpect {
            status { isNotFound() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.disposition") { value(DispositionOfProblem.FAILURE.nameAsLowercase()) }
            jsonPath("$.method") { value(HttpMethod.DELETE.name().lowercase()) }
            jsonPath("$.message") { value("Item id $uuid1 could not be found.") }
            jsonPath("$.instance") { value(instance) }
            jsonPath("$.size") { value(1) }
            jsonPath("$.content.title") { value(ItemNotFoundException::class.java.simpleName) }
            jsonPath("$.content.status") { HttpStatus.NOT_FOUND.value() }
            jsonPath("$.content.detail") { value("Item id $uuid1 could not be found.") }
          }
          verify(exactly = 1) { mockLrmItemService.deleteById(any(), any()) }
        }
      }

      describe("get") {
        it("item is found") {
          every { mockLrmItemService.findById(uuid1) } returns lrmItem()
          val instance = "/items/$uuid1"
          mockMvc.get(instance).andExpect {
            status { isOk() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
            jsonPath("$.method") { value(HttpMethod.GET.name().lowercase()) }
            jsonPath("$.message") { value("retrieved item id $uuid1") }
            jsonPath("$.instance") { value(instance) }
            jsonPath("$.size") { value(1) }
            jsonPath("$.content.description") { value(lrmItem().description) }
            jsonPath("$.content.name") { value(lrmItem().name) }
            jsonPath("$.content.lists") { doesNotExist() }
          }
          verify(exactly = 1) { mockLrmItemService.findById(uuid1) }
        }

        it("item is found ?includeLists=false") {
          every { mockLrmItemService.findById(uuid1) } returns lrmItem()
          val instance = "/items/$uuid1?includeLists=false"
          mockMvc.get(instance).andExpect {
            status { isOk() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
            jsonPath("$.method") { value(HttpMethod.GET.name().lowercase()) }
            jsonPath("$.message") { value("retrieved item id $uuid1") }
            jsonPath("$.instance") { value(instance.substringBeforeLast("?").removeSuffix(instance)) }
            jsonPath("$.size") { value(1) }
            jsonPath("$.content.description") { value(lrmItem().description) }
            jsonPath("$.content.name") { value(lrmItem().name) }
            jsonPath("$.content.lists") { doesNotExist() }
          }
          verify(exactly = 1) { mockLrmItemService.findById(any()) }
        }

        it("item is found ?includeLists=true") {
          every { mockLrmItemService.findByIdIncludeLists(uuid1) } returns lrmItemWithEmptyLists()
          val instance = "/items/$uuid1?includeLists=true"
          mockMvc.get(instance).andExpect {
            status { isOk() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
            jsonPath("$.method") { value(HttpMethod.GET.name().lowercase()) }
            jsonPath("$.message") { value("retrieved item id $uuid1 and it's associated lists") }
            jsonPath("$.instance") { value(instance.substringBeforeLast("?").removeSuffix(instance)) }
            jsonPath("$.size") { value(1) }
            jsonPath("$.content.description") { value(lrmItemWithEmptyLists().description) }
            jsonPath("$.content.name") { value(lrmItemWithEmptyLists().name) }
            jsonPath("$.content.lists") {
              isArray()
              isEmpty()
            }
          }
          verify(exactly = 1) { mockLrmItemService.findByIdIncludeLists(any()) }
        }

        it("item is not found") {
          every { mockLrmItemService.findById(uuid1) } throws ItemNotFoundException(uuid1)
          val instance = "/items/$uuid1"
          mockMvc.get(instance).andExpect {
            status { isNotFound() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.disposition") { value(DispositionOfProblem.FAILURE.nameAsLowercase()) }
            jsonPath("$.method") { value(HttpMethod.GET.name().lowercase()) }
            jsonPath("$.message") { value("Item id $uuid1 could not be found.") }
            jsonPath("$.instance") { value(instance) }
            jsonPath("$.size") { value(1) }
            jsonPath("$.content.title") { value(ItemNotFoundException::class.java.simpleName) }
            jsonPath("$.content.status") { HttpStatus.NOT_FOUND.value() }
            jsonPath("$.content.detail") { value("Item id $uuid1 could not be found.") }
          }
          verify(exactly = 1) { mockLrmItemService.findById(any()) }
        }
      }

      describe("patch") {
        it("item is found and updated") {
          every { mockLrmItemService.patch(uuid1, patchRequest = any()) } returns Pair(lrmItem(), true)
          val instance = "/items/$uuid1"
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
          verify(exactly = 1) { mockLrmItemService.patch(any(), mapOf("name" to lrmItem().name)) }
        }

        it("item is found and not updated") {
          every { mockLrmItemService.patch(uuid1, patchRequest = any()) } returns Pair(lrmItem(), false)
          val instance = "/items/$uuid1"
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
          verify(exactly = 1) { mockLrmItemService.patch(any(), mapOf("name" to lrmItem().name)) }
        }

        it("item is not found") {
          every { mockLrmItemService.patch(uuid1, patchRequest = any()) } throws ListNotFoundException(uuid1)
          val instance = "/items/$uuid1"
          mockMvc.patch(instance) {
            content = Json.encodeToString(mapOf("name" to lrmItem().name))
            contentType = MediaType.APPLICATION_JSON
          }.andExpect {
            status { isNotFound() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.disposition") { value(DispositionOfProblem.FAILURE.nameAsLowercase()) }
            jsonPath("$.method") { value(HttpMethod.PATCH.name().lowercase()) }
            jsonPath("$.message") { value("List id $uuid1 could not be found.") }
            jsonPath("$.instance") { value(instance) }
            jsonPath("$.size") { value(1) }
            jsonPath("$.content.title") { value(ListNotFoundException::class.java.simpleName) }
            jsonPath("$.content.status") { HttpStatus.NOT_FOUND.value() }
            jsonPath("$.content.detail") { value("List id $uuid1 could not be found.") }
          }
          verify(exactly = 1) { mockLrmItemService.patch(any(), mapOf("name" to lrmItem().name)) }
        }
      }
    }

    describe("/items/{id}/list-associations") {
      describe("delete") {
        it("item is removed from list") {
          val lrmListName = "Lorem List Name"
          every {
            mockAssociationService.deleteByItemIdAndListId(itemUuid = uuid1, listUuid = uuid2)
          } returns Pair(lrmItem().name, lrmListName)
          val instance = "/items/$uuid1/list-associations"
          mockMvc.delete(instance) {
            content = Json.encodeToString(uuid2.toString())
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
          verify(exactly = 1) { mockAssociationService.deleteByItemIdAndListId(any(), any()) }
        }

        it("item is not found") {
          every { mockAssociationService.deleteByItemIdAndListId(itemUuid = uuid1, listUuid = uuid2) } throws ItemNotFoundException(uuid1)
          val instance = "/items/$uuid1/list-associations"
          mockMvc.delete(instance) {
            content = Json.encodeToString(uuid2.toString())
            contentType = MediaType.APPLICATION_JSON
          }.andExpect {
            status { isNotFound() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.disposition") { value(DispositionOfProblem.FAILURE.nameAsLowercase()) }
            jsonPath("$.method") { value(HttpMethod.DELETE.name().lowercase()) }
            jsonPath("$.message") { value("Item id $uuid1 could not be found.") }
            jsonPath("$.instance") { value(instance) }
            jsonPath("$.size") { value(1) }
            jsonPath("$.content.title") { value(ItemNotFoundException::class.java.simpleName) }
            jsonPath("$.content.status") { HttpStatus.NOT_FOUND.value() }
            jsonPath("$.content.detail") { value("Item id $uuid1 could not be found.") }
          }
          verify(exactly = 1) { mockAssociationService.deleteByItemIdAndListId(any(), any()) }
        }

        it("list is not found") {
          every { mockAssociationService.deleteByItemIdAndListId(itemUuid = uuid1, listUuid = uuid2) } throws ListNotFoundException(uuid2)
          val instance = "/items/$uuid1/list-associations"
          mockMvc.delete(instance) {
            content = Json.encodeToString(uuid2.toString())
            contentType = MediaType.APPLICATION_JSON
          }.andExpect {
            status { isNotFound() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.disposition") { value(DispositionOfProblem.FAILURE.nameAsLowercase()) }
            jsonPath("$.method") { value(HttpMethod.DELETE.name().lowercase()) }
            jsonPath("$.message") { value("List id $uuid2 could not be found.") }
            jsonPath("$.instance") { value(instance) }
            jsonPath("$.size") { value(1) }
            jsonPath("$.content.title") { value(ListNotFoundException::class.java.simpleName) }
            jsonPath("$.content.status") { HttpStatus.NOT_FOUND.value() }
            jsonPath("$.content.detail") { value("List id $uuid2 could not be found.") }
          }
          verify(exactly = 1) { mockAssociationService.deleteByItemIdAndListId(any(), any()) }
        }
      }

      describe("patch") {
        it("item is moved from one list to another list") {
          val fromListName = "List A"
          val toListName = "List B"
          every {
            mockAssociationService.updateList(itemUuid = uuid1, currentListUuid = uuid2, newListUuid = uuid3)
          } returns Triple(lrmItem().name, fromListName, toListName)
          val instance = "/items/$uuid1/list-associations"
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
          verify(exactly = 1) { mockAssociationService.updateList(any(), any(), any()) }
        }

        it("item is not moved") {
          every {
            mockAssociationService.updateList(itemUuid = uuid1, currentListUuid = uuid2, newListUuid = uuid3)
          } throws ApiException(httpStatus = HttpStatus.I_AM_A_TEAPOT, responseMessage = "Api Exception Detail")
          val instance = "/items/$uuid1/list-associations"
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
          verify(exactly = 1) { mockAssociationService.updateList(any(), any(), any()) }
        }
      }

      describe("post") {
        it("item is added to list") {
          val lrmListName = "Lorem List Name"
          every { mockAssociationService.create(itemUuid = uuid1, listUuid = uuid2) } returns Pair(lrmItem().name, lrmListName)
          val instance = "/items/$uuid1/list-associations"
          mockMvc.post(instance) {
            content = Json.encodeToString(uuid2.toString())
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
          verify(exactly = 1) { mockAssociationService.create(any(), any()) }
        }

        it("item is not found") {
          every { mockAssociationService.create(itemUuid = uuid1, listUuid = uuid2) } throws ItemNotFoundException(uuid1)
          val instance = "/items/$uuid1/list-associations"
          mockMvc.post(instance) {
            content = Json.encodeToString(uuid2.toString())
            contentType = MediaType.APPLICATION_JSON
          }.andExpect {
            status { isNotFound() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.disposition") { value(DispositionOfProblem.FAILURE.nameAsLowercase()) }
            jsonPath("$.method") { value(HttpMethod.POST.name().lowercase()) }
            jsonPath("$.message") { value("Item id $uuid1 could not be found.") }
            jsonPath("$.instance") { value(instance) }
            jsonPath("$.size") { value(1) }
            jsonPath("$.content.title") { value(ItemNotFoundException::class.java.simpleName) }
            jsonPath("$.content.status") { HttpStatus.NOT_FOUND.value() }
            jsonPath("$.content.detail") { value("Item id $uuid1 could not be found.") }
          }
          verify(exactly = 1) { mockAssociationService.create(any(), any()) }
        }

        it("list is not found") {
          every { mockAssociationService.create(itemUuid = uuid1, listUuid = uuid2) } throws ListNotFoundException(uuid2)
          val instance = "/items/$uuid1/list-associations"
          mockMvc.post(instance) {
            content = Json.encodeToString(uuid2.toString())
            contentType = MediaType.APPLICATION_JSON
          }.andExpect {
            status { isNotFound() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.disposition") { value(DispositionOfProblem.FAILURE.nameAsLowercase()) }
            jsonPath("$.method") { value(HttpMethod.POST.name().lowercase()) }
            jsonPath("$.message") { value("List id $uuid2 could not be found.") }
            jsonPath("$.instance") { value(instance) }
            jsonPath("$.size") { value(1) }
            jsonPath("$.content.title") { value(ListNotFoundException::class.java.simpleName) }
            jsonPath("$.content.status") { HttpStatus.NOT_FOUND.value() }
            jsonPath("$.content.detail") { value("List id $uuid2 could not be found.") }
          }
          verify(exactly = 1) { mockAssociationService.create(any(), any()) }
        }
      }
    }

    describe("/items/{id}/list-associations/count") {
      describe("get") {
        it("count of list associations is returned") {
          every { mockAssociationService.countForItemId(uuid1) } returns 999
          val instance = "/items/$uuid1/list-associations/count"
          mockMvc.get(instance).andExpect {
            status { isOk() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
            jsonPath("$.method") { value(HttpMethod.GET.name().lowercase()) }
            jsonPath("$.message") { value("Item is associated with 999 lists.") }
            jsonPath("$.instance") { value(instance) }
            jsonPath("$.size") { value(1) }
            jsonPath("$.content.length()") { value(1) }
            jsonPath("$.content.value") { value(999) }
          }
          verify(exactly = 1) { mockAssociationService.countForItemId(any()) }
        }

        it("item is not found") {
          every { mockAssociationService.countForItemId(uuid1) } throws ApiException(httpStatus = HttpStatus.NOT_FOUND)
          val instance = "/items/$uuid1/list-associations/count"
          mockMvc.get(instance).andExpect {
            status { isNotFound() }
            jsonPath("$.disposition") { value(DispositionOfProblem.FAILURE.nameAsLowercase()) }
            jsonPath("$.method") { value(HttpMethod.GET.name().lowercase()) }
            jsonPath("$.instance") { value(instance) }
            jsonPath("$.size") { value(1) }
            jsonPath("$.content.status") { HttpStatus.NOT_FOUND.value() }
          }
          verify(exactly = 1) { mockAssociationService.countForItemId(any()) }
        }
      }
    }

    describe("/items/{id}/list-associations/delete-all") {
      it("item is removed from all lists") {
        every { mockAssociationService.deleteAllOfItem(uuid1) } returns Pair(lrmItem().name, 999)
        val instance = "/items/$uuid1/list-associations/delete-all"
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
        verify(exactly = 1) { mockAssociationService.deleteAllOfItem(any()) }
      }
    }

    describe("/items/count") {
      describe("get") {
        it("count of items is returned") {
          every { mockLrmItemService.count() } returns 999
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
