package net.flyingfishflash.loremlist.integration.domain.listitem

import io.kotest.datatest.withData
import net.flyingfishflash.loremlist.api.data.request.LrmItemCreateRequest
import net.flyingfishflash.loremlist.core.response.advice.CoreExceptionHandler.Companion.VALIDATION_FAILURE_MESSAGE
import net.flyingfishflash.loremlist.core.response.structure.DispositionOfProblem
import net.flyingfishflash.loremlist.core.response.structure.DispositionOfSuccess
import net.flyingfishflash.loremlist.domain.lrmlist.ListNotFoundException
import net.flyingfishflash.loremlist.domain.lrmlistitem.ListItemNotFoundException
import net.flyingfishflash.loremlist.integration.domain.DomainFunctionTest
import net.flyingfishflash.loremlist.integration.domain.DomainFunctionTest.TestData.invalidUuids
import net.flyingfishflash.loremlist.integration.domain.DomainFunctionTest.TestData.itemCreateRequestsAlpha
import net.flyingfishflash.loremlist.integration.domain.DomainFunctionTest.TestData.listCreateRequests
import org.springframework.http.HttpMethod
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Transactional
class ListItemReadTest(mockMvc: MockMvc) :
  DomainFunctionTest(mockMvc = mockMvc, body = {
    data class ListItemRequestRecord(val listId: UUID, val itemId: UUID?, val listItemCreateRequest: LrmItemCreateRequest)
    var requests: List<ListItemRequestRecord> = emptyList()

    beforeSpec {
      purgeDomain()
      val listId = createAndVerifyList(listRequest = listCreateRequests[0])
      requests = itemCreateRequestsAlpha.map {
        ListItemRequestRecord(listId = listId, itemId = createAndVerifyListItem(listId, it), listItemCreateRequest = it)
      }
      verifyContentValue(url = "/lists/count", expectedValue = 1)
      verifyContentValue(url = "/lists/$listId/items/count", expectedValue = requests.size)
    }

    describe("list item retrieval") {
      context("retrieving all list items: /lists/{list-id}") {
        it("fails when invalid uuid is provided for list id") {
          performRequestAndVerifyResponse(
            method = HttpMethod.GET,
            instance = "/lists/${invalidUuids[0]}",
            expectedDisposition = DispositionOfProblem.FAILURE,
            statusMatcher = status().isBadRequest(),
            additionalMatchers = arrayOf(
              jsonPath("$.message").value("$VALIDATION_FAILURE_MESSAGE listId."),
              jsonPath("$.content.validationErrors.length()").value(1),
            ),
          )
        }

        it("fails when list is not found") {
          val randomId = UUID.randomUUID()
          performRequestAndVerifyResponse(
            method = HttpMethod.GET,
            instance = "/lists/$randomId",
            expectedDisposition = DispositionOfProblem.FAILURE,
            statusMatcher = status().isNotFound(),
            additionalMatchers = arrayOf(
              jsonPath("$.content.title").value(ListNotFoundException::class.java.simpleName),
              jsonPath("$.content.status").value("404"),
              jsonPath("$.content.supplemental.notFound").value(randomId.toString()),
            ),
          )
        }

        it("succeeds when items are included in single list request") {
          performRequestAndVerifyResponse(
            instance = "/lists/${requests[0].listId}?includeItems=true",
            method = HttpMethod.GET,
            expectedDisposition = DispositionOfSuccess.SUCCESS,
            statusMatcher = status().isOk,
            additionalMatchers = arrayOf(
              jsonPath("$.content.items.length()").value(requests.size),
            ),
          )
        }
      }

      context("retrieving a list item: /lists/{list-id}/items/{item-id}") {

        it("fails when invalid uuid is provided for list id and item id") {
          performRequestAndVerifyResponse(
            method = HttpMethod.GET,
            instance = "/lists/${invalidUuids[0]}/items/${invalidUuids[0]}",
            expectedDisposition = DispositionOfProblem.FAILURE,
            statusMatcher = status().isBadRequest(),
            additionalMatchers = arrayOf(
              jsonPath("$.message").value("$VALIDATION_FAILURE_MESSAGE itemId, listId."),
              jsonPath("$.content.validationErrors.length()").value(1),
            ),
          )
        }

        it("fails when list is not found") {
          val randomId = UUID.randomUUID()
          performRequestAndVerifyResponse(
            method = HttpMethod.GET,
            instance = "/lists/$randomId/items/${requests[0].itemId}",
            expectedDisposition = DispositionOfProblem.FAILURE,
            statusMatcher = status().isNotFound(),
            additionalMatchers = arrayOf(
              jsonPath("$.content.title").value(ListNotFoundException::class.java.simpleName),
              jsonPath("$.content.status").value("404"),
              jsonPath("$.content.supplemental.notFound").value(randomId.toString()),
            ),
          )
        }

        it("fails when list item is not found") {
          val randomId = UUID.randomUUID()
          performRequestAndVerifyResponse(
            method = HttpMethod.GET,
            instance = "/lists/${requests[0].listId}/items/$randomId",
            expectedDisposition = DispositionOfProblem.FAILURE,
            statusMatcher = status().isNotFound(),
            additionalMatchers = arrayOf(
              jsonPath("$.content.title").value(ListItemNotFoundException::class.java.simpleName),
              jsonPath("$.content.status").value("404"),
              jsonPath("$.content.supplemental.notFound").value(randomId.toString()),
            ),
          )
        }

        withData(
          nameFn = { "succeeds for '${it.listItemCreateRequest.name}'" },
          requests,
        ) {
          performRequestAndVerifyResponse(
            instance = "/lists/${it.listId}/items/${it.itemId}",
            method = HttpMethod.GET,
            expectedDisposition = DispositionOfSuccess.SUCCESS,
            statusMatcher = status().isOk,
            additionalMatchers = arrayOf(
              jsonPath("$.content.lists.length()").value(1),
            ),
          )
        }
      }
    }
  })
