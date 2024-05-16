package net.flyingfishflash.loremlist.unit.domain.lrmlist

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
import net.flyingfishflash.loremlist.core.response.structure.DispositionOfProblem
import net.flyingfishflash.loremlist.core.response.structure.DispositionOfSuccess
import net.flyingfishflash.loremlist.domain.lrmlist.ListNotFoundException
import net.flyingfishflash.loremlist.domain.lrmlist.LrmList
import net.flyingfishflash.loremlist.domain.lrmlist.LrmListController
import net.flyingfishflash.loremlist.domain.lrmlist.LrmListService
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
import java.util.UUID

/**
 * LrmListController Unit Tests
 */
@WebMvcTest(controllers = [LrmListController::class])
class LrmListControllerTests(mockMvc: MockMvc) : DescribeSpec() {
  override fun extensions() = listOf(SpringExtension)

  @MockkBean
  lateinit var lrmListService: LrmListService

  init {

    val id: Long = 1
    val listUuid = UUID.randomUUID()
    val lrmListRequest = LrmListRequest("Lorem List Name", "Lorem List Description")
    fun lrmList(): LrmList = LrmList(id = 0, uuid = listUuid, name = lrmListRequest.name, description = lrmListRequest.description)
    fun lrmListWithEmptyItems(): LrmList = lrmList().copy(items = setOf())

    afterEach { clearAllMocks() }
    afterSpec { unmockkAll() }

    describe("/lists http get") {
      it("lists are found") {
        val mockReturn = listOf(lrmList())
        every { lrmListService.findAll() } returns mockReturn
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
        verify(exactly = 1) { lrmListService.findAll() }
      }
    }

    describe("/lists?includeItems=false http get") {
      it("lists are found") {
        val mockReturn = listOf(lrmList())
        every { lrmListService.findAll() } returns mockReturn
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
        verify(exactly = 1) { lrmListService.findAll() }
      }
    }

    describe("/lists?includeItems=true http get") {
      it("lists and items are found") {
        val mockReturn = listOf(lrmListWithEmptyItems())
        every { lrmListService.findAllIncludeItems() } returns mockReturn
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
        verify(exactly = 1) { lrmListService.findAllIncludeItems() }
      }
    }

    describe("/lists http post") {
      it("list is created") {
        println(Json.encodeToString(lrmListRequest))
        every { lrmListService.create(lrmListRequest) } returns lrmList()
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
        verify(exactly = 1) { lrmListService.create(lrmListRequest) }
      }

      it("requested list name is an empty string") {
        val instance = "/lists"
        mockMvc.post(instance) {
          content = Json.encodeToString(LrmListRequest("", lrmList().description))
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
        verify(exactly = 0) { lrmListService.create(ofType(LrmListRequest::class)) }
      }

      it("requested list description is an empty string") {
        val instance = "/lists"
        mockMvc.post(instance) {
          content = Json.encodeToString(LrmListRequest(lrmList().name, ""))
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
        verify(exactly = 0) { lrmListService.create(ofType(LrmListRequest::class)) }
      }
    }

    describe("/lists/1 http delete") {
      it("list is deleted") {
        every { lrmListService.deleteSingleById(id) } just Runs
        val instance = "/lists/$id"
        mockMvc.delete(instance).andExpect {
          status { isOk() }
          content { contentType(MediaType.APPLICATION_JSON) }
          jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
          jsonPath("$.method") { value(HttpMethod.DELETE.name().lowercase()) }
          jsonPath("$.message") { value("deleted list id $id") }
          jsonPath("$.instance") { value(instance) }
          jsonPath("$.size") { value(1) }
          jsonPath("$.content.message") { value("content") }
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
          jsonPath("$.message") { value("List id $id could not be found.") }
          jsonPath("$.instance") { value(instance) }
          jsonPath("$.size") { value(1) }
          jsonPath("$.content.title") { value(ListNotFoundException.TITLE) }
          jsonPath("$.content.status") { HttpStatus.NOT_FOUND.value() }
          jsonPath("$.content.detail") { value("List id $id could not be found.") }
        }
        verify(exactly = 1) { lrmListService.deleteSingleById(id) }
      }
    }

    describe("/lists/1 http get") {
      it("list is found") {
        every { lrmListService.findById(id) } returns lrmList()
        val instance = "/lists/$id"
        mockMvc.get(instance).andExpect {
          status { isOk() }
          content { contentType(MediaType.APPLICATION_JSON) }
          jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
          jsonPath("$.method") { value(HttpMethod.GET.name().lowercase()) }
          jsonPath("$.message") { value("retrieved list id $id") }
          jsonPath("$.instance") { value(instance) }
          jsonPath("$.size") { value(1) }
          jsonPath("$.content.description") { value(lrmList().description) }
          jsonPath("$.content.name") { value(lrmList().name) }
        }
        verify(exactly = 1) { lrmListService.findById(id) }
      }

      it("list is not found") {
        every { lrmListService.findById(id) } throws ListNotFoundException(id)
        val instance = "/lists/$id"
        mockMvc.get(instance).andExpect {
          status { isNotFound() }
          content { contentType(MediaType.APPLICATION_JSON) }
          jsonPath("$.disposition") { value(DispositionOfProblem.FAILURE.nameAsLowercase()) }
          jsonPath("$.method") { value(HttpMethod.GET.name().lowercase()) }
          jsonPath("$.message") { value("List id $id could not be found.") }
          jsonPath("$.instance") { value(instance) }
          jsonPath("$.size") { value(1) }
          jsonPath("$.content.title") { value(ListNotFoundException.TITLE) }
          jsonPath("$.content.status") { HttpStatus.NOT_FOUND.value() }
          jsonPath("$.content.detail") { value("List id $id could not be found.") }
        }
        verify(exactly = 1) { lrmListService.findById(id) }
      }
    }

    describe("/lists/1?includeItems=true http get") {
      it("list is found") {
        every { lrmListService.findByIdIncludeItems(id) } returns lrmListWithEmptyItems()
        val instance = "/lists/$id?includeItems=true"
        mockMvc.get("/lists/$id?includeItems=true").andExpect {
          status { isOk() }
          content { contentType(MediaType.APPLICATION_JSON) }
          jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
          jsonPath("$.method") { value(HttpMethod.GET.name().lowercase()) }
          jsonPath("$.message") { value("retrieved list id $id") }
          jsonPath("$.instance") { value(instance.substringBeforeLast("?").removeSuffix(instance)) }
          jsonPath("$.size") { value(1) }
          jsonPath("$.content.description") { value(lrmList().description) }
          jsonPath("$.content.name") { value(lrmList().name) }
          jsonPath("$.content.items") {
            isArray()
            isEmpty()
          }
        }
        verify(exactly = 1) { lrmListService.findByIdIncludeItems(id) }
      }

      it("list is not found") {
        every { lrmListService.findByIdIncludeItems(id) } throws ListNotFoundException(id)
        val instance = "/lists/$id?includeItems=true"
        mockMvc.get(instance).andExpect {
          status { isNotFound() }
          content { contentType(MediaType.APPLICATION_JSON) }
          jsonPath("$.disposition") { value(DispositionOfProblem.FAILURE.nameAsLowercase()) }
          jsonPath("$.method") { value(HttpMethod.GET.name().lowercase()) }
          jsonPath("$.message") { value("List id $id could not be found.") }
          jsonPath("$.instance") { value(instance.substringBeforeLast("?").removeSuffix(instance)) }
          jsonPath("$.size") { value(1) }
          jsonPath("$.content.title") { value(ListNotFoundException.TITLE) }
          jsonPath("$.content.status") { HttpStatus.NOT_FOUND.value() }
          jsonPath("$.content.detail") { value("List id $id could not be found.") }
        }
        verify(exactly = 1) { lrmListService.findByIdIncludeItems(id) }
      }
    }

    describe("/lists/1?includeItems=false http get") {
      it("list is found") {
        every { lrmListService.findById(id) } returns lrmList()
        val instance = "/lists/$id?includeItems=false"
        mockMvc.get(instance).andExpect {
          status { isOk() }
          content { contentType(MediaType.APPLICATION_JSON) }
          jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
          jsonPath("$.method") { value(HttpMethod.GET.name().lowercase()) }
          jsonPath("$.message") { value("retrieved list id $id") }
          jsonPath("$.instance") { value(instance.substringBeforeLast("?").removeSuffix(instance)) }
          jsonPath("$.size") { value(1) }
          jsonPath("$.content.description") { value(lrmList().description) }
          jsonPath("$.content.name") { value(lrmList().name) }
          jsonPath("$.content.items") {
            doesNotExist()
          }
        }
        verify(exactly = 1) { lrmListService.findById(id) }
      }

      it("list is not found") {
        every { lrmListService.findById(id) } throws ListNotFoundException(id)
        val instance = "/lists/$id?includeItems=false"
        mockMvc.get(instance).andExpect {
          status { isNotFound() }
          content { contentType(MediaType.APPLICATION_JSON) }
          jsonPath("$.disposition") { value(DispositionOfProblem.FAILURE.nameAsLowercase()) }
          jsonPath("$.method") { value(HttpMethod.GET.name().lowercase()) }
          jsonPath("$.message") { value("List id $id could not be found.") }
          jsonPath("$.instance") { value(instance.substringBeforeLast("?").removeSuffix(instance)) }
          jsonPath("$.size") { value(1) }
          jsonPath("$.content.title") { value(ListNotFoundException.TITLE) }
          jsonPath("$.content.status") { HttpStatus.NOT_FOUND.value() }
          jsonPath("$.content.detail") { value("List id $id could not be found.") }
        }
        verify(exactly = 1) { lrmListService.findById(id) }
      }
    }

    describe("/lists/1 http patch") {
      it("list is found and updated") {
        every { lrmListService.patch(id, any()) } returns Pair(lrmList(), true)
        val instance = "/lists/$id"
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
        verify(exactly = 1) { lrmListService.patch(id, mapOf("name" to lrmList().name)) }
      }

      it("list is found and not updated") {
        every { lrmListService.patch(id, any()) } returns Pair(lrmList(), false)
        val instance = "/lists/$id"
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
        verify(exactly = 1) { lrmListService.patch(id, mapOf("name" to lrmList().name)) }
      }

      it("list is not found") {
        every { lrmListService.patch(id, any()) } throws ListNotFoundException(id)
        val instance = "/lists/$id"
        mockMvc.patch(instance) {
          content = Json.encodeToString(mapOf("name" to lrmList().name))
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
        verify(exactly = 1) { lrmListService.patch(id, mapOf("name" to lrmList().name)) }
      }
    }
  }
}
