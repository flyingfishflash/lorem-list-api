package net.flyingfishflash.loremlist.integration.domain

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpMethod
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
class ManagementTest(mockMvc: MockMvc) :
  DomainFunctionTest(mockMvc = mockMvc, body = {
    describe("management") {
      it("health") {
        val instance = "/management/health"
        performRequest(HttpMethod.GET, instance).andExpectAll(
          status().isOk(),
          jsonPath("$.length()").value(1),
          jsonPath("$.status").value("UP"),
        )
      }

      it("info") {
        val instance = "/management/info"
        performRequest(HttpMethod.GET, instance).andExpectAll(
          status().isOk(),
          jsonPath("$.build.length()").value(8),
          jsonPath("$.build.name").value("lorem-list api"),
        )
      }
    }
  })
