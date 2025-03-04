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
import net.flyingfishflash.loremlist.api.LrmListApiService
import net.flyingfishflash.loremlist.api.LrmListController
import net.flyingfishflash.loremlist.api.data.request.LrmListCreateRequest
import net.flyingfishflash.loremlist.api.data.response.ApiServiceResponse
import net.flyingfishflash.loremlist.api.data.response.AssociationCreatedResponse
import net.flyingfishflash.loremlist.api.data.response.AssociationDeletedResponse
import net.flyingfishflash.loremlist.api.data.response.AssociationsDeletedResponse
import net.flyingfishflash.loremlist.api.data.response.LrmListDeletedResponse
import net.flyingfishflash.loremlist.api.data.response.LrmListResponse
import net.flyingfishflash.loremlist.core.configuration.SerializationConfig
import net.flyingfishflash.loremlist.core.configuration.WebSecurityConfiguration
import net.flyingfishflash.loremlist.core.response.advice.CoreExceptionHandler.Companion.VALIDATION_FAILURE_MESSAGE
import net.flyingfishflash.loremlist.core.response.structure.ApiMessageNumeric
import net.flyingfishflash.loremlist.core.response.structure.DispositionOfProblem
import net.flyingfishflash.loremlist.core.response.structure.DispositionOfSuccess
import net.flyingfishflash.loremlist.domain.exceptions.DomainException
import net.flyingfishflash.loremlist.domain.lrmitem.ItemNotFoundException
import net.flyingfishflash.loremlist.domain.lrmitem.LrmItem
import net.flyingfishflash.loremlist.domain.lrmitem.LrmItemSuccinct
import net.flyingfishflash.loremlist.domain.lrmlist.ListNotFoundException
import net.flyingfishflash.loremlist.domain.lrmlist.LrmList
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
import java.util.UUID

/**
 * LrmListController Unit Tests
 */
@WebMvcTest(controllers = [LrmListController::class])
@Import(SerializationConfig::class, WebSecurityConfiguration::class)
class LrmListControllerTests(mockMvc: MockMvc) : DescribeSpec() {
  override fun extensions() = listOf(SpringExtension)

  @MockkBean
  lateinit var mockLrmListApiService: LrmListApiService

