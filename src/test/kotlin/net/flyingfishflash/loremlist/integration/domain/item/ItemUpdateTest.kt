package net.flyingfishflash.loremlist.integration.domain.item

import io.kotest.datatest.withData
import net.flyingfishflash.loremlist.api.data.request.LrmItemCreateRequest
import net.flyingfishflash.loremlist.core.response.structure.DispositionOfProblem
import net.flyingfishflash.loremlist.core.response.structure.DispositionOfSuccess
import net.flyingfishflash.loremlist.domain.lrmitem.ItemNotFoundException
import net.flyingfishflash.loremlist.integration.domain.DomainFunctionTest
import net.flyingfishflash.loremlist.integration.domain.DomainFunctionTest.TestData.itemCreateRequestsAlpha
import net.flyingfishflash.loremlist.integration.domain.DomainFunctionTest.TestData.listCreateRequests
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Transactional
class ItemUpdateTest(mockMvc: MockMvc) :
  DomainFunctionTest(mockMvc = mockMvc, body = {

    data class ListItemRequestRecord(val listId: UUID, val itemId: UUID?, val listItemCreateRequest: LrmItemCreateRequest)
    var requestsAlpha: List<ListItemRequestRecord> = emptyList()
    beforeSpec {
      purgeDomain()

      // create a list
      val listWithItems = createAndVerifyList(listRequest = listCreateRequests[0])

      // create list items
      requestsAlpha = itemCreateRequestsAlpha.map {
        ListItemRequestRecord(listId = listWithItems, itemId = createAndVerifyListItem(listWithItems, it), listItemCreateRequest = it)
      }

      // validate state
      verifyContentValue(url = "/lists/count", expectedValue = 1)
      verifyContentValue(url = "/lists/$listWithItems/items/count", expectedValue = requestsAlpha.size)
    }

    describe("item modification") {
      context("modifying an item: /items/{item-id}") {
        it("fails when item to update is not found") {
          val nonExistentUuid = UUID.randomUUID()

          performRequestAndVerifyResponse(
            method = HttpMethod.PATCH,
            instance = "/items/$nonExistentUuid",
            requestBody = "{ \"name\": null, \"description\": null }",
            expectedDisposition = DispositionOfProblem.FAILURE,
            statusMatcher = status().isNotFound(),
            additionalMatchers = arrayOf(
              jsonPath("$.content.title").value(ItemNotFoundException::class.java.simpleName),
              jsonPath("$.content.status").value("404"),
            ),
          )
        }

        it("fails when request body includes unsupported field") {
          performRequestAndVerifyResponse(
            method = HttpMethod.PATCH,
            instance = "/items/${requestsAlpha.first().itemId}",
            requestBody = "{ \"name\": \"lorem ipsum\", \"description\": \"lorem ipsum\", \"zzz\": \"blah\" }",
            expectedDisposition = DispositionOfProblem.ERROR,
            statusMatcher = status().isInternalServerError,
            additionalMatchers = arrayOf(
              jsonPath("$.content.title").value(IllegalArgumentException::class.java.simpleName),
              jsonPath("$.content.status").value(HttpStatus.INTERNAL_SERVER_ERROR.value()),
            ),
          )
        }

        withData(
          nameFn = { record -> "succeeds for all supported fields of '${record.listItemCreateRequest.name}'" },
          requestsAlpha,
        ) { record ->
          performRequestAndVerifyResponse(
            method = HttpMethod.PATCH,
            instance = "/items/${record.itemId}",
            requestBody = "{ \"name\": \"${record.listItemCreateRequest.name} *\", \"description\": \"${record.listItemCreateRequest.description} *\" }",
            expectedDisposition = DispositionOfSuccess.SUCCESS,
            additionalMatchers = arrayOf(
              jsonPath("$.content.id").value(record.itemId.toString()),
              jsonPath("$.content.name").value("${record.listItemCreateRequest.name} *"),
              jsonPath("$.content.description").value("${record.listItemCreateRequest.description} *"),
              jsonPath("$.content.created").isNotEmpty(),
              jsonPath("$.content.updated").isNotEmpty(),
            ),
          )

          performRequestAndVerifyResponse(
            method = HttpMethod.GET,
            instance = "/items/${record.itemId}",
            expectedDisposition = DispositionOfSuccess.SUCCESS,
            additionalMatchers = arrayOf(
              jsonPath("$.content.name").value("${record.listItemCreateRequest.name} *"),
              jsonPath("$.content.description").value("${record.listItemCreateRequest.description} *"),
            ),
          )
        }
      }
    }
  })
