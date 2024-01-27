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
import net.flyingfishflash.loremlist.domain.lrmlist.data.LrmList
import net.flyingfishflash.loremlist.domain.lrmlist.data.dto.LrmListRequest
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
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
    val lrmListRequest = LrmListRequest(lrmListName, lrmListDescription)
    val id = 1L

    describe("/lists http get") {
      it("lists are found") {
        every { lrmListService.findAll() } returns listOf(lrmListMockResponse)
        mockMvc.get("/lists") {
          contentType = MediaType.APPLICATION_JSON
        }.andExpect {
          status { isOk() }
          content { contentType(MediaType.APPLICATION_JSON) }
          jsonPath("$") { isArray() }
          jsonPath("$.[0].name") { value(lrmListName) }
          jsonPath("$.[0].description") { value(lrmListDescription) }
          jsonPath("$.[0].items") {
            isArray()
            isEmpty()
          }
        }
      }
    }

    describe("/lists?withItems=false http get") {
      it("lists are found") {
        every { lrmListService.findAll() } returns listOf(lrmListMockResponse)
        mockMvc.get("/lists?withItems=false") {
          contentType = MediaType.APPLICATION_JSON
        }.andExpect {
          status { isOk() }
          content { contentType(MediaType.APPLICATION_JSON) }
          jsonPath("$") { isArray() }
          jsonPath("$.[0].name") { value(lrmListName) }
          jsonPath("$.[0].description") { value(lrmListDescription) }
          jsonPath("$.[0].items") {
            isArray()
            isEmpty()
          }
        }
      }
    }

    describe("/lists?withItems=true http get") {
      it("lists and items are found") {
        every { lrmListService.findAllListsAndItems() } returns listOf(lrmListMockResponse)
        mockMvc.get("/lists?withItems=true") {
          contentType = MediaType.APPLICATION_JSON
        }.andExpect {
          status { isOk() }
          content { contentType(MediaType.APPLICATION_JSON) }
          jsonPath("$") { isArray() }
          jsonPath("$.[0].name") { value(lrmListName) }
          jsonPath("$.[0].description") { value(lrmListDescription) }
          jsonPath("$.[0].items") {
            isArray()
            isEmpty() // because our mock is not returning a list with any items
          }
        }
      }
    }

    describe("/lists http post") {
      it("list is created") {
        every { lrmListService.create(lrmListRequest) } returns lrmListMockResponse
        mockMvc.post("/lists") {
          content = Json.encodeToString(lrmListRequest)
          contentType = MediaType.APPLICATION_JSON
        }.andExpect {
          status { isOk() }
          content { contentType(MediaType.APPLICATION_JSON) }
          jsonPath("$.description") { value(lrmListDescription) }
          jsonPath("$.name") { value(lrmListName) }
        }
        verify(exactly = 1) { lrmListService.create(lrmListRequest) }
      }

      it("requested list name is an empty string") {
        mockMvc.post("/lists") {
          content = Json.encodeToString(LrmListRequest("", lrmListDescription))
          contentType = MediaType.APPLICATION_JSON
        }.andExpect {
          status { isBadRequest() }
          content { contentType(MediaType.APPLICATION_PROBLEM_JSON) }
          jsonPath("$.title") { value("Bad Request") }
        }
        verify(exactly = 0) { lrmListService.create(ofType(LrmListRequest::class)) }
      }

      it("requested list description is an empty string") {
        mockMvc.post("/lists") {
          content = Json.encodeToString(LrmListRequest(lrmListName, ""))
          contentType = MediaType.APPLICATION_JSON
        }.andExpect {
          status { isBadRequest() }
          content { contentType(MediaType.APPLICATION_PROBLEM_JSON) }
          jsonPath("$.title") { value("Bad Request") }
        }
        verify(exactly = 0) { lrmListService.create(ofType(LrmListRequest::class)) }
      }
    }

    describe("/lists/$id http delete") {
      it("list is deleted") {
        every { lrmListService.deleteSingleById(id) } just Runs
        mockMvc.delete("/lists/$id").andExpect {
          status { isNoContent() }
          header { doesNotExist("content-type") }
        }
        verify(exactly = 1) { lrmListService.deleteSingleById(id) }
      }

      it("list is not found") {
        every { lrmListService.deleteSingleById(id) } throws ListNotFoundException()
        mockMvc.delete("/lists/$id").andExpect {
          status { isNotFound() }
          content { contentType(MediaType.APPLICATION_PROBLEM_JSON) }
          jsonPath("$.title") { value("Not Found") }
        }
        verify(exactly = 1) { lrmListService.deleteSingleById(id) }
      }
    }

    describe("/lists/$id http get") {
      it("list is found") {
        every { lrmListService.findByIdOrListNotFoundException(id) } returns lrmListMockResponse
        mockMvc.get("/lists/$id").andExpect {
          status { isOk() }
          content { contentType(MediaType.APPLICATION_JSON) }
          jsonPath("$.description") { value(lrmListDescription) }
          jsonPath("$.name") { value(lrmListName) }
        }
        verify(exactly = 1) { lrmListService.findByIdOrListNotFoundException(id) }
      }

      it("list is not found") {
        every { lrmListService.findByIdOrListNotFoundException(id) } throws ListNotFoundException()
        mockMvc.get("/lists/$id").andExpect {
          status { isNotFound() }
          content { contentType(MediaType.APPLICATION_PROBLEM_JSON) }
          jsonPath("$.title") { value("Not Found") }
        }
        verify(exactly = 1) { lrmListService.findByIdOrListNotFoundException(id) }
      }
    }

    describe("/lists/$id http patch") {
      it("list is found and updated") {
        every { lrmListService.patch(id, any()) } returns Pair(lrmListMockResponse, true)
        mockMvc.patch("/lists/$id") {
          content = Json.encodeToString(mapOf("name" to lrmListName))
          contentType = MediaType.APPLICATION_JSON
        }.andExpect {
          status { isOk() }
          content { contentType(MediaType.APPLICATION_JSON) }
          jsonPath("$.description") { value(lrmListDescription) }
          jsonPath("$.name") { value(lrmListName) }
        }
        verify(exactly = 1) { lrmListService.patch(id, mapOf("name" to lrmListName)) }
      }

      it("list is found and not updated") {
        every { lrmListService.patch(id, any()) } returns Pair(lrmListMockResponse, false)
        mockMvc.patch("/lists/$id") {
          content = Json.encodeToString(mapOf("name" to lrmListDescription))
          contentType = MediaType.APPLICATION_JSON
        }.andExpect {
          status { isNoContent() }
          content { contentType(MediaType.APPLICATION_JSON) }
          jsonPath("$.description") { value(lrmListDescription) }
          jsonPath("$.name") { value(lrmListName) }
        }
        verify(exactly = 1) { lrmListService.patch(id, mapOf("name" to lrmListDescription)) }
      }

      it("list is not found") {
        every { lrmListService.patch(id, any()) } throws ListNotFoundException()
        mockMvc.patch("/lists/$id") {
          content = Json.encodeToString(mapOf("name" to lrmListDescription))
          contentType = MediaType.APPLICATION_JSON
        }.andExpect {
          status { isNotFound() }
          content { contentType(MediaType.APPLICATION_PROBLEM_JSON) }
          jsonPath("$.title") { value("Not Found") }
        }
        verify(exactly = 1) { lrmListService.patch(id, mapOf("name" to lrmListDescription)) }
      }
    }
  }
}
