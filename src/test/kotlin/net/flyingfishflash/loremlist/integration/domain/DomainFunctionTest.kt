package net.flyingfishflash.loremlist.integration.domain

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.extensions.spring.SpringExtension
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.flyingfishflash.loremlist.api.data.request.LrmItemCreateRequest
import net.flyingfishflash.loremlist.api.data.request.LrmListCreateRequest
import net.flyingfishflash.loremlist.api.data.response.LrmListItemResponse
import net.flyingfishflash.loremlist.api.data.response.LrmListResponse
import net.flyingfishflash.loremlist.core.response.advice.CoreExceptionHandler.Companion.VALIDATION_FAILURE_MESSAGE
import net.flyingfishflash.loremlist.core.response.structure.DispositionOfProblem
import net.flyingfishflash.loremlist.core.response.structure.DispositionOfSuccess
import net.flyingfishflash.loremlist.core.response.structure.ResponseSuccess
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.ResultMatcher
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.*

@SpringBootTest
@ActiveProfiles("h2")
@AutoConfigureMockMvc
abstract class DomainFunctionTest(body: DomainFunctionTest.() -> Unit, private val mockMvc: MockMvc) : DescribeSpec() {
  override fun extensions() = listOf(SpringExtension)

  fun performRequest(method: HttpMethod, url: String, content: String? = null): ResultActions {
    return mockMvc.perform(
      request(method, url).apply {
        with(jwt())
        with(csrf())
        contentType(MediaType.APPLICATION_JSON)
        content?.let {
          println("content: $it")
          content(it)
        }
      },
    )
  }

  fun performRequestAndVerifyResponse(
    instance: String,
    method: HttpMethod,
    requestBody: String? = null,
    expectSuccess: Boolean,
    statusMatcher: ResultMatcher = status().isOk(),
    contentTypeMatcher: ResultMatcher = content().contentType(MediaType.APPLICATION_JSON),
    expectedSize: Int = 1,
    additionalMatchers: Array<ResultMatcher> = emptyArray(),
  ): ResultActions {
    val disposition = if (expectSuccess) DispositionOfSuccess.SUCCESS else DispositionOfProblem.FAILURE
    val result = performRequest(method = method, url = instance, content = requestBody)
    return result.andExpectAll(
      statusMatcher,
      contentTypeMatcher,
      jsonPath("$.disposition").value(disposition.nameAsLowercase()),
      jsonPath("$.method").value(method.name().lowercase()),
      jsonPath("$.instance").value(instance.substringBefore("?")),
      jsonPath("$.size").value(expectedSize),
      *additionalMatchers,
    )
  }

  /** create a list and return the resulting id */
  fun createAndVerifyList(listRequest: LrmListCreateRequest): UUID {
    val result = performRequestAndVerifyResponse(
      instance = "/lists",
      method = HttpMethod.POST,
      requestBody = Json.encodeToString(listRequest),
      expectSuccess = true,
      additionalMatchers = arrayOf(
        jsonPath("$.content.id").isNotEmpty(),
        jsonPath("$.content.name").value(listRequest.name),
        jsonPath("$.content.description").value(listRequest.description),
        jsonPath("$.content.public").value(listRequest.public),
        jsonPath("$.content.created").isNotEmpty(),
        jsonPath("$.content.updated").isNotEmpty(),
        jsonPath("$.content.items").isEmpty(),
      ),
    )
    val response = result.andReturn().response.contentAsString
    return Json.decodeFromString<ResponseSuccess<LrmListResponse>>(response).content.id
  }

