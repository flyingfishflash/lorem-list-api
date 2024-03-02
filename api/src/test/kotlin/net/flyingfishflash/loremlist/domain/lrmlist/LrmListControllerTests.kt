package net.flyingfishflash.loremlist.domain.lrmlist

import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.extensions.spring.SpringExtension
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.verify
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.flyingfishflash.loremlist.core.response.structure.DispositionOfProblem
import net.flyingfishflash.loremlist.core.response.structure.DispositionOfSuccess
import net.flyingfishflash.loremlist.domain.lrmlist.data.LrmList
import net.flyingfishflash.loremlist.domain.lrmlist.data.dto.LrmListRequest
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
@WebMvcTest(controllers = [LrmListController::class])
class LrmListControllerTests(mockMvc: MockMvc) : DescribeSpec() {
  override fun extensions() = listOf(SpringExtension)

  @MockkBean
  lateinit var lrmListService: LrmListService

  init {

    val lrmListName = "Lorem List Name"
    val lrmListDescription = "Lorem List Description"
    val lrmListMockResponse = LrmList(id = 0, name = lrmListName, description = lrmListDescription)
    val lrmListMockResponseWithEmptyItems = LrmList(id = 0, name = lrmListName, description = lrmListDescription, items = setOf())
    val lrmListRequest = LrmListRequest(lrmListName, lrmListDescription)
    val id = 1L

    describe("/lists http get") {
      it("lists are found") {
        val mockReturn = listOf(lrmListMockResponse)
        every { lrmListService.findAll() } returns mockReturn
        val instance = "/lists"
        mockMvc.get(instance) {
          contentType = MediaType.APPLICATION_JSON
        }.andExpect {
          status { isOk() }
          content { contentType(MediaType.APPLICATION_JSON) }
          jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
          jsonPath("$.method") { value(HttpMethod.GET.name().lowercase()) }
          jsonPath("$.message") { value("the message") }
          jsonPath("$.instance") { value(instance) }
          jsonPath("$.size") { value(mockReturn.size) }
          jsonPath("$.content") { exists() }
          jsonPath("$.content") { isArray() }
          jsonPath("$.content.[0].name") { value(lrmListName) }
          jsonPath("$.content.[0].description") { value(lrmListDescription) }
          jsonPath("$.content.[0].items") {
            doesNotExist()
          }
        }
        verify(exactly = 1) { lrmListService.findAll() }
      }
    }

    describe("/lists?withItems=false http get") {
      it("lists are found") {
        val mockReturn = listOf(lrmListMockResponse)
        every { lrmListService.findAll() } returns mockReturn
        val instance = "/lists?withItems=false"
        mockMvc.get(instance) {
          contentType = MediaType.APPLICATION_JSON
        }.andExpect {
          status { isOk() }
          content { contentType(MediaType.APPLICATION_JSON) }
          jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
          jsonPath("$.method") { value(HttpMethod.GET.name().lowercase()) }
          jsonPath("$.message") { value("the message") }
          jsonPath("$.instance") { value(instance.substringBeforeLast("?").removeSuffix(instance)) }
          jsonPath("$.size") { value(mockReturn.size) }
          jsonPath("$.content") { isArray() }
          jsonPath("$.content.[0].name") { value(lrmListName) }
          jsonPath("$.content.[0].description") { value(lrmListDescription) }
          jsonPath("$.content.[0].items") {
            doesNotExist()
          }
        }
        verify(exactly = 1) { lrmListService.findAll() }
      }
    }

    describe("/lists?withItems=true http get") {
      it("lists and items are found") {
        val mockReturn = listOf(lrmListMockResponseWithEmptyItems)
        every { lrmListService.findAllListsAndItems() } returns mockReturn
        val instance = "/lists?withItems=true"
        mockMvc.get(instance) {
          contentType = MediaType.APPLICATION_JSON
        }.andExpect {
          status { isOk() }
          content { contentType(MediaType.APPLICATION_JSON) }
          jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
          jsonPath("$.method") { value(HttpMethod.GET.name().lowercase()) }
          jsonPath("$.message") { value("the message") }
          jsonPath("$.instance") { value(instance.substringBeforeLast("?").removeSuffix(instance)) }
          jsonPath("$.size") { value(mockReturn.size) }
          jsonPath("$.content") { isArray() }
          jsonPath("$.content.[0].name") { value(lrmListName) }
          jsonPath("$.content.[0].description") { value(lrmListDescription) }
          jsonPath("$.content.[0].items") {
            isArray()
            isEmpty()
          }
        }
        verify(exactly = 1) { lrmListService.findAllListsAndItems() }
      }
    }

    describe("/lists http post") {
      it("list is created") {
        println(Json.encodeToString(lrmListRequest))
        every { lrmListService.create(lrmListRequest) } returns lrmListMockResponse
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
          jsonPath("$.content.description") { value(lrmListDescription) }
          jsonPath("$.content.name") { value(lrmListName) }
        }
        verify(exactly = 1) { lrmListService.create(lrmListRequest) }
      }

      it("requested list name is an empty string") {
        val instance = "/lists"
        mockMvc.post(instance) {
          content = Json.encodeToString(LrmListRequest("", lrmListDescription))
          contentType = MediaType.APPLICATION_JSON
        }.andExpect {
          status { isBadRequest() }
          content { contentType(MediaType.APPLICATION_PROBLEM_JSON) }
          jsonPath("$.disposition") { value(DispositionOfProblem.FAILURE.nameAsLowercase()) }
          jsonPath("$.method") { value(HttpMethod.POST.name().lowercase()) }
          jsonPath("$.message") { value("from response problem constructor") }
          jsonPath("$.instance") { value(instance) }
          jsonPath("$.size") { value(1) }
          jsonPath("$.content.title") { value("Bad Request") }
          jsonPath("$.content.status") { HttpStatus.BAD_REQUEST.value() }
        }
        verify(exactly = 0) { lrmListService.create(ofType(LrmListRequest::class)) }
      }

      it("requested list description is an empty string") {
        val instance = "/lists"
        mockMvc.post(instance) {
          content = Json.encodeToString(LrmListRequest(lrmListName, ""))
          contentType = MediaType.APPLICATION_JSON
        }.andExpect {
          status { isBadRequest() }
          content { contentType(MediaType.APPLICATION_PROBLEM_JSON) }
          jsonPath("$.disposition") { value(DispositionOfProblem.FAILURE.nameAsLowercase()) }
          jsonPath("$.method") { value(HttpMethod.POST.name().lowercase()) }
          jsonPath("$.message") { value("from response problem constructor") }
          jsonPath("$.instance") { value(instance) }
          jsonPath("$.size") { value(1) }
          jsonPath("$.content.title") { value("Bad Request") }
          jsonPath("$.content.status") { HttpStatus.BAD_REQUEST.value() }
        }
        verify(exactly = 0) { lrmListService.create(ofType(LrmListRequest::class)) }
      }
    }

