package net.flyingfishflash.loremlist.integration.domain

import io.kotest.core.spec.style.DescribeSpec
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.flyingfishflash.loremlist.core.exceptions.ApiException
import net.flyingfishflash.loremlist.core.response.advice.ApiExceptionHandler.Companion.VALIDATION_FAILURE_MESSAGE
import net.flyingfishflash.loremlist.core.response.structure.DispositionOfProblem
import net.flyingfishflash.loremlist.core.response.structure.DispositionOfSuccess
import net.flyingfishflash.loremlist.core.response.structure.ResponseSuccess
import net.flyingfishflash.loremlist.core.serialization.UUIDSerializer
import net.flyingfishflash.loremlist.domain.association.AssociationNotFoundException
import net.flyingfishflash.loremlist.domain.association.data.ItemToListAssociationUpdateRequest
import net.flyingfishflash.loremlist.domain.lrmitem.ItemNotFoundException
import net.flyingfishflash.loremlist.domain.lrmitem.LrmItem
import net.flyingfishflash.loremlist.domain.lrmitem.data.LrmItemRequest
import net.flyingfishflash.loremlist.domain.lrmlist.ListNotFoundException
import net.flyingfishflash.loremlist.domain.lrmlist.LrmList
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
import java.util.UUID
import kotlin.collections.HashMap

/**
 * LrmList Integration/Functional Tests
 */
