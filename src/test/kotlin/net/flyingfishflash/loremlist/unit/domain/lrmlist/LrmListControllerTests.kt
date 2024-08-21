package net.flyingfishflash.loremlist.unit.domain.lrmlist

import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.extensions.spring.SpringExtension
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.flyingfishflash.loremlist.core.configuration.SerializationConfig
import net.flyingfishflash.loremlist.core.configuration.WebSecurityConfiguration
import net.flyingfishflash.loremlist.core.exceptions.ApiException
import net.flyingfishflash.loremlist.core.response.advice.ApiExceptionHandler.Companion.VALIDATION_FAILURE_MESSAGE
import net.flyingfishflash.loremlist.core.response.structure.DispositionOfProblem
import net.flyingfishflash.loremlist.core.response.structure.DispositionOfSuccess
import net.flyingfishflash.loremlist.domain.LrmComponentType
import net.flyingfishflash.loremlist.domain.association.AssociationService
import net.flyingfishflash.loremlist.domain.association.data.AssociationCreatedResponse
import net.flyingfishflash.loremlist.domain.lrmitem.ItemNotFoundException
import net.flyingfishflash.loremlist.domain.lrmitem.LrmItem
import net.flyingfishflash.loremlist.domain.lrmitem.succinct
import net.flyingfishflash.loremlist.domain.lrmlist.ListNotFoundException
import net.flyingfishflash.loremlist.domain.lrmlist.LrmList
import net.flyingfishflash.loremlist.domain.lrmlist.LrmListController
import net.flyingfishflash.loremlist.domain.lrmlist.LrmListService
import net.flyingfishflash.loremlist.domain.lrmlist.data.LrmListDeleteResponse
import net.flyingfishflash.loremlist.domain.lrmlist.data.LrmListRequest
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
  lateinit var mockAssociationService: AssociationService

  @MockkBean
  lateinit var mockLrmListService: LrmListService

  init {
    val id0 = UUID.fromString("00000000-0000-4000-a000-000000000000")
    val id1 = UUID.fromString("00000000-0000-4000-a000-000000000001")
    val id2 = UUID.fromString("00000000-0000-4000-a000-000000000002")
    val id3 = UUID.fromString("00000000-0000-4000-a000-000000000003")
    val lrmListRequest = LrmListRequest(name = "Lorem List Name", description = "Lorem List Description", public = true)

    fun lrmList(): LrmList = LrmList(
      id = id0,
      name = lrmListRequest.name,
      description = lrmListRequest.description,
      public = lrmListRequest.public,
    )

    fun lrmListWithEmptyItems(): LrmList = lrmList().copy(items = setOf())
    fun lrmList1(): LrmList = LrmList(id = id1, name = "Lorem List Name (id1)", description = "Lorem List Description", public = true)
    fun lrmItem2(): LrmItem = LrmItem(id = id2, name = "Lorem Item Name (id2)", description = "Lorem Item Description")
    fun lrmItem3(): LrmItem = LrmItem(id = id3, name = "Lorem Item Name (id3)", description = "Lorem Item Description")

    afterEach { clearAllMocks() }
    afterSpec { unmockkAll() }

    describe("/lists") {
      describe("delete") {
        it("lists are deleted") {
          val mockReturn = LrmListDeleteResponse(listNames = listOf("Lorem List Name"), associatedItemNames = listOf("Lorem Item Name"))
          every { mockLrmListService.deleteByOwner(ofType(String::class)) } returns mockReturn
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
            jsonPath("$.message") { value("Deleted all lists and disassociated all items.") }
            jsonPath("$.instance") { value(instance) }
            jsonPath("$.size") { value(1) }
            jsonPath("$.content.listNames[0]") { value(mockReturn.listNames[0]) }
            jsonPath("$.content.associatedItemNames[0]") { value(mockReturn.associatedItemNames[0]) }
          }
        }
      }

      describe("get") {
        it("lists are found") {
          val mockReturn = listOf(lrmList())
          every { mockLrmListService.findByOwner(ofType(String::class)) } returns mockReturn
          val instance = "/lists"
          mockMvc.get(instance) {
            with(jwt())
            contentType = MediaType.APPLICATION_JSON
          }.andExpect {
            status { isOk() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
            jsonPath("$.method") { value(HttpMethod.GET.name().lowercase()) }
            jsonPath("$.message") { value("retrieved all lists") }
            jsonPath("$.instance") { value(instance) }
            jsonPath("$.size") { value(mockReturn.size) }
            jsonPath("$.content") { exists() }
            jsonPath("$.content") { isArray() }
            jsonPath("$.content.[0].name") { value(lrmList().name) }
            jsonPath("$.content.[0].description") { value(lrmList().description) }
            jsonPath("$.content.[0].items") {
              doesNotExist()
            }
          }
          verify(exactly = 1) { mockLrmListService.findByOwner(ofType(String::class)) }
        }

        it("lists are found ?includeItems=false") {
          val mockReturn = listOf(lrmList())
          every { mockLrmListService.findByOwner(ofType(String::class)) } returns mockReturn
          val instance = "/lists?includeItems=false"
          mockMvc.get(instance) {
            with(jwt())
            contentType = MediaType.APPLICATION_JSON
          }.andExpect {
            status { isOk() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
            jsonPath("$.method") { value(HttpMethod.GET.name().lowercase()) }
            jsonPath("$.message") { value("retrieved all lists") }
            jsonPath("$.instance") { value(instance.substringBeforeLast("?").removeSuffix(instance)) }
            jsonPath("$.size") { value(mockReturn.size) }
            jsonPath("$.content") { isArray() }
            jsonPath("$.content.[0].name") { value(lrmList().name) }
            jsonPath("$.content.[0].description") { value(lrmList().description) }
            jsonPath("$.content.[0].items") {
              doesNotExist()
            }
          }
          verify(exactly = 1) { mockLrmListService.findByOwner(ofType(String::class)) }
        }

        it("lists are found ?includeItems=true") {
          val mockReturn = listOf(lrmListWithEmptyItems())
          every { mockLrmListService.findByOwnerIncludeItems(ofType(String::class)) } returns mockReturn
          val instance = "/lists?includeItems=true"
          mockMvc.get(instance) {
            with(jwt())
            contentType = MediaType.APPLICATION_JSON
          }.andExpect {
            status { isOk() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
            jsonPath("$.method") { value(HttpMethod.GET.name().lowercase()) }
            jsonPath("$.message") { value("retrieved all lists") }
            jsonPath("$.instance") { value(instance.substringBeforeLast("?").removeSuffix(instance)) }
            jsonPath("$.size") { value(mockReturn.size) }
            jsonPath("$.content") { isArray() }
            jsonPath("$.content.[0].name") { value(lrmList().name) }
            jsonPath("$.content.[0].description") { value(lrmList().description) }
            jsonPath("$.content.[0].items") {
              isArray()
              isEmpty()
            }
          }
          verify(exactly = 1) { mockLrmListService.findByOwnerIncludeItems(ofType(String::class)) }
        }
      }

      describe("post") {
        it("list is created") {
          println(Json.encodeToString(lrmListRequest))
          every { mockLrmListService.create(lrmListRequest, ofType(String::class)) } returns lrmList()
          val instance = "/lists"
          mockMvc.post(instance) {
            with(jwt())
            with(csrf())
            content = Json.encodeToString(lrmListRequest)
            contentType = MediaType.APPLICATION_JSON
          }.andExpect {
            status { isOk() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
            jsonPath("$.method") { value(HttpMethod.POST.name().lowercase()) }
            jsonPath("$.message") { value("created new list") }
            jsonPath("$.instance") { value(instance) }
            jsonPath("$.size") { value(1) }
            jsonPath("$.content.description") { value(lrmList().description) }
            jsonPath("$.content.name") { value(lrmList().name) }
          }
          verify(exactly = 1) { mockLrmListService.create(lrmListRequest, ofType(String::class)) }
        }

        it("requested list name is an empty string") {
          val instance = "/lists"
          mockMvc.post(instance) {
            with(jwt())
            with(csrf())
            content = Json.encodeToString(LrmListRequest("", lrmList().description, lrmList().public))
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
          verify(exactly = 0) { mockLrmListService.create(ofType(LrmListRequest::class), ofType(String::class)) }
        }

        it("requested list description is an empty string") {
          val instance = "/lists"
          mockMvc.post(instance) {
            with(jwt())
            with(csrf())
            content = Json.encodeToString(LrmListRequest(lrmList().name, "", lrmList().public))
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
          verify(exactly = 0) { mockLrmListService.create(ofType(LrmListRequest::class), ofType(String::class)) }
        }
      }
    }

    describe("/lists/public") {
      describe("get") {
        it("lists are found") {
          val mockReturn = listOf(lrmList())
          every { mockLrmListService.findByPublic() } returns mockReturn
          val instance = "/lists/public"
          mockMvc.get(instance) {
            contentType = MediaType.APPLICATION_JSON
          }.andExpect {
            status { isOk() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
            jsonPath("$.method") { value(HttpMethod.GET.name().lowercase()) }
            jsonPath("$.message") { value("retrieved all public lists") }
            jsonPath("$.instance") { value(instance) }
            jsonPath("$.size") { value(mockReturn.size) }
            jsonPath("$.content") { exists() }
            jsonPath("$.content") { isArray() }
            jsonPath("$.content.[0].name") { value(lrmList().name) }
            jsonPath("$.content.[0].description") { value(lrmList().description) }
            jsonPath("$.content.[0].items") {
              doesNotExist()
            }
          }
          verify(exactly = 1) { mockLrmListService.findByPublic() }
        }

        it("lists are found ?includeItems=false") {
          val mockReturn = listOf(lrmList())
          every { mockLrmListService.findByPublic() } returns mockReturn
          val instance = "/lists/public?includeItems=false"
          mockMvc.get(instance) {
            contentType = MediaType.APPLICATION_JSON
          }.andExpect {
            status { isOk() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
            jsonPath("$.method") { value(HttpMethod.GET.name().lowercase()) }
            jsonPath("$.message") { value("retrieved all public lists") }
            jsonPath("$.instance") { value(instance.substringBeforeLast("?").removeSuffix(instance)) }
            jsonPath("$.size") { value(mockReturn.size) }
            jsonPath("$.content") { isArray() }
            jsonPath("$.content.[0].name") { value(lrmList().name) }
            jsonPath("$.content.[0].description") { value(lrmList().description) }
            jsonPath("$.content.[0].items") {
              doesNotExist()
            }
          }
          verify(exactly = 1) { mockLrmListService.findByPublic() }
        }

        it("lists are found ?includeItems=true") {
          val mockReturn = listOf(lrmListWithEmptyItems())
          every { mockLrmListService.findByPublicIncludeItems() } returns mockReturn
          val instance = "/lists/public?includeItems=true"
          mockMvc.get(instance) {
            contentType = MediaType.APPLICATION_JSON
          }.andExpect {
            status { isOk() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
            jsonPath("$.method") { value(HttpMethod.GET.name().lowercase()) }
            jsonPath("$.message") { value("retrieved all public lists") }
            jsonPath("$.instance") { value(instance.substringBeforeLast("?").removeSuffix(instance)) }
            jsonPath("$.size") { value(mockReturn.size) }
            jsonPath("$.content") { isArray() }
            jsonPath("$.content.[0].name") { value(lrmList().name) }
            jsonPath("$.content.[0].description") { value(lrmList().description) }
            jsonPath("$.content.[0].items") {
              isArray()
              isEmpty()
            }
          }
          verify(exactly = 1) { mockLrmListService.findByPublicIncludeItems() }
        }
      }
    }

    describe("/lists/with-no-items") {
      describe("get") {
        it("lists with no item association are found") {
          val mockReturn = listOf(lrmList())
          val expectedMessage = "Retrieved ${mockReturn.size} lists containing no items."
          every { mockLrmListService.findByOwnerAndHavingNoItemAssociations(owner = ofType(String::class)) } returns mockReturn
          val instance = "/lists/with-no-items"
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
          verify(exactly = 1) { mockLrmListService.findByOwnerAndHavingNoItemAssociations(owner = ofType(String::class)) }
        }
      }
    }

    describe("/lists/count") {
      describe("get") {
        it("count of lists is returned") {
          every { mockLrmListService.countByOwner(owner = ofType(String::class)) } returns 999
          val instance = "/lists/count"
          mockMvc.get(instance) {
            with(jwt())
          }.andExpect {
            status { isOk() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
            jsonPath("$.method") { value(HttpMethod.GET.name().lowercase()) }
            jsonPath("$.message") { value("999 lists.") }
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
            mockLrmListService.deleteByOwnerAndId(id = id1, owner = ofType(String::class), removeItemAssociations = false)
          } returns LrmListDeleteResponse(listNames = listOf("dolor sit amet"), associatedItemNames = listOf("Lorem Ipsum"))
          val instance = "/lists/$id1"
          mockMvc.delete(instance) {
            with(jwt())
            with(csrf())
          }.andExpect {
            status { isOk() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
            jsonPath("$.method") { value(HttpMethod.DELETE.name().lowercase()) }
            jsonPath("$.message") { value("Deleted list id $id1.") }
            jsonPath("$.instance") { value(instance) }
            jsonPath("$.size") { value(1) }
            jsonPath("$.content.listNames.length()") { value(1) }
            jsonPath("$.content.listNames.[0]") { value("dolor sit amet") }
            jsonPath("$.content.associatedItemNames.length()") { value(1) }
            jsonPath("$.content.associatedItemNames.[0]") { value("Lorem Ipsum") }
          }
          verify(exactly = 1) {
            mockLrmListService.deleteByOwnerAndId(
              id = ofType(UUID::class),
              owner = ofType(String::class),
              removeItemAssociations = ofType(Boolean::class),
            )
          }
        }

        it("list is not found") {
          every {
            mockLrmListService.deleteByOwnerAndId(id = id1, owner = ofType(String::class), removeItemAssociations = false)
          } throws ListNotFoundException(id1)
          val expectedMessage = ListNotFoundException.defaultMessage()
          val instance = "/lists/$id1"
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
            jsonPath("$.content.title") { value(ListNotFoundException::class.java.simpleName) }
            jsonPath("$.content.status") { HttpStatus.NOT_FOUND.value() }
            jsonPath("$.content.detail") { value(expectedMessage) }
          }
          verify(exactly = 1) {
            mockLrmListService.deleteByOwnerAndId(
              id = ofType(UUID::class),
              owner = ofType(String::class),
              removeItemAssociations = ofType(Boolean::class),
            )
          }
        }
      }

      describe("get") {
        it("list is found") {
          every { mockLrmListService.findByOwnerAndId(id = id1, owner = ofType(String::class)) } returns lrmList()
          val instance = "/lists/$id1"
          mockMvc.get(instance) {
            with(jwt())
          }.andExpect {
            status { isOk() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
            jsonPath("$.method") { value(HttpMethod.GET.name().lowercase()) }
            jsonPath("$.message") { value("retrieved list id $id1") }
            jsonPath("$.instance") { value(instance) }
            jsonPath("$.size") { value(1) }
            jsonPath("$.content.description") { value(lrmList().description) }
            jsonPath("$.content.name") { value(lrmList().name) }
          }
          verify(exactly = 1) { mockLrmListService.findByOwnerAndId(id = ofType(UUID::class), owner = ofType(String::class)) }
        }

        it("list is found ?includeItems=true") {
          every { mockLrmListService.findByOwnerAndIdIncludeItems(id = id1, owner = ofType(String::class)) } returns lrmListWithEmptyItems()
          val instance = "/lists/$id1?includeItems=true"
          mockMvc.get(instance) {
            with(jwt())
          }.andExpect {
            status { isOk() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
            jsonPath("$.method") { value(HttpMethod.GET.name().lowercase()) }
            jsonPath("$.message") { value("retrieved list id $id1") }
            jsonPath("$.instance") { value(instance.substringBeforeLast("?").removeSuffix(instance)) }
            jsonPath("$.size") { value(1) }
            jsonPath("$.content.description") { value(lrmList().description) }
            jsonPath("$.content.name") { value(lrmList().name) }
            jsonPath("$.content.items") {
              isArray()
              isEmpty()
            }
          }
          verify(exactly = 1) { mockLrmListService.findByOwnerAndIdIncludeItems(id = ofType(UUID::class), owner = ofType(String::class)) }
        }

        it("list is found ?includeItems=false") {
          every { mockLrmListService.findByOwnerAndId(id = id1, owner = ofType(String::class)) } returns lrmList()
          val instance = "/lists/$id1?includeItems=false"
          mockMvc.get(instance) {
            with(jwt())
          }.andExpect {
            status { isOk() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
            jsonPath("$.method") { value(HttpMethod.GET.name().lowercase()) }
            jsonPath("$.message") { value("retrieved list id $id1") }
            jsonPath("$.instance") { value(instance.substringBeforeLast("?").removeSuffix(instance)) }
            jsonPath("$.size") { value(1) }
            jsonPath("$.content.description") { value(lrmList().description) }
            jsonPath("$.content.name") { value(lrmList().name) }
            jsonPath("$.content.items") {
              doesNotExist()
            }
          }
          verify(exactly = 1) { mockLrmListService.findByOwnerAndId(id = ofType(UUID::class), owner = ofType(String::class)) }
        }

        it("list is not found") {
          every { mockLrmListService.findByOwnerAndId(id = id1, owner = ofType(String::class)) } throws ListNotFoundException(id1)
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
          verify(exactly = 1) { mockLrmListService.findByOwnerAndId(id = ofType(UUID::class), owner = ofType(String::class)) }
        }
      }

      describe("patch") {
        it("list is found and updated") {
          every { mockLrmListService.patchByOwnerAndId(id = id1, owner = ofType(String::class), any()) } returns Pair(lrmList(), true)
          val instance = "/lists/$id1"
          mockMvc.patch(instance) {
            with(jwt())
            with(csrf())
            content = Json.encodeToString(mapOf("name" to lrmList().name))
            contentType = MediaType.APPLICATION_JSON
          }.andExpect {
            status { isOk() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
            jsonPath("$.method") { value(HttpMethod.PATCH.name().lowercase()) }
            jsonPath("$.message") { value("patched") }
            jsonPath("$.instance") { value(instance) }
            jsonPath("$.size") { value(1) }
            jsonPath("$.content.description") { value(lrmList().description) }
            jsonPath("$.content.name") { value(lrmList().name) }
            jsonPath("$.content.items") {
              doesNotExist()
            }
          }
          verify(exactly = 1) {
            mockLrmListService.patchByOwnerAndId(
              id = ofType(UUID::class),
              owner = ofType(String::class),
              patchRequest = mapOf("name" to lrmList().name),
            )
          }
        }

        it("list is found and not updated") {
          every { mockLrmListService.patchByOwnerAndId(id = id1, owner = ofType(String::class), any()) } returns Pair(lrmList(), false)
          val instance = "/lists/$id1"
          mockMvc.patch(instance) {
            with(jwt())
            with(csrf())
            content = Json.encodeToString(mapOf("name" to lrmList().name))
            contentType = MediaType.APPLICATION_JSON
          }.andExpect {
            status { isNoContent() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
            jsonPath("$.method") { value(HttpMethod.PATCH.name().lowercase()) }
            jsonPath("$.message") { value("not patched - list is up-to-date") }
            jsonPath("$.instance") { value(instance) }
            jsonPath("$.size") { value(1) }
            jsonPath("$.content.description") { value(lrmList().description) }
            jsonPath("$.content.name") { value(lrmList().name) }
            jsonPath("$.content.items") {
              doesNotExist()
            }
          }
          verify(exactly = 1) {
            mockLrmListService.patchByOwnerAndId(
              id = ofType(UUID::class),
              owner = ofType(String::class),
              patchRequest = mapOf("name" to lrmList().name),
            )
          }
        }

        it("list is not found") {
          every { mockLrmListService.patchByOwnerAndId(id = id1, owner = ofType(String::class), any()) } throws ListNotFoundException(id1)
          val instance = "/lists/$id1"
          mockMvc.patch(instance) {
            with(jwt())
            with(csrf())
            content = Json.encodeToString(mapOf("name" to lrmList().name))
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
            mockLrmListService.patchByOwnerAndId(
              id = ofType(UUID::class),
              owner = ofType(String::class),
              patchRequest = mapOf("name" to lrmList().name),
            )
          }
        }
      }
    }

    describe("/lists/{list-id}/items/{item-id}") {
      describe("delete") {
        it("item is removed from list") {
          val lrmItemName = "Lorem Item Name"
          val lrmListName = "Lorem List Name"
          every {
            mockAssociationService.deleteByItemIdAndListId(
              itemId = id2,
              listId = id1,
              componentsOwner = ofType(String::class),
            )
          } returns Pair(lrmItemName, lrmListName)
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
            jsonPath("$.message") { value("Removed item '$lrmItemName' from list '$lrmListName'.") }
            jsonPath("$.instance") { value(instance) }
            jsonPath("$.size") { value(1) }
            jsonPath("$.content.message") { value("Removed item '$lrmItemName' from list '$lrmListName'.") }
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
            mockAssociationService.deleteByItemIdAndListId(
              itemId = id2,
              listId = id1,
              componentsOwner = ofType(String::class),
            )
          } throws ItemNotFoundException(id2)
          val expectedMessage = ItemNotFoundException.defaultMessage()
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
              listId = ofType(UUID::class),
              componentsOwner = ofType(String::class),
            )
          }
        }

        it("list is not found") {
          every {
            mockAssociationService.deleteByItemIdAndListId(itemId = id2, listId = id1, componentsOwner = ofType(String::class))
          } throws ListNotFoundException(id1)
          val expectedMessage = ListNotFoundException.defaultMessage()
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

      describe("post") {
        it("item is added to list") {
          val mockResponse = AssociationCreatedResponse(
            componentName = lrmList1().name,
            associatedComponents = listOf(lrmItem2().succinct()),
          )
          every {
            mockAssociationService.create(
              id = id1,
              idCollection = any(),
              type = LrmComponentType.List,
              componentOwner = ofType(String::class),
            )
          } returns mockResponse
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
            jsonPath("$.message") { value("Assigned item '${lrmItem2().name}' to list '${lrmList1().name}'.") }
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
            associatedComponents = listOf(lrmItem2().succinct(), lrmItem3().succinct()),
          )
          every {
            mockAssociationService.create(
              id = id1,
              idCollection = any(),
              type = LrmComponentType.List,
              componentOwner = ofType(String::class),
            )
          } returns mockResponse
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
            jsonPath("$.message") { value("Assigned ${mockResponse.associatedComponents.size} items to list '${lrmList1().name}'.") }
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
            mockAssociationService.create(
              id = id1,
              idCollection = any(),
              type = LrmComponentType.List,
              componentOwner = ofType(String::class),
            )
          } throws ListNotFoundException(id2)
          val expectedMessage = ListNotFoundException.defaultMessage()
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
            jsonPath("$.message") { value(expectedMessage) }
            jsonPath("$.instance") { value(instance) }
            jsonPath("$.size") { value(1) }
            jsonPath("$.content.title") { value(ListNotFoundException::class.java.simpleName) }
            jsonPath("$.content.status") { ListNotFoundException.HTTP_STATUS.value() }
            jsonPath("$.content.detail") { value(expectedMessage) }
          }
        }

        it("item is not found") {
          every {
            mockAssociationService.create(
              id = id1,
              idCollection = any(),
              type = LrmComponentType.List,
              componentOwner = ofType(String::class),
            )
          } throws ItemNotFoundException(id2)
          val expectedMessage = ItemNotFoundException.defaultMessage()
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
            jsonPath("$.message") { value(expectedMessage) }
            jsonPath("$.instance") { value(instance) }
            jsonPath("$.size") { value(1) }
            jsonPath("$.content.title") { value(ItemNotFoundException::class.java.simpleName) }
            jsonPath("$.content.status") { ItemNotFoundException.HTTP_STATUS.value() }
            jsonPath("$.content.detail") { value(expectedMessage) }
          }
        }
      }
    }

    describe("/lists/{list-id}/items/count") {
      describe("get") {
        it("count of item associations is returned") {
          every { mockAssociationService.countByOwnerForList(listId = id1, listOwner = ofType(String::class)) } returns 999
          val instance = "/lists/$id1/items/count"
          mockMvc.get(instance) {
            with(jwt())
          }.andExpect {
            status { isOk() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
            jsonPath("$.method") { value(HttpMethod.GET.name().lowercase()) }
            jsonPath("$.message") { value("List is associated with 999 items.") }
            jsonPath("$.instance") { value(instance) }
            jsonPath("$.size") { value(1) }
            jsonPath("$.content.length()") { value(1) }
            jsonPath("$.content.value") { value(999) }
          }
          verify(
            exactly = 1,
          ) { mockAssociationService.countByOwnerForList(listId = ofType(UUID::class), listOwner = ofType(String::class)) }
        }

        it("item is not found") {
          every {
            mockAssociationService.countByOwnerForList(listId = id1, listOwner = ofType(String::class))
          } throws ApiException(httpStatus = HttpStatus.NOT_FOUND)
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
          verify(
            exactly = 1,
          ) { mockAssociationService.countByOwnerForList(listId = ofType(UUID::class), listOwner = ofType(String::class)) }
        }
      }
    }

    describe("/lists/{list-id}/items") {
      it("all items are removed from a list") {
        val lrmListName = "Lorem List Name"
        every {
          mockAssociationService.deleteByListOwnerAndListId(
            listId = id1,
            listOwner = ofType(String::class),
          )
        } returns Pair(lrmListName, 999)
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
          jsonPath("$.message") { value("Removed all associated items (999) from list '$lrmListName'.") }
          jsonPath("$.instance") { value(instance) }
          jsonPath("$.size") { value(1) }
          jsonPath("$.content.value") { value(999) }
        }
        verify(
          exactly = 1,
        ) { mockAssociationService.deleteByListOwnerAndListId(listId = ofType(UUID::class), listOwner = ofType(String::class)) }
      }
    }
  }
}
