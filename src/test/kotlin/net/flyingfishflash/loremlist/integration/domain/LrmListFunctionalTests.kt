package net.flyingfishflash.loremlist.integration.domain

import io.kotest.core.spec.style.DescribeSpec
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.flyingfishflash.loremlist.core.response.advice.ApiExceptionHandler.Companion.VALIDATION_FAILURE_MESSAGE
import net.flyingfishflash.loremlist.core.response.structure.DispositionOfProblem
import net.flyingfishflash.loremlist.core.response.structure.DispositionOfSuccess
import net.flyingfishflash.loremlist.domain.association.data.ItemToListAssociationUpdateRequest
import net.flyingfishflash.loremlist.domain.lrmitem.data.LrmItemRequest
import net.flyingfishflash.loremlist.domain.lrmlist.data.LrmListRequest
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post

/**
 * LrmList Integration/Functional Tests
 */
@SpringBootTest
@ActiveProfiles("h2")
@AutoConfigureMockMvc
class LrmListFunctionalTests(mockMvc: MockMvc) : DescribeSpec({

  data class ValidationTest(val postContent: String, val responseMessage: String, val expectedErrorCount: Int)

  val itemOneId: Long = 1
  fun createLrmItemOneRequest(): LrmItemRequest = LrmItemRequest("Lorem Item One Name", "Lorem Item One Description", 0)
  fun updateLrmItemOneRequest(): LrmItemRequest = LrmItemRequest("Updated Lorem Item One Name", "Updated Lorem Item One Description", 1001)

  val itemTwoId: Long = 2
  fun createLrmItemTwoRequest(): LrmItemRequest = LrmItemRequest("Lorem Item Two Name", "Lorem Item Two Description", 0)
  fun updateLrmItemTwoRequest(): LrmItemRequest = LrmItemRequest("Updated Lorem Item Two Name", "Updated Lorem Item Two Description", 1002)

  val itemThreeId: Long = 3
  fun createLrmItemThreeRequest(): LrmItemRequest = LrmItemRequest("Lorem Item Three Name", "Lorem Item Three Description", 0)
  fun updateLrmItemThreeRequest(): LrmItemRequest = LrmItemRequest(
    "Updated Lorem Item Three Name",
    "Updated Lorem Item Three Description",
    1003,
  )

  val itemCreateRequests = listOf(createLrmItemOneRequest(), createLrmItemTwoRequest(), createLrmItemThreeRequest())
  val itemUpdateRequests = listOf(updateLrmItemOneRequest(), updateLrmItemTwoRequest(), updateLrmItemThreeRequest())

  val listOneId: Long = 1
  fun createLrmListOneRequest(): LrmListRequest = LrmListRequest("Lorem List One Name", "Lorem List One Description")
  fun updateLrmListOneRequest(): LrmListRequest = LrmListRequest("Updated Lorem List One Name", "Updated Lorem List One Description")

  val listTwoId: Long = 2
  fun createLrmListTwoRequest(): LrmListRequest = LrmListRequest("Lorem List Two Name", "Lorem List Two Description")
  fun updateLrmListTwoRequest(): LrmListRequest = LrmListRequest("Updated Lorem List Two Name", "Updated Lorem List Two Description")

  val listCreateRequests = listOf(createLrmListOneRequest(), createLrmListTwoRequest())
  val listUpdateRequests = listOf(updateLrmListOneRequest(), updateLrmListTwoRequest())

  describe("comprehensive functional test") {
    describe("management") {
      it("health") {
        val instance = "/management/health"
        mockMvc.get(instance) {
          contentType = MediaType.APPLICATION_JSON
        }.andExpect {
          status { isOk() }
          jsonPath("$.length()") { value(1) }
          jsonPath("$.status") { value("UP") }
        }
      }

      it("info") {
        val instance = "/management/info"
        mockMvc.get(instance) {
          contentType = MediaType.APPLICATION_JSON
        }.andExpect {
          status { isOk() }
          jsonPath("$.build.length()") { value(8) }
          jsonPath("$.build.name") { value("lorem-list api") }
        }
      }
    }

    describe("item: create, read, update") {
      describe("create") {
        describe("invoke validation failures") {

          val conditions: Map<String, ValidationTest> = mapOf(
            "name is null" to
              ValidationTest(
                postContent = "{ \"name\": null, \"description\": null, \"quantity\": 1073741824 }",
                responseMessage = "Failed to read request.",
                expectedErrorCount = 0,
              ),
            "name is only whitespace" to
              ValidationTest(
                postContent = "{ \"name\": \" \", \"description\": \"bLLh|Rvz.x0@W2d9G:a\", \"quantity\": 1073741824 }",
                responseMessage = "$VALIDATION_FAILURE_MESSAGE name.",
                expectedErrorCount = 1,
              ),
            "quantity is less than 0" to
              ValidationTest(
                postContent = "{ \"name\": \"Lorem Ipsum\", \"description\": \"bLLh|Rvz.x0@W2d9G:a\", \"quantity\": -101 }",
                responseMessage = "$VALIDATION_FAILURE_MESSAGE quantity.",
                expectedErrorCount = 1,
              ),
            "description is only whitespace, quantity is less than 0" to
              ValidationTest(
                postContent = "{ \"name\": \"Lorem Ipsum\", \"description\": \" \", \"quantity\": -101 }",
                responseMessage = "$VALIDATION_FAILURE_MESSAGE description, quantity.",
                expectedErrorCount = 2,
              ),
          )

          conditions.forEach { condition ->
            it("condition: ${condition.key}") {
              val instance = "/items"
              mockMvc.post(instance) {
                content = condition.value.postContent
                contentType = MediaType.APPLICATION_JSON
              }.andExpect {
                status { isBadRequest() }
                content { contentType(MediaType.APPLICATION_JSON) }
                jsonPath("$.disposition") { value(DispositionOfProblem.FAILURE.nameAsLowercase()) }
                jsonPath("$.method") { value(HttpMethod.POST.name().lowercase()) }
                jsonPath("$.message") { value(condition.value.responseMessage) }
                jsonPath("$.instance") { value(instance) }
                jsonPath("$.size") { value(1) }
                jsonPath("$.content.length()") { value(5) }
                if (condition.value.expectedErrorCount > 0) {
                  jsonPath("$.content.extensions.validationErrors.length()") { value(condition.value.expectedErrorCount) }
                  jsonPath("$.content.extensions.validationErrors[*]") { isArray() }
                }
              }
            }
          }
        }

        itemCreateRequests.forEachIndexed { index, itemRequest ->
          it("item ${index + 1} is created") {
            val instance = "/items"
            mockMvc.post(instance) {
              content = Json.encodeToString(itemRequest)
              contentType = MediaType.APPLICATION_JSON
            }.andExpect {
              status { isOk() }
              content { contentType(MediaType.APPLICATION_JSON) }
              jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
              jsonPath("$.method") { value(HttpMethod.POST.name().lowercase()) }
              jsonPath("$.message") { value("created new item") }
              jsonPath("$.instance") { value(instance) }
              jsonPath("$.size") { value(1) }
              jsonPath("$.content.length()") { value(7) }
              jsonPath("$.content.id") { value(index + 1) }
              jsonPath("$.content.uuid") { isNotEmpty() }
              jsonPath("$.content.name") { value(itemRequest.name) }
              jsonPath("$.content.description") { value(itemRequest.description) }
              jsonPath("$.content.quantity") { value(itemRequest.quantity) }
              jsonPath("$.content.created") { isNotEmpty() }
              jsonPath("$.content.updated") { isNotEmpty() }
            }
          }
        }
      }

      describe("read") {
        it("item is not found") {
          val instance = "/items/999"
          mockMvc.get(instance) {
            contentType = MediaType.APPLICATION_JSON
          }.andExpect {
            status { isNotFound() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.disposition") { value(DispositionOfProblem.FAILURE.nameAsLowercase()) }
            jsonPath("$.method") { value(HttpMethod.GET.name().lowercase()) }
            jsonPath("$.message") { value("Item id 999 could not be found.") }
            jsonPath("$.instance") { value(instance) }
            jsonPath("$.size") { value(1) }
            jsonPath("$.content.length()") { value(4) }
            jsonPath("$.content.title") { value("Item Not Found Exception") }
            jsonPath("$.content.status") { value("404") }
            jsonPath("$.content.detail") { value("Item id 999 could not be found.") }
          }
        }

        itemCreateRequests.forEachIndexed { index, itemRequest ->
          it("item ${index + 1} is found") {
            val instance = "/items/${index + 1}"
            mockMvc.get(instance) {
              contentType = MediaType.APPLICATION_JSON
            }.andExpect {
              status { isOk() }
              content { contentType(MediaType.APPLICATION_JSON) }
              jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
              jsonPath("$.method") { value(HttpMethod.GET.name().lowercase()) }
              jsonPath("$.message") { value("retrieved item id ${index + 1}") }
              jsonPath("$.instance") { value(instance) }
              jsonPath("$.size") { value(1) }
              jsonPath("$.content.length()") { value(7) }
              jsonPath("$.content.id") { value(index + 1) }
              jsonPath("$.content.uuid") { isNotEmpty() }
              jsonPath("$.content.name") { value(itemRequest.name) }
              jsonPath("$.content.description") { value(itemRequest.description) }
              jsonPath("$.content.quantity") { value(itemRequest.quantity) }
              jsonPath("$.content.created") { isNotEmpty() }
              jsonPath("$.content.updated") { isNotEmpty() }
              jsonPath("$.content.lists") { doesNotExist() }
            }
          }
        }

        itemCreateRequests.forEachIndexed { index, itemRequest ->
          it("item ${index + 1} is found with all items") {
            val instance = "/items"
            mockMvc.get(instance) {
              contentType = MediaType.APPLICATION_JSON
            }.andExpect {
              status { isOk() }
              content { contentType(MediaType.APPLICATION_JSON) }
              jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
              jsonPath("$.method") { value(HttpMethod.GET.name().lowercase()) }
              jsonPath("$.message") { value("retrieved all items") }
              jsonPath("$.instance") { value(instance) }
              jsonPath("$.size") { value(itemCreateRequests.size) }
              jsonPath("$.content") { exists() }
              jsonPath("$.content") { isArray() }
              jsonPath("$.content.length()") { value(itemCreateRequests.size) }
              jsonPath("$.content.[$index].id") { value(index + 1) }
              jsonPath("$.content.[$index].uuid") { isNotEmpty() }
              jsonPath("$.content.[$index].name") { value(itemRequest.name) }
              jsonPath("$.content.[$index].description") { value(itemRequest.description) }
              jsonPath("$.content.[$index].quantity") { value(itemRequest.quantity) }
              jsonPath("$.content.[$index].created") { isNotEmpty() }
              jsonPath("$.content.[$index].updated") { isNotEmpty() }
              jsonPath("$.content.[$index].lists") { doesNotExist() }
            }
          }
        }
      }

      describe("update") {
        describe("invoke validation failures") {

          val conditions: Map<String, ValidationTest> = mapOf(
            "name is only whitespace" to
              ValidationTest(
                postContent = "{ \"name\": \" \" }",
                responseMessage = "$VALIDATION_FAILURE_MESSAGE name.",
                expectedErrorCount = 1,
              ),
            "description is empty" to
              ValidationTest(
                postContent = "{ \"description\": \"\" }",
                responseMessage = "$VALIDATION_FAILURE_MESSAGE description.",
                expectedErrorCount = 2,
              ),
            "quantity is less than 0" to
              ValidationTest(
                postContent = "{ \"quantity\": -1 }",
                responseMessage = "$VALIDATION_FAILURE_MESSAGE quantity.",
                expectedErrorCount = 1,
              ),
            "name is only whitespace, description is empty" to
              ValidationTest(
                postContent = "{ \"name\": \" \", \"description\": \"\" }",
                responseMessage = "$VALIDATION_FAILURE_MESSAGE description, name.",
                expectedErrorCount = 3,
              ),
          )

          conditions.forEach { condition ->
            it("condition: ${condition.key}") {
              val instance = "/items/1"
              mockMvc.patch(instance) {
                content = condition.value.postContent
                contentType = MediaType.APPLICATION_JSON
              }.andExpect {
                status { isBadRequest() }
                content { contentType(MediaType.APPLICATION_JSON) }
                jsonPath("$.disposition") { value(DispositionOfProblem.FAILURE.nameAsLowercase()) }
                jsonPath("$.method") { value(HttpMethod.PATCH.name().lowercase()) }
                jsonPath("$.message") { value(condition.value.responseMessage) }
                jsonPath("$.instance") { value(instance) }
                jsonPath("$.size") { value(1) }
                jsonPath("$.content.length()") { value(5) }
                jsonPath("$.content.extensions.validationErrors.length()") { value(condition.value.expectedErrorCount) }
                jsonPath("$.content.extensions.validationErrors[*]") { isArray() }
              }
            }
          }
        }

        itemUpdateRequests.forEachIndexed { index, itemRequest ->
          it("item ${index + 1} is found and updated") {
            val instance = "/items/${index + 1}"
            mockMvc.patch(instance) {
              content = Json.encodeToString(itemRequest)
              contentType = MediaType.APPLICATION_JSON
            }.andExpect {
              status { isOk() }
              content { contentType(MediaType.APPLICATION_JSON) }
              jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
              jsonPath("$.method") { value(HttpMethod.PATCH.name().lowercase()) }
              jsonPath("$.message") { value("patched") }
              jsonPath("$.instance") { value(instance) }
              jsonPath("$.size") { value(1) }
              jsonPath("$.content.length()") { value(7) }
              jsonPath("$.content.id") { value(index + 1) }
              jsonPath("$.content.uuid") { isNotEmpty() }
              jsonPath("$.content.description") { value(itemRequest.description) }
              jsonPath("$.content.name") { value(itemRequest.name) }
              jsonPath("$.content.quantity") { value(itemRequest.quantity) }
              jsonPath("$.content.created") { isNotEmpty() }
              jsonPath("$.content.updated") { isNotEmpty() }
            }
          }
        }
      }

      it("item count is three") {
        val instance = "/items/count"
        mockMvc.get(instance).andExpect {
          status { isOk() }
          content { contentType(MediaType.APPLICATION_JSON) }
          jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
          jsonPath("$.method") { value(HttpMethod.GET.name().lowercase()) }
          jsonPath("$.message") { value("3 items.") }
          jsonPath("$.instance") { value(instance) }
          jsonPath("$.size") { value(1) }
          jsonPath("$.content.length()") { value(1) }
          jsonPath("$.content.value") { value(3) }
        }
      }
    }

    describe("list: create, read, update") {
      describe("create") {
        describe("invoke validation failures") {
//          it("name is null") {
//            val instance = "/lists"
//            mockMvc.post(instance) {
//              content = "{ \"name\": null, \"description\": null }"
//              contentType = MediaType.APPLICATION_JSON
//            }.andExpect {
//              status { isBadRequest() }
//              content { contentType(MediaType.APPLICATION_JSON) }
//              jsonPath("$.disposition") { value(DispositionOfProblem.FAILURE.nameAsLowercase()) }
//              jsonPath("$.method") { value(HttpMethod.POST.name().lowercase()) }
//              jsonPath("$.message") { value("Failed to read request.") }
//              jsonPath("$.instance") { value(instance) }
//              jsonPath("$.size") { value(1) }
//              jsonPath("$.content.length()") { value(5) }
//            }
//          }

          val conditions: Map<String, ValidationTest> = mapOf(
            "name is null" to
              ValidationTest(
                postContent = "{ \"name\": null, \"description\": null }",
                responseMessage = "Failed to read request.",
                expectedErrorCount = 0,
              ),
            "name is only whitespace" to
              ValidationTest(
                postContent = "{ \"name\": \" \", \"description\": \"bLLh|Rvz.x0@W2d9G:a\" }",
                responseMessage = "$VALIDATION_FAILURE_MESSAGE name.",
                expectedErrorCount = 1,
              ),
            "name is empty, description is only whitespace" to
              ValidationTest(
                postContent = "{ \"name\": \"\", \"description\": \" \" }",
                responseMessage = "$VALIDATION_FAILURE_MESSAGE description, name.",
                expectedErrorCount = 3,
              ),
          )

          conditions.forEach { condition ->
            it("condition: ${condition.key}") {
              val instance = "/lists"
              mockMvc.post(instance) {
                content = condition.value.postContent
                contentType = MediaType.APPLICATION_JSON
              }.andExpect {
                status { isBadRequest() }
                content { contentType(MediaType.APPLICATION_JSON) }
                jsonPath("$.disposition") { value(DispositionOfProblem.FAILURE.nameAsLowercase()) }
                jsonPath("$.method") { value(HttpMethod.POST.name().lowercase()) }
                jsonPath("$.message") { value(condition.value.responseMessage) }
                jsonPath("$.instance") { value(instance) }
                jsonPath("$.size") { value(1) }
                jsonPath("$.content.length()") { value(5) }
                if (condition.value.expectedErrorCount > 0) {
                  jsonPath("$.content.extensions.validationErrors.length()") { value(condition.value.expectedErrorCount) }
                  jsonPath("$.content.extensions.validationErrors[*]") { isArray() }
                }
              }
            }
          }
        }

        listCreateRequests.forEachIndexed { index, listRequest ->
          it("list ${index + 1} is created") {
            val instance = "/lists"
            mockMvc.post(instance) {
              content = Json.encodeToString(listRequest)
              contentType = MediaType.APPLICATION_JSON
            }.andExpect {
              status { isOk() }
              content { contentType(MediaType.APPLICATION_JSON) }
              jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
              jsonPath("$.method") { value(HttpMethod.POST.name().lowercase()) }
              jsonPath("$.message") { value("created new list") }
              jsonPath("$.instance") { value(instance) }
              jsonPath("$.size") { value(1) }
              jsonPath("$.content.length()") { value(6) }
              jsonPath("$.content.id") { value(index + 1) }
              jsonPath("$.content.uuid") { isNotEmpty() }
              jsonPath("$.content.name") { value(listRequest.name) }
              jsonPath("$.content.description") { value(listRequest.description) }
              jsonPath("$.content.created") { isNotEmpty() }
              jsonPath("$.content.updated") { isNotEmpty() }
            }
          }
        }
      }

      describe("read") {
        it("list is not found") {
          val instance = "/lists/999"
          mockMvc.get(instance) {
            contentType = MediaType.APPLICATION_JSON
          }.andExpect {
            status { isNotFound() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.disposition") { value(DispositionOfProblem.FAILURE.nameAsLowercase()) }
            jsonPath("$.method") { value(HttpMethod.GET.name().lowercase()) }
            jsonPath("$.message") { value("List id 999 could not be found.") }
            jsonPath("$.instance") { value(instance) }
            jsonPath("$.size") { value(1) }
            jsonPath("$.content.length()") { value(4) }
            jsonPath("$.content.title") { value("List Not Found Exception") }
            jsonPath("$.content.status") { value("404") }
            jsonPath("$.content.detail") { value("List id 999 could not be found.") }
          }
        }

        listCreateRequests.forEachIndexed { index, listRequest ->
          it("list ${index + 1} is found") {
            val instance = "/lists/${index + 1}"
            mockMvc.get(instance) {
              contentType = MediaType.APPLICATION_JSON
            }.andExpect {
              status { isOk() }
              content { contentType(MediaType.APPLICATION_JSON) }
              jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
              jsonPath("$.method") { value(HttpMethod.GET.name().lowercase()) }
              jsonPath("$.message") { value("retrieved list id ${index + 1}") }
              jsonPath("$.instance") { value(instance) }
              jsonPath("$.size") { value(1) }
              jsonPath("$.content.length()") { value(6) }
              jsonPath("$.content.id") { value(index + 1) }
              jsonPath("$.content.uuid") { isNotEmpty() }
              jsonPath("$.content.name") { value(listRequest.name) }
              jsonPath("$.content.description") { value(listRequest.description) }
              jsonPath("$.content.created") { isNotEmpty() }
              jsonPath("$.content.updated") { isNotEmpty() }
              jsonPath("$.content.lists") { doesNotExist() }
            }
          }

          it("list ${index + 1} is found with all lists") {
            val instance = "/lists"
            mockMvc.get(instance) {
              contentType = MediaType.APPLICATION_JSON
            }.andExpect {
              status { isOk() }
              content { contentType(MediaType.APPLICATION_JSON) }
              jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
              jsonPath("$.method") { value(HttpMethod.GET.name().lowercase()) }
              jsonPath("$.message") { value("retrieved all lists") }
              jsonPath("$.instance") { value(instance) }
              jsonPath("$.size") { value(listCreateRequests.size) }
              jsonPath("$.content") { exists() }
              jsonPath("$.content") { isArray() }
              jsonPath("$.content.length()") { value(listCreateRequests.size) }
              jsonPath("$.content.[$index].id") { value(index + 1) }
              jsonPath("$.content.[$index].uuid") { isNotEmpty() }
              jsonPath("$.content.[$index].name") { value(listRequest.name) }
              jsonPath("$.content.[$index].description") { value(listRequest.description) }
              jsonPath("$.content.[$index].created") { isNotEmpty() }
              jsonPath("$.content.[$index].updated") { isNotEmpty() }
              jsonPath("$.content.[$index].lists") { doesNotExist() }
            }
          }
        }
      }

      describe("update") {
        describe("invoke validation failures") {

          val conditions: Map<String, ValidationTest> = mapOf(
            "name is only whitespace" to
              ValidationTest(
                postContent = "{ \"name\": \" \" }",
                responseMessage = "$VALIDATION_FAILURE_MESSAGE name.",
                expectedErrorCount = 1,
              ),
            "description is only whitespace" to
              ValidationTest(
                postContent = "{ \"description\": \"\" }",
                responseMessage = "$VALIDATION_FAILURE_MESSAGE description.",
                expectedErrorCount = 2,
              ),
            "name is only whitespace, description is only whitespace" to
              ValidationTest(
                postContent = "{ \"name\": \" \", \"description\": \"\" }",
                responseMessage = "$VALIDATION_FAILURE_MESSAGE description, name.",
                expectedErrorCount = 3,
              ),
          )

          conditions.forEach { condition ->
            it("condition: ${condition.key}") {
              val instance = "/lists/1"
              mockMvc.patch(instance) {
                content = condition.value.postContent
                contentType = MediaType.APPLICATION_JSON
              }.andExpect {
                status { isBadRequest() }
                content { contentType(MediaType.APPLICATION_JSON) }
                jsonPath("$.disposition") { value(DispositionOfProblem.FAILURE.nameAsLowercase()) }
                jsonPath("$.method") { value(HttpMethod.PATCH.name().lowercase()) }
                jsonPath("$.message") { value(condition.value.responseMessage) }
                jsonPath("$.instance") { value(instance) }
                jsonPath("$.size") { value(1) }
                jsonPath("$.content.length()") { value(5) }
                jsonPath("$.content.extensions.validationErrors.length()") { value(condition.value.expectedErrorCount) }
                jsonPath("$.content.extensions.validationErrors[*]") { isArray() }
              }
            }
          }
        }

        listUpdateRequests.forEachIndexed { index, listRequest ->
          it("list ${index + 1} is updated") {
            val instance = "/lists/${index + 1}"
            mockMvc.patch(instance) {
              content = Json.encodeToString(listRequest)
              contentType = MediaType.APPLICATION_JSON
            }.andExpect {
              status { isOk() }
              content { contentType(MediaType.APPLICATION_JSON) }
              jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
              jsonPath("$.method") { value(HttpMethod.PATCH.name().lowercase()) }
              jsonPath("$.message") { value("patched") }
              jsonPath("$.instance") { value(instance) }
              jsonPath("$.size") { value(1) }
              jsonPath("$.content.length()") { value(6) }
              jsonPath("$.content.id") { value(index + 1) }
              jsonPath("$.content.uuid") { isNotEmpty() }
              jsonPath("$.content.description") { value(listRequest.description) }
              jsonPath("$.content.name") { value(listRequest.name) }
              jsonPath("$.content.created") { isNotEmpty() }
              jsonPath("$.content.updated") { isNotEmpty() }
            }
          }

          it("list ${index + 1} is up-to-date") {
            val instance = "/lists/${index + 1}"
            mockMvc.patch(instance) {
              content = Json.encodeToString(listRequest)
              contentType = MediaType.APPLICATION_JSON
            }.andExpect {
              status { isNoContent() }
              content { contentType(MediaType.APPLICATION_JSON) }
              jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
              jsonPath("$.method") { value(HttpMethod.PATCH.name().lowercase()) }
              jsonPath("$.message") { value("not patched") }
              jsonPath("$.instance") { value(instance) }
              jsonPath("$.size") { value(1) }
              jsonPath("$.content.length()") { value(6) }
              jsonPath("$.content.id") { value(index + 1) }
              jsonPath("$.content.uuid") { isNotEmpty() }
              jsonPath("$.content.description") { value(listRequest.description) }
              jsonPath("$.content.name") { value(listRequest.name) }
              jsonPath("$.content.created") { isNotEmpty() }
              jsonPath("$.content.updated") { isNotEmpty() }
            }
          }
        }
      }

      it("list count is two") {
        val instance = "/lists/count"
        mockMvc.get(instance).andExpect {
          status { isOk() }
          content { contentType(MediaType.APPLICATION_JSON) }
          jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
          jsonPath("$.method") { value(HttpMethod.GET.name().lowercase()) }
          jsonPath("$.message") { value("2 lists.") }
          jsonPath("$.instance") { value(instance) }
          jsonPath("$.size") { value(1) }
          jsonPath("$.content.length()") { value(1) }
          jsonPath("$.content.value") { value(2) }
        }
      }
    }

    describe("item -> list association: create, read, update, delete") {
      describe("invoke validation failures") {
        it("count: item id must be greater than zero") {
          val instance = "/items/0/list-associations/count"
          mockMvc.get(instance) {
            contentType = MediaType.APPLICATION_JSON
          }.andExpect {
            status { isBadRequest() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.disposition") { value(DispositionOfProblem.FAILURE.nameAsLowercase()) }
            jsonPath("$.method") { value(HttpMethod.GET.name().lowercase()) }
            jsonPath("$.message") { value("$VALIDATION_FAILURE_MESSAGE id.") }
            jsonPath("$.instance") { value(instance) }
            jsonPath("$.size") { value(1) }
            jsonPath("$.content.length()") { value(5) }
            jsonPath("$.content.extensions.validationErrors.length()") { value(1) }
          }
        }

        it("create: list id must be greater than zero") {
          val instance = "/items/$itemOneId/list-associations"
          mockMvc.post(instance) {
            content = Json.encodeToString(0)
            contentType = MediaType.APPLICATION_JSON
          }.andExpect {
            status { isBadRequest() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.disposition") { value(DispositionOfProblem.FAILURE.nameAsLowercase()) }
            jsonPath("$.method") { value(HttpMethod.POST.name().lowercase()) }
            jsonPath("$.message") { value("$VALIDATION_FAILURE_MESSAGE listId.") }
            jsonPath("$.instance") { value(instance) }
            jsonPath("$.size") { value(1) }
            jsonPath("$.content.length()") { value(5) }
            jsonPath("$.content.extensions.validationErrors.length()") { value(1) }
          }
        }

        it("update: source and destination list id's must be greater than 0") {
          val instance = "/items/$itemOneId/list-associations"
          mockMvc.patch(instance) {
            content = Json.encodeToString(ItemToListAssociationUpdateRequest(fromListId = 0, toListId = -1))
            contentType = MediaType.APPLICATION_JSON
          }.andExpect {
            status { isBadRequest() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.disposition") { value(DispositionOfProblem.FAILURE.nameAsLowercase()) }
            jsonPath("$.method") { value(HttpMethod.PATCH.name().lowercase()) }
            jsonPath("$.message") { value("$VALIDATION_FAILURE_MESSAGE fromListId, toListId.") }
            jsonPath("$.instance") { value(instance) }
            jsonPath("$.size") { value(1) }
            jsonPath("$.content.length()") { value(5) }
            jsonPath("$.content.extensions.validationErrors.length()") { value(2) }
          }
        }

        it("delete: list id must be greater than zero") {
          val instance = "/items/$itemOneId/list-associations"
          mockMvc.delete(instance) {
            content = Json.encodeToString(0)
            contentType = MediaType.APPLICATION_JSON
          }.andExpect {
            status { isBadRequest() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.disposition") { value(DispositionOfProblem.FAILURE.nameAsLowercase()) }
            jsonPath("$.method") { value(HttpMethod.DELETE.name().lowercase()) }
            jsonPath("$.message") { value("$VALIDATION_FAILURE_MESSAGE listId.") }
            jsonPath("$.instance") { value(instance) }
            jsonPath("$.size") { value(1) }
            jsonPath("$.content.length()") { value(5) }
            jsonPath("$.content.extensions.validationErrors.length()") { value(1) }
          }
        }
      }

      it("item 1 is added to list 1") {
        val instance = "/items/$itemOneId/list-associations"
        mockMvc.post(instance) {
          content = Json.encodeToString(listOneId)
          contentType = MediaType.APPLICATION_JSON
        }.andExpect {
          status { isOk() }
          content { contentType(MediaType.APPLICATION_JSON) }
          jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
          jsonPath("$.method") { value(HttpMethod.POST.name().lowercase()) }
          jsonPath("$.message") { value("Assigned item '${updateLrmItemOneRequest().name}' to list '${updateLrmListOneRequest().name}'.") }
          jsonPath("$.instance") { value(instance) }
          jsonPath("$.size") { value(1) }
          jsonPath("$.content.message") {
            value(
              "Assigned item '${updateLrmItemOneRequest().name}' to list '${updateLrmListOneRequest().name}'.",
            )
          }
        }
      }

      it("list 1 includes item 1") {
        val instance = "/lists/$listOneId?includeItems=true"
        mockMvc.get(instance) {
          contentType = MediaType.APPLICATION_JSON
        }.andExpect {
          status { isOk() }
          content { contentType(MediaType.APPLICATION_JSON) }
          jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
          jsonPath("$.method") { value(HttpMethod.GET.name().lowercase()) }
          jsonPath("$.message") { value("retrieved list id $listOneId") }
          jsonPath("$.instance") { value(instance.substringBeforeLast("?").removeSuffix(instance)) }
          jsonPath("$.size") { value(1) }
          jsonPath("$.content.id") { value(listOneId) }
          jsonPath("$.content.uuid") { isNotEmpty() }
          jsonPath("$.content.name") { value(updateLrmListOneRequest().name) }
          jsonPath("$.content.description") { value(updateLrmListOneRequest().description) }
          jsonPath("$.content.created") { isNotEmpty() }
          jsonPath("$.content.updated") { isNotEmpty() }
          jsonPath("$.content.items.length()") { value(1) }
          jsonPath("$.content.items.[0].name") { value(updateLrmItemOneRequest().name) }
          jsonPath("$.content.items.[0].description") { value(updateLrmItemOneRequest().description) }
          jsonPath("$.content.items.[0].quantity") { value(updateLrmItemOneRequest().quantity) }
        }
      }

      it("item 2 is added to list 1") {
        val instance = "/items/$itemTwoId/list-associations"
        mockMvc.post(instance) {
          content = Json.encodeToString(listOneId)
          contentType = MediaType.APPLICATION_JSON
        }.andExpect {
          status { isOk() }
          content { contentType(MediaType.APPLICATION_JSON) }
          jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
          jsonPath("$.method") { value(HttpMethod.POST.name().lowercase()) }
          jsonPath("$.message") { value("Assigned item '${updateLrmItemTwoRequest().name}' to list '${updateLrmListOneRequest().name}'.") }
          jsonPath("$.instance") { value(instance) }
          jsonPath("$.size") { value(1) }
          jsonPath("$.content.message") {
            value(
              "Assigned item '${updateLrmItemTwoRequest().name}' to list '${updateLrmListOneRequest().name}'.",
            )
          }
        }
      }

      it("list 1 includes items 1 and 2") {
        val instance = "/lists/$listOneId?includeItems=true"
        mockMvc.get(instance) {
          contentType = MediaType.APPLICATION_JSON
        }.andExpect {
          status { isOk() }
          content { contentType(MediaType.APPLICATION_JSON) }
          jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
          jsonPath("$.method") { value(HttpMethod.GET.name().lowercase()) }
          jsonPath("$.message") { value("retrieved list id $listOneId") }
          jsonPath("$.instance") { value(instance.substringBeforeLast("?").removeSuffix(instance)) }
          jsonPath("$.size") { value(1) }
          jsonPath("$.content.id") { value(listOneId) }
          jsonPath("$.content.uuid") { isNotEmpty() }
          jsonPath("$.content.name") { value(updateLrmListOneRequest().name) }
          jsonPath("$.content.description") { value(updateLrmListOneRequest().description) }
          jsonPath("$.content.created") { isNotEmpty() }
          jsonPath("$.content.updated") { isNotEmpty() }
          jsonPath("$.content.items.length()") { value(2) }
          jsonPath("$.content.items.[0].name") { value(updateLrmItemOneRequest().name) }
          jsonPath("$.content.items.[0].description") { value(updateLrmItemOneRequest().description) }
          jsonPath("$.content.items.[0].quantity") { value(updateLrmItemOneRequest().quantity) }
          jsonPath("$.content.items.[1].name") { value(updateLrmItemTwoRequest().name) }
          jsonPath("$.content.items.[1].description") { value(updateLrmItemTwoRequest().description) }
          jsonPath("$.content.items.[1].quantity") { value(updateLrmItemTwoRequest().quantity) }
        }
      }

      it("item 1 is not added to list 1 when it's already been added") {
        val instance = "/items/$itemOneId/list-associations"
        mockMvc.post(instance) {
          content = Json.encodeToString(listOneId)
          contentType = MediaType.APPLICATION_JSON
        }.andExpect {
          status { isUnprocessableEntity() }
          content { contentType(MediaType.APPLICATION_JSON) }
          jsonPath("$.disposition") { value(DispositionOfProblem.FAILURE.nameAsLowercase()) }
          jsonPath("$.method") { value(HttpMethod.POST.name().lowercase()) }
          jsonPath("$.message") { value("Item id $itemOneId could not be added to list id $listOneId: It's already been added.") }
          jsonPath("$.instance") { value(instance) }
          jsonPath("$.size") { value(1) }
          jsonPath("$.content.length()") { value(4) }
          jsonPath("$.content.status") { value(HttpStatus.UNPROCESSABLE_ENTITY.value()) }
        }
      }

      it("item 999 is not added to list 1 because the item couldn't be found") {
        val instance = "/items/999/list-associations"
        mockMvc.post(instance) {
          content = Json.encodeToString(listOneId)
          contentType = MediaType.APPLICATION_JSON
        }.andExpect {
          status { isNotFound() }
          content { contentType(MediaType.APPLICATION_JSON) }
          jsonPath("$.disposition") { value(DispositionOfProblem.FAILURE.nameAsLowercase()) }
          jsonPath("$.method") { value(HttpMethod.POST.name().lowercase()) }
          jsonPath("$.message") { value("Item id 999 could not be added to list id $listOneId: Item id 999 could not be found.") }
          jsonPath("$.instance") { value(instance) }
          jsonPath("$.size") { value(1) }
          jsonPath("$.content.length()") { value(4) }
          jsonPath("$.content.status") { value(HttpStatus.NOT_FOUND.value()) }
        }
      }

      it("item 1 is not added to list 999 because the list couldn't be found") {
        val instance = "/items/$itemOneId/list-associations"
        mockMvc.post(instance) {
          content = Json.encodeToString(999)
          contentType = MediaType.APPLICATION_JSON
        }.andExpect {
          status { isNotFound() }
          content { contentType(MediaType.APPLICATION_JSON) }
          jsonPath("$.disposition") { value(DispositionOfProblem.FAILURE.nameAsLowercase()) }
          jsonPath("$.method") { value(HttpMethod.POST.name().lowercase()) }
          jsonPath("$.message") { value("Item id $itemOneId could not be added to list id 999: List id 999 could not be found.") }
          jsonPath("$.instance") { value(instance) }
          jsonPath("$.size") { value(1) }
          jsonPath("$.content.length()") { value(4) }
          jsonPath("$.content.status") { value(HttpStatus.NOT_FOUND.value()) }
        }
      }

      it("item 1 is moved from list 1 to list 2") {
        val instance = "/items/$itemOneId/list-associations"
        mockMvc.patch(instance) {
          content = Json.encodeToString(ItemToListAssociationUpdateRequest(fromListId = 1, toListId = 2))
          contentType = MediaType.APPLICATION_JSON
        }.andExpect {
          status { isOk() }
          content { contentType(MediaType.APPLICATION_JSON) }
          jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
          jsonPath("$.method") { value(HttpMethod.PATCH.name().lowercase()) }
          jsonPath("$.message") {
            value("Moved item 'Updated Lorem Item One Name' from list 'Updated Lorem List One Name' to list 'Updated Lorem List Two Name'.")
          }
          jsonPath("$.instance") { value(instance) }
          jsonPath("$.size") { value(1) }
          jsonPath("$.content.message") {
            value("Moved item 'Updated Lorem Item One Name' from list 'Updated Lorem List One Name' to list 'Updated Lorem List Two Name'.")
          }
        }
      }

      it("list 1 includes only item 2") {
        val instance = "/lists/$listOneId?includeItems=true"
        mockMvc.get(instance) {
          contentType = MediaType.APPLICATION_JSON
        }.andExpect {
          status { isOk() }
          content { contentType(MediaType.APPLICATION_JSON) }
          jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
          jsonPath("$.method") { value(HttpMethod.GET.name().lowercase()) }
          jsonPath("$.message") { value("retrieved list id $listOneId") }
          jsonPath("$.instance") { value(instance.substringBeforeLast("?").removeSuffix(instance)) }
          jsonPath("$.size") { value(1) }
          jsonPath("$.content.id") { value(listOneId) }
          jsonPath("$.content.uuid") { isNotEmpty() }
          jsonPath("$.content.name") { value(updateLrmListOneRequest().name) }
          jsonPath("$.content.description") { value(updateLrmListOneRequest().description) }
          jsonPath("$.content.created") { isNotEmpty() }
          jsonPath("$.content.updated") { isNotEmpty() }
          jsonPath("$.content.items.length()") { value(1) }
          jsonPath("$.content.items.[0].length()") { value(7) }
          jsonPath("$.content.items.[0].uuid") { isNotEmpty() }
          jsonPath("$.content.items.[0].name") { value(updateLrmItemTwoRequest().name) }
          jsonPath("$.content.items.[0].description") { value(updateLrmItemTwoRequest().description) }
          jsonPath("$.content.items.[0].quantity") { value(updateLrmItemTwoRequest().quantity) }
          jsonPath("$.content.items.[0].created") { isNotEmpty() }
          jsonPath("$.content.items.[0].updated") { isNotEmpty() }
        }
      }

      it("item 1 is not deleted from list 1 because the association couldn't be found") {
        val instance = "/items/$itemOneId/list-associations"
        mockMvc.delete(instance) {
          content = listOneId
          contentType = MediaType.APPLICATION_JSON
        }.andExpect {
          status { isNotFound() }
          content { contentType(MediaType.APPLICATION_JSON) }
          jsonPath("$.disposition") { value(DispositionOfProblem.FAILURE.nameAsLowercase()) }
          jsonPath("$.method") { value(HttpMethod.DELETE.name().lowercase()) }
          jsonPath("$.instance") { value(instance) }
          jsonPath("$.message") { value("Item id 1 could not be removed from list id 1: Association not found.") }
          jsonPath("$.size") { value(1) }
          jsonPath("$.content.length()") { value(4) }
        }
      }

      it("item 2 is associated with only list 1") {
        val instance = "/items/$itemTwoId?includeLists=true"
        mockMvc.get(instance) {
          contentType = MediaType.APPLICATION_JSON
        }.andExpect {
          status { isOk() }
          content { contentType(MediaType.APPLICATION_JSON) }
          jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
          jsonPath("$.method") { value(HttpMethod.GET.name().lowercase()) }
          jsonPath("$.message") { value("retrieved item id $itemTwoId and it's associated lists") }
          jsonPath("$.instance") { value(instance.substringBeforeLast("?").removeSuffix(instance)) }
          jsonPath("$.size") { value(1) }
          jsonPath("$.content.id") { value(itemTwoId) }
          jsonPath("$.content.uuid") { isNotEmpty() }
          jsonPath("$.content.name") { value(updateLrmItemTwoRequest().name) }
          jsonPath("$.content.description") { value(updateLrmItemTwoRequest().description) }
          jsonPath("$.content.quantity") { value(updateLrmItemTwoRequest().quantity) }
          jsonPath("$.content.created") { isNotEmpty() }
          jsonPath("$.content.updated") { isNotEmpty() }
          jsonPath("$.content.lists.length()") { value(1) }
          jsonPath("$.content.lists.[0].length()") { value(3) }
          jsonPath("$.content.lists.[0].uuid") { isNotEmpty() }
          jsonPath("$.content.lists.[0].name") { value(updateLrmListOneRequest().name) }
        }
      }

      it("list 2 includes only item 1") {
        val instance = "/lists/$listTwoId?includeItems=true"
        mockMvc.get(instance) {
          contentType = MediaType.APPLICATION_JSON
        }.andExpect {
          status { isOk() }
          content { contentType(MediaType.APPLICATION_JSON) }
          jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
          jsonPath("$.method") { value(HttpMethod.GET.name().lowercase()) }
          jsonPath("$.message") { value("retrieved list id $listTwoId") }
          jsonPath("$.instance") { value(instance.substringBeforeLast("?").removeSuffix(instance)) }
          jsonPath("$.size") { value(1) }
          jsonPath("$.content.id") { value(listTwoId) }
          jsonPath("$.content.uuid") { isNotEmpty() }
          jsonPath("$.content.name") { value(updateLrmListTwoRequest().name) }
          jsonPath("$.content.description") { value(updateLrmListTwoRequest().description) }
          jsonPath("$.content.created") { isNotEmpty() }
          jsonPath("$.content.updated") { isNotEmpty() }
          jsonPath("$.content.items.length()") { value(1) }
          jsonPath("$.content.items.[0].length()") { value(7) }
          jsonPath("$.content.items.[0].uuid") { isNotEmpty() }
          jsonPath("$.content.items.[0].name") { value(updateLrmItemOneRequest().name) }
          jsonPath("$.content.items.[0].description") { value(updateLrmItemOneRequest().description) }
          jsonPath("$.content.items.[0].quantity") { value(updateLrmItemOneRequest().quantity) }
          jsonPath("$.content.items.[0].created") { isNotEmpty() }
          jsonPath("$.content.items.[0].updated") { isNotEmpty() }
        }
      }

      it("item 1 is associated with only list 2") {
        val instance = "/items/$itemOneId?includeLists=true"
        mockMvc.get(instance) {
          contentType = MediaType.APPLICATION_JSON
        }.andExpect {
          status { isOk() }
          content { contentType(MediaType.APPLICATION_JSON) }
          jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
          jsonPath("$.method") { value(HttpMethod.GET.name().lowercase()) }
          jsonPath("$.message") { value("retrieved item id $itemOneId and it's associated lists") }
          jsonPath("$.instance") { value(instance.substringBeforeLast("?").removeSuffix(instance)) }
          jsonPath("$.size") { value(1) }
          jsonPath("$.content.id") { value(itemOneId) }
          jsonPath("$.content.uuid") { isNotEmpty() }
          jsonPath("$.content.name") { value(updateLrmItemOneRequest().name) }
          jsonPath("$.content.description") { value(updateLrmItemOneRequest().description) }
          jsonPath("$.content.quantity") { value(updateLrmItemOneRequest().quantity) }
          jsonPath("$.content.created") { isNotEmpty() }
          jsonPath("$.content.updated") { isNotEmpty() }
          jsonPath("$.content.lists.length()") { value(1) }
          jsonPath("$.content.lists.[0].length()") { value(3) }
          jsonPath("$.content.lists.[0].uuid") { isNotEmpty() }
          jsonPath("$.content.lists.[0].name") { value(updateLrmListTwoRequest().name) }
        }
      }

      it("item 1 has one list association") {
        val instance = "/items/$itemOneId/list-associations/count"
        mockMvc.get(instance) {
          contentType = MediaType.APPLICATION_JSON
        }.andExpect {
          status { isOk() }
          content { contentType(MediaType.APPLICATION_JSON) }
          jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
          jsonPath("$.method") { value(HttpMethod.GET.name().lowercase()) }
          jsonPath("$.message") { value("item is associated with 1 lists.") }
          jsonPath("$.instance") { value(instance) }
          jsonPath("$.size") { value(1) }
          jsonPath("$.content.length()") { value(1) }
          jsonPath("$.content.value") { value(1) }
        }
      }

      it("item 2 has one list association") {
        val instance = "/items/$itemTwoId/list-associations/count"
        mockMvc.get(instance) {
          contentType = MediaType.APPLICATION_JSON
        }.andExpect {
          status { isOk() }
          content { contentType(MediaType.APPLICATION_JSON) }
          jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
          jsonPath("$.method") { value(HttpMethod.GET.name().lowercase()) }
          jsonPath("$.message") { value("item is associated with 1 lists.") }
          jsonPath("$.instance") { value(instance) }
          jsonPath("$.size") { value(1) }
          jsonPath("$.content.length()") { value(1) }
          jsonPath("$.content.value") { value(1) }
        }
      }

      it("list 1 has one item association") {
        val instance = "/lists/$listOneId/item-associations/count"
        mockMvc.get(instance) {
          contentType = MediaType.APPLICATION_JSON
        }.andExpect {
          status { isOk() }
          content { contentType(MediaType.APPLICATION_JSON) }
          jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
          jsonPath("$.method") { value(HttpMethod.GET.name().lowercase()) }
          jsonPath("$.message") { value("List is associated with 1 items.") }
          jsonPath("$.instance") { value(instance) }
          jsonPath("$.size") { value(1) }
          jsonPath("$.content.length()") { value(1) }
          jsonPath("$.content.value") { value(1) }
        }
      }

      it("list 2 has one item association") {
        val instance = "/lists/$listTwoId/item-associations/count"
        mockMvc.get(instance) {
          contentType = MediaType.APPLICATION_JSON
        }.andExpect {
          status { isOk() }
          content { contentType(MediaType.APPLICATION_JSON) }
          jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
          jsonPath("$.method") { value(HttpMethod.GET.name().lowercase()) }
          jsonPath("$.message") { value("List is associated with 1 items.") }
          jsonPath("$.instance") { value(instance) }
          jsonPath("$.size") { value(1) }
          jsonPath("$.content.length()") { value(1) }
          jsonPath("$.content.value") { value(1) }
        }
      }

      it("remove item 1 from all lists") {
        val instance = "/items/$itemOneId/list-associations/delete-all"
        mockMvc.delete(instance) {
          contentType = MediaType.APPLICATION_JSON
        }.andExpect {
          status { isOk() }
          content { contentType(MediaType.APPLICATION_JSON) }
          jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
          jsonPath("$.method") { value(HttpMethod.DELETE.name().lowercase()) }
          jsonPath("$.message") { value("Removed item '${updateLrmItemOneRequest().name}' from all associated lists (1).") }
          jsonPath("$.instance") { value(instance) }
          jsonPath("$.size") { value(1) }
          jsonPath("$.content.length()") { value(1) }
          jsonPath("$.content.value") { value(1) }
        }
      }

      it("item 1 has zero list associations") {
        val instance = "/items/$itemOneId/list-associations/count"
        mockMvc.get(instance) {
          contentType = MediaType.APPLICATION_JSON
        }.andExpect {
          status { isOk() }
          content { contentType(MediaType.APPLICATION_JSON) }
          jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
          jsonPath("$.method") { value(HttpMethod.GET.name().lowercase()) }
          jsonPath("$.instance") { value(instance) }
          jsonPath("$.message") { value("item is associated with 0 lists.") }
          jsonPath("$.size") { value(1) }
          jsonPath("$.content.length()") { value(1) }
          jsonPath("$.content.value") { value(0) }
        }
      }
    }

    describe("item delete") {
      it("item 3 has zero list associations") {
        val instance = "/items/$itemThreeId/list-associations/count"
        mockMvc.get(instance) {
          contentType = MediaType.APPLICATION_JSON
        }.andExpect {
          status { isOk() }
          content { contentType(MediaType.APPLICATION_JSON) }
          jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
          jsonPath("$.method") { value(HttpMethod.GET.name().lowercase()) }
          jsonPath("$.instance") { value(instance) }
          jsonPath("$.message") { value("item is associated with 0 lists.") }
          jsonPath("$.size") { value(1) }
          jsonPath("$.content.length()") { value(1) }
          jsonPath("$.content.value") { value(0) }
        }
      }

      it("delete item 3") {
        val instance = "/items/$itemThreeId"
        mockMvc.delete(instance).andExpect {
          status { isOk() }
          content { contentType(MediaType.APPLICATION_JSON) }
          jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
          jsonPath("$.method") { value(HttpMethod.DELETE.name().lowercase()) }
          jsonPath("$.instance") { value(instance) }
          jsonPath("$.message") { value("Deleted item id $itemThreeId.") }
          jsonPath("$.size") { value(1) }
          jsonPath("$.content.length()") { value(2) }
          jsonPath("$.content.countItemToListAssociations") { value(0) }
          jsonPath("$.content.associatedListNames.length()") { value(0) }
        }
      }

      it("item 2 is added to list 2") {
        val instance = "/items/$itemTwoId/list-associations"
        mockMvc.post(instance) {
          content = Json.encodeToString(listTwoId)
          contentType = MediaType.APPLICATION_JSON
        }.andExpect {
          status { isOk() }
          content { contentType(MediaType.APPLICATION_JSON) }
          jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
          jsonPath("$.method") { value(HttpMethod.POST.name().lowercase()) }
          jsonPath("$.message") { value("Assigned item '${updateLrmItemTwoRequest().name}' to list '${updateLrmListTwoRequest().name}'.") }
          jsonPath("$.instance") { value(instance) }
          jsonPath("$.size") { value(1) }
          jsonPath("$.content.message") {
            value(
              "Assigned item '${updateLrmItemTwoRequest().name}' to list '${updateLrmListTwoRequest().name}'.",
            )
          }
        }
      }

      it("item 2 has two list associations") {
        val instance = "/items/$itemTwoId/list-associations/count"
        mockMvc.get(instance) {
          contentType = MediaType.APPLICATION_JSON
        }.andExpect {
          status { isOk() }
          content { contentType(MediaType.APPLICATION_JSON) }
          jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
          jsonPath("$.method") { value(HttpMethod.GET.name().lowercase()) }
          jsonPath("$.instance") { value(instance) }
          jsonPath("$.message") { value("item is associated with 2 lists.") }
          jsonPath("$.size") { value(1) }
          jsonPath("$.content.length()") { value(1) }
          jsonPath("$.content.value") { value(2) }
        }
      }

      it("delete item 2 (removeListAssociations = false)") {
        val instance = "/items/$itemTwoId"
        mockMvc.delete(instance).andExpect {
          // client should be checking for http 422 error and proceed to confirm removal from X lists
          status { isUnprocessableEntity() }
          content { contentType(MediaType.APPLICATION_JSON) }
          jsonPath("$.disposition") { value(DispositionOfProblem.FAILURE.nameAsLowercase()) }
          jsonPath("$.method") { value(HttpMethod.DELETE.name().lowercase()) }
          jsonPath("$.instance") { value(instance) }
          jsonPath("$.message") {
            value(
              "Item id $itemTwoId could not be deleted because it's associated with 2 list(s). " +
                "First remove the item from each list.",
            )
          }
          jsonPath("$.size") { value(1) }
          jsonPath("$.content.detail") {
            value(
              "Item id $itemTwoId could not be deleted because it's associated with 2 list(s). " +
                "First remove the item from each list.",
            )
          }
        }
      }

      it("delete item 2 (removeListAssociations = true)") {
        val instance = "/items/$itemTwoId?removeListAssociations=true"
        mockMvc.delete(instance).andExpect {
          status { isOk() }
          content { contentType(MediaType.APPLICATION_JSON) }
          jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
          jsonPath("$.method") { value(HttpMethod.DELETE.name().lowercase()) }
          jsonPath("$.instance") { value(instance.substringBeforeLast("?").removeSuffix(instance)) }
          jsonPath("$.message") { value("Deleted item id $itemTwoId.") }
          jsonPath("$.size") { value(1) }
          jsonPath("$.content.length()") { value(2) }
          jsonPath("$.content.countItemToListAssociations") { value(2) }
          jsonPath("$.content.associatedListNames.length()") { value(2) }
          jsonPath("$.content.associatedListNames.[0]") { value(updateLrmListOneRequest().name) }
          jsonPath("$.content.associatedListNames.[1]") { value(updateLrmListTwoRequest().name) }
        }
      }

      it("total item count is one") {
        val instance = "/items/count"
        mockMvc.get(instance).andExpect {
          status { isOk() }
          content { contentType(MediaType.APPLICATION_JSON) }
          jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
          jsonPath("$.method") { value(HttpMethod.GET.name().lowercase()) }
          jsonPath("$.message") { value("1 items.") }
          jsonPath("$.instance") { value(instance) }
          jsonPath("$.size") { value(1) }
          jsonPath("$.content.length()") { value(1) }
          jsonPath("$.content.value") { value(1) }
        }
      }
    }

    describe("list delete") {
      it("delete list with no item associations") {
        TODO()
      }

      it("delete list with item associations") {
        TODO()
      }

      it("total list count is zero") {
        val instance = "/lists/count"
        mockMvc.get(instance).andExpect {
          status { isOk() }
          content { contentType(MediaType.APPLICATION_JSON) }
          jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
          jsonPath("$.method") { value(HttpMethod.GET.name().lowercase()) }
          jsonPath("$.message") { value("0 lists.") }
          jsonPath("$.instance") { value(instance) }
          jsonPath("$.size") { value(1) }
          jsonPath("$.content.length()") { value(1) }
          jsonPath("$.content.value") { value(0) }
        }
      }
    }
  }
})
