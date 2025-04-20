package net.flyingfishflash.loremlist.integration.domain.item

import io.kotest.datatest.withData
import net.flyingfishflash.loremlist.api.data.request.LrmItemCreateRequest
import net.flyingfishflash.loremlist.core.response.advice.CoreExceptionHandler.Companion.VALIDATION_FAILURE_MESSAGE
import net.flyingfishflash.loremlist.core.response.structure.DispositionOfProblem
import net.flyingfishflash.loremlist.core.response.structure.DispositionOfSuccess
import net.flyingfishflash.loremlist.domain.exceptions.DomainException
import net.flyingfishflash.loremlist.integration.domain.DomainFunctionTest
import net.flyingfishflash.loremlist.integration.domain.DomainFunctionTest.TestData.invalidUuids
import net.flyingfishflash.loremlist.integration.domain.DomainFunctionTest.TestData.itemCreateRequestsAlpha
import net.flyingfishflash.loremlist.integration.domain.DomainFunctionTest.TestData.itemCreateRequestsBeta
import net.flyingfishflash.loremlist.integration.domain.DomainFunctionTest.TestData.listCreateRequests
import org.springframework.http.HttpMethod
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.*

class ItemDeleteTest(mockMvc: MockMvc) :
  DomainFunctionTest(mockMvc = mockMvc, body = {
    data class ListItemRequestRecord(val listId: UUID, val itemId: UUID?, val listItemCreateRequest: LrmItemCreateRequest)
    var requestsAlpha: List<ListItemRequestRecord> = emptyList()
    var requestsBeta: List<ListItemRequestRecord> = emptyList()

    beforeSpec {
      purgeDomain()
      val listWithItems = createAndVerifyList(listRequest = listCreateRequests[0])
      val listWithNoItems = createAndVerifyList(listRequest = listCreateRequests[1])

      requestsAlpha = itemCreateRequestsAlpha.map {
        ListItemRequestRecord(listId = listWithItems, itemId = createAndVerifyListItem(listWithItems, it), listItemCreateRequest = it)
      }

      requestsBeta = itemCreateRequestsBeta.map {
        ListItemRequestRecord(listId = listWithNoItems, itemId = createAndVerifyListItem(listWithNoItems, it), listItemCreateRequest = it)
      }

      performRequestAndVerifyResponse(
        method = HttpMethod.DELETE,
        instance = "/lists/$listWithNoItems/items",
        expectedDisposition = DispositionOfSuccess.SUCCESS,
      )

      verifyContentValue(url = "/lists/count", expectedValue = 2)
      verifyContentValue(url = "/lists/$listWithItems/items/count", expectedValue = requestsAlpha.size)
      verifyContentValue(url = "/lists/$listWithNoItems/items/count", expectedValue = 0)
      verifyContentSize(url = "/items/with-no-lists", expectedSize = requestsBeta.size)
    }

    describe("item deletion ") {
      context("deleting an item: /items/{item-id}") {
        it("fails when invalid uuid is provided for item id") {
          performRequestAndVerifyResponse(
            method = HttpMethod.DELETE,
            instance = "/items/${invalidUuids[0]}",
            expectedDisposition = DispositionOfProblem.FAILURE,
            statusMatcher = status().isBadRequest(),
            additionalMatchers = arrayOf(
              jsonPath("$.message").value("$VALIDATION_FAILURE_MESSAGE itemId."),
              jsonPath("$.content.validationErrors.length()").value(1),
            ),
          )
        }

        it("fails when item to delete is not found") {
          val nonExistentId = UUID.randomUUID()
          performRequestAndVerifyResponse(
            method = HttpMethod.DELETE,
            instance = "/items/$nonExistentId",
            expectedDisposition = DispositionOfProblem.FAILURE,
            statusMatcher = status().isNotFound(),
            additionalMatchers = arrayOf(
              jsonPath("$.content.title").value(DomainException::class.java.simpleName),
              jsonPath("$.content.status").value("404"),
            ),
          )
        }

        withData(
          nameFn = { "fails with http 422 for '${it.listItemCreateRequest.name}'" },
          ts = requestsAlpha,
        ) {
          performRequestAndVerifyResponse(
            instance = "/items/${it.itemId}",
            method = HttpMethod.DELETE,
            expectedDisposition = DispositionOfProblem.FAILURE,
            statusMatcher = status().isUnprocessableEntity,
          )
        }

        withData(
          nameFn = { "succeeds for '${it.listItemCreateRequest.name}' [no list]" },
          ts = requestsBeta,
        ) {
          performRequestAndVerifyResponse(
            instance = "/items/${it.itemId}",
            method = HttpMethod.DELETE,
            expectedDisposition = DispositionOfSuccess.SUCCESS,
            statusMatcher = status().isOk,
          )
        }
      }

      context("deleting all items: /items") {
        it("succeeds") {
          performRequestAndVerifyResponse(
            method = HttpMethod.DELETE,
            instance = "/items",
            expectedDisposition = DispositionOfSuccess.SUCCESS,
          )

          verifyContentValue(url = "/items/count", expectedValue = 0)
        }
      }
    }
  })
