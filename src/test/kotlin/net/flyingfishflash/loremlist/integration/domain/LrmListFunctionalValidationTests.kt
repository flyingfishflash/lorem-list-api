package net.flyingfishflash.loremlist.integration.domain

import io.kotest.core.spec.style.DescribeSpec
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.flyingfishflash.loremlist.api.data.request.LrmItemCreateRequest
import net.flyingfishflash.loremlist.api.data.response.LrmItemResponse
import net.flyingfishflash.loremlist.core.response.advice.CoreExceptionHandler.Companion.VALIDATION_FAILURE_MESSAGE
import net.flyingfishflash.loremlist.core.response.structure.DispositionOfProblem
import net.flyingfishflash.loremlist.core.response.structure.ResponseSuccess
import net.flyingfishflash.loremlist.core.serialization.UUIDSerializer
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockHttpServletRequestDsl
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActionsDsl
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put
import java.util.UUID

@SpringBootTest
@ActiveProfiles("h2")
@AutoConfigureMockMvc
class LrmListFunctionalValidationTests(mockMvc: MockMvc) :
  DescribeSpec({

    data class ValidationTest(
      val expectedErrorCount: Int = 1,
      val httpMethod: HttpMethod,
      val instance: String,
      val requestContent: String? = null,
      val responseMessage: String = "$VALIDATION_FAILURE_MESSAGE id.",
    )

    val invalidUuids = listOf(
      UUID.fromString("00000000-0000-a000-9000-000000000000"),
      UUID.fromString("00000000-0000-a000-9000-000000000001"),
      UUID.fromString("00000000-0000-a000-9000-000000000002"),
    )

    // version 4 uuids
    val validUuids = listOf(
      UUID.fromString("00000000-0000-4000-a000-000000000000"),
      UUID.fromString("00000000-0000-4000-a000-000000000001"),
    )

    fun createLrmItemOneRequest(): LrmItemCreateRequest = LrmItemCreateRequest("Lorem Item One Name", "Lorem Item One Description", 0)

    val itemUuids: MutableMap<Int, UUID> = HashMap()

    fun buildMvc(httpMethod: HttpMethod, instance: String, dsl: MockHttpServletRequestDsl.() -> Unit = {}): ResultActionsDsl {
      val methodUnsupported = "$httpMethod is not supported by MockMvc"
      require(
        httpMethod != HttpMethod.TRACE &&
          httpMethod != HttpMethod.HEAD &&
          httpMethod != HttpMethod.OPTIONS,
      ) { methodUnsupported }

      return when (httpMethod) {
        HttpMethod.DELETE -> mockMvc.delete(instance, dsl = dsl)
        HttpMethod.GET -> mockMvc.get(instance, dsl = dsl)
        HttpMethod.PATCH -> mockMvc.patch(instance, dsl = dsl)
        HttpMethod.POST -> mockMvc.post(instance, dsl = dsl)
        HttpMethod.PUT -> mockMvc.put(instance, dsl = dsl)
        else -> {
          throw IllegalArgumentException(methodUnsupported)
        }
      }
    }

    fun doValidationTest(condition: Map.Entry<String, ValidationTest>) {
      buildMvc(httpMethod = condition.value.httpMethod, instance = condition.value.instance) {
        with(jwt())
        contentType = MediaType.APPLICATION_JSON
        if (condition.value.httpMethod != HttpMethod.GET) {
          with(csrf())
          content = condition.value.requestContent
        }
      }.andExpect {
        status { isBadRequest() }
        jsonPath("$.disposition") { value(DispositionOfProblem.FAILURE.nameAsLowercase()) }
        jsonPath("$.message") { value(condition.value.responseMessage) }
        if (condition.value.expectedErrorCount > 0) {
          jsonPath("$.content.validationErrors.length()") { value(condition.value.expectedErrorCount) }
          jsonPath("$.content.validationErrors[*]") { isArray() }
        }
      }
    }

    describe("/items") {
      describe("invoke validation failures") {
        val conditions: Map<String, ValidationTest> = mapOf(
          "(post) name is null" to
            ValidationTest(
              expectedErrorCount = 0,
              httpMethod = HttpMethod.POST,
              instance = "/items",
              requestContent = "{ \"name\": null, \"description\": null, \"quantity\": 1073741824 }",
              responseMessage = "Failed to read request.",
            ),
          "(post) name is only whitespace" to
            ValidationTest(
              expectedErrorCount = 1,
              httpMethod = HttpMethod.POST,
              instance = "/items",
              requestContent = "{ \"name\": \" \", \"description\": \"bLLh|Rvz.x0@W2d9G:a\", \"quantity\": 1073741824 }",
              responseMessage = "$VALIDATION_FAILURE_MESSAGE name.",
            ),
          "(post) quantity is less than 0" to
            ValidationTest(
              expectedErrorCount = 1,
              httpMethod = HttpMethod.POST,
              instance = "/items",
              requestContent = "{ \"name\": \"Lorem Ipsum\", \"description\": \"bLLh|Rvz.x0@W2d9G:a\", \"quantity\": -101 }",
              responseMessage = "$VALIDATION_FAILURE_MESSAGE quantity.",
            ),
          "(post) description is only whitespace, quantity is less than 0" to
            ValidationTest(
              expectedErrorCount = 2,
              httpMethod = HttpMethod.POST,
              instance = "/items",
              requestContent = "{ \"name\": \"Lorem Ipsum\", \"description\": \" \", \"quantity\": -101 }",
              responseMessage = "$VALIDATION_FAILURE_MESSAGE description, quantity.",
            ),
        )

        conditions.forEach { condition ->
          it("-> ${condition.key}") {
            doValidationTest(condition)
          }
        }
      }
    }

    describe("/items/{item-id}") {
      describe("invoke path variable validation failures") {
        val conditions: Map<String, ValidationTest> = mapOf(
          "uuid is invalid /items/{item-id} (delete)" to ValidationTest(
            httpMethod = HttpMethod.DELETE,
            instance = "/items/${invalidUuids[0]}",
            responseMessage = "$VALIDATION_FAILURE_MESSAGE itemId.",
          ),
          "uuid is invalid /items/{item-id} (get)" to ValidationTest(
            httpMethod = HttpMethod.GET,
            instance = "/items/${invalidUuids[0]}",
            responseMessage = "$VALIDATION_FAILURE_MESSAGE itemId.",
          ),
          "uuid is invalid /items/{item-id} (patch)" to ValidationTest(
            httpMethod = HttpMethod.PATCH,
            instance = "/items/${invalidUuids[0]}",
            requestContent = "{ \"name\": \" \" }",
            responseMessage = "$VALIDATION_FAILURE_MESSAGE itemId.",
          ),
          "uuid is invalid /items/{item-id}/lists (delete)" to ValidationTest(
            httpMethod = HttpMethod.DELETE,
            instance = "/items/${invalidUuids[0]}/lists",
            requestContent = Json.encodeToString(UUIDSerializer, validUuids[0]),
            responseMessage = "$VALIDATION_FAILURE_MESSAGE itemId.",
          ),
          "uuid is invalid /items/{item-id}/lists/{list-id} (delete)" to ValidationTest(
            httpMethod = HttpMethod.DELETE,
            instance = "/items/${invalidUuids[0]}/lists/${invalidUuids[1]}",
            responseMessage = "$VALIDATION_FAILURE_MESSAGE itemId, listId.",
          ),
          "uuid is invalid /items/{item-id}/lists/count (get)" to ValidationTest(
            httpMethod = HttpMethod.GET,
            instance = "/items/${invalidUuids[0]}/lists/count",
            responseMessage = "$VALIDATION_FAILURE_MESSAGE itemId.",
          ),
          "uuid is invalid /items/{id}/lists/{current-list-id}/{destination-list-id} (patch)" to ValidationTest(
            httpMethod = HttpMethod.PATCH,
            instance = "/items/${invalidUuids[0]}/lists/${invalidUuids[1]}/${invalidUuids[2]}",
            responseMessage = "$VALIDATION_FAILURE_MESSAGE currentListId, destinationListId, itemId.",
          ),
        )

        conditions.forEach { condition ->
          it("-> ${condition.key}") {
            doValidationTest(condition)
          }
        }
      }

      describe("invoke validation failures") {
        it("item is created") {
          val instance = "/items"
          val response = mockMvc.post(instance) {
            with(jwt())
            with(csrf())
            content = Json.encodeToString(createLrmItemOneRequest())
            contentType = MediaType.APPLICATION_JSON
          }.andExpect { status { isOk() } }.andReturn().response.contentAsString
          val id = Json.decodeFromString<ResponseSuccess<LrmItemResponse>>(response).content.id
          itemUuids[0] = id
        }

        val conditions: Map<String, ValidationTest> = mapOf(
          "(patch) name is only whitespace" to
            ValidationTest(
              expectedErrorCount = 1,
              httpMethod = HttpMethod.PATCH,
              instance = "/items/${itemUuids[0]}",
              requestContent = "{ \"name\": \" \" }",
              responseMessage = "$VALIDATION_FAILURE_MESSAGE name.",
            ),
          "(patch) quantity is less than 0" to
            ValidationTest(
              expectedErrorCount = 1,
              httpMethod = HttpMethod.PATCH,
              instance = "/items/${itemUuids[0]}",
              requestContent = "{ \"quantity\": -1 }",
              responseMessage = "$VALIDATION_FAILURE_MESSAGE quantity.",
            ),
          // only first field to be patched is validated and fails, exiting patch process. description is never validated
          "(patch) name is only whitespace, description is empty" to
            ValidationTest(
              expectedErrorCount = 1,
              httpMethod = HttpMethod.PATCH,
              instance = "/items/${itemUuids[0]}",
              requestContent = "{ \"name\": \" \", \"description\": \" \" }",
              responseMessage = "$VALIDATION_FAILURE_MESSAGE name.",
            ),
        )

        conditions.forEach { condition ->
          it("-> ${condition.key}") {
            doValidationTest(condition)
          }
        }

        it("item is deleted") {
          val instance = "/items/${itemUuids[0]}"
          mockMvc.delete(instance) {
            with(jwt())
            with(csrf())
            contentType = MediaType.APPLICATION_JSON
          }.andExpect {
            status { isOk() }
          }
        }
      }
    }

    describe("/lists") {
      describe("invoke validation failures") {
        val conditions: Map<String, ValidationTest> = mapOf(
          "(post) name is null" to
            ValidationTest(
              httpMethod = HttpMethod.POST,
              instance = "/lists",
              expectedErrorCount = 0,
              requestContent = "{ \"name\": null, \"description\": null, \"public\": true }",
              responseMessage = "Failed to read request.",
            ),
          "(post) name is only whitespace" to
            ValidationTest(
              httpMethod = HttpMethod.POST,
              instance = "/lists",
              expectedErrorCount = 1,
              requestContent = "{ \"name\": \" \", \"description\": \"bLLh|Rvz.x0@W2d9G:a\", \"public\": true }",
              responseMessage = "$VALIDATION_FAILURE_MESSAGE name.",
            ),
          "(post) name is empty, description is only whitespace" to
            ValidationTest(
              httpMethod = HttpMethod.POST,
              instance = "/lists",
              expectedErrorCount = 3,
              requestContent = "{ \"name\": \"\", \"description\": \" \", \"public\": true }",
              responseMessage = "$VALIDATION_FAILURE_MESSAGE description, name.",
            ),
        )

        conditions.forEach { condition ->
          it("-> ${condition.key}") {
            doValidationTest(condition)
          }
        }
      }
    }

    describe("/lists/{list-id}") {
      describe("invoke path variable validation failures") {
        val conditions: Map<String, ValidationTest> = mapOf(
          "uuid is invalid /lists/{list-id} (delete)" to ValidationTest(
            httpMethod = HttpMethod.DELETE,
            instance = "/lists/${invalidUuids[0]}",
            responseMessage = "$VALIDATION_FAILURE_MESSAGE listId.",
          ),
          "uuid is invalid /lists/{list-id} (get)" to ValidationTest(
            httpMethod = HttpMethod.GET,
            instance = "/lists/${invalidUuids[0]}",
            responseMessage = "$VALIDATION_FAILURE_MESSAGE listId.",
          ),
          "uuid is invalid /lists/{list-id} (patch)" to ValidationTest(
            httpMethod = HttpMethod.PATCH,
            instance = "/lists/${invalidUuids[0]}",
            requestContent = "{ \"name\": \" \" }",
            responseMessage = "$VALIDATION_FAILURE_MESSAGE listId.",
          ),
          "uuid is invalid /lists/{list-id}/items (delete)" to ValidationTest(
            httpMethod = HttpMethod.DELETE,
            instance = "/lists/${invalidUuids[0]}/items",
            responseMessage = "$VALIDATION_FAILURE_MESSAGE listId.",
          ),
          "uuid is invalid /lists/{list-id}/items/{item-id} (delete)" to ValidationTest(
            httpMethod = HttpMethod.DELETE,
            instance = "/lists/${invalidUuids[0]}/items/${invalidUuids[1]}",
            responseMessage = "$VALIDATION_FAILURE_MESSAGE itemId, listId.",
          ),
          "uuid is invalid /lists/{list-id}/items/count (get)" to ValidationTest(
            httpMethod = HttpMethod.GET,
            instance = "/lists/${invalidUuids[0]}/items/count",
            responseMessage = "$VALIDATION_FAILURE_MESSAGE listId.",
          ),
        )

        conditions.forEach { condition ->
          it("-> ${condition.key}") {
            doValidationTest(condition)
          }
        }
      }
    }
  })
