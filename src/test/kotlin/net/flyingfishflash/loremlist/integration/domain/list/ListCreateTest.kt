package net.flyingfishflash.loremlist.integration.domain.list

import io.kotest.datatest.withData
import net.flyingfishflash.loremlist.core.response.structure.DispositionOfProblem
import net.flyingfishflash.loremlist.integration.domain.DomainFunctionTest
import org.springframework.http.HttpMethod
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional

@Transactional
class ListCreateTest(mockMvc: MockMvc) :
  DomainFunctionTest(mockMvc = mockMvc, body = {

    beforeSpec { purgeDomain() }

    describe("list creation") {
      context("creating a list: /lists") {
        withData(
          nameFn = { it.description },
          TestData.listCreateValidationScenarios,
        ) { scenario ->
          performRequestAndVerifyResponse(
            method = HttpMethod.POST,
            instance = "/lists",
            requestBody = scenario.postContent,
            expectedDisposition = DispositionOfProblem.FAILURE,
            statusMatcher = status().isBadRequest(),
            additionalMatchers = if (scenario.expectedErrorCount > 0) {
              arrayOf(
                jsonPath("$.message").value(scenario.responseMessage),
                jsonPath("$.content.validationErrors.length()").value(scenario.expectedErrorCount),
              )
            } else {
              arrayOf(
                jsonPath("$.message").value(scenario.responseMessage),
              )
            },
          )
        }

        TestData.listCreateRequests.forEach { listRequest ->
          it("succeeds for '${listRequest.name}'") {
            createAndVerifyList(listRequest = listRequest)
          }
        }
      }
    }
  })