  /** create a list item and return the resulting id */
  fun createAndVerifyListItem(listId: UUID, itemRequest: LrmItemCreateRequest): UUID {
    val result = performRequestAndVerifyResponse(
      instance = "/lists/$listId/items",
      method = HttpMethod.POST,
      requestBody = Json.encodeToString(itemRequest),
      expectSuccess = true,
      additionalMatchers = arrayOf(
        jsonPath("$.content.id").isNotEmpty(),
        jsonPath("$.content.name").value(itemRequest.name),
        jsonPath("$.content.description").value(itemRequest.description),
        jsonPath("$.content.quantity").value(itemRequest.quantity),
        jsonPath("$.content.isSuppressed").value(itemRequest.isSuppressed),
        jsonPath("$.content.created").isNotEmpty(),
        jsonPath("$.content.updated").isNotEmpty(),
        jsonPath("$.content.lists").isNotEmpty(),
        jsonPath("$.content.lists.length()").value(1),
      ),
    )
    val response = result.andReturn().response.contentAsString
    return Json.decodeFromString<ResponseSuccess<LrmListItemResponse>>(response).content.id
  }

  fun verifyContentSize(url: String, expectedSize: Int) {
    performRequestAndVerifyResponse(
      method = HttpMethod.GET,
      instance = url,
      expectSuccess = true,
      expectedSize = expectedSize,
    )
  }

  fun verifyContentValue(url: String, expectedValue: Int) {
    performRequestAndVerifyResponse(
      method = HttpMethod.GET,
      instance = url,
      expectSuccess = true,
      additionalMatchers = arrayOf(
        jsonPath("$.content.value").value(expectedValue),
      ),
    )
  }

  fun verifyContentListsLength(itemId: UUID, expectedListsCount: Int) {
    performRequestAndVerifyResponse(
      method = HttpMethod.GET,
      instance = "/items/$itemId",
      expectSuccess = true,
      additionalMatchers = arrayOf(
        jsonPath("$.content.lists.length()").value(expectedListsCount),
      ),
    )
  }

  fun verifyContentItemsLength(listId: UUID, expectedItemsCount: Int) {
    performRequestAndVerifyResponse(
      method = HttpMethod.GET,
      instance = "/lists/$listId",
      expectSuccess = true,
      additionalMatchers = arrayOf(
        jsonPath("$.content.items.length()").value(expectedItemsCount),
      ),
    )
  }

  fun verifyListIsFound(listId: UUID) {
    performRequestAndVerifyResponse(
      method = HttpMethod.GET,
      instance = "/lists/$listId",
      expectSuccess = true,
    )
  }

  fun verifyListIsNotFound(listId: UUID) {
    performRequestAndVerifyResponse(
      method = HttpMethod.GET,
      instance = "/lists/$listId",
      expectSuccess = false,
      statusMatcher = status().isNotFound(),
    )
  }

  fun verifyItemIsFound(itemId: UUID) {
    performRequestAndVerifyResponse(
      method = HttpMethod.GET,
      instance = "/items/$itemId",
      expectSuccess = true,
    )
  }

  fun purgeDomain() {
    performRequestAndVerifyResponse(
      method = HttpMethod.DELETE,
      instance = "/maintenance/purge",
      expectSuccess = true,
      additionalMatchers = arrayOf(),
    )
  }

  // Test data factory
  object TestData {
    data class ValidationScenario(val description: String, val postContent: String, val responseMessage: String, val expectedErrorCount: Int)
    data class ListCreateUpdateRequestPair(val createRequest: LrmListCreateRequest, val updateRequest: LrmListCreateRequest)
    data class ListCreateItemCreateRequestPair(val listCreateRequest: LrmListCreateRequest, val itemCreateRequest: LrmItemCreateRequest)

    val invalidUuids = listOf(UUID.fromString("00000000-0000-a000-9000-000000000000"))

    val listCreateRequests = (1..3).map { createListRequest(it) }
    private val listUpdateRequests = (1..3).map { createListRequest(it, updated = true) }
    val itemCreateRequestsAlpha = (1..3).map { createListItemRequest(it) }
    val itemCreateRequestsBeta = (1..10).map { createListItemRequest(it) }
    val itemUpdateRequests = (1..3).map { createListItemRequest(it, updated = true) }

    val listCreateUpdateRequestPairs = listCreateRequests.zip(listUpdateRequests) { create, update -> ListCreateUpdateRequestPair(create, update) }
    val listCreateItemCreateRequestPairs = listCreateRequests.zip(itemCreateRequestsAlpha) { listCreate, itemCreate ->
      ListCreateItemCreateRequestPair(listCreate, itemCreate)
    }

