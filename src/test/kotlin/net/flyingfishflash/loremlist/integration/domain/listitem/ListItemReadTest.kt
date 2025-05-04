package net.flyingfishflash.loremlist.integration.domain.listitem

import io.kotest.datatest.withData
import net.flyingfishflash.loremlist.api.data.request.LrmItemCreateRequest
import net.flyingfishflash.loremlist.api.data.request.LrmListCreateRequest
import net.flyingfishflash.loremlist.core.response.advice.CoreExceptionHandler.Companion.VALIDATION_FAILURE_MESSAGE
import net.flyingfishflash.loremlist.core.response.structure.DispositionOfProblem
import net.flyingfishflash.loremlist.core.response.structure.DispositionOfSuccess
import net.flyingfishflash.loremlist.domain.exceptions.DomainException
import net.flyingfishflash.loremlist.domain.lrmlist.ListNotFoundException
import net.flyingfishflash.loremlist.domain.lrmlistitem.ListItemNotFoundException
import net.flyingfishflash.loremlist.integration.domain.DomainFunctionTest
import net.flyingfishflash.loremlist.integration.domain.DomainFunctionTest.TestData.invalidUuids
import net.flyingfishflash.loremlist.integration.domain.DomainFunctionTest.TestData.itemCreateRequestsAlpha
import net.flyingfishflash.loremlist.integration.domain.DomainFunctionTest.TestData.itemCreateRequestsBeta
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

    data class ListRequestRecord(val listId: UUID, val listCreateRequest: LrmListCreateRequest)
    var requestsBeta = emptyList<ListItemRequestRecord>()
    var listWithItems: ListRequestRecord? = null
    var listWithNoItems: ListRequestRecord? = null

    beforeSpec {
      purgeDomain()
      listWithItems = ListRequestRecord(createAndVerifyList(listCreateRequests[0]), listCreateRequests[0])
      requests = itemCreateRequestsAlpha.map {
        ListItemRequestRecord(listId = listWithItems.listId, itemId = createAndVerifyListItem(listWithItems.listId, it), listItemCreateRequest = it)
      }

      listWithNoItems = ListRequestRecord(createAndVerifyList(listCreateRequests[1]), listCreateRequests[1])
      requestsBeta = itemCreateRequestsBeta.map {
        ListItemRequestRecord(listWithNoItems.listId, createAndVerifyListItem(listWithNoItems.listId, it), it)
      }

      performRequestAndVerifyResponse(
        method = HttpMethod.DELETE,
        instance = "/lists/${listWithNoItems.listId}/items",
        expectedDisposition = DispositionOfSuccess.SUCCESS,
      )

      verifyContentValue(url = "/lists/count", expectedValue = 2)
      verifyContentValue(url = "/lists/${listWithItems.listId}/items/count", expectedValue = requests.size)
      verifyContentValue(url = "/lists/${listWithNoItems.listId}/items/count", expectedValue = 0)
      verifyContentSize(url = "/items/with-no-lists", expectedSize = requestsBeta.size)
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

      context("retrieving items eligible for addition to a list: /lists/{list-id}/items/eligible") {
        withData(
          nameFn = { (listName, listId, expectedSize) -> "succeeds for '$listName' with $expectedSize items" },
          listOf(
            // eligible items will not include requestsAlpha because those items are already in this list
            Triple(listWithItems?.listCreateRequest?.name, listWithItems?.listId, requestsBeta.size),
            // eligible items will include all items since none are in this list
            Triple(listWithNoItems?.listCreateRequest?.name, listWithNoItems?.listId, requests.size + requestsBeta.size),
          ),
        ) { (desc, listId, expectedSize) ->
          verifyContentSize(url = "/lists/$listId/items/eligible", expectedSize = expectedSize)
        }

        it("fails when list is not found") {
          val randomId = UUID.randomUUID()
          performRequestAndVerifyResponse(
            method = HttpMethod.GET,
            instance = "/lists/$randomId/items/eligible",
            expectedDisposition = DispositionOfProblem.FAILURE,
            statusMatcher = status().isNotFound(),
            additionalMatchers = arrayOf(
              jsonPath("$.content.title").value(DomainException::class.java.simpleName),
              jsonPath("$.content.status").value("404"),
              jsonPath("$.content.cause.name").value(ListNotFoundException::class.simpleName),
            ),
          )
        }

        it("fails when invalid uuid is provided for list id") {
          performRequestAndVerifyResponse(
            method = HttpMethod.GET,
            instance = "/lists/${invalidUuids[0]}/items/eligible",
            expectedDisposition = DispositionOfProblem.FAILURE,
            statusMatcher = status().isBadRequest(),
            additionalMatchers = arrayOf(
              jsonPath("$.message").value("$VALIDATION_FAILURE_MESSAGE listId."),
              jsonPath("$.content.validationErrors.length()").value(1),
            ),
          )
        }
      }
    }
  })
