package net.flyingfishflash.loremlist.unit.api

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
import net.flyingfishflash.loremlist.api.LrmItemApiService
import net.flyingfishflash.loremlist.api.LrmItemController
import net.flyingfishflash.loremlist.api.data.request.LrmItemCreateRequest
import net.flyingfishflash.loremlist.api.data.response.ApiServiceResponse
import net.flyingfishflash.loremlist.api.data.response.LrmItemDeletedResponse
import net.flyingfishflash.loremlist.api.data.response.LrmItemResponse
import net.flyingfishflash.loremlist.core.configuration.SerializationConfig
import net.flyingfishflash.loremlist.core.response.structure.ApiMessageNumeric
import net.flyingfishflash.loremlist.core.response.structure.DispositionOfProblem
import net.flyingfishflash.loremlist.core.response.structure.DispositionOfSuccess
import net.flyingfishflash.loremlist.domain.exceptions.DomainException
import net.flyingfishflash.loremlist.domain.lrmitem.ItemNotFoundException
import net.flyingfishflash.loremlist.domain.lrmitem.LrmItem
import net.flyingfishflash.loremlist.domain.lrmlist.ListNotFoundException
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
import java.util.*

/**
 * LrmListController Unit Tests
 */
@WebMvcTest(controllers = [LrmItemController::class])
@Import(SerializationConfig::class)
class LrmItemControllerTests(mockMvc: MockMvc) : DescribeSpec() {
  override fun extensions() = listOf(SpringExtension)

  @MockkBean
  lateinit var mockLrmItemApiService: LrmItemApiService

