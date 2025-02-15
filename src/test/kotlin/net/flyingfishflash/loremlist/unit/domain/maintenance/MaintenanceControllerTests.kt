package net.flyingfishflash.loremlist.unit.domain.maintenance

import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.extensions.spring.SpringExtension
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.unmockkAll
import net.flyingfishflash.loremlist.api.MaintenanceApiService
import net.flyingfishflash.loremlist.api.MaintenanceController
import net.flyingfishflash.loremlist.api.data.response.ApiServiceResponse
import net.flyingfishflash.loremlist.api.data.response.DomainPurgedResponse
import net.flyingfishflash.loremlist.core.response.structure.DispositionOfSuccess
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete

@WebMvcTest(controllers = [MaintenanceController::class])
class MaintenanceControllerTests(mockMvc: MockMvc) : DescribeSpec() {
  override fun extensions() = listOf(SpringExtension)

  @MockkBean
  lateinit var mockMaintenanceApiService: MaintenanceApiService

  init {
    afterEach { clearAllMocks() }
    afterSpec { unmockkAll() }

    describe("/maintenance/purge") {
      describe("delete") {
        it("domain is purged") {
          val domainPurgedResponse = DomainPurgedResponse(associationDeletedCount = 997, itemDeletedCount = 998, listDeletedCount = 999)
          val apiServiceResponse = ApiServiceResponse(content = domainPurgedResponse, message = "irrelevant")
          every { mockMaintenanceApiService.purge() } returns apiServiceResponse
          val instance = "/maintenance/purge"
          mockMvc.delete(instance) {
            with(jwt())
            with(csrf())
            contentType = MediaType.APPLICATION_JSON
          }.andExpect {
            status { isOk() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
            jsonPath("$.method") { value(HttpMethod.DELETE.name().lowercase()) }
            jsonPath("$.message") { value("irrelevant") }
            jsonPath("$.instance") { value(instance) }
            jsonPath("$.size") { value(1) }
            jsonPath("$.content.associationDeletedCount") { value(apiServiceResponse.content.associationDeletedCount) }
            jsonPath("$.content.itemDeletedCount") { value(apiServiceResponse.content.itemDeletedCount) }
            jsonPath("$.content.listDeletedCount") { value(apiServiceResponse.content.listDeletedCount) }
          }
        }
      }
    }
  }
}
