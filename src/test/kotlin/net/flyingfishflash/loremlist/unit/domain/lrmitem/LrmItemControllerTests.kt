package net.flyingfishflash.loremlist.unit.domain.lrmitem

import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.extensions.spring.SpringExtension
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.datetime.Clock.System.now
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.flyingfishflash.loremlist.core.configuration.SerializationConfig
import net.flyingfishflash.loremlist.core.exceptions.ApiException
import net.flyingfishflash.loremlist.core.response.advice.ApiExceptionHandler.Companion.VALIDATION_FAILURE_MESSAGE
import net.flyingfishflash.loremlist.core.response.structure.DispositionOfProblem
import net.flyingfishflash.loremlist.core.response.structure.DispositionOfSuccess
import net.flyingfishflash.loremlist.domain.LrmComponentType
import net.flyingfishflash.loremlist.domain.association.AssociationService
import net.flyingfishflash.loremlist.domain.association.data.AssociationCreatedResponse
import net.flyingfishflash.loremlist.domain.lrmitem.ItemNotFoundException
import net.flyingfishflash.loremlist.domain.lrmitem.LrmItem
import net.flyingfishflash.loremlist.domain.lrmitem.LrmItemController
import net.flyingfishflash.loremlist.domain.lrmitem.LrmItemService
import net.flyingfishflash.loremlist.domain.lrmitem.data.LrmItemDeleteResponse
import net.flyingfishflash.loremlist.domain.lrmitem.data.LrmItemRequest
import net.flyingfishflash.loremlist.domain.lrmlist.ListNotFoundException
import net.flyingfishflash.loremlist.domain.lrmlist.LrmList
import net.flyingfishflash.loremlist.domain.lrmlist.succinct
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
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
@Import(SerializationConfig::class)
class LrmItemControllerTests(mockMvc: MockMvc) : DescribeSpec() {
  override fun extensions() = listOf(SpringExtension)

  @MockkBean
  lateinit var mockLrmItemService: LrmItemService

  @MockkBean
  lateinit var mockAssociationService: AssociationService

