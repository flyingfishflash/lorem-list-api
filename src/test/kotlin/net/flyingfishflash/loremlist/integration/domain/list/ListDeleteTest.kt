package net.flyingfishflash.loremlist.integration.domain.list

import io.kotest.matchers.ints.beGreaterThanOrEqualTo
import io.kotest.matchers.should
import net.flyingfishflash.loremlist.api.data.request.LrmItemCreateRequest
import net.flyingfishflash.loremlist.api.data.request.LrmListCreateRequest
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

class ListDeleteTest(mockMvc: MockMvc) :
  DomainFunctionTest(mockMvc = mockMvc, body = {
    val listsWithItems = linkedMapOf<UUID, LrmListCreateRequest>()
    val itemIdMapAlpha = linkedMapOf<UUID, LrmItemCreateRequest>()
    val itemIdMapBeta = linkedMapOf<UUID, LrmItemCreateRequest>()
    val emptyListsAlpha = linkedMapOf<UUID, LrmListCreateRequest>()
    val emptyListsBeta = linkedMapOf<UUID, LrmListCreateRequest>()
    lateinit var listWithItemsAlpha: UUID
    lateinit var listWithItemsBeta: UUID

    beforeSpec {
      purgeDomain()

      listCreateRequests.forEach { request -> listsWithItems[createAndVerifyList(request)] = request }
      listCreateRequests.forEach { request -> emptyListsAlpha[createAndVerifyList(request)] = request }
      listCreateRequests.forEach { request -> emptyListsBeta[createAndVerifyList(request)] = request }

      listsWithItems.size should { beGreaterThanOrEqualTo(2) }
      emptyListsAlpha.size should { beGreaterThanOrEqualTo(1) }
      emptyListsBeta.size should { beGreaterThanOrEqualTo(3) }

      listWithItemsAlpha = listsWithItems.keys.first()
      listWithItemsBeta = listsWithItems.keys.last()

      itemCreateRequestsAlpha.forEach { request ->
        val itemId = createAndVerifyListItem(listId = listWithItemsAlpha, itemRequest = request)
        itemIdMapAlpha[itemId] = request
      }

      itemCreateRequestsBeta.forEach { request ->
        val itemId = createAndVerifyListItem(listId = listWithItemsBeta, itemRequest = request)
        itemIdMapBeta[itemId] = request
      }

      verifyContentValue(url = "/lists/count", expectedValue = listsWithItems.size + emptyListsAlpha.size + emptyListsBeta.size)
      verifyContentValue(url = "/items/count", expectedValue = itemIdMapAlpha.size + itemIdMapBeta.size)
    }

    describe("list deletion ") {
      context("deleting a list: /lists/{list-id}") {
        it("fails when invalid uuid is provided for list id") {
          performRequestAndVerifyResponse(
            method = HttpMethod.DELETE,
            instance = "/lists/${invalidUuids[0]}",
            expectedDisposition = DispositionOfProblem.FAILURE,
            statusMatcher = status().isBadRequest(),
            additionalMatchers = arrayOf(
              jsonPath("$.message").value("$VALIDATION_FAILURE_MESSAGE listId."),
              jsonPath("$.content.validationErrors.length()").value(1),
            ),
          )
        }

        it("fails when list to delete is not found") {
          val nonExistentId = UUID.randomUUID()
          performRequestAndVerifyResponse(
            method = HttpMethod.DELETE,
            instance = "/lists/$nonExistentId",
            expectedDisposition = DispositionOfProblem.FAILURE,
            statusMatcher = status().isNotFound(),
            additionalMatchers = arrayOf(
              jsonPath("$.content.title").value(DomainException::class.java.simpleName),
              jsonPath("$.content.status").value("404"),
            ),
          )
        }

        context("deleting a list with items") {
          it("fails when ?removeItemAssociations is omitted") {
            performRequestAndVerifyResponse(
              method = HttpMethod.DELETE,
              instance = "/lists/$listWithItemsAlpha",
              expectedDisposition = DispositionOfProblem.FAILURE,
              statusMatcher = status().isUnprocessableEntity,
              additionalMatchers = arrayOf(
                jsonPath("$.message").exists(),
              ),
            )

            verifyListIsFound(listWithItemsAlpha)
            itemIdMapAlpha.keys.forEach { itemId -> verifyContentListsLength(itemId = itemId, expectedListsCount = 1) }
          }

          it("fails when ?removeItemAssociations=false") {
            performRequestAndVerifyResponse(
              method = HttpMethod.DELETE,
              instance = "/lists/$listWithItemsAlpha?removeItemAssociations=false",
              expectedDisposition = DispositionOfProblem.FAILURE,
              statusMatcher = status().isUnprocessableEntity,
              additionalMatchers = arrayOf(
                jsonPath("$.message").exists(),
              ),
            )

            verifyListIsFound(listWithItemsAlpha)
            itemIdMapAlpha.keys.forEach { itemId -> verifyContentListsLength(itemId = itemId, expectedListsCount = 1) }
          }

          it("succeeds when ?removeItemAssociations=true") {
            performRequestAndVerifyResponse(
              method = HttpMethod.DELETE,
              instance = "/lists/$listWithItemsAlpha?removeItemAssociations=true",
              expectedDisposition = DispositionOfSuccess.SUCCESS,
              additionalMatchers = arrayOf(
                jsonPath("$.message").exists(),
                jsonPath("$.content.associatedItemNames.length()").value(itemIdMapAlpha.size),
              ),
            )

            verifyListIsNotFound(listWithItemsAlpha)
            itemIdMapAlpha.keys.forEach { itemId -> verifyContentListsLength(itemId = itemId, expectedListsCount = 0) }
          }
        }
        context("deleting a list without items") {
          val emptyList0 = emptyListsBeta.keys.elementAt(0)
          val emptyList1 = emptyListsBeta.keys.elementAt(1)
          val emptyList2 = emptyListsBeta.keys.elementAt(2)

          it("succeeds when ?removeItemAssociations is omitted") {
            performRequestAndVerifyResponse(
              method = HttpMethod.DELETE,
              instance = "/lists/$emptyList0",
              expectedDisposition = DispositionOfSuccess.SUCCESS,
              additionalMatchers = arrayOf(
                jsonPath("$.content").exists(),
              ),
            )

            verifyListIsNotFound(emptyList0)
          }

          it("succeeds when ?removeItemAssociations=true") {
            performRequestAndVerifyResponse(
              method = HttpMethod.DELETE,
              instance = "/lists/$emptyList1?removeItemAssociations=true",
              expectedDisposition = DispositionOfSuccess.SUCCESS,
              additionalMatchers = arrayOf(
                jsonPath("$.content.associatedItemNames.length()").value(0),
              ),
            )

            verifyListIsNotFound(emptyList1)
          }

          it("succeeds when ?removeItemAssociations=false") {
            performRequestAndVerifyResponse(
              method = HttpMethod.DELETE,
              instance = "/lists/$emptyList2?removeItemAssociations=false",
              expectedDisposition = DispositionOfSuccess.SUCCESS,
              additionalMatchers = arrayOf(
                jsonPath("$.content.associatedItemNames.length()").value(0),
              ),
            )

            verifyListIsNotFound(emptyList2)
          }
        }
      }

      context("deleting all lists") {
        it("succeeds (all items remain)") {
          verifyContentValue(url = "/lists/count", expectedValue = 5)
          verifyContentValue(url = "/items/count", expectedValue = itemIdMapAlpha.size + itemIdMapBeta.size)

          performRequestAndVerifyResponse(
            method = HttpMethod.DELETE,
            instance = "/lists",
            expectedDisposition = DispositionOfSuccess.SUCCESS,
          )

          verifyContentValue(url = "/lists/count", expectedValue = 0)
          verifyContentValue(url = "/items/count", expectedValue = itemIdMapAlpha.size + itemIdMapBeta.size)
        }
      }
    }
  })
