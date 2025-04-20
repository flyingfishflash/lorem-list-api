package net.flyingfishflash.loremlist.integration.domain.list

import io.kotest.datatest.withData
import net.flyingfishflash.loremlist.api.data.request.LrmItemCreateRequest
import net.flyingfishflash.loremlist.api.data.request.LrmListCreateRequest
import net.flyingfishflash.loremlist.core.response.advice.CoreExceptionHandler.Companion.VALIDATION_FAILURE_MESSAGE
import net.flyingfishflash.loremlist.domain.lrmlist.ListNotFoundException
import net.flyingfishflash.loremlist.integration.domain.DomainFunctionTest
import net.flyingfishflash.loremlist.integration.domain.DomainFunctionTest.TestData.invalidUuids
import net.flyingfishflash.loremlist.integration.domain.DomainFunctionTest.TestData.itemCreateRequestsAlpha
import net.flyingfishflash.loremlist.integration.domain.DomainFunctionTest.TestData.listCreateRequests
import org.springframework.http.HttpMethod
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.*

class ListReadTest(mockMvc: MockMvc) :
  DomainFunctionTest(mockMvc = mockMvc, body = {
    var listIdMap = mapOf<UUID, LrmListCreateRequest>()
    var itemIdMap = mapOf<UUID, LrmItemCreateRequest>()

    beforeSpec {
      purgeDomain()

      listIdMap = listCreateRequests.associateBy { request -> createAndVerifyList(request) }
      itemIdMap = itemCreateRequestsAlpha.associateBy { request ->
        createAndVerifyListItem(listId = listIdMap.keys.first(), itemRequest = request)
      }

      verifyContentValue("/lists/count", listIdMap.size)
    }

    describe("list retrieval") {
      context("retrieving all lists") {
        it("succeeds when ?includeItems=true") {
          performRequestAndVerifyResponse(
            method = HttpMethod.GET,
            instance = "/lists?includeItems=true",
            expectSuccess = true,
            expectedSize = listIdMap.size,
            additionalMatchers = arrayOf(
              jsonPath("$.content.length()").value(listIdMap.size),
              jsonPath("$..items.length()").value(itemIdMap.size),
            ),
          )
        }

        it("succeeds when ?includeItems=false") {
          performRequestAndVerifyResponse(
            method = HttpMethod.GET,
            instance = "/lists?includeItems=false",
            expectSuccess = true,
            expectedSize = listIdMap.size,
            additionalMatchers = arrayOf(
              jsonPath("$.content.length()").value(listIdMap.size),
              jsonPath("$..items.length()").value(0),
            ),
          )
        }

        it("succeeds when ?includeItems is omitted (default behavior)") {
          performRequestAndVerifyResponse(
            method = HttpMethod.GET,
            instance = "/lists",
            expectSuccess = true,
            expectedSize = listIdMap.size,
            additionalMatchers = arrayOf(
              jsonPath("$.content.length()").value(listIdMap.size),
              jsonPath("$..items.length()").value(0),
            ),
          )
        }
      }

      context("retrieving a list") {
        it("fails when list is not found") {
          val listId = UUID.randomUUID()
          performRequestAndVerifyResponse(
            method = HttpMethod.GET,
            instance = "/lists/$listId",
            expectSuccess = false,
            statusMatcher = status().isNotFound(),
            additionalMatchers = arrayOf(
              jsonPath("$.content.title").value(ListNotFoundException::class.java.simpleName),
              jsonPath("$.content.status").value("404"),
              jsonPath("$.content.supplemental.notFound").value(listId.toString()),
            ),
          )
        }
      }

      context("retrieving a list when ?includeItems is true") {
        withData(
          nameFn = { uuid -> "succeeds when uuid is $uuid" },
          listIdMap.entries.toList(),
        ) { (uuid, request) ->
          performRequestAndVerifyResponse(
            method = HttpMethod.GET,
            instance = "/lists/$uuid?includeItems=true",
            expectSuccess = true,
            additionalMatchers = arrayOf(
              jsonPath("$.content.id").value(uuid.toString()),
              jsonPath("$.content.name").value(request.name),
              jsonPath("$.content.description").value(request.description),
              jsonPath("$.content.public").value(request.public),
              if (uuid == listIdMap.keys.first()) {
                jsonPath("$..items.length()").value(itemIdMap.size)
              } else {
                jsonPath("$..items.length()").value(0)
              },

            ),
          )
        }
      }

      context("retrieving a list when ?includeItems is false") {
        withData(
          nameFn = { uuid -> "succeeds when uuid is $uuid" },
          listIdMap.entries.toList(),
        ) { (uuid, request) ->
          performRequestAndVerifyResponse(
            method = HttpMethod.GET,
            instance = "/lists/$uuid?includeItems=false",
            expectSuccess = true,
            additionalMatchers = arrayOf(
              jsonPath("$.content.id").value(uuid.toString()),
              jsonPath("$.content.name").value(request.name),
              jsonPath("$.content.description").value(request.description),
              jsonPath("$.content.public").value(request.public),
            ),
          )
        }
      }

      context("retrieving a list when ?includeItems is omitted (default behavior)") {
        withData(
          nameFn = { uuid -> "succeeds when uuid is $uuid" },
          listIdMap.entries.toList(),
        ) { (uuid, request) ->
          performRequestAndVerifyResponse(
            method = HttpMethod.GET,
            instance = "/lists/$uuid",
            expectSuccess = true,
            additionalMatchers = arrayOf(
              jsonPath("$.content.id").value(uuid.toString()),
              jsonPath("$.content.name").value(request.name),
              jsonPath("$.content.description").value(request.description),
              jsonPath("$.content.public").value(request.public),
            ),
          )
        }
      }

      context("path variable validation") {
        it("fails when invalid uuid is provided for list id") {
          performRequestAndVerifyResponse(
            method = HttpMethod.GET,
            instance = "/lists/${invalidUuids[0]}",
            expectSuccess = false,
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
