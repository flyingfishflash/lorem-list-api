package net.flyingfishflash.loremlist.integration.domain.list

import io.kotest.datatest.withData
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.flyingfishflash.loremlist.api.data.request.LrmListCreateRequest
import net.flyingfishflash.loremlist.core.response.advice.CoreExceptionHandler.Companion.VALIDATION_FAILURE_MESSAGE
import net.flyingfishflash.loremlist.core.response.structure.DispositionOfProblem
import net.flyingfishflash.loremlist.core.response.structure.DispositionOfSuccess
import net.flyingfishflash.loremlist.domain.lrmlist.ListNotFoundException
import net.flyingfishflash.loremlist.integration.domain.DomainFunctionTest
import net.flyingfishflash.loremlist.integration.domain.DomainFunctionTest.TestData.invalidUuids
import net.flyingfishflash.loremlist.integration.domain.DomainFunctionTest.TestData.listCreateUpdateRequestPairs
import org.springframework.http.HttpMethod
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import java.util.*

/**
 * Integration tests for list update operations
 */
@Transactional
class ListUpdateTest(mockMvc: MockMvc) :
  DomainFunctionTest(mockMvc = mockMvc, body = {
    data class ListRequestRecord(val uuid: UUID, val createRequest: LrmListCreateRequest, val updateRequest: LrmListCreateRequest)
    var requestRecords = listOf<ListRequestRecord>()

    beforeSpec {
      purgeDomain()

      requestRecords = listCreateUpdateRequestPairs.associateBy { pair -> createAndVerifyList(pair.createRequest) }.map { (uuid, pair) ->
        ListRequestRecord(
          uuid = uuid,
          createRequest = pair.createRequest,
          updateRequest = pair.updateRequest,
        )
      }
    }

    describe("list modification") {
      context("modifying a list: /lists/{list-id}") {
        it("fails when invalid uuid is provided for list id") {
          performRequestAndVerifyResponse(
            method = HttpMethod.PATCH,
            instance = "/lists/${invalidUuids[0]}",
            requestBody = Json.encodeToString(listCreateUpdateRequestPairs.first().updateRequest),
            expectedDisposition = DispositionOfProblem.FAILURE,
            statusMatcher = status().isBadRequest(),
            additionalMatchers = arrayOf(
              jsonPath("$.message").value("$VALIDATION_FAILURE_MESSAGE listId."),
              jsonPath("$.content.validationErrors.length()").value(1),
            ),
          )
        }

        it("fails when list to update is not found") {
          val nonExistentUuid = UUID.randomUUID()
          val sampleUpdate = listCreateUpdateRequestPairs.first().updateRequest

          performRequestAndVerifyResponse(
            method = HttpMethod.PATCH,
            instance = "/lists/$nonExistentUuid",
            requestBody = Json.encodeToString(sampleUpdate),
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
          TestData.listUpdateValidationScenarios,
        ) { scenario ->
          performRequestAndVerifyResponse(
            method = HttpMethod.PATCH,
            instance = "/lists/${requestRecords[0].uuid}",
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
          nameFn = { record -> "succeeds for all fields of '${record.createRequest.name}'" },
          requestRecords,
        ) { record ->
          performRequestAndVerifyResponse(
            method = HttpMethod.PATCH,
            instance = "/lists/${record.uuid}",
            requestBody = Json.encodeToString(record.updateRequest),
            expectedDisposition = DispositionOfSuccess.SUCCESS,
            additionalMatchers = arrayOf(
              jsonPath("$.content.id").value(record.uuid.toString()),
              jsonPath("$.content.name").value(record.updateRequest.name),
              jsonPath("$.content.description").value(record.updateRequest.description),
              jsonPath("$.content.public").value(record.updateRequest.public),
              jsonPath("$.content.created").isNotEmpty(),
              jsonPath("$.content.updated").isNotEmpty(),
              jsonPath("$.content.items").isEmpty(),
            ),
          )

          performRequestAndVerifyResponse(
            method = HttpMethod.GET,
            instance = "/lists/${record.uuid}",
            expectedDisposition = DispositionOfSuccess.SUCCESS,
            additionalMatchers = arrayOf(
              jsonPath("$.content.name").value(record.updateRequest.name),
              jsonPath("$.content.description").value(record.updateRequest.description),
              jsonPath("$.content.public").value(record.updateRequest.public),
            ),
          )
        }
      }
    }
  })