  init {
    val now = now()
    val id0 = UUID.fromString("00000000-0000-4000-a000-000000000000")
    val id1 = UUID.fromString("00000000-0000-4000-a000-000000000001")
    val id2 = UUID.fromString("00000000-0000-4000-a000-000000000002")
    val id3 = UUID.fromString("00000000-0000-4000-a000-000000000003")
    val lrmListCreateRequest = LrmListCreateRequest(name = "Lorem List Name", description = "Lorem List Description", public = true)
    val irrelevantMessage = "ksADs8y96KRa1Zo4ipMdr5t8faudmFj4c564S02MjsNG6TXEO7yctC08Bb53bCB7"

    fun lrmList0(): LrmList = LrmList(
      id = id0,
      name = lrmListCreateRequest.name,
      description = lrmListCreateRequest.description,
      public = lrmListCreateRequest.public,
      owner = "Lorem Ipsum Owner",
      created = now,
      creator = "Lorem Ipsum Created By",
      updated = now,
      updater = "Lorem Ipsum Updated By",
    )

    fun lrmList1(): LrmList = LrmList(
      id = id1,
      name = "Lorem List Name (id1)",
      description = "Lorem List Description",
      public = true,
      owner = "Lorem Ipsum Owner",
      created = now,
      creator = "Lorem Ipsum Created By",
      updated = now,
      updater = "Lorem Ipsum Updated By",
    )
    fun lrmItem2(): LrmItem = LrmItem(
      id = id2,
      name = "Lorem Item Name (id2)",
      description = "Lorem Item Description",
      quantity = 0,
      owner = "Lorem Ipsum Owner",
      created = now,
      creator = "Lorem Ipsum Created By",
      updated = now,
      updater = "Lorem Ipsum Updated By",
    )
    fun lrmItem3(): LrmItem = LrmItem(
      id = id3,
      name = "Lorem Item Name (id3)",
      description = "Lorem Item Description",
      quantity = 0,
      owner = "Lorem Ipsum Owner",
      created = now,
      creator = "Lorem Ipsum Created By",
      updated = now,
      updater = "Lorem Ipsum Updated By",
    )

    afterEach { clearAllMocks() }
    afterSpec { unmockkAll() }

    describe("/lists") {
      describe("delete") {
        it("lists are deleted") {
          val mockReturn = LrmListDeletedResponse(listNames = listOf("Lorem List Name"), associatedItemNames = listOf("Lorem Item Name"))
          every { mockLrmListApiService.deleteByOwner(ofType(String::class)) } returns
            ApiServiceResponse(mockReturn, message = irrelevantMessage)
          val instance = "/lists"
          mockMvc.delete(instance) {
            with(jwt())
            with(csrf())
            contentType = MediaType.APPLICATION_JSON
          }.andExpect {
            status { isOk() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
            jsonPath("$.method") { value(HttpMethod.DELETE.name().lowercase()) }
            jsonPath("$.message") { value(irrelevantMessage) }
            jsonPath("$.instance") { value(instance) }
            jsonPath("$.size") { value(1) }
            jsonPath("$.content.listNames[0]") { value(mockReturn.listNames[0]) }
            jsonPath("$.content.associatedItemNames[0]") { value(mockReturn.associatedItemNames[0]) }
          }
        }
      }

      describe("get") {
        it("lists are found") {
          val mockReturn = listOf(LrmListResponse.fromLrmList(lrmList0()))
          every { mockLrmListApiService.findByOwnerExcludeItems(ofType(String::class)) } returns
            ApiServiceResponse(mockReturn, message = irrelevantMessage)
          val instance = "/lists"
          mockMvc.get(instance) {
            with(jwt())
            contentType = MediaType.APPLICATION_JSON
          }.andExpect {
            status { isOk() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
            jsonPath("$.method") { value(HttpMethod.GET.name().lowercase()) }
            jsonPath("$.message") { value(irrelevantMessage) }
            jsonPath("$.instance") { value(instance) }
            jsonPath("$.size") { value(mockReturn.size) }
            jsonPath("$.content") { exists() }
            jsonPath("$.content") { isArray() }
            jsonPath("$.content.[0].name") { value(lrmList0().name) }
            jsonPath("$.content.[0].description") { value(lrmList0().description) }
            jsonPath("$.content.[0].items") { isEmpty() }
          }
          verify(exactly = 1) { mockLrmListApiService.findByOwnerExcludeItems(ofType(String::class)) }
        }

        it("lists are found ?includeItems=false") {
          val mockReturn = listOf(LrmListResponse.fromLrmList(lrmList0()))
          every { mockLrmListApiService.findByOwnerExcludeItems(ofType(String::class)) } returns
            ApiServiceResponse(mockReturn, message = irrelevantMessage)
          val instance = "/lists?includeItems=false"
          mockMvc.get(instance) {
            with(jwt())
            contentType = MediaType.APPLICATION_JSON
          }.andExpect {
            status { isOk() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
            jsonPath("$.method") { value(HttpMethod.GET.name().lowercase()) }
            jsonPath("$.message") { value(irrelevantMessage) }
            jsonPath("$.instance") { value(instance.substringBeforeLast("?").removeSuffix(instance)) }
            jsonPath("$.size") { value(mockReturn.size) }
            jsonPath("$.content") { isArray() }
            jsonPath("$.content.[0].name") { value(lrmList0().name) }
            jsonPath("$.content.[0].description") { value(lrmList0().description) }
            jsonPath("$.content.[0].items") { isEmpty() }
          }
          verify(exactly = 1) { mockLrmListApiService.findByOwnerExcludeItems(ofType(String::class)) }
        }

        it("lists are found ?includeItems=true") {
          val mockReturn = listOf(LrmListResponse.fromLrmList(lrmList0()))
          every { mockLrmListApiService.findByOwner(ofType(String::class)) } returns
            ApiServiceResponse(mockReturn, message = irrelevantMessage)
          val instance = "/lists?includeItems=true"
          mockMvc.get(instance) {
            with(jwt())
            contentType = MediaType.APPLICATION_JSON
          }.andExpect {
            status { isOk() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
            jsonPath("$.method") { value(HttpMethod.GET.name().lowercase()) }
            jsonPath("$.message") { value(irrelevantMessage) }
            jsonPath("$.instance") { value(instance.substringBeforeLast("?").removeSuffix(instance)) }
            jsonPath("$.size") { value(mockReturn.size) }
            jsonPath("$.content") { isArray() }
            jsonPath("$.content.[0].name") { value(lrmList0().name) }
            jsonPath("$.content.[0].description") { value(lrmList0().description) }
            jsonPath("$.content.[0].items") {
              isArray()
              isEmpty()
            }
          }
          verify(exactly = 1) { mockLrmListApiService.findByOwner(ofType(String::class)) }
        }
      }

      describe("post") {
        it("list is created") {
          println(Json.encodeToString(lrmListCreateRequest))
          every { mockLrmListApiService.create(lrmListCreateRequest, ofType(String::class)) } returns
            ApiServiceResponse(LrmListResponse.fromLrmList(lrmList0()), message = irrelevantMessage)
          val instance = "/lists"
          mockMvc.post(instance) {
            with(jwt())
            with(csrf())
            content = Json.encodeToString(lrmListCreateRequest)
            contentType = MediaType.APPLICATION_JSON
          }.andExpect {
            status { isOk() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
            jsonPath("$.method") { value(HttpMethod.POST.name().lowercase()) }
            jsonPath("$.message") { value(irrelevantMessage) }
            jsonPath("$.instance") { value(instance) }
            jsonPath("$.size") { value(1) }
            jsonPath("$.content.description") { value(lrmList0().description) }
            jsonPath("$.content.name") { value(lrmList0().name) }
          }
          verify(exactly = 1) { mockLrmListApiService.create(lrmListCreateRequest, ofType(String::class)) }
        }

        it("requested list name is an empty string") {
          val instance = "/lists"
          mockMvc.post(instance) {
            with(jwt())
            with(csrf())
            content = Json.encodeToString(LrmListCreateRequest("", lrmList0().description, lrmList0().public))
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
          }
          verify(exactly = 0) { mockLrmListApiService.create(ofType(LrmListCreateRequest::class), ofType(String::class)) }
        }

        it("requested list description is an empty string") {
          val instance = "/lists"
          mockMvc.post(instance) {
            with(jwt())
            with(csrf())
            content = Json.encodeToString(LrmListCreateRequest(lrmList0().name, "", lrmList0().public))
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
          }
          verify(exactly = 0) { mockLrmListApiService.create(ofType(LrmListCreateRequest::class), ofType(String::class)) }
        }
      }
    }

    describe("/lists/with-no-items") {
      describe("get") {
        it("lists with no item association are found") {
          val mockReturn = listOf(LrmListResponse.fromLrmList(lrmList0()))
          every { mockLrmListApiService.findByOwnerAndHavingNoItemAssociations(owner = ofType(String::class)) } returns
            ApiServiceResponse(content = mockReturn, message = irrelevantMessage)
          val instance = "/lists/with-no-items"
          mockMvc.get(instance) {
            with(jwt())
            contentType = MediaType.APPLICATION_JSON
          }.andExpect {
            status { isOk() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
            jsonPath("$.method") { value(HttpMethod.GET.name().lowercase()) }
            jsonPath("$.message") { value(irrelevantMessage) }
            jsonPath("$.instance") { value(instance) }
            jsonPath("$.size") { value(mockReturn.size) }
            jsonPath("$.content") { exists() }
            jsonPath("$.content") { isArray() }
            jsonPath("$.content.[0].name") { value(mockReturn[0].name) }
            jsonPath("$.content.[0].description") { value(mockReturn[0].description) }
          }
          verify(exactly = 1) { mockLrmListApiService.findByOwnerAndHavingNoItemAssociations(owner = ofType(String::class)) }
        }
      }
    }

    describe("/lists/count") {
      describe("get") {
        it("count of lists is returned") {
          every { mockLrmListApiService.countByOwner(owner = ofType(String::class)) } returns
            ApiServiceResponse(content = ApiMessageNumeric(999), message = irrelevantMessage)
          val instance = "/lists/count"
          mockMvc.get(instance) {
            with(jwt())
          }.andExpect {
            status { isOk() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
            jsonPath("$.method") { value(HttpMethod.GET.name().lowercase()) }
            jsonPath("$.message") { value(irrelevantMessage) }
            jsonPath("$.instance") { value(instance) }
            jsonPath("$.size") { value(1) }
            jsonPath("$.content.length()") { value(1) }
            jsonPath("$.content.value") { value(999) }
          }
        }
      }
    }

    describe("/lists/{list-id}") {
      describe("delete") {
        it("list is deleted") {
          // nonsensical conditioning of the delete response:
          // if the count of item to list associations is 0, then associatedListNames should be an empty list
          every {
            mockLrmListApiService.deleteByOwnerAndId(id = id1, owner = ofType(String::class), removeItemAssociations = false)
          } returns
            ApiServiceResponse(
              content = LrmListDeletedResponse(listNames = listOf("dolor sit amet"), associatedItemNames = listOf("Lorem Ipsum")),
              message = irrelevantMessage,
            )
          val instance = "/lists/$id1"
          mockMvc.delete(instance) {
            with(jwt())
            with(csrf())
          }.andExpect {
            status { isOk() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
            jsonPath("$.method") { value(HttpMethod.DELETE.name().lowercase()) }
            jsonPath("$.message") { value(irrelevantMessage) }
            jsonPath("$.instance") { value(instance) }
            jsonPath("$.size") { value(1) }
            jsonPath("$.content.listNames.length()") { value(1) }
            jsonPath("$.content.listNames.[0]") { value("dolor sit amet") }
            jsonPath("$.content.associatedItemNames.length()") { value(1) }
            jsonPath("$.content.associatedItemNames.[0]") { value("Lorem Ipsum") }
          }
          verify(exactly = 1) {
            mockLrmListApiService.deleteByOwnerAndId(
              id = ofType(UUID::class),
              owner = ofType(String::class),
              removeItemAssociations = ofType(Boolean::class),
            )
          }
        }

        it("list is not found") {
          every {
            mockLrmListApiService.deleteByOwnerAndId(id = id1, owner = ofType(String::class), removeItemAssociations = false)
          } throws ListNotFoundException(id1)
          val instance = "/lists/$id1"
          mockMvc.delete(instance) {
            with(jwt())
            with(csrf())
          }.andExpect {
            status { isNotFound() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.disposition") { value(DispositionOfProblem.FAILURE.nameAsLowercase()) }
            jsonPath("$.method") { value(HttpMethod.DELETE.name().lowercase()) }
            jsonPath("$.instance") { value(instance) }
            jsonPath("$.size") { value(1) }
            jsonPath("$.content.title") { value(ListNotFoundException::class.java.simpleName) }
            jsonPath("$.content.status") { HttpStatus.NOT_FOUND.value() }
          }
          verify(exactly = 1) {
            mockLrmListApiService.deleteByOwnerAndId(
              id = ofType(UUID::class),
              owner = ofType(String::class),
              removeItemAssociations = ofType(Boolean::class),
            )
          }
        }
      }

      describe("get") {
        it("list is found") {
          every { mockLrmListApiService.findByOwnerAndId(id = id1, owner = ofType(String::class)) } returns
            ApiServiceResponse((LrmListResponse.fromLrmList(lrmList0())), message = irrelevantMessage)
          val instance = "/lists/$id1"
          mockMvc.get(instance) {
            with(jwt())
          }.andExpect {
            status { isOk() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
            jsonPath("$.method") { value(HttpMethod.GET.name().lowercase()) }
            jsonPath("$.message") { value(irrelevantMessage) }
            jsonPath("$.instance") { value(instance) }
            jsonPath("$.size") { value(1) }
            jsonPath("$.content.description") { value(lrmList0().description) }
            jsonPath("$.content.name") { value(lrmList0().name) }
          }
          verify(exactly = 1) { mockLrmListApiService.findByOwnerAndId(id = ofType(UUID::class), owner = ofType(String::class)) }
        }

        it("list is found ?includeItems=true") {
          every { mockLrmListApiService.findByOwnerAndId(id = id1, owner = ofType(String::class)) } returns
            ApiServiceResponse(LrmListResponse.fromLrmList(lrmList0()), message = irrelevantMessage)
          val instance = "/lists/$id1?includeItems=true"
          mockMvc.get(instance) {
            with(jwt())
          }.andExpect {
            status { isOk() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
            jsonPath("$.method") { value(HttpMethod.GET.name().lowercase()) }
            jsonPath("$.message") { value(irrelevantMessage) }
            jsonPath("$.instance") { value(instance.substringBeforeLast("?").removeSuffix(instance)) }
            jsonPath("$.size") { value(1) }
            jsonPath("$.content.description") { value(lrmList0().description) }
            jsonPath("$.content.name") { value(lrmList0().name) }
            jsonPath("$.content.items") {
              isArray()
              isEmpty()
            }
          }
          verify(exactly = 1) { mockLrmListApiService.findByOwnerAndId(id = ofType(UUID::class), owner = ofType(String::class)) }
        }

        it("list is found ?includeItems=false") {
          every { mockLrmListApiService.findByOwnerAndIdExcludeItems(id = id1, owner = ofType(String::class)) } returns
            ApiServiceResponse(LrmListResponse.fromLrmList(lrmList0()), message = irrelevantMessage)
          val instance = "/lists/$id1?includeItems=false"
          mockMvc.get(instance) {
            with(jwt())
          }.andExpect {
            status { isOk() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
            jsonPath("$.method") { value(HttpMethod.GET.name().lowercase()) }
            jsonPath("$.message") { value(irrelevantMessage) }
            jsonPath("$.instance") { value(instance.substringBeforeLast("?").removeSuffix(instance)) }
            jsonPath("$.size") { value(1) }
            jsonPath("$.content.description") { value(lrmList0().description) }
            jsonPath("$.content.name") { value(lrmList0().name) }
            jsonPath("$.content.items") { isEmpty() }
          }
          verify(exactly = 1) {
            mockLrmListApiService.findByOwnerAndIdExcludeItems(id = ofType(UUID::class), owner = ofType(String::class))
          }
        }

        it("list is not found") {
          every { mockLrmListApiService.findByOwnerAndId(id = id1, owner = ofType(String::class)) } throws
            ListNotFoundException(id1)
          val instance = "/lists/$id1"
          mockMvc.get(instance) {
            with(jwt())
          }.andExpect {
            status { isNotFound() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.disposition") { value(DispositionOfProblem.FAILURE.nameAsLowercase()) }
            jsonPath("$.method") { value(HttpMethod.GET.name().lowercase()) }
            jsonPath("$.instance") { value(instance) }
            jsonPath("$.size") { value(1) }
            jsonPath("$.content.title") { value(ListNotFoundException::class.java.simpleName) }
            jsonPath("$.content.status") { HttpStatus.NOT_FOUND.value() }
          }
          verify(exactly = 1) { mockLrmListApiService.findByOwnerAndId(id = ofType(UUID::class), owner = ofType(String::class)) }
        }
      }

      describe("patch") {
        it("list is found and updated") {
          every { mockLrmListApiService.patchByOwnerAndId(id = id1, owner = ofType(String::class), any()) } returns
            ApiServiceResponse(content = LrmListResponse.fromLrmList(lrmList0()), message = irrelevantMessage)
          val instance = "/lists/$id1"
          mockMvc.patch(instance) {
            with(jwt())
            with(csrf())
            content = Json.encodeToString(mapOf("name" to lrmList0().name))
            contentType = MediaType.APPLICATION_JSON
          }.andExpect {
            status { isOk() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
            jsonPath("$.method") { value(HttpMethod.PATCH.name().lowercase()) }
            jsonPath("$.message") { value(irrelevantMessage) }
            jsonPath("$.instance") { value(instance) }
            jsonPath("$.size") { value(1) }
            jsonPath("$.content.description") { value(lrmList0().description) }
            jsonPath("$.content.name") { value(lrmList0().name) }
            jsonPath("$.content.items") { isEmpty() }
          }
          verify(exactly = 1) {
            mockLrmListApiService.patchByOwnerAndId(
              id = ofType(UUID::class),
              owner = ofType(String::class),
              patchRequest = mapOf("name" to lrmList0().name),
            )
          }
        }

        it("list is found and not updated") {
          val expectedMessage = "api service response message must include the text 'not updated' to return 204"
          every { mockLrmListApiService.patchByOwnerAndId(id = id1, owner = ofType(String::class), any()) } returns
            ApiServiceResponse(content = LrmListResponse.fromLrmList(lrmList0()), message = expectedMessage)
          val instance = "/lists/$id1"
          mockMvc.patch(instance) {
            with(jwt())
            with(csrf())
            content = Json.encodeToString(mapOf("name" to lrmList0().name))
            contentType = MediaType.APPLICATION_JSON
          }.andExpect {
            status { isNoContent() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
            jsonPath("$.method") { value(HttpMethod.PATCH.name().lowercase()) }
            jsonPath("$.message") { value(expectedMessage) }
            jsonPath("$.instance") { value(instance) }
            jsonPath("$.size") { value(1) }
            jsonPath("$.content.description") { value(lrmList0().description) }
            jsonPath("$.content.name") { value(lrmList0().name) }
            jsonPath("$.content.items") { isEmpty() }
          }
          verify(exactly = 1) {
            mockLrmListApiService.patchByOwnerAndId(
              id = ofType(UUID::class),
              owner = ofType(String::class),
              patchRequest = mapOf("name" to lrmList0().name),
            )
          }
        }

        it("list is not found") {
          every { mockLrmListApiService.patchByOwnerAndId(id = id1, owner = ofType(String::class), any()) } throws
            ListNotFoundException(id1)
          val instance = "/lists/$id1"
          mockMvc.patch(instance) {
            with(jwt())
            with(csrf())
            content = Json.encodeToString(mapOf("name" to lrmList0().name))
            contentType = MediaType.APPLICATION_JSON
          }.andExpect {
            status { isNotFound() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.disposition") { value(DispositionOfProblem.FAILURE.nameAsLowercase()) }
            jsonPath("$.method") { value(HttpMethod.PATCH.name().lowercase()) }
            jsonPath("$.instance") { value(instance) }
            jsonPath("$.size") { value(1) }
            jsonPath("$.content.title") { value(ListNotFoundException::class.java.simpleName) }
            jsonPath("$.content.status") { HttpStatus.NOT_FOUND.value() }
          }
          verify(exactly = 1) {
            mockLrmListApiService.patchByOwnerAndId(
              id = ofType(UUID::class),
              owner = ofType(String::class),
              patchRequest = mapOf("name" to lrmList0().name),
            )
          }
        }
      }
    }

    describe("/lists/{list-id}/items/{item-id}") {
      describe("delete") {
        it("item is removed from list") {
          val lrmItemName = "58cVf5N8rSstjC6L"
          val lrmListName = "nxuS5LKlpP9TVhzV"
          every {
            mockLrmListApiService.deleteItemAssociationByItemIdAndListIdAndComponentsOwner(
              itemId = id2,
              listId = id1,
              componentsOwner = ofType(String::class),
            )
          } returns
            ApiServiceResponse(
              content = AssociationDeletedResponse(itemName = lrmItemName, listName = lrmListName),
              message = irrelevantMessage,
            )
          val instance = "/lists/$id1/items/$id2"
          mockMvc.delete(instance) {
            with(jwt())
            with(csrf())
            contentType = MediaType.APPLICATION_JSON
          }.andExpect {
            status { isOk() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
            jsonPath("$.method") { value(HttpMethod.DELETE.name().lowercase()) }
            jsonPath("$.message") { value(irrelevantMessage) }
            jsonPath("$.instance") { value(instance) }
            jsonPath("$.size") { value(1) }
          }
          verify(exactly = 1) {
            mockLrmListApiService.deleteItemAssociationByItemIdAndListIdAndComponentsOwner(
              itemId = ofType(UUID::class),
              listId = ofType(UUID::class),
              componentsOwner = ofType(String::class),
            )
          }
        }

        it("item is not found") {
          every {
            mockLrmListApiService.deleteItemAssociationByItemIdAndListIdAndComponentsOwner(
              itemId = id2,
              listId = id1,
              componentsOwner = ofType(String::class),
            )
          } throws ItemNotFoundException(id2)
//          val expectedMessage = ItemNotFoundException.defaultMessage()
          val instance = "/lists/$id1/items/$id2"
          mockMvc.delete(instance) {
            with(jwt())
            with(csrf())
            contentType = MediaType.APPLICATION_JSON
          }.andExpect {
            status { isNotFound() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.disposition") { value(DispositionOfProblem.FAILURE.nameAsLowercase()) }
            jsonPath("$.method") { value(HttpMethod.DELETE.name().lowercase()) }
            jsonPath("$.instance") { value(instance) }
            jsonPath("$.size") { value(1) }
            jsonPath("$.content.title") { value(ItemNotFoundException::class.java.simpleName) }
            jsonPath("$.content.status") { HttpStatus.NOT_FOUND.value() }
          }
          verify(exactly = 1) {
            mockLrmListApiService.deleteItemAssociationByItemIdAndListIdAndComponentsOwner(
              itemId = ofType(UUID::class),
              listId = ofType(UUID::class),
              componentsOwner = ofType(String::class),
            )
          }
        }

        it("list is not found") {
          every {
            mockLrmListApiService.deleteItemAssociationByItemIdAndListIdAndComponentsOwner(
              itemId = id2,
              listId = id1,
              componentsOwner = ofType(String::class),
            )
          } throws ListNotFoundException(id1)
          val instance = "/lists/$id1/items/$id2"
          mockMvc.delete(instance) {
            with(jwt())
            with(csrf())
            contentType = MediaType.APPLICATION_JSON
          }.andExpect {
            status { isNotFound() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.disposition") { value(DispositionOfProblem.FAILURE.nameAsLowercase()) }
            jsonPath("$.method") { value(HttpMethod.DELETE.name().lowercase()) }
            jsonPath("$.instance") { value(instance) }
            jsonPath("$.size") { value(1) }
            jsonPath("$.content.title") { value(ListNotFoundException::class.java.simpleName) }
            jsonPath("$.content.status") { HttpStatus.NOT_FOUND.value() }
          }
          verify(exactly = 1) {
            mockLrmListApiService.deleteItemAssociationByItemIdAndListIdAndComponentsOwner(
              itemId = ofType(UUID::class),
              listId = ofType(UUID::class),
              componentsOwner = ofType(String::class),
            )
          }
        }
      }

      describe("post") {
        it("item is added to list") {
          val mockResponse = AssociationCreatedResponse(
            componentName = lrmList1().name,
            associatedComponents = listOf(LrmItemSuccinct.fromLrmItem(lrmItem2())),
          )
          every {
            mockLrmListApiService.createItemAssociations(
              listId = id1,
              itemIdCollection = any(),
              owner = ofType(String::class),
            )
          } returns ApiServiceResponse(content = mockResponse, message = irrelevantMessage)
          val instance = "/lists/$id1/items"
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
            jsonPath("$.message") { value(irrelevantMessage) }
            jsonPath("$.instance") { value(instance) }
            jsonPath("$.size") { value(1) }
            jsonPath("$.content.componentName") { value(mockResponse.componentName) }
            jsonPath("$.content.associatedComponents.length()") { value(1) }
            jsonPath("$.content.associatedComponents[0].type") { value("item") }
            jsonPath("$.content.associatedComponents[0].id") { value(mockResponse.associatedComponents[0].id.toString()) }
            jsonPath("$.content.associatedComponents[0].name") { value(mockResponse.associatedComponents[0].name) }
          }
        }

        it("item is added to lists") {
          val mockResponse = AssociationCreatedResponse(
            componentName = lrmList1().name,
            associatedComponents = listOf(LrmItemSuccinct.fromLrmItem(lrmItem2()), LrmItemSuccinct.fromLrmItem(lrmItem3())),
          )
          every {
            mockLrmListApiService.createItemAssociations(
              listId = id1,
              itemIdCollection = any(),
              owner = ofType(String::class),
            )
          } returns ApiServiceResponse(content = mockResponse, message = irrelevantMessage)
          val instance = "/lists/$id1/items"
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
            jsonPath("$.message") { value(irrelevantMessage) }
            jsonPath("$.instance") { value(instance) }
            jsonPath("$.size") { value(1) }
            jsonPath("$.content.componentName") { value(mockResponse.componentName) }
            jsonPath("$.content.associatedComponents.length()") { value(2) }
            jsonPath("$.content.associatedComponents[0].type") { value("item") }
            jsonPath("$.content.associatedComponents[0].id") { value(mockResponse.associatedComponents[0].id.toString()) }
            jsonPath("$.content.associatedComponents[0].name") { value(mockResponse.associatedComponents[0].name) }
          }
        }

        it("list is not found") {
          every {
            mockLrmListApiService.createItemAssociations(
              listId = id1,
              itemIdCollection = any(),
              owner = ofType(String::class),
            )
          } throws ListNotFoundException(id2)
          val instance = "/lists/$id1/items"
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
            jsonPath("$.instance") { value(instance) }
            jsonPath("$.size") { value(1) }
            jsonPath("$.content.title") { value(ListNotFoundException::class.java.simpleName) }
            jsonPath("$.content.status") { HttpStatus.NOT_FOUND.value() }
          }
        }

        it("item is not found") {
          every {
            mockLrmListApiService.createItemAssociations(
              listId = id1,
              itemIdCollection = any(),
              owner = ofType(String::class),
            )
          } throws ItemNotFoundException(id2)
//          val expectedMessage = ItemNotFoundException.defaultMessage()
          val instance = "/lists/$id1/items"
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
            jsonPath("$.instance") { value(instance) }
            jsonPath("$.size") { value(1) }
            jsonPath("$.content.title") { value(ItemNotFoundException::class.java.simpleName) }
            jsonPath("$.content.status") { HttpStatus.NOT_FOUND.value() }
          }
        }
      }
    }

    describe("/lists/{list-id}/items/count") {
      describe("get") {
        it("count of item associations is returned") {
          every { mockLrmListApiService.countItemAssociationsByListIdAndListOwner(listId = id1, listOwner = ofType(String::class)) } returns
            ApiServiceResponse(content = ApiMessageNumeric(999), message = irrelevantMessage)
          val instance = "/lists/$id1/items/count"
          mockMvc.get(instance) {
            with(jwt())
          }.andExpect {
            status { isOk() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
            jsonPath("$.method") { value(HttpMethod.GET.name().lowercase()) }
            jsonPath("$.message") { value(irrelevantMessage) }
            jsonPath("$.instance") { value(instance) }
            jsonPath("$.size") { value(1) }
            jsonPath("$.content.length()") { value(1) }
            jsonPath("$.content.value") { value(999) }
          }
          verify(exactly = 1) {
            mockLrmListApiService.countItemAssociationsByListIdAndListOwner(listId = ofType(UUID::class), listOwner = ofType(String::class))
          }
        }

        it("item is not found") {
          every {
            mockLrmListApiService.countItemAssociationsByListIdAndListOwner(listId = id1, listOwner = ofType(String::class))
          } throws DomainException(httpStatus = HttpStatus.NOT_FOUND)
          val instance = "/lists/$id1/items/count"
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
            mockLrmListApiService.countItemAssociationsByListIdAndListOwner(listId = ofType(UUID::class), listOwner = ofType(String::class))
          }
        }
      }
    }

    describe("/lists/{list-id}/items") {
      it("all items are removed from a list") {
        every {
          mockLrmListApiService.deleteItemAssociationsByListIdAndListOwner(
            listId = id1,
            listOwner = ofType(String::class),
          )
        } returns ApiServiceResponse(AssociationsDeletedResponse(itemName = "irrelevant", 999), message = irrelevantMessage)
        val instance = "/lists/$id1/items"
        mockMvc.delete(instance) {
          with(jwt())
          with(csrf())
          contentType = MediaType.APPLICATION_JSON
        }.andExpect {
          status { isOk() }
          content { contentType(MediaType.APPLICATION_JSON) }
          jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
          jsonPath("$.method") { value(HttpMethod.DELETE.name().lowercase()) }
          jsonPath("$.message") { value(irrelevantMessage) }
          jsonPath("$.instance") { value(instance) }
          jsonPath("$.size") { value(1) }
          jsonPath("$.content.value") { value(999) }
        }
        verify(exactly = 1) {
          mockLrmListApiService.deleteItemAssociationsByListIdAndListOwner(listId = ofType(UUID::class), listOwner = ofType(String::class))
        }
      }
    }
  }
}
