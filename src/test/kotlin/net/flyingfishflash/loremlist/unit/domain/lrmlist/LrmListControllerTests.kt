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
import net.flyingfishflash.loremlist.core.exceptions.ApiException
import net.flyingfishflash.loremlist.core.response.advice.ApiExceptionHandler.Companion.VALIDATION_FAILURE_MESSAGE
import net.flyingfishflash.loremlist.core.response.structure.DispositionOfProblem
import net.flyingfishflash.loremlist.core.response.structure.DispositionOfSuccess
import net.flyingfishflash.loremlist.domain.association.AssociationService
import net.flyingfishflash.loremlist.domain.lrmitem.ItemNotFoundException
import net.flyingfishflash.loremlist.domain.lrmlist.ListNotFoundException
import net.flyingfishflash.loremlist.domain.lrmlist.LrmList
import net.flyingfishflash.loremlist.domain.lrmlist.LrmListController
import net.flyingfishflash.loremlist.domain.lrmlist.LrmListService
import net.flyingfishflash.loremlist.domain.lrmlist.data.LrmListDeleteResponse
import net.flyingfishflash.loremlist.domain.lrmlist.data.LrmListRequest
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
import java.util.UUID

/**
 * LrmListController Unit Tests
 */
@WebMvcTest(controllers = [LrmListController::class])
class LrmListControllerTests(mockMvc: MockMvc) : DescribeSpec() {
  override fun extensions() = listOf(SpringExtension)

  @MockkBean
  lateinit var mockAssociationService: AssociationService

  @MockkBean
  lateinit var mockLrmListService: LrmListService