  init {
    val now = now()
    val id0 = UUID.fromString("00000000-0000-4000-a000-000000000000")
    val id1 = UUID.fromString("00000000-0000-4000-a000-000000000001")
    val id2 = UUID.fromString("00000000-0000-4000-a000-000000000002")
    val id3 = UUID.fromString("00000000-0000-4000-a000-000000000003")
    val lrmItemRequest = LrmItemRequest("Lorem Item Name", "Lorem Item Description")

    fun lrmItem(): LrmItem = LrmItem(
      id = id0,
      name = lrmItemRequest.name,
      description = lrmItemRequest.description,
      quantity = 0,
      created = now,
      createdBy = "Lorem Ipsum Created By",
      updated = now,
      updatedBy = "Lorem Ipsum Updated By",
      lists = null,

    )
    fun lrmItemWithEmptyLists() = lrmItem().copy(lists = setOf())
    fun lrmItem1(): LrmItem = LrmItem(
      id = id1,
      name = lrmItemRequest.name,
      description = lrmItemRequest.description,
      quantity = 0,
      created = now,
      createdBy = "Lorem Ipsum Created By",
      updated = now,
      updatedBy = "Lorem Ipsum Updated By",
      lists = null,
    )
    fun lrmList2(): LrmList = LrmList(
      id = id2,
      name = "Lorem List Name (id2)",
      description = "Lorem List Description",
      public = true,
      created = now,
      createdBy = "Lorem Ipsum Created By",
      updated = now,
      updatedBy = "Lorem Ipsum Updated By",
      items = null,
    )
    fun lrmList3(): LrmList = LrmList(
      id = id3,
      name = "Lorem List Name (id3)",
      description = "Lorem List Description",
      public = true,
      created = now,
      createdBy = "Lorem Ipsum Created By",
      updated = now,
      updatedBy = "Lorem Ipsum Updated By",
      items = null,
    )

    afterEach { clearAllMocks() }
    afterSpec { unmockkAll() }

    describe("/items") {
      describe("delete") {
        it("items are deleted") {
          val mockReturn =
            LrmItemDeleteResponse(itemNames = listOf("Deleted Lorem Item Name"), associatedListNames = listOf("Associated Lorem List Name"))
          every { mockLrmItemService.deleteByOwner(owner = ofType(String::class)) } returns mockReturn
          val instance = "/items"
          mockMvc.delete(instance) {
            with(jwt())
            with(csrf())
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
          every { mockLrmItemService.findByOwner(owner = ofType(String::class)) } returns mockReturn
          val instance = "/items"
          mockMvc.get(instance) {
            with(jwt())
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
          verify(exactly = 1) { mockLrmItemService.findByOwner(owner = ofType(String::class)) }
        }

        it("items are found ?includeLists=false") {
          val mockReturn = listOf(lrmItem())
          every { mockLrmItemService.findByOwner(owner = ofType(String::class)) } returns mockReturn
          val instance = "/items?includeLists=false"
          mockMvc.get(instance) {
            with(jwt())
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
          verify(exactly = 1) { mockLrmItemService.findByOwner(owner = ofType(String::class)) }
        }

        it("items are found ?includeLists=true") {
          val mockReturn = listOf(lrmItemWithEmptyLists())
          every { mockLrmItemService.findByOwnerIncludeLists(owner = ofType(String::class)) } returns mockReturn
          val instance = "/items?includeLists=true"
          mockMvc.get(instance) {
            with(jwt())
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
          verify(exactly = 1) { mockLrmItemService.findByOwnerIncludeLists(owner = ofType(String::class)) }
        }
      }

      describe("post") {
        it("item is created") {
          println(Json.encodeToString(lrmItemRequest))
          every { mockLrmItemService.create(lrmItemRequest, owner = ofType(String::class)) } returns lrmItem()
          val instance = "/items"
          mockMvc.post(instance) {
            with(jwt())
            with(csrf())
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
          verify(exactly = 1) { mockLrmItemService.create(lrmItemRequest, owner = ofType(String::class)) }
        }

        it("item is not created") {
          println(Json.encodeToString(lrmItemRequest))
          every { mockLrmItemService.create(lrmItemRequest, owner = ofType(String::class)) } throws ApiException()
          val instance = "/items"
          mockMvc.post(instance) {
            with(jwt())
            with(csrf())
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
          verify(exactly = 1) { mockLrmItemService.create(lrmItemRequest, owner = ofType(String::class)) }
        }

        it("requested item name is an empty string") {
          val instance = "/items"
          mockMvc.post(instance) {
            with(jwt())
            with(csrf())
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
          verify(exactly = 0) { mockLrmItemService.create(ofType(LrmItemRequest::class), owner = ofType(String::class)) }
        }

        it("requested item description is an empty string") {
          val instance = "/items"
          mockMvc.post(instance) {
            with(jwt())
            with(csrf())
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
          verify(exactly = 0) { mockLrmItemService.create(ofType(LrmItemRequest::class), owner = ofType(String::class)) }
        }

        it("requested item quantity is less than 0") {
          val instance = "/items"
          mockMvc.post(instance) {
            with(jwt())
            with(csrf())
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
          verify(exactly = 0) { mockLrmItemService.create(ofType(LrmItemRequest::class), owner = ofType(String::class)) }
        }
      }
    }

    describe("/items/{item-id}") {
      describe("delete") {
        it("item is deleted") {
          // nonsensical conditioning of the delete response:
          // if the count of item to list associations is 0, then associatedListNames should be an empty list
          every { mockLrmItemService.deleteByOwnerAndId(id = id1, owner = ofType(String::class), removeListAssociations = false) } returns
            LrmItemDeleteResponse(itemNames = listOf("dolor sit amet"), associatedListNames = listOf("Lorem Ipsum"))
          val instance = "/items/$id1"
          mockMvc.delete(instance) {
            with(jwt())
            with(csrf())
          }.andExpect {
            status { isOk() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
            jsonPath("$.method") { value(HttpMethod.DELETE.name().lowercase()) }
            jsonPath("$.message") { value("Deleted item id $id1.") }
            jsonPath("$.instance") { value(instance) }
            jsonPath("$.size") { value(1) }
            jsonPath("$.content.itemNames.length()") { value(1) }
            jsonPath("$.content.itemNames.[0]") { value("dolor sit amet") }
            jsonPath("$.content.associatedListNames.[0]") { value("Lorem Ipsum") }
          }
          verify(exactly = 1) {
            mockLrmItemService.deleteByOwnerAndId(
              id = ofType(UUID::class),
              owner = ofType(String::class),
              removeListAssociations = ofType(Boolean::class),
            )
          }
        }

        it("item is not found") {
          every {
            mockLrmItemService.deleteByOwnerAndId(id = id1, owner = ofType(String::class), removeListAssociations = false)
          } throws ItemNotFoundException(id1)
          val expectedMessage = ItemNotFoundException.defaultMessage()
          val instance = "/items/$id1"
          mockMvc.delete(instance) {
            with(jwt())
            with(csrf())
          }.andExpect {
            status { isNotFound() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.disposition") { value(DispositionOfProblem.FAILURE.nameAsLowercase()) }
            jsonPath("$.method") { value(HttpMethod.DELETE.name().lowercase()) }
            jsonPath("$.message") { value(expectedMessage) }
            jsonPath("$.instance") { value(instance) }
            jsonPath("$.size") { value(1) }
            jsonPath("$.content.title") { value(ItemNotFoundException::class.java.simpleName) }
            jsonPath("$.content.status") { HttpStatus.NOT_FOUND.value() }
            jsonPath("$.content.detail") { value(expectedMessage) }
          }
          verify(exactly = 1) {
            mockLrmItemService.deleteByOwnerAndId(
              id = ofType(UUID::class),
              owner = ofType(String::class),
              removeListAssociations = ofType(Boolean::class),
            )
          }
        }
      }

      describe("get") {
        it("item is found") {
          every { mockLrmItemService.findByOwnerAndId(id = id1, owner = ofType(String::class)) } returns lrmItem()
          val instance = "/items/$id1"
          mockMvc.get(instance) {
            with(jwt())
          }.andExpect {
            status { isOk() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
            jsonPath("$.method") { value(HttpMethod.GET.name().lowercase()) }
            jsonPath("$.message") { value("retrieved item id $id1") }
            jsonPath("$.instance") { value(instance) }
            jsonPath("$.size") { value(1) }
            jsonPath("$.content.description") { value(lrmItem().description) }
            jsonPath("$.content.name") { value(lrmItem().name) }
            jsonPath("$.content.lists") { doesNotExist() }
          }
          verify(exactly = 1) { mockLrmItemService.findByOwnerAndId(id = id1, owner = ofType(String::class)) }
        }

        it("item is found ?includeLists=false") {
          every { mockLrmItemService.findByOwnerAndId(id = id1, owner = ofType(String::class)) } returns lrmItem()
          val instance = "/items/$id1?includeLists=false"
          mockMvc.get(instance) {
            with(jwt())
          }.andExpect {
            status { isOk() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
            jsonPath("$.method") { value(HttpMethod.GET.name().lowercase()) }
            jsonPath("$.message") { value("retrieved item id $id1") }
            jsonPath("$.instance") { value(instance.substringBeforeLast("?").removeSuffix(instance)) }
            jsonPath("$.size") { value(1) }
            jsonPath("$.content.description") { value(lrmItem().description) }
            jsonPath("$.content.name") { value(lrmItem().name) }
            jsonPath("$.content.lists") { doesNotExist() }
          }
          verify(exactly = 1) { mockLrmItemService.findByOwnerAndId(id = ofType(UUID::class), owner = ofType(String::class)) }
        }

        it("item is found ?includeLists=true") {
          every { mockLrmItemService.findByOwnerAndIdIncludeLists(id = id1, owner = ofType(String::class)) } returns lrmItemWithEmptyLists()
          val instance = "/items/$id1?includeLists=true"
          mockMvc.get(instance) {
            with(jwt())
          }.andExpect {
            status { isOk() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
            jsonPath("$.method") { value(HttpMethod.GET.name().lowercase()) }
            jsonPath("$.message") { value("retrieved item id $id1 and it's associated lists") }
            jsonPath("$.instance") { value(instance.substringBeforeLast("?").removeSuffix(instance)) }
            jsonPath("$.size") { value(1) }
            jsonPath("$.content.description") { value(lrmItemWithEmptyLists().description) }
            jsonPath("$.content.name") { value(lrmItemWithEmptyLists().name) }
            jsonPath("$.content.lists") {
              isArray()
              isEmpty()
            }
          }
          verify(exactly = 1) { mockLrmItemService.findByOwnerAndIdIncludeLists(id = ofType(UUID::class), owner = ofType(String::class)) }
        }

        it("item is not found") {
          every { mockLrmItemService.findByOwnerAndId(id = id1, owner = ofType(String::class)) } throws ItemNotFoundException(id1)
          val expectedMessage = ItemNotFoundException.defaultMessage()
          val instance = "/items/$id1"
          mockMvc.get(instance) {
            with(jwt())
          }.andExpect {
            status { isNotFound() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.disposition") { value(DispositionOfProblem.FAILURE.nameAsLowercase()) }
            jsonPath("$.method") { value(HttpMethod.GET.name().lowercase()) }
            jsonPath("$.message") { value(expectedMessage) }
            jsonPath("$.instance") { value(instance) }
            jsonPath("$.size") { value(1) }
            jsonPath("$.content.title") { value(ItemNotFoundException::class.java.simpleName) }
            jsonPath("$.content.status") { HttpStatus.NOT_FOUND.value() }
            jsonPath("$.content.detail") { value(expectedMessage) }
          }
          verify(exactly = 1) { mockLrmItemService.findByOwnerAndId(id = ofType(UUID::class), owner = ofType(String::class)) }
        }
      }

      describe("patch") {
        it("item is found and updated") {
          every {
            mockLrmItemService.patchByOwnerAndId(
              id = id1,
              owner = ofType(String::class),
              patchRequest = any(),
            )
          } returns Pair(lrmItem(), true)
          val instance = "/items/$id1"
          mockMvc.patch(instance) {
            with(jwt())
            with(csrf())
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
          verify(exactly = 1) {
            mockLrmItemService.patchByOwnerAndId(id = ofType(UUID::class), owner = ofType(String::class), mapOf("name" to lrmItem().name))
          }
        }

        it("item is found and not updated") {
          every {
            mockLrmItemService.patchByOwnerAndId(
              id = id1,
              owner = ofType(String::class),
              patchRequest = any(),
            )
          } returns Pair(lrmItem(), false)
          val instance = "/items/$id1"
          mockMvc.patch(instance) {
            with(jwt())
            with(csrf())
            content = Json.encodeToString(mapOf("name" to lrmItem().name))
            contentType = MediaType.APPLICATION_JSON
          }.andExpect {
            status { isNoContent() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
            jsonPath("$.method") { value(HttpMethod.PATCH.name().lowercase()) }
            jsonPath("$.message") { value("not patched - item is up-to-date") }
            jsonPath("$.instance") { value(instance) }
            jsonPath("$.size") { value(1) }
            jsonPath("$.content.description") { value(lrmItem().description) }
            jsonPath("$.content.name") { value(lrmItem().name) }
            jsonPath("$.content.items") { doesNotExist() }
          }
          verify(exactly = 1) {
            mockLrmItemService.patchByOwnerAndId(id = ofType(UUID::class), owner = ofType(String::class), mapOf("name" to lrmItem().name))
          }
        }

        it("item is not found") {
          every {
            mockLrmItemService.patchByOwnerAndId(id = id1, owner = ofType(String::class), patchRequest = any())
          } throws ListNotFoundException(id1)
          val expectedMessage = ListNotFoundException.defaultMessage()
          val instance = "/items/$id1"
          mockMvc.patch(instance) {
            with(jwt())
            with(csrf())
            content = Json.encodeToString(mapOf("name" to lrmItem().name))
            contentType = MediaType.APPLICATION_JSON
          }.andExpect {
            status { isNotFound() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.disposition") { value(DispositionOfProblem.FAILURE.nameAsLowercase()) }
            jsonPath("$.method") { value(HttpMethod.PATCH.name().lowercase()) }
            jsonPath("$.message") { value(expectedMessage) }
            jsonPath("$.instance") { value(instance) }
            jsonPath("$.size") { value(1) }
            jsonPath("$.content.title") { value(ListNotFoundException::class.java.simpleName) }
            jsonPath("$.content.status") { HttpStatus.NOT_FOUND.value() }
            jsonPath("$.content.detail") { value(expectedMessage) }
          }
          verify(exactly = 1) {
            mockLrmItemService.patchByOwnerAndId(id = ofType(UUID::class), owner = ofType(String::class), mapOf("name" to lrmItem().name))
          }
        }
      }
    }

    describe("/items/{item-id}/lists/{list-id}") {
      describe("delete") {
        it("item is removed from list") {
          val lrmListName = "Lorem List Name"
          every {
            mockAssociationService.deleteByItemIdAndListId(itemId = id1, listId = id2, componentsOwner = ofType(String::class))
          } returns Pair(lrmItem().name, lrmListName)
          val instance = "/items/$id1/lists/$id2"
          mockMvc.delete(instance) {
            with(jwt())
            with(csrf())
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
          verify(exactly = 1) {
            mockAssociationService.deleteByItemIdAndListId(
              itemId = ofType(UUID::class),
              listId = ofType(UUID::class),
              componentsOwner = ofType(String::class),
            )
          }
        }

        it("item is not found") {
          every {
            mockAssociationService.deleteByItemIdAndListId(itemId = id1, listId = id2, componentsOwner = ofType(String::class))
          } throws ItemNotFoundException(id1)
          val expectedMessage = ItemNotFoundException.defaultMessage()
          val instance = "/items/$id1/lists/$id2"
          mockMvc.delete(instance) {
            with(jwt())
            with(csrf())
            contentType = MediaType.APPLICATION_JSON
          }.andExpect {
            status { isNotFound() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.disposition") { value(DispositionOfProblem.FAILURE.nameAsLowercase()) }
            jsonPath("$.method") { value(HttpMethod.DELETE.name().lowercase()) }
            jsonPath("$.message") { value(expectedMessage) }
            jsonPath("$.instance") { value(instance) }
            jsonPath("$.size") { value(1) }
            jsonPath("$.content.title") { value(ItemNotFoundException::class.java.simpleName) }
            jsonPath("$.content.status") { HttpStatus.NOT_FOUND.value() }
            jsonPath("$.content.detail") { value(expectedMessage) }
          }
          verify(exactly = 1) {
            mockAssociationService.deleteByItemIdAndListId(
              itemId = ofType(UUID::class),
              ofType(UUID::class),
              componentsOwner = ofType(String::class),
            )
          }
        }

        it("list is not found") {
          every {
            mockAssociationService.deleteByItemIdAndListId(itemId = id1, listId = id2, componentsOwner = ofType(String::class))
          } throws ListNotFoundException(id2)
          val expectedMessage = ListNotFoundException.defaultMessage()
          val instance = "/items/$id1/lists/$id2"
          mockMvc.delete(instance) {
            with(jwt())
            with(csrf())
            contentType = MediaType.APPLICATION_JSON
          }.andExpect {
            status { isNotFound() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.disposition") { value(DispositionOfProblem.FAILURE.nameAsLowercase()) }
            jsonPath("$.method") { value(HttpMethod.DELETE.name().lowercase()) }
            jsonPath("$.message") { value(expectedMessage) }
            jsonPath("$.instance") { value(instance) }
            jsonPath("$.size") { value(1) }
            jsonPath("$.content.title") { value(ListNotFoundException::class.java.simpleName) }
            jsonPath("$.content.status") { HttpStatus.NOT_FOUND.value() }
            jsonPath("$.content.detail") { value(expectedMessage) }
          }
          verify(exactly = 1) {
            mockAssociationService.deleteByItemIdAndListId(
              itemId = ofType(UUID::class),
              listId = ofType(UUID::class),
              componentsOwner = ofType(String::class),
            )
          }
        }
      }

      describe("patch") {
        it("item is moved from one list to another list") {
          val fromListName = "List A"
          val toListName = "List B"
          every {
            mockAssociationService.updateList(
              itemId = id1,
              currentListId = id2,
              destinationListId = id3,
              componentsOwner = ofType(String::class),
            )
          } returns Triple(lrmItem().name, fromListName, toListName)
          val instance = "/items/$id1/lists/$id2/$id3"
          mockMvc.patch(instance) {
            with(jwt())
            with(csrf())
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
          verify(exactly = 1) {
            mockAssociationService.updateList(
              itemId = ofType(UUID::class),
              currentListId = ofType(UUID::class),
              destinationListId = ofType(UUID::class),
              componentsOwner = ofType(String::class),
            )
          }
        }

        it("item is not moved") {
          every {
            mockAssociationService.updateList(
              itemId = id1,
              currentListId = id2,
              destinationListId = id3,
              componentsOwner = ofType(String::class),
            )
          } throws ApiException(httpStatus = HttpStatus.I_AM_A_TEAPOT, responseMessage = "Api Exception Detail")
          val instance = "/items/$id1/lists/$id2/$id3"
          mockMvc.patch(instance) {
            with(jwt())
            with(csrf())
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
          verify(exactly = 1) {
            mockAssociationService.updateList(
              itemId = ofType(UUID::class),
              currentListId = ofType(UUID::class),
              destinationListId = ofType(UUID::class),
              componentsOwner = ofType(String::class),
            )
          }
        }
      }

      describe("post") {
        it("item is added to list") {
          val mockResponse = AssociationCreatedResponse(
            componentName = lrmItem1().name,
            associatedComponents = listOf(lrmList2().succinct()),
          )
          every {
            mockAssociationService.create(
              id = id1,
              idCollection = any(),
              LrmComponentType.Item,
              componentsOwner = ofType(String::class),
            )
          } returns mockResponse
          val instance = "/items/$id1/lists"
          mockMvc.post(instance) {
            with(jwt())
            with(csrf())
            // posted content is irrelevant for this test
            content = Json.encodeToString(setOf(UUID.randomUUID().toString()))
            contentType = MediaType.APPLICATION_JSON
          }.andExpect {
            status { isOk() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
            jsonPath("$.method") { value(HttpMethod.POST.name().lowercase()) }
            jsonPath("$.message") { value("Assigned item '${lrmItem().name}' to list '${lrmList2().name}'.") }
            jsonPath("$.instance") { value(instance) }
            jsonPath("$.size") { value(1) }
            jsonPath("$.content.componentName") { value(mockResponse.componentName) }
            jsonPath("$.content.associatedComponents.length()") { value(1) }
            jsonPath("$.content.associatedComponents[0].type") { value("list") }
            jsonPath("$.content.associatedComponents[0].id") { value(mockResponse.associatedComponents[0].id.toString()) }
            jsonPath("$.content.associatedComponents[0].name") { value(mockResponse.associatedComponents[0].name) }
          }
        }

        it("item is added to lists") {
          val mockResponse = AssociationCreatedResponse(
            componentName = lrmItem1().name,
            associatedComponents = listOf(lrmList2().succinct(), lrmList3().succinct()),
          )
          every {
            mockAssociationService.create(
              id = id1,
              idCollection = any(),
              LrmComponentType.Item,
              componentsOwner = ofType(String::class),
            )
          } returns mockResponse
          val instance = "/items/$id1/lists"
          mockMvc.post(instance) {
            with(jwt())
            with(csrf())
            // posted content is irrelevant for this test
            content = Json.encodeToString(setOf(UUID.randomUUID().toString()))
            contentType = MediaType.APPLICATION_JSON
          }.andExpect {
            status { isOk() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
            jsonPath("$.method") { value(HttpMethod.POST.name().lowercase()) }
            jsonPath("$.message") { value("Assigned item '${lrmItem1().name}' to ${mockResponse.associatedComponents.size} lists.") }
            jsonPath("$.instance") { value(instance) }
            jsonPath("$.size") { value(1) }
            jsonPath("$.content.componentName") { value(mockResponse.componentName) }
            jsonPath("$.content.associatedComponents.length()") { value(mockResponse.associatedComponents.size) }
            jsonPath("$.content.associatedComponents[0].type") { value("list") }
            jsonPath("$.content.associatedComponents[0].id") { value(mockResponse.associatedComponents[0].id.toString()) }
            jsonPath("$.content.associatedComponents[0].name") { value(mockResponse.associatedComponents[0].name) }
          }
        }

        it("item is not found") {
          every {
            mockAssociationService.create(
              id = id1,
              idCollection = any(),
              LrmComponentType.Item,
              componentsOwner = ofType(String::class),
            )
          } throws ItemNotFoundException(id1)
          val expectedMessage = ItemNotFoundException.defaultMessage()
          val instance = "/items/$id1/lists"
          mockMvc.post(instance) {
            with(jwt())
            with(csrf())
            // posted content is irrelevant for this test
            content = Json.encodeToString(setOf(UUID.randomUUID().toString()))
            contentType = MediaType.APPLICATION_JSON
          }.andExpect {
            status { isNotFound() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.disposition") { value(DispositionOfProblem.FAILURE.nameAsLowercase()) }
            jsonPath("$.method") { value(HttpMethod.POST.name().lowercase()) }
            jsonPath("$.message") { value(expectedMessage) }
            jsonPath("$.instance") { value(instance) }
            jsonPath("$.size") { value(1) }
            jsonPath("$.content.title") { value(ItemNotFoundException::class.java.simpleName) }
            jsonPath("$.content.status") { ItemNotFoundException.HTTP_STATUS.value() }
            jsonPath("$.content.detail") { value(expectedMessage) }
          }
        }

        it("list is not found") {
          every {
            mockAssociationService.create(
              id = id1,
              idCollection = any(),
              LrmComponentType.Item,
              componentsOwner = ofType(String::class),
            )
          } throws ListNotFoundException(id2)
          val expectedMessage = ListNotFoundException.defaultMessage()
          val instance = "/items/$id1/lists"
          mockMvc.post(instance) {
            with(jwt())
            with(csrf())
            // posted content is irrelevant for this test
            content = Json.encodeToString(setOf(UUID.randomUUID().toString()))
            contentType = MediaType.APPLICATION_JSON
          }.andExpect {
            status { isNotFound() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.disposition") { value(DispositionOfProblem.FAILURE.nameAsLowercase()) }
            jsonPath("$.method") { value(HttpMethod.POST.name().lowercase()) }
            jsonPath("$.message") { value(expectedMessage) }
            jsonPath("$.instance") { value(instance) }
            jsonPath("$.size") { value(1) }
            jsonPath("$.content.title") { value(ListNotFoundException::class.java.simpleName) }
            jsonPath("$.content.status") { ListNotFoundException.HTTP_STATUS.value() }
            jsonPath("$.content.detail") { value(expectedMessage) }
          }
        }
      }
    }

    describe("/items/{item-id}/lists/count") {
      describe("get") {
        it("count of list associations is returned") {
          every {
            mockAssociationService.countByIdAndItemOwnerForItem(
              itemId = id1,
              itemOwner = ofType(String::class),
            )
          } returns 999
          val instance = "/items/$id1/lists/count"
          mockMvc.get(instance) {
            with(jwt())
          }.andExpect {
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
          verify(exactly = 1) {
            mockAssociationService.countByIdAndItemOwnerForItem(
              itemId = ofType(UUID::class),
              itemOwner = ofType(String::class),
            )
          }
        }

        it("item is not found") {
          every {
            mockAssociationService.countByIdAndItemOwnerForItem(
              itemId = id1,
              itemOwner = ofType(String::class),
            )
          } throws ApiException(httpStatus = HttpStatus.NOT_FOUND)
          val instance = "/items/$id1/lists/count"
          mockMvc.get(instance) {
            with(jwt())
          }.andExpect {
            status { isNotFound() }
            jsonPath("$.disposition") { value(DispositionOfProblem.FAILURE.nameAsLowercase()) }
            jsonPath("$.method") { value(HttpMethod.GET.name().lowercase()) }
            jsonPath("$.instance") { value(instance) }
            jsonPath("$.size") { value(1) }
            jsonPath("$.content.status") { HttpStatus.NOT_FOUND.value() }
          }
          verify(exactly = 1) {
            mockAssociationService.countByIdAndItemOwnerForItem(
              itemId = ofType(UUID::class),
              itemOwner = ofType(String::class),
            )
          }
        }
      }
    }

    describe("/items/{item-id}/lists") {
      it("item is removed from all lists") {
        every {
          mockAssociationService.deleteByItemOwnerAndItemId(itemId = ofType(UUID::class), itemOwner = ofType(String::class))
        } returns Pair(lrmItem().name, 999)
        val instance = "/items/$id1/lists"
        mockMvc.delete(instance) {
          with(jwt())
          with(csrf())
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
        verify(
          exactly = 1,
        ) { mockAssociationService.deleteByItemOwnerAndItemId(itemId = ofType(UUID::class), itemOwner = ofType(String::class)) }
      }
    }

    describe("/items/count") {
      describe("get") {
        it("count of items is returned") {
          every { mockLrmItemService.countByOwner(owner = ofType(String::class)) } returns 999
          val instance = "/items/count"
          mockMvc.get(instance) {
            with(jwt())
          }.andExpect {
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

    describe("/items/with-no-lists") {
      describe("get") {
        it("items with no list association are found") {
          val mockReturn = listOf(lrmItem())
          val expectedMessage = "Retrieved ${mockReturn.size} items that are not a part of a list."
          every { mockLrmItemService.findByOwnerAndHavingNoListAssociations(owner = ofType(String::class)) } returns mockReturn
          val instance = "/items/with-no-lists"
          mockMvc.get(instance) {
            with(jwt())
            contentType = MediaType.APPLICATION_JSON
          }.andExpect {
            status { isOk() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
            jsonPath("$.method") { value(HttpMethod.GET.name().lowercase()) }
            jsonPath("$.message") { value(expectedMessage) }
            jsonPath("$.instance") { value(instance) }
            jsonPath("$.size") { value(mockReturn.size) }
            jsonPath("$.content") { exists() }
            jsonPath("$.content") { isArray() }
            jsonPath("$.content.[0].name") { value(mockReturn[0].name) }
            jsonPath("$.content.[0].description") { value(mockReturn[0].description) }
          }
          verify(exactly = 1) { mockLrmItemService.findByOwnerAndHavingNoListAssociations(owner = ofType(String::class)) }
        }
      }
    }
  }
}