  init {
    val now = now()
    val id0 = UUID.fromString("00000000-0000-4000-a000-000000000000")
    val id1 = UUID.fromString("00000000-0000-4000-a000-000000000001")
//    val id2 = UUID.fromString("00000000-0000-4000-a000-000000000002")
//    val id3 = UUID.fromString("00000000-0000-4000-a000-000000000003")
    val lrmItemCreateRequest = LrmItemCreateRequest(name = "Lorem Item Name", description = "Lorem Item Description", isSuppressed = false)

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

    describe("/items") {
      describe("delete") {
        it("items are deleted") {
          val serviceResponse =
            LrmItemDeletedResponse(itemNames = listOf("Deleted Lorem Item Name"), associatedListNames = listOf("Associated Lorem List Name"))
          val message = "Deleted all (${serviceResponse.itemNames.size}) of your items from ${serviceResponse.associatedListNames.size} lists."
          val mockReturn = ApiServiceResponse(serviceResponse, message)
          every { mockLrmItemApiService.deleteByOwner(owner = ofType(String::class)) } returns mockReturn
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
            jsonPath("$.message") { value(message) }
            jsonPath("$.instance") { value(instance) }
            jsonPath("$.size") { value(1) }
            jsonPath("$.content.itemNames[0]") { value(mockReturn.content.itemNames[0]) }
            jsonPath("$.content.associatedListNames[0]") { value(mockReturn.content.associatedListNames[0]) }
          }
        }
      }

      describe("get") {
        it("items are found") {
          val mockServiceResponse = listOf(LrmItemResponse.fromLrmItem(lrmItem()))
          val mockApiServiceResponse = ApiServiceResponse(mockServiceResponse, "message is irrelevant")
          every { mockLrmItemApiService.findByOwner(owner = ofType(String::class)) } returns mockApiServiceResponse
          val instance = "/items"
          mockMvc.get(instance) {
            with(jwt())
            contentType = MediaType.APPLICATION_JSON
          }.andExpect {
            status { isOk() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
            jsonPath("$.method") { value(HttpMethod.GET.name().lowercase()) }
            jsonPath("$.message") { value("message is irrelevant") }
            jsonPath("$.instance") { value(instance) }
            jsonPath("$.size") { value(mockServiceResponse.size) }
            jsonPath("$.content") { exists() }
            jsonPath("$.content") { isArray() }
            jsonPath("$.content.[0].name") { value(mockServiceResponse[0].name) }
            jsonPath("$.content.[0].description") { value(mockServiceResponse[0].description) }
            jsonPath("$.content.[0].items") {
              doesNotExist()
            }
          }
          verify(exactly = 1) { mockLrmItemApiService.findByOwner(owner = ofType(String::class)) }
        }
      }
    }

    describe("/items/{item-id}") {
      describe("delete") {
        it("item is deleted") {
          val mockServiceResponse = LrmItemDeletedResponse(itemNames = listOf("dolor sit amet"), associatedListNames = listOf("Lorem Ipsum"))
          val mockApiServiceResponse = ApiServiceResponse(mockServiceResponse, "message is irrelevant")
          every {
            mockLrmItemApiService.deleteByOwnerAndId(id = id1, owner = ofType(String::class), removeListAssociations = false)
          } returns mockApiServiceResponse
          val instance = "/items/$id1"
          mockMvc.delete(instance) {
            with(jwt())
            with(csrf())
          }.andExpect {
            status { isOk() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
            jsonPath("$.method") { value(HttpMethod.DELETE.name().lowercase()) }
            jsonPath("$.message") { mockApiServiceResponse.message }
            jsonPath("$.instance") { value(instance) }
            jsonPath("$.size") { value(1) }
            jsonPath("$.content.itemNames.length()") { value(1) }
            jsonPath("$.content.itemNames.[0]") { value(mockApiServiceResponse.content.itemNames[0]) }
            jsonPath("$.content.associatedListNames.[0]") { value(mockApiServiceResponse.content.associatedListNames[0]) }
          }
          verify(exactly = 1) {
            mockLrmItemApiService.deleteByOwnerAndId(
              id = ofType(UUID::class),
              owner = ofType(String::class),
              removeListAssociations = ofType(Boolean::class),
            )
          }
        }

        it("item is not found") {
          every {
            mockLrmItemApiService.deleteByOwnerAndId(id = id1, owner = ofType(String::class), removeListAssociations = false)
          } throws ItemNotFoundException(id1)
//          val expectedMessage = EntityNotFoundException.defaultMessage()
          val instance = "/items/$id1"
          mockMvc.delete(instance) {
            with(jwt())
            with(csrf())
          }.andExpect {
            status { isNotFound() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.disposition") { value(DispositionOfProblem.FAILURE.nameAsLowercase()) }
            jsonPath("$.method") { value(HttpMethod.DELETE.name().lowercase()) }
//            jsonPath("$.message") { value(expectedMessage) }
            jsonPath("$.instance") { value(instance) }
            jsonPath("$.size") { value(1) }
            jsonPath("$.content.title") { value(ItemNotFoundException::class.java.simpleName) }
            jsonPath("$.content.status") { HttpStatus.NOT_FOUND.value() }
//            jsonPath("$.content.detail") { value(expectedMessage) }
          }
          verify(exactly = 1) {
            mockLrmItemApiService.deleteByOwnerAndId(
              id = ofType(UUID::class),
              owner = ofType(String::class),
              removeListAssociations = ofType(Boolean::class),
            )
          }
        }
      }

      describe("get") {
        it("item is found") {
          val mockServiceResponse = LrmItemResponse.fromLrmItem(lrmItem())
          val mockApiServiceResponse = ApiServiceResponse(mockServiceResponse, "message is irrelevant")
          every { mockLrmItemApiService.findByOwnerAndId(id = id1, owner = ofType(String::class)) } returns
            mockApiServiceResponse
          val instance = "/items/$id1"
          mockMvc.get(instance) {
            with(jwt())
          }.andExpect {
            status { isOk() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
            jsonPath("$.method") { value(HttpMethod.GET.name().lowercase()) }
            jsonPath("$.message") { value("message is irrelevant") }
            jsonPath("$.instance") { value(instance) }
            jsonPath("$.size") { value(1) }
            jsonPath("$.content.description") { value(lrmItem().description) }
            jsonPath("$.content.name") { value(lrmItem().name) }
          }
          verify(exactly = 1) { mockLrmItemApiService.findByOwnerAndId(id = id1, owner = ofType(String::class)) }
        }

        it("item is not found") {
          every { mockLrmItemApiService.findByOwnerAndId(id = id1, owner = ofType(String::class)) } throws
            ItemNotFoundException(id1)
//          val expectedMessage = ItemNotFoundException.defaultMessage()
          val instance = "/items/$id1"
          mockMvc.get(instance) {
            with(jwt())
          }.andExpect {
            status { isNotFound() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.disposition") { value(DispositionOfProblem.FAILURE.nameAsLowercase()) }
            jsonPath("$.method") { value(HttpMethod.GET.name().lowercase()) }
//            jsonPath("$.message") { value(expectedMessage) }
            jsonPath("$.instance") { value(instance) }
            jsonPath("$.size") { value(1) }
            jsonPath("$.content.title") { value(ItemNotFoundException::class.java.simpleName) }
            jsonPath("$.content.status") { HttpStatus.NOT_FOUND.value() }
//            jsonPath("$.content.detail") { value(expectedMessage) }
          }
          verify(exactly = 1) { mockLrmItemApiService.findByOwnerAndId(id = ofType(UUID::class), owner = ofType(String::class)) }
        }
      }

      describe("patch") {
        it("item is found and updated") {
          val mockApiServiceResponse = ApiServiceResponse(LrmItemResponse.fromLrmItem(lrmItem()), "message is irrelevant")
          every {
            mockLrmItemApiService.patchByOwnerAndId(
              id = id1,
              owner = ofType(String::class),
              patchRequest = any(),
            )
          } returns mockApiServiceResponse
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
            jsonPath("$.message") { value(mockApiServiceResponse.message) }
            jsonPath("$.instance") { value(instance) }
            jsonPath("$.size") { value(1) }
            jsonPath("$.content.description") { value(mockApiServiceResponse.content.description) }
            jsonPath("$.content.name") { value(mockApiServiceResponse.content.name) }
          }
          verify(exactly = 1) {
            mockLrmItemApiService.patchByOwnerAndId(
              id = ofType(UUID::class),
              owner = ofType(String::class),
              mapOf("name" to lrmItem().name),
            )
          }
        }

//        it("item is found and not updated") {
//          every {
//            mockLrmItemApiService.patchByOwnerAndId(
//              id = id1,
//              owner = ofType(String::class),
//              patchRequest = any(),
//            )
//          } returns ApiServiceResponse(LrmItemResponse.fromLrmItem(lrmItem()), "")
//          val instance = "/items/$id1"
//          mockMvc.patch(instance) {
//            with(jwt())
//            with(csrf())
//            content = Json.encodeToString(mapOf("name" to lrmItem().name))
//            contentType = MediaType.APPLICATION_JSON
//          }.andExpect {
//            status { isNoContent() }
//            content { contentType(MediaType.APPLICATION_JSON) }
//            jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
//            jsonPath("$.method") { value(HttpMethod.PATCH.name().lowercase()) }
//            jsonPath("$.message") { value("not patched - item is up-to-date") }
//            jsonPath("$.instance") { value(instance) }
//            jsonPath("$.size") { value(1) }
//            jsonPath("$.content.description") { value(lrmItem().description) }
//            jsonPath("$.content.name") { value(lrmItem().name) }
//            jsonPath("$.content.items") { doesNotExist() }
//          }
//          verify(exactly = 1) {
//            mockLrmItemApiService.patchByOwnerAndId(
//              id = ofType(UUID::class),
//              owner = ofType(String::class),
//              mapOf("name" to lrmItem().name),
//            )
//          }
//        }

        it("item is not found") {
          every {
            mockLrmItemApiService.patchByOwnerAndId(id = id1, owner = ofType(String::class), patchRequest = any())
          } throws ListNotFoundException(id1)
//          val expectedMessage = ListNotFoundException.defaultMessage()
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
//            jsonPath("$.message") { value(expectedMessage) }
            jsonPath("$.instance") { value(instance) }
            jsonPath("$.size") { value(1) }
            jsonPath("$.content.title") { value(ListNotFoundException::class.java.simpleName) }
            jsonPath("$.content.status") { HttpStatus.NOT_FOUND.value() }
//            jsonPath("$.content.detail") { value(expectedMessage) }
          }
          verify(exactly = 1) {
            mockLrmItemApiService.patchByOwnerAndId(
              id = ofType(UUID::class),
              owner = ofType(String::class),
              mapOf("name" to lrmItem().name),
            )
          }
        }
      }
    }

    describe("/items/{item-id}/lists/count") {
      describe("get") {
        it("count of list associations is returned") {
          val mockApiServiceResponse = ApiServiceResponse(ApiMessageNumeric(999), "message is irrelevant")
          every {
            mockLrmItemApiService.countListAssociationsByItemIdAndItemOwner(
              itemId = id1,
              itemOwner = ofType(String::class),
            )
          } returns mockApiServiceResponse
          val instance = "/items/$id1/lists/count"
          mockMvc.get(instance) {
            with(jwt())
          }.andExpect {
            status { isOk() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
            jsonPath("$.method") { value(HttpMethod.GET.name().lowercase()) }
            jsonPath("$.message") { value(mockApiServiceResponse.message) }
            jsonPath("$.instance") { value(instance) }
            jsonPath("$.size") { value(1) }
            jsonPath("$.content.length()") { value(1) }
            jsonPath("$.content.value") { value(mockApiServiceResponse.content.value) }
          }
          verify(exactly = 1) {
            mockLrmItemApiService.countListAssociationsByItemIdAndItemOwner(
              itemId = ofType(UUID::class),
              itemOwner = ofType(String::class),
            )
          }
        }

        it("item is not found") {
          every {
            mockLrmItemApiService.countListAssociationsByItemIdAndItemOwner(
              itemId = id1,
              itemOwner = ofType(String::class),
            )
          } throws DomainException(httpStatus = HttpStatus.NOT_FOUND)
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
            mockLrmItemApiService.countListAssociationsByItemIdAndItemOwner(
              itemId = ofType(UUID::class),
              itemOwner = ofType(String::class),
            )
          }
        }
      }
    }

    describe("/items/count") {
      describe("get") {
        it("count of items is returned") {
          every {
            mockLrmItemApiService.countByOwner(owner = ofType(String::class))
          } returns ApiServiceResponse(ApiMessageNumeric(999L), "999 items.")
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
            jsonPath("$.content.value") { value(999) }
          }
        }
      }
    }

    describe("/items/with-no-lists") {
      describe("get") {
        it("items with no list association are found") {
          val mockApiServiceResponse = ApiServiceResponse(listOf(LrmItemResponse.fromLrmItem(lrmItem())), "message is irrelevant")
          every { mockLrmItemApiService.findByOwnerAndHavingNoListAssociations(owner = ofType(String::class)) } returns mockApiServiceResponse
          val instance = "/items/with-no-lists"
          mockMvc.get(instance) {
            with(jwt())
            contentType = MediaType.APPLICATION_JSON
          }.andExpect {
            status { isOk() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
            jsonPath("$.method") { value(HttpMethod.GET.name().lowercase()) }
            jsonPath("$.message") { value(mockApiServiceResponse.message) }
            jsonPath("$.instance") { value(instance) }
            jsonPath("$.size") { value(mockApiServiceResponse.content.size) }
            jsonPath("$.content") { exists() }
            jsonPath("$.content") { isArray() }
            jsonPath("$.content.[0].name") { value(mockApiServiceResponse.content[0].name) }
            jsonPath("$.content.[0].description") { value(mockApiServiceResponse.content[0].description) }
          }
          verify(exactly = 1) { mockLrmItemApiService.findByOwnerAndHavingNoListAssociations(owner = ofType(String::class)) }
        }
      }
    }
  }
}