  init {
    val uuid0 = UUID.fromString("00000000-0000-4000-a000-000000000000")
    val uuid1 = UUID.fromString("00000000-0000-4000-a000-000000000001")
    val uuid2 = UUID.fromString("00000000-0000-4000-a000-000000000002")
    val lrmListRequest = LrmListRequest("Lorem List Name", "Lorem List Description")

    fun lrmList(): LrmList = LrmList(uuid = uuid0, name = lrmListRequest.name, description = lrmListRequest.description)
    fun lrmListWithEmptyItems(): LrmList = lrmList().copy(items = setOf())

    afterEach { clearAllMocks() }
    afterSpec { unmockkAll() }

    describe("/lists") {
      describe("delete") {
        it("lists are deleted") {
          val mockReturn = LrmListDeleteResponse(listNames = listOf("Lorem List Name"), associatedItemNames = listOf("Lorem Item Name"))
          every { mockLrmListService.deleteAll() } returns mockReturn
          val instance = "/lists"
          mockMvc.delete(instance) {
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
          every { mockLrmListService.findAll() } returns mockReturn
          val instance = "/lists"
          mockMvc.get(instance) {
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
          verify(exactly = 1) { mockLrmListService.findAll() }
        }

        it("lists are found ?includeItems=false") {
          val mockReturn = listOf(lrmList())
          every { mockLrmListService.findAll() } returns mockReturn
          val instance = "/lists?includeItems=false"
          mockMvc.get(instance) {
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
          verify(exactly = 1) { mockLrmListService.findAll() }
        }

        it("lists are found ?includeItems=true") {
          val mockReturn = listOf(lrmListWithEmptyItems())
          every { mockLrmListService.findAllIncludeItems() } returns mockReturn
          val instance = "/lists?includeItems=true"
          mockMvc.get(instance) {
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
          verify(exactly = 1) { mockLrmListService.findAllIncludeItems() }
        }
      }

      describe("post") {
        it("list is created") {
          println(Json.encodeToString(lrmListRequest))
          every { mockLrmListService.create(lrmListRequest) } returns lrmList()
          val instance = "/lists"
          mockMvc.post(instance) {
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
          verify(exactly = 1) { mockLrmListService.create(lrmListRequest) }
        }

        it("requested list name is an empty string") {
          val instance = "/lists"
          mockMvc.post(instance) {
            content = Json.encodeToString(LrmListRequest("", lrmList().description))
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
          verify(exactly = 0) { mockLrmListService.create(ofType(LrmListRequest::class)) }
        }

        it("requested list description is an empty string") {
          val instance = "/lists"
          mockMvc.post(instance) {
            content = Json.encodeToString(LrmListRequest(lrmList().name, ""))
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
          verify(exactly = 0) { mockLrmListService.create(ofType(LrmListRequest::class)) }
        }
      }
    }

    describe("/lists/count") {
      describe("get") {
        it("count of lists is returned") {
          every { mockLrmListService.count() } returns 999
          val instance = "/lists/count"
          mockMvc.get(instance).andExpect {
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

    describe("/lists/{id}") {
      describe("delete") {
        it("list is deleted") {
          // nonsensical conditioning of the delete response:
          // if the count of item to list associations is 0, then associatedListNames should be an empty list
          every {
            mockLrmListService.deleteById(uuid1, removeItemAssociations = false)
          } returns LrmListDeleteResponse(listNames = listOf("dolor sit amet"), associatedItemNames = listOf("Lorem Ipsum"))
          val instance = "/lists/$uuid1"
          mockMvc.delete(instance).andExpect {
            status { isOk() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
            jsonPath("$.method") { value(HttpMethod.DELETE.name().lowercase()) }
            jsonPath("$.message") { value("Deleted list id $uuid1.") }
            jsonPath("$.instance") { value(instance) }
            jsonPath("$.size") { value(1) }
            jsonPath("$.content.listNames.length()") { value(1) }
            jsonPath("$.content.listNames.[0]") { value("dolor sit amet") }
            jsonPath("$.content.associatedItemNames.length()") { value(1) }
            jsonPath("$.content.associatedItemNames.[0]") { value("Lorem Ipsum") }
          }
          verify(exactly = 1) { mockLrmListService.deleteById(any(), any()) }
        }

        it("list is not found") {
          every { mockLrmListService.deleteById(uuid1, removeItemAssociations = false) } throws ListNotFoundException(uuid1)
          val instance = "/lists/$uuid1"
          mockMvc.delete(instance).andExpect {
            status { isNotFound() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.disposition") { value(DispositionOfProblem.FAILURE.nameAsLowercase()) }
            jsonPath("$.method") { value(HttpMethod.DELETE.name().lowercase()) }
            jsonPath("$.message") { value("List id $uuid1 could not be found.") }
            jsonPath("$.instance") { value(instance) }
            jsonPath("$.size") { value(1) }
            jsonPath("$.content.title") { value(ListNotFoundException::class.java.simpleName) }
            jsonPath("$.content.status") { HttpStatus.NOT_FOUND.value() }
            jsonPath("$.content.detail") { value("List id $uuid1 could not be found.") }
          }
          verify(exactly = 1) { mockLrmListService.deleteById(any(), any()) }
        }
      }

      describe("get") {
        it("list is found") {
          every { mockLrmListService.findById(uuid1) } returns lrmList()
          val instance = "/lists/$uuid1"
          mockMvc.get(instance).andExpect {
            status { isOk() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
            jsonPath("$.method") { value(HttpMethod.GET.name().lowercase()) }
            jsonPath("$.message") { value("retrieved list id $uuid1") }
            jsonPath("$.instance") { value(instance) }
            jsonPath("$.size") { value(1) }
            jsonPath("$.content.description") { value(lrmList().description) }
            jsonPath("$.content.name") { value(lrmList().name) }
          }
          verify(exactly = 1) { mockLrmListService.findById(any()) }
        }

        it("list is found ?includeItems=true") {
          every { mockLrmListService.findByIdIncludeItems(uuid1) } returns lrmListWithEmptyItems()
          val instance = "/lists/$uuid1?includeItems=true"
          mockMvc.get(instance).andExpect {
            status { isOk() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
            jsonPath("$.method") { value(HttpMethod.GET.name().lowercase()) }
            jsonPath("$.message") { value("retrieved list id $uuid1") }
            jsonPath("$.instance") { value(instance.substringBeforeLast("?").removeSuffix(instance)) }
            jsonPath("$.size") { value(1) }
            jsonPath("$.content.description") { value(lrmList().description) }
            jsonPath("$.content.name") { value(lrmList().name) }
            jsonPath("$.content.items") {
              isArray()
              isEmpty()
            }
          }
          verify(exactly = 1) { mockLrmListService.findByIdIncludeItems(any()) }
        }

        it("list is found ?includeItems=false") {
          every { mockLrmListService.findById(uuid1) } returns lrmList()
          val instance = "/lists/$uuid1?includeItems=false"
          mockMvc.get(instance).andExpect {
            status { isOk() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
            jsonPath("$.method") { value(HttpMethod.GET.name().lowercase()) }
            jsonPath("$.message") { value("retrieved list id $uuid1") }
            jsonPath("$.instance") { value(instance.substringBeforeLast("?").removeSuffix(instance)) }
            jsonPath("$.size") { value(1) }
            jsonPath("$.content.description") { value(lrmList().description) }
            jsonPath("$.content.name") { value(lrmList().name) }
            jsonPath("$.content.items") {
              doesNotExist()
            }
          }
          verify(exactly = 1) { mockLrmListService.findById(any()) }
        }

        it("list is not found") {
          every { mockLrmListService.findById(uuid1) } throws ListNotFoundException(uuid1)
          val instance = "/lists/$uuid1"
          mockMvc.get(instance).andExpect {
            status { isNotFound() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.disposition") { value(DispositionOfProblem.FAILURE.nameAsLowercase()) }
            jsonPath("$.method") { value(HttpMethod.GET.name().lowercase()) }
            jsonPath("$.instance") { value(instance) }
            jsonPath("$.size") { value(1) }
            jsonPath("$.content.title") { value(ListNotFoundException::class.java.simpleName) }
            jsonPath("$.content.status") { HttpStatus.NOT_FOUND.value() }
          }
          verify(exactly = 1) { mockLrmListService.findById(any()) }
        }
      }

      describe("patch") {
        it("list is found and updated") {
          every { mockLrmListService.patch(uuid1, any()) } returns Pair(lrmList(), true)
          val instance = "/lists/$uuid1"
          mockMvc.patch(instance) {
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
          verify(exactly = 1) { mockLrmListService.patch(any(), mapOf("name" to lrmList().name)) }
        }

        it("list is found and not updated") {
          every { mockLrmListService.patch(uuid1, any()) } returns Pair(lrmList(), false)
          val instance = "/lists/$uuid1"
          mockMvc.patch(instance) {
            content = Json.encodeToString(mapOf("name" to lrmList().name))
            contentType = MediaType.APPLICATION_JSON
          }.andExpect {
            status { isNoContent() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
            jsonPath("$.method") { value(HttpMethod.PATCH.name().lowercase()) }
            jsonPath("$.message") { value("not patched") }
            jsonPath("$.instance") { value(instance) }
            jsonPath("$.size") { value(1) }
            jsonPath("$.content.description") { value(lrmList().description) }
            jsonPath("$.content.name") { value(lrmList().name) }
            jsonPath("$.content.items") {
              doesNotExist()
            }
          }
          verify(exactly = 1) { mockLrmListService.patch(any(), mapOf("name" to lrmList().name)) }
        }

        it("list is not found") {
          every { mockLrmListService.patch(uuid1, any()) } throws ListNotFoundException(uuid1)
          val instance = "/lists/$uuid1"
          mockMvc.patch(instance) {
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
          verify(exactly = 1) { mockLrmListService.patch(any(), mapOf("name" to lrmList().name)) }
        }
      }
    }

    describe("/lists/{id}/item-associations") {
      describe("delete") {
        it("item is removed from list") {
          val lrmItemName = "Lorem Item Name"
          val lrmListName = "Lorem List Name"
          every {
            mockAssociationService.deleteByItemIdAndListId(
              itemUuid = uuid2,
              listUuid = uuid1,
            )
          } returns Pair(lrmItemName, lrmListName)
          val instance = "/lists/$uuid1/item-associations"
          mockMvc.delete(instance) {
            content = Json.encodeToString(uuid2.toString())
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
          verify(exactly = 1) { mockAssociationService.deleteByItemIdAndListId(any(), any()) }
        }

        it("item is not found") {
          every { mockAssociationService.deleteByItemIdAndListId(itemUuid = uuid2, listUuid = uuid1) } throws ItemNotFoundException(uuid2)
          val instance = "/lists/$uuid1/item-associations"
          mockMvc.delete(instance) {
            content = Json.encodeToString(uuid2.toString())
            contentType = MediaType.APPLICATION_JSON
          }.andExpect {
            status { isNotFound() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.disposition") { value(DispositionOfProblem.FAILURE.nameAsLowercase()) }
            jsonPath("$.method") { value(HttpMethod.DELETE.name().lowercase()) }
            jsonPath("$.message") { value("Item id $uuid2 could not be found.") }
            jsonPath("$.instance") { value(instance) }
            jsonPath("$.size") { value(1) }
            jsonPath("$.content.title") { value(ItemNotFoundException::class.java.simpleName) }
            jsonPath("$.content.status") { HttpStatus.NOT_FOUND.value() }
            jsonPath("$.content.detail") { value("Item id $uuid2 could not be found.") }
          }
          verify(exactly = 1) { mockAssociationService.deleteByItemIdAndListId(any(), any()) }
        }

        it("list is not found") {
          every { mockAssociationService.deleteByItemIdAndListId(itemUuid = uuid2, listUuid = uuid1) } throws ListNotFoundException(uuid1)
          val instance = "/lists/$uuid1/item-associations"
          mockMvc.delete(instance) {
            content = Json.encodeToString(uuid2.toString())
            contentType = MediaType.APPLICATION_JSON
          }.andExpect {
            status { isNotFound() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.disposition") { value(DispositionOfProblem.FAILURE.nameAsLowercase()) }
            jsonPath("$.method") { value(HttpMethod.DELETE.name().lowercase()) }
            jsonPath("$.message") { value("List id $uuid1 could not be found.") }
            jsonPath("$.instance") { value(instance) }
            jsonPath("$.size") { value(1) }
            jsonPath("$.content.title") { value(ListNotFoundException::class.java.simpleName) }
            jsonPath("$.content.status") { HttpStatus.NOT_FOUND.value() }
            jsonPath("$.content.detail") { value("List id $uuid1 could not be found.") }
          }
          verify(exactly = 1) { mockAssociationService.deleteByItemIdAndListId(any(), any()) }
        }
      }

      describe("post") {
        it("item is added to list") {
          val lrmItemName = "Lorem Item Name"
          val lrmListName = "Lorem List Name"
          every { mockAssociationService.create(itemUuid = uuid1, listUuid = uuid2) } returns Pair(lrmItemName, lrmListName)
          val instance = "/lists/$uuid2/item-associations"
          mockMvc.post(instance) {
            content = Json.encodeToString(uuid1.toString())
            contentType = MediaType.APPLICATION_JSON
          }.andExpect {
            status { isOk() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
            jsonPath("$.method") { value(HttpMethod.POST.name().lowercase()) }
            jsonPath("$.message") { value("Assigned item '$lrmItemName' to list '$lrmListName'.") }
            jsonPath("$.instance") { value(instance) }
            jsonPath("$.size") { value(1) }
            jsonPath("$.content.message") { value("Assigned item '$lrmItemName' to list '$lrmListName'.") }
          }
          verify(exactly = 1) { mockAssociationService.create(any(), any()) }
        }

        it("item is not found") {
          every { mockAssociationService.create(itemUuid = uuid1, listUuid = uuid2) } throws ItemNotFoundException(uuid1)
          val instance = "/lists/$uuid2/item-associations"
          mockMvc.post(instance) {
            content = Json.encodeToString(uuid1.toString())
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
          val instance = "/lists/$uuid2/item-associations"
          mockMvc.post(instance) {
            content = Json.encodeToString(uuid1.toString())
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

    describe("/lists/{id}/item-associations/count") {
      describe("get") {
        it("count of item associations is returned") {
          every { mockAssociationService.countForListId(uuid1) } returns 999
          val instance = "/lists/$uuid1/item-associations/count"
          mockMvc.get(instance).andExpect {
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
          verify(exactly = 1) { mockAssociationService.countForListId(any()) }
        }

        it("item is not found") {
          every { mockAssociationService.countForListId(uuid1) } throws ApiException(httpStatus = HttpStatus.NOT_FOUND)
          val instance = "/lists/$uuid1/item-associations/count"
          mockMvc.get(instance).andExpect {
            status { isNotFound() }
            jsonPath("$.disposition") { value(DispositionOfProblem.FAILURE.nameAsLowercase()) }
            jsonPath("$.method") { value(HttpMethod.GET.name().lowercase()) }
            jsonPath("$.instance") { value(instance) }
            jsonPath("$.size") { value(1) }
            jsonPath("$.content.status") { HttpStatus.NOT_FOUND.value() }
          }
          verify(exactly = 1) { mockAssociationService.countForListId(any()) }
        }
      }
    }

    describe("/lists/{id}/item-associations/delete-all") {
      it("all items are removed from a list") {
        val lrmListName = "Lorem List Name"
        every { mockAssociationService.deleteAllOfList(uuid1) } returns Pair(lrmListName, 999)
        val instance = "/lists/$uuid1/item-associations/delete-all"
        mockMvc.delete(instance) {
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
        verify(exactly = 1) { mockAssociationService.deleteAllOfList(any()) }
      }
    }
  }
}