    describe("/lists/1 http delete") {
      it("list is deleted") {
        every { lrmListService.deleteSingleById(id) } just Runs
        val instance = "/lists/$id"
        mockMvc.delete(instance).andExpect {
          status { isNoContent() }
          content { contentType(MediaType.APPLICATION_JSON) }
          jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
          jsonPath("$.method") { value(HttpMethod.DELETE.name().lowercase()) }
          jsonPath("$.message") { value("deleted list id $id") }
          jsonPath("$.instance") { value(instance) }
          jsonPath("$.size") { value(1) }
          jsonPath("$.content") { value("content") }
        }
        verify(exactly = 1) { lrmListService.deleteSingleById(id) }
      }

      it("list is not found") {
        every { lrmListService.deleteSingleById(id) } throws ListNotFoundException(id)
        val instance = "/lists/$id"
        mockMvc.delete(instance).andExpect {
          status { isNotFound() }
          content { contentType(MediaType.APPLICATION_JSON) }
          jsonPath("$.disposition") { value(DispositionOfProblem.FAILURE.nameAsLowercase()) }
          jsonPath("$.method") { value(HttpMethod.DELETE.name().lowercase()) }
          jsonPath("$.message") { value("from response problem constructor") }
          jsonPath("$.instance") { value(instance) }
          jsonPath("$.size") { value(1) }
          jsonPath("$.content.title") { value(ListNotFoundException.TITLE) }
          jsonPath("$.content.status") { HttpStatus.NOT_FOUND.value() }
          jsonPath("$.content.detail") { value("List not found for id $id") }
        }
        verify(exactly = 1) { lrmListService.deleteSingleById(id) }
      }
    }