    private fun createListRequest(index: Int, updated: Boolean = false) = LrmListCreateRequest(
      name = if (updated) "list $index *" else "list $index",
      description = if (updated) "list $index description *" else "list $index description",
      public = updated != (index % 2 == 0),
    )

    private fun createListItemRequest(index: Int, updated: Boolean = false) = LrmItemCreateRequest(
      name = if (updated) "list item $index *" else "list item $index",
      description = if (updated) "list item $index description *" else "list item $index description",
      quantity = if (updated) 1000 + index else index,
      isSuppressed = updated != (index % 2 == 0),
    )

    // request body validation test scenarios
    val listCreateValidationScenarios = listOf(
      ValidationScenario(
        description = "fails when name is null",
        postContent = "{ \"name\": null, \"description\": null }",
        responseMessage = "Failed to read request.",
        expectedErrorCount = 0,
      ),
      ValidationScenario(
        description = "fails when name is only whitespace",
        postContent = Json.encodeToString(LrmListCreateRequest(name = " ", description = "bLLh|Rvz.x0@W2d9G:a", public = true)),
        responseMessage = "$VALIDATION_FAILURE_MESSAGE name.",
        expectedErrorCount = 1,
      ),
      ValidationScenario(
        description = "fails when name is empty, description is only whitespace",
        postContent = Json.encodeToString(LrmListCreateRequest(name = "", description = " ", public = true)),
        responseMessage = "$VALIDATION_FAILURE_MESSAGE description, name.",
        expectedErrorCount = 3,
      ),
    )

    val listItemCreateValidationScenarios = listOf(
      ValidationScenario(
        description = "fails when name is null",
        postContent = "{ \"name\": null, \"description\": null }",
        responseMessage = "Failed to read request.",
        expectedErrorCount = 0,
      ),
      ValidationScenario(
        description = "fails when name is only whitespace",
        postContent = Json.encodeToString(LrmItemCreateRequest(name = " ", description = "bLLh|Rvz.x0@W2d9G:a", quantity = 0, isSuppressed = false)),
        responseMessage = "$VALIDATION_FAILURE_MESSAGE name.",
        expectedErrorCount = 1,
      ),
      ValidationScenario(
        description = "fails when name is empty, description is only whitespace",
        postContent = Json.encodeToString(LrmItemCreateRequest(name = "", description = " ", quantity = 0, isSuppressed = false)),
        responseMessage = "$VALIDATION_FAILURE_MESSAGE description, name.",
        expectedErrorCount = 3,
      ),
      ValidationScenario(
        description = "fails when quantity is less than 0",
        postContent = Json.encodeToString(LrmItemCreateRequest(name = "S0hztGQBNl", description = "S0hztGQBNl", quantity = -1, isSuppressed = false)),
        responseMessage = "$VALIDATION_FAILURE_MESSAGE quantity.",
        expectedErrorCount = 1,
      ),
    )

    val listUpdateValidationScenarios = listOf(
      ValidationScenario(
        description = "fails when name is null",
        postContent = "{ \"name\": null, \"description\": null }",
        responseMessage = "$VALIDATION_FAILURE_MESSAGE patchRequest.",
        expectedErrorCount = 2,
      ),
      ValidationScenario(
        description = "fails when name is only whitespace",
        postContent = Json.encodeToString(LrmListCreateRequest(name = " ", description = "bLLh|Rvz.x0@W2d9G:a", public = true)),
        responseMessage = "$VALIDATION_FAILURE_MESSAGE patchRequest.",
        expectedErrorCount = 1,
      ),
      ValidationScenario(
        description = "fails when name is empty, description is only whitespace",
        postContent = Json.encodeToString(LrmListCreateRequest(name = "", description = " ", public = true)),
        responseMessage = "$VALIDATION_FAILURE_MESSAGE patchRequest.",
        expectedErrorCount = 2,
      ),
    )
  }

  init {
    body()
  }
}
