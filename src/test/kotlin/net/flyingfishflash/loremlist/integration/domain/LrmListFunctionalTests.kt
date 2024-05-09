package net.flyingfishflash.loremlist.integration.domain

import io.kotest.core.spec.style.DescribeSpec
import net.flyingfishflash.loremlist.core.response.structure.DispositionOfSuccess
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

/**
 * LrmList Integration/Functional Tests
 */
@SpringBootTest
@AutoConfigureMockMvc
class LrmListFunctionalTests(mockMvc: MockMvc) : DescribeSpec({

  describe("/lists http get") {
    it("lists are found") {
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
        jsonPath("$.size") { value(0) }
        jsonPath("$.content") { exists() }
        jsonPath("$.content") { isArray() }
//          jsonPath("$.content.[0].name") { value(lrmList().name) }
//          jsonPath("$.content.[0].description") { value(lrmList().description) }
//          jsonPath("$.content.[0].items") {
//            doesNotExist()
//          }
      }
    }
  }
})