    describe("/lists/1 http get") {
      it("list is found") {
        every { lrmListService.findByIdOrListNotFoundExceptionListAndItems(id) } returns lrmListMockResponseWithEmptyItems
        val instance = "/lists/$id"
        mockMvc.get(instance).andExpect {
          status { isOk() }
          content { contentType(MediaType.APPLICATION_JSON) }
          jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
          jsonPath("$.method") { value(HttpMethod.GET.name().lowercase()) }
          jsonPath("$.message") { value("retrieved list $id") }
          jsonPath("$.instance") { value(instance) }
          jsonPath("$.size") { value(1) }
          jsonPath("$.content.description") { value(lrmListDescription) }
          jsonPath("$.content.name") { value(lrmListName) }
          jsonPath("$.content.items") {
            isArray()
            isEmpty()
          }
        }
        verify(exactly = 1) { lrmListService.findByIdOrListNotFoundExceptionListAndItems(id) }
      }

      it("list is not found") {
        every { lrmListService.findByIdOrListNotFoundExceptionListAndItems(id) } throws ListNotFoundException(id)
        val instance = "/lists/$id"
        mockMvc.get(instance).andExpect {
          status { isNotFound() }
          content { contentType(MediaType.APPLICATION_JSON) }
          jsonPath("$.disposition") { value(DispositionOfProblem.FAILURE.nameAsLowercase()) }
          jsonPath("$.method") { value(HttpMethod.GET.name().lowercase()) }
          jsonPath("$.message") { value("from response problem constructor") }
          jsonPath("$.instance") { value(instance) }
          jsonPath("$.size") { value(1) }
          jsonPath("$.content.title") { value(ListNotFoundException.TITLE) }
          jsonPath("$.content.status") { HttpStatus.NOT_FOUND.value() }
          jsonPath("$.content.detail") { value("List not found for id $id") }
        }
        verify(exactly = 1) { lrmListService.findByIdOrListNotFoundExceptionListAndItems(id) }
      }
    }

    describe("/lists/1?withItems=true http get") {
      it("list is found") {
        every { lrmListService.findByIdOrListNotFoundExceptionListAndItems(id) } returns lrmListMockResponseWithEmptyItems
        val instance = "/lists/$id?withItems=true"
        mockMvc.get("/lists/$id?withItems=true").andExpect {
          status { isOk() }
          content { contentType(MediaType.APPLICATION_JSON) }
          jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
          jsonPath("$.method") { value(HttpMethod.GET.name().lowercase()) }
          jsonPath("$.message") { value("retrieved list $id") }
          jsonPath("$.instance") { value(instance.substringBeforeLast("?").removeSuffix(instance)) }
          jsonPath("$.size") { value(1) }
          jsonPath("$.content.description") { value(lrmListDescription) }
          jsonPath("$.content.name") { value(lrmListName) }
          jsonPath("$.content.items") {
            isArray()
            isEmpty()
          }
        }
        verify(exactly = 1) { lrmListService.findByIdOrListNotFoundExceptionListAndItems(id) }
      }

      it("list is not found") {
        every { lrmListService.findByIdOrListNotFoundExceptionListAndItems(id) } throws ListNotFoundException(id)
        val instance = "/lists/$id?withItems=true"
        mockMvc.get(instance).andExpect {
          status { isNotFound() }
          content { contentType(MediaType.APPLICATION_JSON) }
          jsonPath("$.disposition") { value(DispositionOfProblem.FAILURE.nameAsLowercase()) }
          jsonPath("$.method") { value(HttpMethod.GET.name().lowercase()) }
          jsonPath("$.message") { value("from response problem constructor") }
          jsonPath("$.instance") { value(instance.substringBeforeLast("?").removeSuffix(instance)) }
          jsonPath("$.size") { value(1) }
          jsonPath("$.content.title") { value(ListNotFoundException.TITLE) }
          jsonPath("$.content.status") { HttpStatus.NOT_FOUND.value() }
          jsonPath("$.content.detail") { value("List not found for id $id") }
        }
        verify(exactly = 1) { lrmListService.findByIdOrListNotFoundExceptionListAndItems(id) }
      }
    }

