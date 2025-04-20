package net.flyingfishflash.loremlist.integration.domain.item

import io.kotest.datatest.withData
import net.flyingfishflash.loremlist.api.data.request.LrmItemCreateRequest
import net.flyingfishflash.loremlist.core.response.advice.CoreExceptionHandler.Companion.VALIDATION_FAILURE_MESSAGE
import net.flyingfishflash.loremlist.core.response.structure.DispositionOfProblem
import net.flyingfishflash.loremlist.core.response.structure.DispositionOfSuccess
import net.flyingfishflash.loremlist.domain.exceptions.DomainException
import net.flyingfishflash.loremlist.domain.lrmitem.ItemNotFoundException
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
import java.util.UUID

@Transactional
class ItemReadTest(mockMvc: MockMvc) :
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

    describe("item retrieval") {
      context("counting items: /items/count") {
        it("succeeds") {
          verifyContentValue(url = "/items/count", expectedValue = requestsAlpha.size + requestsBeta.size)
        }
      }

      context("retrieving all items: /items") {
        it("succeeds") {
          performRequestAndVerifyResponse(
            instance = "/items",
            method = HttpMethod.GET,
            expectedDisposition = DispositionOfSuccess.SUCCESS,
            expectedSize = requestsAlpha.size + requestsBeta.size,
            statusMatcher = status().isOk,
            additionalMatchers = arrayOf(
              jsonPath("$.content.length()").value(requestsAlpha.size + requestsBeta.size),
            ),
          )
        }
      }

      context("retrieving all items with no list associations: /items/with-no-lists") {
        it("succeeds") {
          verifyContentSize(url = "/items/with-no-lists", expectedSize = requestsBeta.size)
        }
      }

      context("retrieving an item: /items/{item-id}") {
        it("fails when invalid uuid is provided for item id") {
          performRequestAndVerifyResponse(
            method = HttpMethod.GET,
            instance = "/items/${invalidUuids[0]}",
            expectedDisposition = DispositionOfProblem.FAILURE,
            statusMatcher = status().isBadRequest(),
            additionalMatchers = arrayOf(
              jsonPath("$.message").value("$VALIDATION_FAILURE_MESSAGE itemId."),
              jsonPath("$.content.validationErrors.length()").value(1),
            ),
          )
        }

        it("fails when item is not found") {
          val randomId = UUID.randomUUID()
          performRequestAndVerifyResponse(
            method = HttpMethod.GET,
            instance = "/items/$randomId",
            expectedDisposition = DispositionOfProblem.FAILURE,
            statusMatcher = status().isNotFound(),
            additionalMatchers = arrayOf(
              jsonPath("$.content.title").value(ItemNotFoundException::class.java.simpleName),
              jsonPath("$.content.status").value("404"),
              jsonPath("$.content.supplemental.notFound").value(randomId.toString()),
            ),
          )
        }

        withData(
          nameFn = { "succeeds for '${it.listItemCreateRequest.name}'" },
          ts = requestsAlpha,
        ) {
          performRequestAndVerifyResponse(
            instance = "/items/${it.itemId}",
            method = HttpMethod.GET,
            expectedDisposition = DispositionOfSuccess.SUCCESS,
            statusMatcher = status().isOk,
            additionalMatchers = arrayOf(
              jsonPath("$.content.lists.length()").value(1),
            ),
          )
        }

        withData(
          nameFn = { "succeeds for '${it.listItemCreateRequest.name}' [no lists]" },
          ts = requestsBeta,
        ) {
          performRequestAndVerifyResponse(
            instance = "/items/${it.itemId}",
            method = HttpMethod.GET,
            expectedDisposition = DispositionOfSuccess.SUCCESS,
            statusMatcher = status().isOk,
            additionalMatchers = arrayOf(
              jsonPath("$.content.lists.length()").value(0),
            ),
          )
        }
      }

      context("retrieving an items list association count: /items/{item-id}/lists/count") {
        it("fails when invalid uuid is provided for item id") {
          performRequestAndVerifyResponse(
            method = HttpMethod.GET,
            instance = "/items/${invalidUuids[0]}/lists/count",
            expectedDisposition = DispositionOfProblem.FAILURE,
            statusMatcher = status().isBadRequest(),
            additionalMatchers = arrayOf(
              jsonPath("$.message").value("$VALIDATION_FAILURE_MESSAGE itemId."),
              jsonPath("$.content.validationErrors.length()").value(1),
            ),
          )
        }

        it("fails when item is not found") {
          val randomId = UUID.randomUUID()
          performRequestAndVerifyResponse(
            method = HttpMethod.GET,
            instance = "/items/$randomId/lists/count",
            expectedDisposition = DispositionOfProblem.FAILURE,
            statusMatcher = status().isNotFound(),
            additionalMatchers = arrayOf(
              jsonPath("$.content.title").value(DomainException::class.java.simpleName),
              jsonPath("$.content.status").value("404"),
              jsonPath("$.content.cause.name").value(ItemNotFoundException::class.simpleName),
            ),
          )
        }

        withData(
          nameFn = { "succeeds for '${it.listItemCreateRequest.name}'" },
          ts = requestsAlpha,
        ) {
          verifyContentValue(
            url = "/items/${it.itemId}/lists/count",
            expectedValue = 1,
          )
        }

        withData(
          nameFn = { "succeeds for '${it.listItemCreateRequest.name}' [no lists]" },
          ts = requestsBeta,
        ) {
          verifyContentValue(
            url = "/items/${it.itemId}/lists/count",
            expectedValue = 0,
          )
        }
      }
    }
  })
