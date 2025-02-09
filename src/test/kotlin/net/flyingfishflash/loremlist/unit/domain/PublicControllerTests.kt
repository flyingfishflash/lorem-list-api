package net.flyingfishflash.loremlist.unit.domain

import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.extensions.spring.SpringExtension
import io.mockk.every
import io.mockk.verify
import kotlinx.datetime.Clock.System.now
import net.flyingfishflash.loremlist.core.configuration.SerializationConfig
import net.flyingfishflash.loremlist.core.configuration.WebSecurityConfiguration
import net.flyingfishflash.loremlist.core.response.structure.DispositionOfSuccess
import net.flyingfishflash.loremlist.domain.lrmlist.LrmList
import net.flyingfishflash.loremlist.domain.lrmlist.LrmListPublicController
import net.flyingfishflash.loremlist.domain.lrmlist.LrmListService
import net.flyingfishflash.loremlist.domain.lrmlist.data.LrmListCreateRequest
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
  lateinit var mockLrmListService: LrmListService

  init {

    val id0 = UUID.fromString("00000000-0000-4000-a000-000000000000")
    val lrmListCreateRequest = LrmListCreateRequest(name = "Lorem List Name", description = "Lorem List Description", public = true)
    val now = now()

    fun lrmList(): LrmList = LrmList(
      id = id0,
      name = lrmListCreateRequest.name,
      description = lrmListCreateRequest.description,
      public = lrmListCreateRequest.public,
      created = now,
      createdBy = "Lorum Ipsum Created By",
      updated = now,
      updatedBy = "Lorem Ipsum Updated By",
      items = null,
    )

    fun lrmListWithEmptyItems(): LrmList = lrmList().copy(items = setOf())

    describe("/public/lists") {
      describe("get") {
        it("lists are found") {
          val mockReturn = listOf(lrmList())
          every { mockLrmListService.findByPublic() } returns mockReturn
          val instance = "/public/lists"
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
          val instance = "/public/lists?includeItems=false"
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
          val instance = "/public/lists?includeItems=true"
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
  }
}