@SpringBootTest
@ActiveProfiles("h2")
@AutoConfigureMockMvc
class LrmListFunctionalTests(mockMvc: MockMvc) : DescribeSpec({

  data class ValidationTest(val postContent: String, val responseMessage: String, val expectedErrorCount: Int)

  fun createLrmItemOneRequest(): LrmItemRequest = LrmItemRequest("Lorem Item One Name", "Lorem Item One Description", 0)
  fun createLrmItemTwoRequest(): LrmItemRequest = LrmItemRequest("Lorem Item Two Name", "Lorem Item Two Description", 0)
  fun createLrmItemThreeRequest(): LrmItemRequest = LrmItemRequest("Lorem Item Three Name", "Lorem Item Three Description", 0)

  val itemCreateRequests = listOf(createLrmItemOneRequest(), createLrmItemTwoRequest(), createLrmItemThreeRequest())

  fun updateLrmItemOneRequest(): LrmItemRequest = LrmItemRequest("Updated Lorem Item One Name", "Updated Lorem Item One Description", 1001)

  fun updateLrmItemTwoRequest(): LrmItemRequest = LrmItemRequest("Updated Lorem Item Two Name", "Updated Lorem Item Two Description", 1002)

  fun updateLrmItemThreeRequest(): LrmItemRequest = LrmItemRequest(
    "Updated Lorem Item Three Name",
    "Updated Lorem Item Three Description",
    1003,
  )

  val itemUpdateRequests = listOf(updateLrmItemOneRequest(), updateLrmItemTwoRequest(), updateLrmItemThreeRequest())

  val itemUuids: MutableMap<Int, UUID> = HashMap()
  val listUuids: MutableMap<Int, UUID> = HashMap()

  fun createLrmListOneRequest(): LrmListRequest = LrmListRequest("Lorem List One Name", "Lorem List One Description")
  fun createLrmListTwoRequest(): LrmListRequest = LrmListRequest("Lorem List Two Name", "Lorem List Two Description")
  val listCreateRequests = listOf(createLrmListOneRequest(), createLrmListTwoRequest())

  fun updateLrmListOneRequest(): LrmListRequest = LrmListRequest("Updated Lorem List One Name", "Updated Lorem List One Description")

  fun updateLrmListTwoRequest(): LrmListRequest = LrmListRequest("Updated Lorem List Two Name", "Updated Lorem List Two Description")

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
        itemCreateRequests.forEachIndexed { index, itemRequest ->
          it("item $index is created") {
            val instance = "/items"
            val response = mockMvc.post(instance) {
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
              jsonPath("$.content.length()") { value(6) }
              jsonPath("$.content.uuid") { isNotEmpty() }
              jsonPath("$.content.name") { value(itemRequest.name) }
              jsonPath("$.content.description") { value(itemRequest.description) }
              jsonPath("$.content.quantity") { value(itemRequest.quantity) }
              jsonPath("$.content.created") { isNotEmpty() }
              jsonPath("$.content.updated") { isNotEmpty() }
            }.andReturn().response.contentAsString
            val uuid = Json.decodeFromString<ResponseSuccess<LrmItem>>(response).content.uuid
            itemUuids[index] = uuid
          }
        }
      }

      describe("read") {
        it("item is not found") {
          val uuid = UUID.randomUUID()
          val instance = "/items/$uuid"
          mockMvc.get(instance) {
            contentType = MediaType.APPLICATION_JSON
          }.andExpect {
            status { isNotFound() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.disposition") { value(DispositionOfProblem.FAILURE.nameAsLowercase()) }
            jsonPath("$.method") { value(HttpMethod.GET.name().lowercase()) }
            jsonPath("$.message") { value("Item id $uuid could not be found.") }
            jsonPath("$.instance") { value(instance) }
            jsonPath("$.size") { value(1) }
            jsonPath("$.content.length()") { value(4) }
            jsonPath("$.content.title") { value(ItemNotFoundException::class.java.simpleName) }
            jsonPath("$.content.status") { value("404") }
            jsonPath("$.content.detail") { value("Item id $uuid could not be found.") }
          }
        }

        itemCreateRequests.forEachIndexed { index, itemRequest ->
          it("item $index is found") {
            val instance = "/items/${itemUuids[index]}"
            mockMvc.get(instance) {
              contentType = MediaType.APPLICATION_JSON
            }.andExpect {
              status { isOk() }
              content { contentType(MediaType.APPLICATION_JSON) }
              jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
              jsonPath("$.method") { value(HttpMethod.GET.name().lowercase()) }
              jsonPath("$.message") { value("retrieved item id ${itemUuids[index]}") }
              jsonPath("$.instance") { value(instance) }
              jsonPath("$.size") { value(1) }
              jsonPath("$.content.length()") { value(6) }
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
          it("item $index is found with all items") {
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
              jsonPath("$.content.[$index].uuid") { value("${itemUuids[index]}") }
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
        itemUpdateRequests.forEachIndexed { index, itemRequest ->
          it("item $index is found and updated") {
            val instance = "/items/${itemUuids[index]}"
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
              jsonPath("$.content.length()") { value(6) }
              jsonPath("$.content.uuid") { value("${itemUuids[index]}") }
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
//                jsonPath("$.content.length()") { value(5) }
                if (condition.value.expectedErrorCount > 0) {
                  jsonPath("$.content.validationErrors.length()") { value(condition.value.expectedErrorCount) }
                  jsonPath("$.content.validationErrors[*]") { isArray() }
                }
              }
            }
          }
        }

        listCreateRequests.forEachIndexed { index, listRequest ->
          it("list $index is created") {
            val instance = "/lists"
            val response = mockMvc.post(instance) {
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
              jsonPath("$.content.length()") { value(5) }
              jsonPath("$.content.uuid") { isNotEmpty() }
              jsonPath("$.content.name") { value(listRequest.name) }
              jsonPath("$.content.description") { value(listRequest.description) }
              jsonPath("$.content.created") { isNotEmpty() }
              jsonPath("$.content.updated") { isNotEmpty() }
            }.andReturn().response.contentAsString
            val uuid = Json.decodeFromString<ResponseSuccess<LrmList>>(response).content.uuid
            listUuids[index] = uuid
          }
        }
      }

      describe("read") {
        it("list is not found") {
          val randomUuid = UUID.randomUUID()
          val instance = "/lists/$randomUuid"
          mockMvc.get(instance) {
            contentType = MediaType.APPLICATION_JSON
          }.andExpect {
            status { isNotFound() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.disposition") { value(DispositionOfProblem.FAILURE.nameAsLowercase()) }
            jsonPath("$.method") { value(HttpMethod.GET.name().lowercase()) }
            jsonPath("$.message") { value("List id $randomUuid could not be found.") }
            jsonPath("$.instance") { value(instance) }
            jsonPath("$.size") { value(1) }
            jsonPath("$.content.length()") { value(4) }
            jsonPath("$.content.title") { value(ListNotFoundException::class.java.simpleName) }
            jsonPath("$.content.status") { value("404") }
            jsonPath("$.content.detail") { value("List id $randomUuid could not be found.") }
          }
        }

        listCreateRequests.forEachIndexed { index, listRequest ->
          it("list $index is found") {
            val instance = "/lists/${listUuids[index]}"
            mockMvc.get(instance) {
              contentType = MediaType.APPLICATION_JSON
            }.andExpect {
              status { isOk() }
              content { contentType(MediaType.APPLICATION_JSON) }
              jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
              jsonPath("$.method") { value(HttpMethod.GET.name().lowercase()) }
              jsonPath("$.message") { value("retrieved list id ${listUuids[index]}") }
              jsonPath("$.instance") { value(instance) }
              jsonPath("$.size") { value(1) }
              jsonPath("$.content.length()") { value(5) }
              jsonPath("$.content.uuid") { isNotEmpty() }
              jsonPath("$.content.name") { value(listRequest.name) }
              jsonPath("$.content.description") { value(listRequest.description) }
              jsonPath("$.content.created") { isNotEmpty() }
              jsonPath("$.content.updated") { isNotEmpty() }
              jsonPath("$.content.lists") { doesNotExist() }
            }
          }

          it("list $index is found with all lists") {
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
              jsonPath("$.content.[$index].uuid") { value("${listUuids[index]}") }
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
        listUpdateRequests.forEachIndexed { index, listRequest ->
          it("list $index is updated") {
            val instance = "/lists/${listUuids[index]}"
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
              jsonPath("$.content.length()") { value(5) }
              jsonPath("$.content.uuid") { value("${listUuids[index]}") }
              jsonPath("$.content.description") { value(listRequest.description) }
              jsonPath("$.content.name") { value(listRequest.name) }
              jsonPath("$.content.created") { isNotEmpty() }
              jsonPath("$.content.updated") { isNotEmpty() }
            }
          }

          it("list $index is up-to-date") {
            val instance = "/lists/${listUuids[index]}"
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
              jsonPath("$.content.length()") { value(5) }
              jsonPath("$.content.uuid") { value("${listUuids[index]}") }
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

    describe("item: list association: create, read, update, delete") {
      it("item 0 is added to list 0") {
        val instance = "/items/${itemUuids[0]}/list-associations"
        mockMvc.post(instance) {
          content = Json.encodeToString(listUuids[0].toString())
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

      it("list 0 includes item 0") {
        val instance = "/lists/${listUuids[0]}?includeItems=true"
        mockMvc.get(instance) {
          contentType = MediaType.APPLICATION_JSON
        }.andExpect {
          status { isOk() }
          content { contentType(MediaType.APPLICATION_JSON) }
          jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
          jsonPath("$.method") { value(HttpMethod.GET.name().lowercase()) }
          jsonPath("$.message") { value("retrieved list id ${listUuids[0]}") }
          jsonPath("$.instance") { value(instance.substringBeforeLast("?").removeSuffix(instance)) }
          jsonPath("$.size") { value(1) }
          jsonPath("$.content.uuid") { value("${listUuids[0]}") }
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

      it("item 1 is added to list 0") {
        val instance = "/items/${itemUuids[1]}/list-associations"
        mockMvc.post(instance) {
          content = Json.encodeToString(listUuids[0].toString())
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

      it("list 0 includes items 0 and 1") {
        val instance = "/lists/${listUuids[0]}?includeItems=true"
        mockMvc.get(instance) {
          contentType = MediaType.APPLICATION_JSON
        }.andExpect {
          status { isOk() }
          content { contentType(MediaType.APPLICATION_JSON) }
          jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
          jsonPath("$.method") { value(HttpMethod.GET.name().lowercase()) }
          jsonPath("$.message") { value("retrieved list id ${listUuids[0]}") }
          jsonPath("$.instance") { value(instance.substringBeforeLast("?").removeSuffix(instance)) }
          jsonPath("$.size") { value(1) }
          jsonPath("$.content.uuid") { value("${listUuids[0]}") }
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

      it("item 0 is not added to list 0 when it's already been added") {
        val instance = "/items/${itemUuids[0]}/list-associations"
        mockMvc.post(instance) {
          content = Json.encodeToString(listUuids[0].toString())
          contentType = MediaType.APPLICATION_JSON
        }.andExpect {
          status { isUnprocessableEntity() }
          content { contentType(MediaType.APPLICATION_JSON) }
          jsonPath("$.disposition") { value(DispositionOfProblem.FAILURE.nameAsLowercase()) }
          jsonPath("$.method") { value(HttpMethod.POST.name().lowercase()) }
          jsonPath("$.message") { value("Item id ${itemUuids[0]} could not be added to list id ${listUuids[0]}: It's already been added.") }
          jsonPath("$.instance") { value(instance) }
          jsonPath("$.size") { value(1) }
          jsonPath("$.content.length()") { value(5) }
          jsonPath("$.content.status") { value(HttpStatus.UNPROCESSABLE_ENTITY.value()) }
        }
      }

      it("an item is not added to list 0 because the item couldn't be found") {
        val randomUuid = UUID.randomUUID()
        val instance = "/items/$randomUuid/list-associations"
        mockMvc.post(instance) {
          content = Json.encodeToString(listUuids[0].toString())
          contentType = MediaType.APPLICATION_JSON
        }.andExpect {
          status { isNotFound() }
          content { contentType(MediaType.APPLICATION_JSON) }
          jsonPath("$.disposition") { value(DispositionOfProblem.FAILURE.nameAsLowercase()) }
          jsonPath("$.method") { value(HttpMethod.POST.name().lowercase()) }
          jsonPath("$.message") {
            value("Item id $randomUuid could not be added to list id ${listUuids[0]}: Item id $randomUuid could not be found.")
          }
          jsonPath("$.instance") { value(instance) }
          jsonPath("$.size") { value(1) }
          jsonPath("$.content.length()") { value(5) }
          jsonPath("$.content.status") { value(HttpStatus.NOT_FOUND.value()) }
          jsonPath("$.content.cause") { isNotEmpty() }
          jsonPath("$.content.cause.name") { value(ItemNotFoundException::class.java.simpleName) }
          jsonPath("$.content.cause.message") { isNotEmpty() }
        }
      }

      it("item 0 is not added to a list because the list couldn't be found") {
        val randomUuid = UUID.randomUUID()
        val instance = "/items/${itemUuids[0]}/list-associations"
        mockMvc.post(instance) {
          content = Json.encodeToString(randomUuid.toString())
          contentType = MediaType.APPLICATION_JSON
        }.andExpect {
          status { isNotFound() }
          content { contentType(MediaType.APPLICATION_JSON) }
          jsonPath("$.disposition") { value(DispositionOfProblem.FAILURE.nameAsLowercase()) }
          jsonPath("$.method") { value(HttpMethod.POST.name().lowercase()) }
          jsonPath("$.message") {
            value("Item id ${itemUuids[0]} could not be added to list id $randomUuid: List id $randomUuid could not be found.")
          }
          jsonPath("$.instance") { value(instance) }
          jsonPath("$.size") { value(1) }
          jsonPath("$.content.length()") { value(5) }
          jsonPath("$.content.status") { value(HttpStatus.NOT_FOUND.value()) }
          jsonPath("$.content.cause") { isNotEmpty() }
          jsonPath("$.content.cause.name") { value(ListNotFoundException::class.java.simpleName) }
          jsonPath("$.content.cause.message") { isNotEmpty() }
        }
      }

      it("item 0 is moved from list 0 to list 1") {
        val instance = "/items/${itemUuids[0]}/list-associations"
        mockMvc.patch(instance) {
          content = Json.encodeToString(
            ItemToListAssociationUpdateRequest(currentListUuid = listUuids[0]!!, newListUuid = listUuids[1]!!),
          )
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

      it("list 0 includes only item 1") {
        val instance = "/lists/${listUuids[0]}?includeItems=true"
        mockMvc.get(instance) {
          contentType = MediaType.APPLICATION_JSON
        }.andExpect {
          status { isOk() }
          content { contentType(MediaType.APPLICATION_JSON) }
          jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
          jsonPath("$.method") { value(HttpMethod.GET.name().lowercase()) }
          jsonPath("$.message") { value("retrieved list id ${listUuids[0]}") }
          jsonPath("$.instance") { value(instance.substringBeforeLast("?").removeSuffix(instance)) }
          jsonPath("$.size") { value(1) }
          jsonPath("$.content.uuid") { value("${listUuids[0]}") }
          jsonPath("$.content.name") { value(updateLrmListOneRequest().name) }
          jsonPath("$.content.description") { value(updateLrmListOneRequest().description) }
          jsonPath("$.content.created") { isNotEmpty() }
          jsonPath("$.content.updated") { isNotEmpty() }
          jsonPath("$.content.items.length()") { value(1) }
          jsonPath("$.content.items.[0].length()") { value(6) }
          jsonPath("$.content.items.[0].uuid") { value("${itemUuids[1]}") }
          jsonPath("$.content.items.[0].name") { value(updateLrmItemTwoRequest().name) }
          jsonPath("$.content.items.[0].description") { value(updateLrmItemTwoRequest().description) }
          jsonPath("$.content.items.[0].quantity") { value(updateLrmItemTwoRequest().quantity) }
          jsonPath("$.content.items.[0].created") { isNotEmpty() }
          jsonPath("$.content.items.[0].updated") { isNotEmpty() }
        }
      }

      it("item 0 is not deleted from list 0 because the association couldn't be found") {
        val instance = "/items/${itemUuids[0]}/list-associations"
        mockMvc.delete(instance) {
          content = "\"${listUuids[0]}\""
          contentType = MediaType.APPLICATION_JSON
        }.andExpect {
          status { isNotFound() }
          content { contentType(MediaType.APPLICATION_JSON) }
          jsonPath("$.disposition") { value(DispositionOfProblem.FAILURE.nameAsLowercase()) }
          jsonPath("$.method") { value(HttpMethod.DELETE.name().lowercase()) }
          jsonPath("$.instance") { value(instance) }
          jsonPath(
            "$.message",
          ) { value("Item id ${itemUuids[0]} could not be removed from list id ${listUuids[0]}: Association not found.") }
          jsonPath("$.size") { value(1) }
          jsonPath("$.content.length()") { value(5) }
          jsonPath("$.content.cause") { isNotEmpty() }
          jsonPath("$.content.cause.name") { value(AssociationNotFoundException::class.java.simpleName) }
          jsonPath("$.content.cause.message") { isNotEmpty() }
        }
      }

      it("item 1 is associated with only list 0") {
        val instance = "/items/${itemUuids[1]}?includeLists=true"
        mockMvc.get(instance) {
          contentType = MediaType.APPLICATION_JSON
        }.andExpect {
          status { isOk() }
          content { contentType(MediaType.APPLICATION_JSON) }
          jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
          jsonPath("$.method") { value(HttpMethod.GET.name().lowercase()) }
          jsonPath("$.message") { value("retrieved item id ${itemUuids[1]} and it's associated lists") }
          jsonPath("$.instance") { value(instance.substringBeforeLast("?").removeSuffix(instance)) }
          jsonPath("$.size") { value(1) }
          jsonPath("$.content.uuid") { value("${itemUuids[1]}") }
          jsonPath("$.content.name") { value(updateLrmItemTwoRequest().name) }
          jsonPath("$.content.description") { value(updateLrmItemTwoRequest().description) }
          jsonPath("$.content.quantity") { value(updateLrmItemTwoRequest().quantity) }
          jsonPath("$.content.created") { isNotEmpty() }
          jsonPath("$.content.updated") { isNotEmpty() }
          jsonPath("$.content.lists.length()") { value(1) }
          jsonPath("$.content.lists.[0].length()") { value(2) }
          jsonPath("$.content.lists.[0].uuid") { value("${listUuids[0]}") }
          jsonPath("$.content.lists.[0].name") { value(updateLrmListOneRequest().name) }
        }
      }

      it("list 1 includes only item 0") {
        val instance = "/lists/${listUuids[1]}?includeItems=true"
        mockMvc.get(instance) {
          contentType = MediaType.APPLICATION_JSON
        }.andExpect {
          status { isOk() }
          content { contentType(MediaType.APPLICATION_JSON) }
          jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
          jsonPath("$.method") { value(HttpMethod.GET.name().lowercase()) }
          jsonPath("$.message") { value("retrieved list id ${listUuids[1]}") }
          jsonPath("$.instance") { value(instance.substringBeforeLast("?").removeSuffix(instance)) }
          jsonPath("$.size") { value(1) }
          jsonPath("$.content.uuid") { value("${listUuids[1]}") }
          jsonPath("$.content.name") { value(updateLrmListTwoRequest().name) }
          jsonPath("$.content.description") { value(updateLrmListTwoRequest().description) }
          jsonPath("$.content.created") { isNotEmpty() }
          jsonPath("$.content.updated") { isNotEmpty() }
          jsonPath("$.content.items.length()") { value(1) }
          jsonPath("$.content.items.[0].length()") { value(6) }
          jsonPath("$.content.items.[0].uuid") { value("${itemUuids[0]}") }
          jsonPath("$.content.items.[0].name") { value(updateLrmItemOneRequest().name) }
          jsonPath("$.content.items.[0].description") { value(updateLrmItemOneRequest().description) }
          jsonPath("$.content.items.[0].quantity") { value(updateLrmItemOneRequest().quantity) }
          jsonPath("$.content.items.[0].created") { isNotEmpty() }
          jsonPath("$.content.items.[0].updated") { isNotEmpty() }
        }
      }

      it("item 0 is associated with only list 1") {
        val instance = "/items/${itemUuids[0]}?includeLists=true"
        mockMvc.get(instance) {
          contentType = MediaType.APPLICATION_JSON
        }.andExpect {
          status { isOk() }
          content { contentType(MediaType.APPLICATION_JSON) }
          jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
          jsonPath("$.method") { value(HttpMethod.GET.name().lowercase()) }
          jsonPath("$.message") { value("retrieved item id ${itemUuids[0]} and it's associated lists") }
          jsonPath("$.instance") { value(instance.substringBeforeLast("?").removeSuffix(instance)) }
          jsonPath("$.size") { value(1) }
          jsonPath("$.content.uuid") { value("${itemUuids[0]}") }
          jsonPath("$.content.name") { value(updateLrmItemOneRequest().name) }
          jsonPath("$.content.description") { value(updateLrmItemOneRequest().description) }
          jsonPath("$.content.quantity") { value(updateLrmItemOneRequest().quantity) }
          jsonPath("$.content.created") { isNotEmpty() }
          jsonPath("$.content.updated") { isNotEmpty() }
          jsonPath("$.content.lists.length()") { value(1) }
          jsonPath("$.content.lists.[0].length()") { value(2) }
          jsonPath("$.content.lists.[0].uuid") { value("${listUuids[1]}") }
          jsonPath("$.content.lists.[0].name") { value(updateLrmListTwoRequest().name) }
        }
      }

      it("item 0 has one list association") {
        val instance = "/items/${itemUuids[0]}/list-associations/count"
        mockMvc.get(instance) {
          contentType = MediaType.APPLICATION_JSON
        }.andExpect {
          status { isOk() }
          content { contentType(MediaType.APPLICATION_JSON) }
          jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
          jsonPath("$.method") { value(HttpMethod.GET.name().lowercase()) }
          jsonPath("$.message") { value("Item is associated with 1 lists.") }
          jsonPath("$.instance") { value(instance) }
          jsonPath("$.size") { value(1) }
          jsonPath("$.content.length()") { value(1) }
          jsonPath("$.content.value") { value(1) }
        }
      }

      it("item 1 has one list association") {
        val instance = "/items/${itemUuids[1]}/list-associations/count"
        mockMvc.get(instance) {
          contentType = MediaType.APPLICATION_JSON
        }.andExpect {
          status { isOk() }
          content { contentType(MediaType.APPLICATION_JSON) }
          jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
          jsonPath("$.method") { value(HttpMethod.GET.name().lowercase()) }
          jsonPath("$.message") { value("Item is associated with 1 lists.") }
          jsonPath("$.instance") { value(instance) }
          jsonPath("$.size") { value(1) }
          jsonPath("$.content.length()") { value(1) }
          jsonPath("$.content.value") { value(1) }
        }
      }

      it("list 0 has one item association") {
        val instance = "/lists/${listUuids[0]}/item-associations/count"
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

      it("list 1 has one item association") {
        val instance = "/lists/${listUuids[1]}/item-associations/count"
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

      it("remove item 0 from all lists") {
        val instance = "/items/${itemUuids[0]}/list-associations/delete-all"
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

      it("item 0 has zero list associations") {
        val instance = "/items/${itemUuids[0]}/list-associations/count"
        mockMvc.get(instance) {
          contentType = MediaType.APPLICATION_JSON
        }.andExpect {
          status { isOk() }
          content { contentType(MediaType.APPLICATION_JSON) }
          jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
          jsonPath("$.method") { value(HttpMethod.GET.name().lowercase()) }
          jsonPath("$.instance") { value(instance) }
          jsonPath("$.message") { value("Item is associated with 0 lists.") }
          jsonPath("$.size") { value(1) }
          jsonPath("$.content.length()") { value(1) }
          jsonPath("$.content.value") { value(0) }
        }
      }
    }

    describe("list: item association: create, read, delete)") {
      it("list 0 has one item associations") {
        val instance = "/lists/${listUuids[0]}/item-associations/count"
        mockMvc.get(instance) {
          contentType = MediaType.APPLICATION_JSON
        }.andExpect {
          status { isOk() }
          content { contentType(MediaType.APPLICATION_JSON) }
          jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
          jsonPath("$.method") { value(HttpMethod.GET.name().lowercase()) }
          jsonPath("$.instance") { value(instance) }
          jsonPath("$.message") { value("List is associated with 1 items.") }
          jsonPath("$.size") { value(1) }
          jsonPath("$.content.length()") { value(1) }
          jsonPath("$.content.value") { value(1) }
        }
      }

      it("list 1 has zero item associations") {
        val instance = "/lists/${listUuids[1]}/item-associations/count"
        mockMvc.get(instance) {
          contentType = MediaType.APPLICATION_JSON
        }.andExpect {
          status { isOk() }
          content { contentType(MediaType.APPLICATION_JSON) }
          jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
          jsonPath("$.method") { value(HttpMethod.GET.name().lowercase()) }
          jsonPath("$.instance") { value(instance) }
          jsonPath("$.message") { value("List is associated with 0 items.") }
          jsonPath("$.size") { value(1) }
          jsonPath("$.content.length()") { value(1) }
          jsonPath("$.content.value") { value(0) }
        }
      }

      it("item 0 is added to list 1") {
        val instance = "/lists/${listUuids[1]}/item-associations"
        mockMvc.post(instance) {
          content = Json.encodeToString(itemUuids[0].toString())
          contentType = MediaType.APPLICATION_JSON
        }.andExpect {
          status { isOk() }
          content { contentType(MediaType.APPLICATION_JSON) }
          jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
          jsonPath("$.method") { value(HttpMethod.POST.name().lowercase()) }
          jsonPath("$.message") { value("Assigned item '${updateLrmItemOneRequest().name}' to list '${updateLrmListTwoRequest().name}'.") }
          jsonPath("$.instance") { value(instance) }
          jsonPath("$.size") { value(1) }
          jsonPath("$.content.message") {
            value(
              "Assigned item '${updateLrmItemOneRequest().name}' to list '${updateLrmListTwoRequest().name}'.",
            )
          }
        }
      }

      it("item 0 is not added to list 1 when it's already been added") {
        val instance = "/lists/${listUuids[1]}/item-associations"
        mockMvc.post(instance) {
          content = Json.encodeToString(itemUuids[0].toString())
          contentType = MediaType.APPLICATION_JSON
        }.andExpect {
          status { isUnprocessableEntity() }
          content { contentType(MediaType.APPLICATION_JSON) }
          jsonPath("$.disposition") { value(DispositionOfProblem.FAILURE.nameAsLowercase()) }
          jsonPath("$.method") { value(HttpMethod.POST.name().lowercase()) }
          jsonPath("$.message") { value("Item id ${itemUuids[0]} could not be added to list id ${listUuids[1]}: It's already been added.") }
          jsonPath("$.instance") { value(instance) }
          jsonPath("$.size") { value(1) }
          jsonPath("$.content.length()") { value(5) }
          jsonPath("$.content.status") { value(HttpStatus.UNPROCESSABLE_ENTITY.value()) }
        }
      }

      it("item 1 is added to list 1") {
        val expected = "Assigned item '${updateLrmItemTwoRequest().name}' to list '${updateLrmListTwoRequest().name}'."
        val instance = "/lists/${listUuids[1]}/item-associations"
        mockMvc.post(instance) {
          content = Json.encodeToString(itemUuids[1].toString())
          contentType = MediaType.APPLICATION_JSON
        }.andExpect {
          status { isOk() }
          content { contentType(MediaType.APPLICATION_JSON) }
          jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
          jsonPath("$.method") { value(HttpMethod.POST.name().lowercase()) }
          jsonPath("$.message") { value(expected) }
          jsonPath("$.instance") { value(instance) }
          jsonPath("$.size") { value(1) }
          jsonPath("$.content.message") { value(expected) }
        }
      }

      it("item 2 is added to list 1") {
        val expected = "Assigned item '${updateLrmItemThreeRequest().name}' to list '${updateLrmListTwoRequest().name}'."
        val instance = "/lists/${listUuids[1]}/item-associations"
        mockMvc.post(instance) {
          content = Json.encodeToString(itemUuids[2].toString())
          contentType = MediaType.APPLICATION_JSON
        }.andExpect {
          status { isOk() }
          content { contentType(MediaType.APPLICATION_JSON) }
          jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
          jsonPath("$.method") { value(HttpMethod.POST.name().lowercase()) }
          jsonPath("$.message") { value(expected) }
          jsonPath("$.instance") { value(instance) }
          jsonPath("$.size") { value(1) }
          jsonPath("$.content.message") { value(expected) }
        }
      }

      it("list 1 includes three items") {
        val instance = "/lists/${listUuids[1]}?includeItems=true"
        mockMvc.get(instance) {
          contentType = MediaType.APPLICATION_JSON
        }.andExpect {
          status { isOk() }
          content { contentType(MediaType.APPLICATION_JSON) }
          jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
          jsonPath("$.method") { value(HttpMethod.GET.name().lowercase()) }
          jsonPath("$.message") { value("retrieved list id ${listUuids[1]}") }
          jsonPath("$.instance") { value(instance.substringBeforeLast("?").removeSuffix(instance)) }
          jsonPath("$.size") { value(1) }
          jsonPath("$.content.uuid") { value("${listUuids[1]}") }
          jsonPath("$.content.name") { value(updateLrmListTwoRequest().name) }
          jsonPath("$.content.description") { value(updateLrmListTwoRequest().description) }
          jsonPath("$.content.created") { isNotEmpty() }
          jsonPath("$.content.updated") { isNotEmpty() }
          jsonPath("$.content.items.length()") { value(3) }
        }
      }

      it("item 2 is deleted from list 1") {
        val expected = "Removed item '${updateLrmItemThreeRequest().name}' from list '${updateLrmListTwoRequest().name}'."
        val instance = "/lists/${listUuids[1]}/item-associations"
        mockMvc.delete(instance) {
          content = "\"${itemUuids[2]}\""
          contentType = MediaType.APPLICATION_JSON
        }.andExpect {
          status { isOk() }
          content { contentType(MediaType.APPLICATION_JSON) }
          jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
          jsonPath("$.method") { value(HttpMethod.DELETE.name().lowercase()) }
          jsonPath("$.instance") { value(instance) }
          jsonPath("$.message") { value(expected) }
          jsonPath("$.size") { value(1) }
          jsonPath("$.content.length()") { value(1) }
        }
      }

      it("item 2 is not deleted from list 1 because the association couldn't be found") {
        val instance = "/lists/${listUuids[1]}/item-associations"
        mockMvc.delete(instance) {
          content = "\"${itemUuids[2]}\""
          contentType = MediaType.APPLICATION_JSON
        }.andExpect {
          status { isNotFound() }
          content { contentType(MediaType.APPLICATION_JSON) }
          jsonPath("$.disposition") { value(DispositionOfProblem.FAILURE.nameAsLowercase()) }
          jsonPath("$.method") { value(HttpMethod.DELETE.name().lowercase()) }
          jsonPath("$.instance") { value(instance) }
          jsonPath(
            "$.message",
          ) { value("Item id ${itemUuids[2]} could not be removed from list id ${listUuids[1]}: Association not found.") }
          jsonPath("$.size") { value(1) }
          jsonPath("$.content.length()") { value(5) }
          jsonPath("$.content.cause") { isNotEmpty() }
          jsonPath("$.content.cause.name") { value(AssociationNotFoundException::class.java.simpleName) }
          jsonPath("$.content.cause.message") { isNotEmpty() }
        }
      }

      it("list 1 has 2 item associations") {
        val instance = "/lists/${listUuids[1]}/item-associations/count"
        mockMvc.get(instance) {
          contentType = MediaType.APPLICATION_JSON
        }.andExpect {
          status { isOk() }
          content { contentType(MediaType.APPLICATION_JSON) }
          jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
          jsonPath("$.method") { value(HttpMethod.GET.name().lowercase()) }
          jsonPath("$.instance") { value(instance) }
          jsonPath("$.message") { value("List is associated with 2 items.") }
          jsonPath("$.size") { value(1) }
          jsonPath("$.content.length()") { value(1) }
          jsonPath("$.content.value") { value(2) }
        }
      }

      it("items 0 and 1 are deleted from list 1") {
        val expected = "Removed all associated items (2) from list '${updateLrmListTwoRequest().name}'."
        val instance = "/lists/${listUuids[1]}/item-associations/delete-all"
        mockMvc.delete(instance) {
          contentType = MediaType.APPLICATION_JSON
        }.andExpect {
          status { isOk() }
          content { contentType(MediaType.APPLICATION_JSON) }
          jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
          jsonPath("$.method") { value(HttpMethod.DELETE.name().lowercase()) }
          jsonPath("$.instance") { value(instance) }
          jsonPath("$.message") { value(expected) }
          jsonPath("$.size") { value(1) }
          jsonPath("$.content.length()") { value(1) }
        }
      }

      it("an item is not added to list 0 because the item couldn't be found") {
        val randomUuid = UUID.randomUUID()
        val instance = "/lists/${listUuids[1]}/item-associations"
        mockMvc.post(instance) {
          content = Json.encodeToString(randomUuid.toString())
          contentType = MediaType.APPLICATION_JSON
        }.andExpect {
          status { isNotFound() }
          content { contentType(MediaType.APPLICATION_JSON) }
          jsonPath("$.disposition") { value(DispositionOfProblem.FAILURE.nameAsLowercase()) }
          jsonPath("$.method") { value(HttpMethod.POST.name().lowercase()) }
          jsonPath("$.message") {
            value("Item id $randomUuid could not be added to list id ${listUuids[1]}: Item id $randomUuid could not be found.")
          }
          jsonPath("$.instance") { value(instance) }
          jsonPath("$.size") { value(1) }
          jsonPath("$.content.length()") { value(5) }
          jsonPath("$.content.status") { value(HttpStatus.NOT_FOUND.value()) }
          jsonPath("$.content.cause") { isNotEmpty() }
          jsonPath("$.content.cause.name") { value(ItemNotFoundException::class.java.simpleName) }
          jsonPath("$.content.cause.message") { isNotEmpty() }
        }
      }

      it("item 0 is not added to a list because the list couldn't be found") {
        val randomUuid = UUID.randomUUID()
        val instance = "/lists/$randomUuid/item-associations"
        mockMvc.post(instance) {
          content = Json.encodeToString(itemUuids[0].toString())
          contentType = MediaType.APPLICATION_JSON
        }.andExpect {
          status { isNotFound() }
          content { contentType(MediaType.APPLICATION_JSON) }
          jsonPath("$.disposition") { value(DispositionOfProblem.FAILURE.nameAsLowercase()) }
          jsonPath("$.method") { value(HttpMethod.POST.name().lowercase()) }
          jsonPath("$.message") {
            value("Item id ${itemUuids[0]} could not be added to list id $randomUuid: List id $randomUuid could not be found.")
          }
          jsonPath("$.instance") { value(instance) }
          jsonPath("$.size") { value(1) }
          jsonPath("$.content.length()") { value(5) }
          jsonPath("$.content.status") { value(HttpStatus.NOT_FOUND.value()) }
          jsonPath("$.content.cause") { isNotEmpty() }
          jsonPath("$.content.cause.name") { value(ListNotFoundException::class.java.simpleName) }
          jsonPath("$.content.cause.message") { isNotEmpty() }
        }
      }

      it("list 0 has one item associations+") {
        val instance = "/lists/${listUuids[0]}/item-associations/count"
        mockMvc.get(instance) {
          contentType = MediaType.APPLICATION_JSON
        }.andExpect {
          status { isOk() }
          content { contentType(MediaType.APPLICATION_JSON) }
          jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
          jsonPath("$.method") { value(HttpMethod.GET.name().lowercase()) }
          jsonPath("$.instance") { value(instance) }
          jsonPath("$.message") { value("List is associated with 1 items.") }
          jsonPath("$.size") { value(1) }
          jsonPath("$.content.length()") { value(1) }
          jsonPath("$.content.value") { value(1) }
        }
      }

      it("list 1 has zero item associations+") {
        val instance = "/lists/${listUuids[1]}/item-associations/count"
        mockMvc.get(instance) {
          contentType = MediaType.APPLICATION_JSON
        }.andExpect {
          status { isOk() }
          content { contentType(MediaType.APPLICATION_JSON) }
          jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
          jsonPath("$.method") { value(HttpMethod.GET.name().lowercase()) }
          jsonPath("$.instance") { value(instance) }
          jsonPath("$.message") { value("List is associated with 0 items.") }
          jsonPath("$.size") { value(1) }
          jsonPath("$.content.length()") { value(1) }
          jsonPath("$.content.value") { value(0) }
        }
      }
    }

    describe("item: delete") {
      it("item 2 has zero list associations") {
        val instance = "/items/${itemUuids[2]}/list-associations/count"
        mockMvc.get(instance) {
          contentType = MediaType.APPLICATION_JSON
        }.andExpect {
          status { isOk() }
          content { contentType(MediaType.APPLICATION_JSON) }
          jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
          jsonPath("$.method") { value(HttpMethod.GET.name().lowercase()) }
          jsonPath("$.instance") { value(instance) }
          jsonPath("$.message") { value("Item is associated with 0 lists.") }
          jsonPath("$.size") { value(1) }
          jsonPath("$.content.length()") { value(1) }
          jsonPath("$.content.value") { value(0) }
        }
      }

      it("delete item 2") {
        val instance = "/items/${itemUuids[2]}"
        mockMvc.delete(instance).andExpect {
          status { isOk() }
          content { contentType(MediaType.APPLICATION_JSON) }
          jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
          jsonPath("$.method") { value(HttpMethod.DELETE.name().lowercase()) }
          jsonPath("$.instance") { value(instance) }
          jsonPath("$.message") { value("Deleted item id ${itemUuids[2]}.") }
          jsonPath("$.size") { value(1) }
          jsonPath("$.content.length()") { value(2) }
          jsonPath("$.content.associatedListCount") { value(0) }
          jsonPath("$.content.associatedListNames.length()") { value(0) }
        }
      }

      it("item 1 is added to list 1") {
        val instance = "/items/${itemUuids[1]}/list-associations"
        mockMvc.post(instance) {
          content = Json.encodeToString(UUIDSerializer, listUuids[1]!!)
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

      it("item 1 has two list associations") {
        val instance = "/items/${itemUuids[1]}/list-associations/count"
        mockMvc.get(instance) {
          contentType = MediaType.APPLICATION_JSON
        }.andExpect {
          status { isOk() }
          content { contentType(MediaType.APPLICATION_JSON) }
          jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
          jsonPath("$.method") { value(HttpMethod.GET.name().lowercase()) }
          jsonPath("$.instance") { value(instance) }
          jsonPath("$.message") { value("Item is associated with 2 lists.") }
          jsonPath("$.size") { value(1) }
          jsonPath("$.content.length()") { value(1) }
          jsonPath("$.content.value") { value(2) }
        }
      }

      it("delete item 1 (removeListAssociations = false)") {
        val expected = "Item id ${itemUuids[1]} could not be deleted: Item ${itemUuids[1]} is associated with 2 list(s). " +
          "First remove the item from each list."
        val instance = "/items/${itemUuids[1]}"
        mockMvc.delete(instance).andExpect {
          // client should be checking for http 422 error and proceed to confirm removal from X lists
          status { isUnprocessableEntity() }
          content { contentType(MediaType.APPLICATION_JSON) }
          jsonPath("$.disposition") { value(DispositionOfProblem.FAILURE.nameAsLowercase()) }
          jsonPath("$.method") { value(HttpMethod.DELETE.name().lowercase()) }
          jsonPath("$.instance") { value(instance) }
          jsonPath("$.message") { value(expected) }
          jsonPath("$.size") { value(1) }
          jsonPath("$.content.detail") { value(expected) }
          jsonPath("$.content.cause") { isNotEmpty() }
          jsonPath("$.content.cause.name") { value(ApiException::class.java.simpleName) }
          jsonPath("$.content.cause.message") { isNotEmpty() }
        }
      }

      it("delete item 1 (removeListAssociations = true)") {
        val instance = "/items/${itemUuids[1]}?removeListAssociations=true"
        mockMvc.delete(instance).andExpect {
          status { isOk() }
          content { contentType(MediaType.APPLICATION_JSON) }
          jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
          jsonPath("$.method") { value(HttpMethod.DELETE.name().lowercase()) }
          jsonPath("$.instance") { value(instance.substringBeforeLast("?").removeSuffix(instance)) }
          jsonPath("$.message") { value("Deleted item id ${itemUuids[1]}.") }
          jsonPath("$.size") { value(1) }
          jsonPath("$.content.length()") { value(2) }
          jsonPath("$.content.associatedListCount") { value(2) }
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

    describe("list: delete") {
      it("list 1 has zero item associations+") {
        val instance = "/lists/${listUuids[1]}/item-associations/count"
        mockMvc.get(instance) {
          contentType = MediaType.APPLICATION_JSON
        }.andExpect {
          status { isOk() }
          content { contentType(MediaType.APPLICATION_JSON) }
          jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
          jsonPath("$.method") { value(HttpMethod.GET.name().lowercase()) }
          jsonPath("$.instance") { value(instance) }
          jsonPath("$.message") { value("List is associated with 0 items.") }
          jsonPath("$.size") { value(1) }
          jsonPath("$.content.length()") { value(1) }
          jsonPath("$.content.value") { value(0) }
        }
      }

      it("delete list 1") {
        val instance = "/lists/${listUuids[1]}"
        mockMvc.delete(instance).andExpect {
          status { isOk() }
          content { contentType(MediaType.APPLICATION_JSON) }
          jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
          jsonPath("$.method") { value(HttpMethod.DELETE.name().lowercase()) }
          jsonPath("$.instance") { value(instance) }
          jsonPath("$.message") { value("Deleted list id ${listUuids[1]}.") }
          jsonPath("$.size") { value(1) }
          jsonPath("$.content.length()") { value(2) }
          jsonPath("$.content.listNames.length()") { value(1) }
          jsonPath("$.content.associatedItemNames.length()") { value(0) }
        }
      }

      it("total list count is one") {
        val instance = "/lists/count"
        mockMvc.get(instance).andExpect {
          status { isOk() }
          content { contentType(MediaType.APPLICATION_JSON) }
          jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
          jsonPath("$.method") { value(HttpMethod.GET.name().lowercase()) }
          jsonPath("$.message") { value("1 lists.") }
          jsonPath("$.instance") { value(instance) }
          jsonPath("$.size") { value(1) }
          jsonPath("$.content.length()") { value(1) }
          jsonPath("$.content.value") { value(1) }
        }
      }

      describe("delete list with item associations") {
        val instance = "/lists/${listUuids[0]}/item-associations/count"
        mockMvc.get(instance) {
          contentType = MediaType.APPLICATION_JSON
        }.andExpect {
          status { isOk() }
          content { contentType(MediaType.APPLICATION_JSON) }
          jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
          jsonPath("$.method") { value(HttpMethod.GET.name().lowercase()) }
          jsonPath("$.instance") { value(instance) }
          jsonPath("$.message") { value("List is associated with 0 items.") }
          jsonPath("$.size") { value(1) }
          jsonPath("$.content.length()") { value(1) }
          jsonPath("$.content.value") { value(0) }
        }
      }

      it("item 0 is added to list 0") {
        val instance = "/lists/${listUuids[0]}/item-associations"
        mockMvc.post(instance) {
          content = Json.encodeToString(itemUuids[0].toString())
          contentType = MediaType.APPLICATION_JSON
        }.andExpect {
          status { isOk() }
          content { contentType(MediaType.APPLICATION_JSON) }
          jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
          jsonPath("$.method") { value(HttpMethod.POST.name().lowercase()) }
          jsonPath("$.message") {
            value(
              "Assigned item '${updateLrmItemOneRequest().name}' " +
                "to list '${updateLrmListOneRequest().name}'.",
            )
          }
          jsonPath("$.instance") { value(instance) }
          jsonPath("$.size") { value(1) }
          jsonPath("$.content.message") {
            value(
              "Assigned item '${updateLrmItemOneRequest().name}' " +
                "to list '${updateLrmListOneRequest().name}'.",
            )
          }
        }
      }

      it("delete list 0 (removeItemAssociations = false)") {
        val expected =
          "List id ${listUuids[0]} could not be deleted: List ${listUuids[0]} is associated with 1 item(s). " +
            "First remove each item from the list."
        val instance = "/lists/${listUuids[0]}"
        mockMvc.delete(instance).andExpect {
          // client should be checking for http 422 error and proceed to confirm removal from X lists
          status { isUnprocessableEntity() }
          content { contentType(MediaType.APPLICATION_JSON) }
          jsonPath("$.disposition") { value(DispositionOfProblem.FAILURE.nameAsLowercase()) }
          jsonPath("$.method") { value(HttpMethod.DELETE.name().lowercase()) }
          jsonPath("$.instance") { value(instance) }
          jsonPath("$.message") { value(expected) }
          jsonPath("$.size") { value(1) }
          jsonPath("$.content.detail") { value(expected) }
          jsonPath("$.content.cause") { isNotEmpty() }
          jsonPath("$.content.cause.name") { value(ApiException::class.java.simpleName) }
          jsonPath("$.content.cause.message") { isNotEmpty() }
        }
      }

      it("list 0 has one item associations") {
        val instance = "/lists/${listUuids[0]}/item-associations/count"
        mockMvc.get(instance) {
          contentType = MediaType.APPLICATION_JSON
        }.andExpect {
          status { isOk() }
          content { contentType(MediaType.APPLICATION_JSON) }
          jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
          jsonPath("$.method") { value(HttpMethod.GET.name().lowercase()) }
          jsonPath("$.instance") { value(instance) }
          jsonPath("$.message") { value("List is associated with 1 items.") }
          jsonPath("$.size") { value(1) }
          jsonPath("$.content.length()") { value(1) }
          jsonPath("$.content.value") { value(1) }
        }
      }

      it("delete list 0 (removeItemAssociations = true)") {
        val expected = "Deleted list id ${listUuids[0]}."
        val instance = "/lists/${listUuids[0]}?removeItemAssociations=true"
        mockMvc.delete(instance).andExpect {
          status { isOk() }
          content { contentType(MediaType.APPLICATION_JSON) }
          jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
          jsonPath("$.method") { value(HttpMethod.DELETE.name().lowercase()) }
          jsonPath("$.instance") { value(instance.substringBeforeLast("?").removeSuffix(instance)) }
          jsonPath("$.message") { value(expected) }
          jsonPath("$.size") { value(1) }
          jsonPath("$.content.length()") { value(2) }
          jsonPath("$.content.listNames.length()") { value(1) }
          jsonPath("$.content.listNames[0]") { value(updateLrmListOneRequest().name) }
          jsonPath("$.content.associatedItemNames.length()") { value(1) }
          jsonPath("$.content.associatedItemNames.[0]") { value(updateLrmItemOneRequest().name) }
        }
      }
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
})
