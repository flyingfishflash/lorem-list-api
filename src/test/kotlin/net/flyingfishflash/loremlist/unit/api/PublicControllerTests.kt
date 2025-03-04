package net.flyingfishflash.loremlist.unit.api

import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.extensions.spring.SpringExtension
import io.mockk.every
import io.mockk.verify
import kotlinx.datetime.Clock.System.now
import net.flyingfishflash.loremlist.api.LrmListApiService
import net.flyingfishflash.loremlist.api.LrmListPublicController
import net.flyingfishflash.loremlist.api.data.request.LrmListCreateRequest
import net.flyingfishflash.loremlist.api.data.response.ApiServiceResponse
import net.flyingfishflash.loremlist.api.data.response.LrmListResponse
import net.flyingfishflash.loremlist.core.configuration.SerializationConfig
import net.flyingfishflash.loremlist.core.configuration.WebSecurityConfiguration
import net.flyingfishflash.loremlist.core.response.structure.DispositionOfSuccess
import net.flyingfishflash.loremlist.domain.lrmlist.LrmList
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import java.util.*

/**
 * PublicController Unit Tests
 */
@WebMvcTest(controllers = [LrmListPublicController::class])
@Import(SerializationConfig::class, WebSecurityConfiguration::class)
class PublicControllerTests(mockMvc: MockMvc) : DescribeSpec() {
  override fun extensions() = listOf(SpringExtension)

  @MockkBean
  lateinit var mockLrmListService: LrmListApiService

  init {

    val id0 = UUID.fromString("00000000-0000-4000-a000-000000000000")
    val lrmListCreateRequest = LrmListCreateRequest(name = "Lorem List Name", description = "Lorem List Description", public = true)
    val now = now()
    val irrelevantMessage = "message is irrelevant"

    fun lrmList(): LrmList = LrmList(
      id = id0,
      name = lrmListCreateRequest.name,
      description = lrmListCreateRequest.description,
      public = lrmListCreateRequest.public,
      owner = "Lorem Ipsum Owner",
      created = now,
      creator = "Lorum Ipsum Created By",
      updated = now,
      updater = "Lorem Ipsum Updated By",
    )

    fun lrmListWithEmptyItems(): LrmList = lrmList().copy()

    describe("/public/lists") {
      describe("get") {
        it("lists are found") {
          val mockReturn = listOf(LrmListResponse.fromLrmList(lrmList()))
          every { mockLrmListService.findByPublicExcludeItems() } returns
            ApiServiceResponse(content = mockReturn, message = irrelevantMessage)
          val instance = "/public/lists"
          mockMvc.get(instance) {
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
            jsonPath("$.content.[0].name") { value(lrmList().name) }
            jsonPath("$.content.[0].description") { value(lrmList().description) }
            jsonPath("$.content.[0].items") { isEmpty() }
          }
          verify(exactly = 1) { mockLrmListService.findByPublicExcludeItems() }
        }

        it("lists are found ?includeItems=false") {
          val mockReturn = listOf(LrmListResponse.fromLrmList(lrmList()))
          every { mockLrmListService.findByPublicExcludeItems() } returns
            ApiServiceResponse(content = mockReturn, message = irrelevantMessage)
          val instance = "/public/lists?includeItems=false"
          mockMvc.get(instance) {
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
            jsonPath("$.content.[0].name") { value(lrmList().name) }
            jsonPath("$.content.[0].description") { value(lrmList().description) }
            jsonPath("$.content.[0].items") { isEmpty() }
          }
          verify(exactly = 1) { mockLrmListService.findByPublicExcludeItems() }
        }

        it("lists are found ?includeItems=true") {
          val mockReturn = listOf(LrmListResponse.fromLrmList(lrmListWithEmptyItems()))
          every { mockLrmListService.findByPublic() } returns ApiServiceResponse(content = mockReturn, message = irrelevantMessage)
          val instance = "/public/lists?includeItems=true"
          mockMvc.get(instance) {
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
            jsonPath("$.content.[0].name") { value(lrmList().name) }
            jsonPath("$.content.[0].description") { value(lrmList().description) }
            jsonPath("$.content.[0].items") {
              isArray()
              isEmpty()
            }
          }
          verify(exactly = 1) { mockLrmListService.findByPublic() }
        }
      }
    }
  }
}