    describe("/lists/1?withItems=false http get") {
      it("list is found") {
        every { lrmListService.findByIdOrListNotFoundException(id) } returns lrmListMockResponse
        val instance = "/lists/$id?withItems=false"
        mockMvc.get(instance).andExpect {
          status { isOk() }
          content { contentType(MediaType.APPLICATION_JSON) }
          jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
          jsonPath("$.method") { value(HttpMethod.GET.name().lowercase()) }
          jsonPath("$.message") { value("retrieved list $id") }
          jsonPath("$.instance") { value(instance.substringBeforeLast("?").removeSuffix(instance)) }
          jsonPath("$.size") { value(1) }
          jsonPath("$.content.description") { value(lrmListDescription) }
          jsonPath("$.content.name") { value(lrmListName) }
          jsonPath("$.content.items") {
            doesNotExist()
          }
        }
        verify(exactly = 1) { lrmListService.findByIdOrListNotFoundException(id) }
      }

      it("list is not found") {
        every { lrmListService.findByIdOrListNotFoundException(id) } throws ListNotFoundException(id)
        val instance = "/lists/$id?withItems=false"
        mockMvc.get(instance).andExpect {
          status { isNotFound() }
          content { contentType(MediaType.APPLICATION_JSON) }
          jsonPath("$.disposition") { value(DispositionOfProblem.FAILURE.nameAsLowercase()) }
          jsonPath("$.method") { value(HttpMethod.GET.name().lowercase()) }
          jsonPath("$.message") { value("from response problem constructor") }
          jsonPath("$.instance") { value(instance.substringBeforeLast("?").removeSuffix(instance)) }
          jsonPath("$.size") { value(1) }
          jsonPath("$.content.title") { value(ListNotFoundException.TITLE) }
          jsonPath("$.content.status") { HttpStatus.NOT_FOUND.value() }
          jsonPath("$.content.detail") { value("List not found for id $id") }
        }
        verify(exactly = 1) { lrmListService.findByIdOrListNotFoundException(id) }
      }
    }

    describe("/lists/1 http patch") {
      it("list is found and updated") {
        every { lrmListService.patch(id, any()) } returns Pair(lrmListMockResponse, true)
        val instance = "/lists/$id"
        mockMvc.patch(instance) {
          content = Json.encodeToString(mapOf("name" to lrmListName))
          contentType = MediaType.APPLICATION_JSON
        }.andExpect {
          status { isOk() }
          content { contentType(MediaType.APPLICATION_JSON) }
          jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
          jsonPath("$.method") { value(HttpMethod.PATCH.name().lowercase()) }
          jsonPath("$.message") { value("patched") }
          jsonPath("$.instance") { value(instance) }
          jsonPath("$.size") { value(1) }
          jsonPath("$.content.description") { value(lrmListDescription) }
          jsonPath("$.content.name") { value(lrmListName) }
          jsonPath("$.content.items") {
            doesNotExist()
          }
        }
        verify(exactly = 1) { lrmListService.patch(id, mapOf("name" to lrmListName)) }
      }

      it("list is found and not updated") {
        every { lrmListService.patch(id, any()) } returns Pair(lrmListMockResponse, false)
        val instance = "/lists/$id"
        mockMvc.patch(instance) {
          content = Json.encodeToString(mapOf("name" to lrmListDescription))
          contentType = MediaType.APPLICATION_JSON
        }.andExpect {
          status { isNoContent() }
          content { contentType(MediaType.APPLICATION_JSON) }
          jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
          jsonPath("$.method") { value(HttpMethod.PATCH.name().lowercase()) }
          jsonPath("$.message") { value("not patched") }
          jsonPath("$.instance") { value(instance) }
          jsonPath("$.size") { value(1) }
          jsonPath("$.content.description") { value(lrmListDescription) }
          jsonPath("$.content.name") { value(lrmListName) }
          jsonPath("$.content.items") {
            doesNotExist()
          }
        }
        verify(exactly = 1) { lrmListService.patch(id, mapOf("name" to lrmListDescription)) }
      }

      it("list is not found") {
        every { lrmListService.patch(id, any()) } throws ListNotFoundException(id)
        val instance = "/lists/$id"
        mockMvc.patch(instance) {
          content = Json.encodeToString(mapOf("name" to lrmListDescription))
          contentType = MediaType.APPLICATION_JSON
        }.andExpect {
          status { isNotFound() }
          content { contentType(MediaType.APPLICATION_JSON) }
          jsonPath("$.disposition") { value(DispositionOfProblem.FAILURE.nameAsLowercase()) }
          jsonPath("$.method") { value(HttpMethod.PATCH.name().lowercase()) }
          jsonPath("$.message") { value("from response problem constructor") }
          jsonPath("$.instance") { value(instance) }
          jsonPath("$.size") { value(1) }
          jsonPath("$.content.title") { value(ListNotFoundException.TITLE) }
          jsonPath("$.content.status") { HttpStatus.NOT_FOUND.value() }
          jsonPath("$.content.detail") { value("List not found for id $id") }
        }
        verify(exactly = 1) { lrmListService.patch(id, mapOf("name" to lrmListDescription)) }
      }
    }
  }
}
