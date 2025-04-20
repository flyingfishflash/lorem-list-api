package net.flyingfishflash.loremlist.integration.domain.listitem

import io.kotest.datatest.withData
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.flyingfishflash.loremlist.api.data.request.LrmItemCreateRequest
import net.flyingfishflash.loremlist.core.response.advice.CoreExceptionHandler.Companion.VALIDATION_FAILURE_MESSAGE
import net.flyingfishflash.loremlist.core.response.structure.DispositionOfProblem
import net.flyingfishflash.loremlist.domain.lrmlist.ListNotFoundException
import net.flyingfishflash.loremlist.integration.domain.DomainFunctionTest
import net.flyingfishflash.loremlist.integration.domain.DomainFunctionTest.TestData.invalidUuids
import net.flyingfishflash.loremlist.integration.domain.DomainFunctionTest.TestData.listCreateItemCreateRequestPairs
import org.springframework.http.HttpMethod
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.*

class ListItemCreateTest(mockMvc: MockMvc) :
  DomainFunctionTest(mockMvc = mockMvc, body = {
    data class ListItemRequestRecord(val listId: UUID, val listItemCreateRequest: LrmItemCreateRequest)
    var requestRecords = listOf<ListItemRequestRecord>()

    beforeSpec {
      purgeDomain()

      // create each list and associate its uuid with a corresponding list item create request
      // when the creation test is executed this results in three lists, each with one list item
      requestRecords = listCreateItemCreateRequestPairs.associateBy { pair -> createAndVerifyList(pair.listCreateRequest) }.map { (uuid, pair) ->
        ListItemRequestRecord(
          listId = uuid,
          listItemCreateRequest = pair.itemCreateRequest,
        )
      }

      verifyContentValue(url = "/lists/count", expectedValue = requestRecords.size)
      verifyContentValue(url = "/items/count", expectedValue = 0)
      requestRecords.forEach { verifyContentValue(url = "/lists/${it.listId}/items/count", expectedValue = 0) }
    }

    describe("list item creation") {
      context("creating a list item: /lists/{list-id}/items") {
        it("fails when invalid uuid is provided for list id") {
          performRequestAndVerifyResponse(
            method = HttpMethod.POST,
            instance = "/lists/${invalidUuids[0]}/items",
            requestBody = Json.encodeToString(listCreateItemCreateRequestPairs.first().itemCreateRequest),
            expectedDisposition = DispositionOfProblem.FAILURE,
            statusMatcher = status().isBadRequest(),
            additionalMatchers = arrayOf(
              jsonPath("$.message").value("$VALIDATION_FAILURE_MESSAGE listId."),
              jsonPath("$.content.validationErrors.length()").value(1),
            ),
          )
        }

        it("fails when list to create new item for is not found") {
          val randomUUID = UUID.randomUUID()
          val requestBody = listCreateItemCreateRequestPairs.first().itemCreateRequest

          performRequestAndVerifyResponse(
            method = HttpMethod.POST,
            instance = "/lists/$randomUUID/items",
            requestBody = Json.encodeToString(requestBody),
            expectedDisposition = DispositionOfProblem.FAILURE,
            statusMatcher = status().isNotFound(),
            additionalMatchers = arrayOf(
              jsonPath("$.content.title").value(ListNotFoundException::class.java.simpleName),
              jsonPath("$.content.status").value("404"),
            ),
          )
        }

        withData(
          nameFn = { it.description },
          TestData.listItemCreateValidationScenarios,
        ) { scenario ->
          performRequestAndVerifyResponse(
            method = HttpMethod.POST,
            instance = "/lists/${requestRecords[0].listId}/items",
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

        withData(
          nameFn = { "succeeds for '${it.listItemCreateRequest.name}" },
          requestRecords,
        ) {
          createAndVerifyListItem(listId = it.listId, itemRequest = it.listItemCreateRequest)
        }

        verifyContentValue(url = "/lists/count", expectedValue = requestRecords.size)
        verifyContentValue(url = "/items/count", expectedValue = requestRecords.size)
        requestRecords.forEach { verifyContentValue(url = "/lists/${it.listId}/items/count", expectedValue = 1) }
      }
    }
  })
