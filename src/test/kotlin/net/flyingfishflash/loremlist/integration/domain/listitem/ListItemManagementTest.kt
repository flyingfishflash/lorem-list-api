package net.flyingfishflash.loremlist.integration.domain.listitem

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.flyingfishflash.loremlist.api.data.request.LrmListItemAddRequest
import net.flyingfishflash.loremlist.core.response.advice.CoreExceptionHandler.Companion.VALIDATION_FAILURE_MESSAGE
import net.flyingfishflash.loremlist.core.response.structure.DispositionOfProblem
import net.flyingfishflash.loremlist.core.response.structure.DispositionOfSuccess
import net.flyingfishflash.loremlist.domain.exceptions.DomainException
import net.flyingfishflash.loremlist.domain.lrmitem.ItemNotFoundException
import net.flyingfishflash.loremlist.domain.lrmlist.ListNotFoundException
import net.flyingfishflash.loremlist.integration.domain.DomainFunctionTest
import net.flyingfishflash.loremlist.integration.domain.DomainFunctionTest.TestData.invalidUuids
import org.springframework.http.HttpMethod
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.*

class ListItemManagementTest(mockMvc: MockMvc) :
  DomainFunctionTest(mockMvc = mockMvc, body = {
    var listIds = listOf<UUID>()
    var itemIds = listOf<UUID>()

    beforeSpec {
      purgeDomain()

      listIds = TestData.listCreateRequests.map { createAndVerifyList(it) }
      itemIds = TestData.itemCreateRequestsAlpha.map { createAndVerifyListItem(listId = listIds[0], it) }

      verifyContentValue(url = "/lists/count", expectedValue = TestData.listCreateRequests.size)
      verifyContentValue(url = "/items/count", expectedValue = TestData.itemCreateRequestsAlpha.size)
      verifyContentValue(url = "/lists/${listIds[0]}/items/count", expectedValue = TestData.itemCreateRequestsAlpha.size)
      verifyContentValue(url = "/lists/${listIds[1]}/items/count", expectedValue = 0)
      verifyContentValue(url = "/lists/${listIds[2]}/items/count", expectedValue = 0)
    }

    describe("list item management") {
      context("moving an item between lists: /lists/{list-id}/items/{item-id}/{destination-list-id}") {
        it("fails when invalid uuid is provided for list id, item id, and destination list id") {
          performRequestAndVerifyResponse(
            method = HttpMethod.PATCH,
            instance = "/lists/${invalidUuids[0]}/items/${invalidUuids[0]}/${invalidUuids[0]}",
            expectedDisposition = DispositionOfProblem.FAILURE,
            statusMatcher = status().isBadRequest(),
            additionalMatchers = arrayOf(
              jsonPath("$.message").value("$VALIDATION_FAILURE_MESSAGE destinationListId, itemId, listId."),
              jsonPath("$.content.validationErrors.length()").value(1),
            ),
          )
        }

        it("fails when source list is not found") {
          performRequestAndVerifyResponse(
            method = HttpMethod.PATCH,
            instance = "/lists/${UUID.randomUUID()}/items/${itemIds[0]}/${listIds[1]}",
            expectedDisposition = DispositionOfProblem.FAILURE,
            statusMatcher = status().isNotFound,
            additionalMatchers = arrayOf(
              jsonPath("$.content.title").value(DomainException::class.simpleName.toString()),
              jsonPath("$.content.cause.name").value(ListNotFoundException::class.simpleName.toString()),
            ),
          )
        }

        it("fails when destination list is not found") {
          performRequestAndVerifyResponse(
            method = HttpMethod.PATCH,
            instance = "/lists/${listIds[0]}/items/${itemIds[0]}/${UUID.randomUUID()}",
            expectedDisposition = DispositionOfProblem.FAILURE,
            statusMatcher = status().isNotFound,
            additionalMatchers = arrayOf(
              jsonPath("$.content.title").value(DomainException::class.simpleName.toString()),
              jsonPath("$.content.cause.name").value(ListNotFoundException::class.simpleName.toString()),
            ),
          )
        }

        it("fails when item is not found") {
          performRequestAndVerifyResponse(
            method = HttpMethod.PATCH,
            instance = "/lists/${listIds[0]}/items/${UUID.randomUUID()}/${listIds[1]}",
            expectedDisposition = DispositionOfProblem.FAILURE,
            statusMatcher = status().isNotFound,
            additionalMatchers = arrayOf(
              jsonPath("$.content.title").value(DomainException::class.simpleName.toString()),
              jsonPath("$.content.cause.name").value(ItemNotFoundException::class.simpleName.toString()),
            ),
          )
        }

        it("succeeds when moving the first item from the first list to the second list") {
          performRequestAndVerifyResponse(
            method = HttpMethod.PATCH,
            instance = "/lists/${listIds[0]}/items/${itemIds[0]}/${listIds[1]}",
            expectedDisposition = DispositionOfSuccess.SUCCESS,
            additionalMatchers = arrayOf(
              jsonPath("$.content.message").isNotEmpty(),
            ),
          )

          verifyContentValue(url = "/lists/${listIds[0]}/items/count", expectedValue = itemIds.size - 1)
          verifyContentValue(url = "/lists/${listIds[1]}/items/count", expectedValue = 1)
          verifyContentListsLength(itemId = itemIds[0], expectedListsCount = 1)
        }
      }

      context("removing an item from a list: /lists/{list-id}/items/{item-id}") {

        it("fails when invalid uuid is provided for list id and item id") {
          performRequestAndVerifyResponse(
            method = HttpMethod.DELETE,
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
          performRequestAndVerifyResponse(
            method = HttpMethod.DELETE,
            instance = "/lists/${UUID.randomUUID()}/items/${itemIds[0]}",
            expectedDisposition = DispositionOfProblem.FAILURE,
            statusMatcher = status().isNotFound(),
            additionalMatchers = arrayOf(
              jsonPath("$.content.title").value(DomainException::class.simpleName.toString()),
              jsonPath("$.content.cause.name").value(ListNotFoundException::class.simpleName.toString()),
            ),
          )
        }

        it("fails when item is not found") {
          performRequestAndVerifyResponse(
            method = HttpMethod.DELETE,
            instance = "/lists/${listIds[0]}/items/${UUID.randomUUID()}",
            expectedDisposition = DispositionOfProblem.FAILURE,
            statusMatcher = status().isNotFound(),
            additionalMatchers = arrayOf(
              jsonPath("$.content.title").value(DomainException::class.simpleName.toString()),
              jsonPath("$.content.cause.name").value(ItemNotFoundException::class.simpleName.toString()),
            ),
          )
        }

        it("succeeds when removing the first item from second list") {
          performRequestAndVerifyResponse(
            method = HttpMethod.DELETE,
            instance = "/lists/${listIds[1]}/items/${itemIds[0]}",
            expectedDisposition = DispositionOfSuccess.SUCCESS,
          )

          verifyContentValue(url = "/lists/${listIds[1]}/items/count", expectedValue = 0)
          verifyContentListsLength(itemId = itemIds[0], expectedListsCount = 0)
          verifyContentSize(url = "/items/with-no-lists", expectedSize = 1) // The removed item
        }
      }

      context("adding items to a list: /lists/{list-id}/items") {
        it("fails when invalid uuid is provided for list id") {
          performRequestAndVerifyResponse(
            method = HttpMethod.PUT,
            instance = "/lists/${invalidUuids[0]}/items",
            requestBody = Json.encodeToString(LrmListItemAddRequest(itemIdCollection = setOf(itemIds[1], itemIds[2]))),
            expectedDisposition = DispositionOfProblem.FAILURE,
            statusMatcher = status().isBadRequest(),
            additionalMatchers = arrayOf(
              jsonPath("$.message").value("$VALIDATION_FAILURE_MESSAGE listId."),
              jsonPath("$.content.validationErrors.length()").value(1),
            ),
          )
        }

        it("fails deserialization when itemIdCollection contains non-UUID") {
          performRequestAndVerifyResponse(
            method = HttpMethod.PUT,
            instance = "/lists/${listIds[1]}/items",
            requestBody = "{\"itemIdCollection\":[\"000000000000a000900000000000000\"]}",
            expectedDisposition = DispositionOfProblem.FAILURE,
            statusMatcher = status().isBadRequest(),
          )
        }

        it("fails validation when itemCollection contains non version 3 or version 4 UUID") {
          performRequestAndVerifyResponse(
            method = HttpMethod.PUT,
            instance = "/lists/${listIds[1]}/items",
            requestBody = Json.encodeToString(LrmListItemAddRequest(itemIdCollection = setOf(invalidUuids[0]))),
            expectedDisposition = DispositionOfProblem.FAILURE,
            statusMatcher = status().isBadRequest(),
          )
        }

        it("fails validation when request body contains empty itemIdCollection") {
          performRequestAndVerifyResponse(
            method = HttpMethod.PUT,
            instance = "/lists/${listIds[1]}/items",
            requestBody = "{\"itemIdCollection\":[]}",
            expectedDisposition = DispositionOfProblem.FAILURE,
            statusMatcher = status().isBadRequest(),
          )
        }

        it("fails when list is not found") {
          performRequestAndVerifyResponse(
            method = HttpMethod.PUT,
            instance = "/lists/${UUID.randomUUID()}/items",
            requestBody = Json.encodeToString(LrmListItemAddRequest(itemIdCollection = setOf(itemIds[1], itemIds[2]))),
            expectedDisposition = DispositionOfProblem.FAILURE,
            statusMatcher = status().isNotFound(),
            additionalMatchers = arrayOf(
              jsonPath("$.content.title").value(DomainException::class.simpleName.toString()),
              jsonPath("$.content.cause.name").value(ListNotFoundException::class.simpleName.toString()),
            ),
          )
        }

        it("succeeds when adding the second and third items to the second list") {
          performRequestAndVerifyResponse(
            method = HttpMethod.PUT,
            instance = "/lists/${listIds[1]}/items",
            requestBody = Json.encodeToString(LrmListItemAddRequest(itemIdCollection = setOf(itemIds[1], itemIds[2]))),
            expectedDisposition = DispositionOfSuccess.SUCCESS,
          )

          verifyContentValue(url = "/lists/${listIds[1]}/items/count", expectedValue = 2)
          verifyContentListsLength(itemId = itemIds[1], expectedListsCount = 2) // Item belongs to lists 0 and 1
          verifyContentListsLength(itemId = itemIds[2], expectedListsCount = 2) // Item belongs to lists 0 and 1
        }
      }

      context("removing all items from a list: /lists/{list-id}/items") {
        it("fails when invalid uuid is provided for list id and item id") {
          performRequestAndVerifyResponse(
            method = HttpMethod.DELETE,
            instance = "/lists/${invalidUuids[0]}/items",
            expectedDisposition = DispositionOfProblem.FAILURE,
            statusMatcher = status().isBadRequest(),
            additionalMatchers = arrayOf(
              jsonPath("$.message").value("$VALIDATION_FAILURE_MESSAGE listId."),
              jsonPath("$.content.validationErrors.length()").value(1),
            ),
          )
        }

        it("fails when list is not found") {
          performRequestAndVerifyResponse(
            method = HttpMethod.DELETE,
            instance = "/lists/${UUID.randomUUID()}/items",
            expectedDisposition = DispositionOfProblem.FAILURE,
            statusMatcher = status().isNotFound(),
            additionalMatchers = arrayOf(
              jsonPath("$.content.title").value(DomainException::class.simpleName.toString()),
              jsonPath("$.content.cause.name").value(ListNotFoundException::class.simpleName.toString()),
            ),
          )
        }

        it("succeeds when removing all items from the second list") {
          performRequestAndVerifyResponse(
            method = HttpMethod.DELETE,
            instance = "/lists/${listIds[1]}/items",
            expectedDisposition = DispositionOfSuccess.SUCCESS,
          )

          verifyContentValue(url = "/lists/${listIds[1]}/items/count", expectedValue = 0)
          verifyContentListsLength(itemId = itemIds[1], expectedListsCount = 1) // Now only in list 0
          verifyContentListsLength(itemId = itemIds[2], expectedListsCount = 1) // Now only in list 0
        }
      }
    }
  })
