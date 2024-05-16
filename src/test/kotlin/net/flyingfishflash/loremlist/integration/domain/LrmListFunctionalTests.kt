package net.flyingfishflash.loremlist.integration.domain

import io.kotest.core.spec.style.DescribeSpec
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.flyingfishflash.loremlist.core.response.structure.DispositionOfProblem
import net.flyingfishflash.loremlist.core.response.structure.DispositionOfSuccess
import net.flyingfishflash.loremlist.domain.lrmitem.data.LrmItemMoveToListRequest
import net.flyingfishflash.loremlist.domain.lrmitem.data.LrmItemRequest
import net.flyingfishflash.loremlist.domain.lrmlist.data.LrmListRequest
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post

/**
 * LrmList Integration/Functional Tests
 */
@SpringBootTest
@AutoConfigureMockMvc
class LrmListFunctionalTests(mockMvc: MockMvc) : DescribeSpec({

  val itemOneId: Long = 1
  fun createLrmItemOneRequest(): LrmItemRequest = LrmItemRequest("Lorem Item One Name", "Lorem Item One Description", 0)
  fun updateLrmItemOneRequest(): LrmItemRequest = LrmItemRequest("Updated Lorem Item One Name", "Updated Lorem Item One Description", 1001)

  val itemTwoId: Long = 2
  fun createLrmItemTwoRequest(): LrmItemRequest = LrmItemRequest("Lorem Item Two Name", "Lorem Item Two Description", 0)
  fun updateLrmItemTwoRequest(): LrmItemRequest = LrmItemRequest("Updated Lorem Item Two Name", "Updated Lorem Item Two Description", 1002)

  val listOneId: Long = 1
  fun createLrmListOneRequest(): LrmListRequest = LrmListRequest("Lorem List One Name", "Lorem List One Description")
  fun updateLrmListOneRequest(): LrmListRequest = LrmListRequest("Updated Lorem List One Name", "Updated Lorem List One Description")

  val listTwoId: Long = 2
  fun createLrmListTwoRequest(): LrmListRequest = LrmListRequest("Lorem List Two Name", "Lorem List Two Description")
  fun updateLrmListTwoRequest(): LrmListRequest = LrmListRequest("Updated Lorem List Two Name", "Updated Lorem List Two Description")

  describe("comprehensive functional test") {
    describe("item create, read, and update") {
      it("item 1 is created") {
        val instance = "/items"
        mockMvc.post(instance) {
          content = Json.encodeToString(createLrmItemOneRequest())
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
          jsonPath("$.content.id") { value(itemOneId) }
          jsonPath("$.content.uuid") { isNotEmpty() }
          jsonPath("$.content.name") { value(createLrmItemOneRequest().name) }
          jsonPath("$.content.description") { value(createLrmItemOneRequest().description) }
          jsonPath("$.content.quantity") { value(createLrmItemOneRequest().quantity) }
          jsonPath("$.content.created") { isNotEmpty() }
          jsonPath("$.content.updated") { isNotEmpty() }
        }
      }

      it("item 1 is found") {
        val instance = "/items/$itemOneId"
        mockMvc.get(instance) {
          contentType = MediaType.APPLICATION_JSON
        }.andExpect {
          status { isOk() }
          content { contentType(MediaType.APPLICATION_JSON) }
          jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
          jsonPath("$.method") { value(HttpMethod.GET.name().lowercase()) }
          jsonPath("$.message") { value("retrieved item id $itemOneId") }
          jsonPath("$.instance") { value(instance) }
          jsonPath("$.size") { value(1) }
          jsonPath("$.content.length()") { value(7) }
          jsonPath("$.content.id") { value(itemOneId) }
          jsonPath("$.content.uuid") { isNotEmpty() }
          jsonPath("$.content.name") { value(createLrmItemOneRequest().name) }
          jsonPath("$.content.description") { value(createLrmItemOneRequest().description) }
          jsonPath("$.content.quantity") { value(createLrmItemOneRequest().quantity) }
          jsonPath("$.content.created") { isNotEmpty() }
          jsonPath("$.content.updated") { isNotEmpty() }
          jsonPath("$.content.lists") { doesNotExist() }
        }
      }

      it("item 1 is found with all items") {
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
          jsonPath("$.size") { value(1) }
          jsonPath("$.content") { exists() }
          jsonPath("$.content") { isArray() }
          jsonPath("$.content.length()") { value(1) }
          jsonPath("$.content.[0].id") { value(itemOneId) }
          jsonPath("$.content.[0].uuid") { isNotEmpty() }
          jsonPath("$.content.[0].name") { value(createLrmItemOneRequest().name) }
          jsonPath("$.content.[0].description") { value(createLrmItemOneRequest().description) }
          jsonPath("$.content.[0].quantity") { value(createLrmItemOneRequest().quantity) }
          jsonPath("$.content.[0].created") { isNotEmpty() }
          jsonPath("$.content.[0].updated") { isNotEmpty() }
          jsonPath("$.content.[0].lists") { doesNotExist() }
        }
      }

      it("item 1 is found and updated") {
        val instance = "/items/$itemOneId"
        mockMvc.patch(instance) {
          content = Json.encodeToString(updateLrmItemOneRequest())
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
          jsonPath("$.content.id") { value(itemOneId) }
          jsonPath("$.content.uuid") { isNotEmpty() }
          jsonPath("$.content.description") { value(updateLrmItemOneRequest().description) }
          jsonPath("$.content.name") { value(updateLrmItemOneRequest().name) }
          jsonPath("$.content.quantity") { value(updateLrmItemOneRequest().quantity) }
          jsonPath("$.content.created") { isNotEmpty() }
          jsonPath("$.content.updated") { isNotEmpty() }
        }
      }

      it("item 2 is created") {
        val instance = "/items"
        mockMvc.post(instance) {
          content = Json.encodeToString(createLrmItemTwoRequest())
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
          jsonPath("$.content.id") { value(itemTwoId) }
          jsonPath("$.content.uuid") { isNotEmpty() }
          jsonPath("$.content.name") { value(createLrmItemTwoRequest().name) }
          jsonPath("$.content.description") { value(createLrmItemTwoRequest().description) }
          jsonPath("$.content.quantity") { value(createLrmItemTwoRequest().quantity) }
          jsonPath("$.content.created") { isNotEmpty() }
          jsonPath("$.content.updated") { isNotEmpty() }
        }
      }

      it("item 2 is found") {
        val instance = "/items/$itemTwoId"
        mockMvc.get(instance) {
          contentType = MediaType.APPLICATION_JSON
        }.andExpect {
          status { isOk() }
          content { contentType(MediaType.APPLICATION_JSON) }
          jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
          jsonPath("$.method") { value(HttpMethod.GET.name().lowercase()) }
          jsonPath("$.message") { value("retrieved item id $itemTwoId") }
          jsonPath("$.instance") { value(instance) }
          jsonPath("$.size") { value(1) }
          jsonPath("$.content.length()") { value(7) }
          jsonPath("$.content.id") { value(itemTwoId) }
          jsonPath("$.content.uuid") { isNotEmpty() }
          jsonPath("$.content.name") { value(createLrmItemTwoRequest().name) }
          jsonPath("$.content.description") { value(createLrmItemTwoRequest().description) }
          jsonPath("$.content.quantity") { value(createLrmItemTwoRequest().quantity) }
          jsonPath("$.content.created") { isNotEmpty() }
          jsonPath("$.content.updated") { isNotEmpty() }
          jsonPath("$.content.lists") { doesNotExist() }
        }
      }

      it("item 2 is found with all items") {
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
          jsonPath("$.size") { value(2) }
          jsonPath("$.content") { exists() }
          jsonPath("$.content") { isArray() }
          jsonPath("$.content.length()") { value(2) }
          jsonPath("$.content.[1].id") { value(itemTwoId) }
          jsonPath("$.content.[1].uuid") { isNotEmpty() }
          jsonPath("$.content.[1].name") { value(createLrmItemTwoRequest().name) }
          jsonPath("$.content.[1].description") { value(createLrmItemTwoRequest().description) }
          jsonPath("$.content.[1].quantity") { value(createLrmItemTwoRequest().quantity) }
          jsonPath("$.content.[1].created") { isNotEmpty() }
          jsonPath("$.content.[1].updated") { isNotEmpty() }
          jsonPath("$.content.[1].lists") { doesNotExist() }
        }
      }

      it("item 2 is found and updated") {
        val instance = "/items/$itemTwoId"
        mockMvc.patch(instance) {
          content = Json.encodeToString(updateLrmItemTwoRequest())
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
          jsonPath("$.content.id") { value(itemTwoId) }
          jsonPath("$.content.uuid") { isNotEmpty() }
          jsonPath("$.content.description") { value(updateLrmItemTwoRequest().description) }
          jsonPath("$.content.name") { value(updateLrmItemTwoRequest().name) }
          jsonPath("$.content.quantity") { value(updateLrmItemTwoRequest().quantity) }
          jsonPath("$.content.created") { isNotEmpty() }
          jsonPath("$.content.updated") { isNotEmpty() }
        }
      }
    }

    describe("list create, read, and update") {
      it("list 1 is created") {
        val instance = "/lists"
        mockMvc.post(instance) {
          content = Json.encodeToString(createLrmListOneRequest())
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
          jsonPath("$.content.id") { value(listOneId) }
          jsonPath("$.content.uuid") { isNotEmpty() }
          jsonPath("$.content.name") { value(createLrmListOneRequest().name) }
          jsonPath("$.content.description") { value(createLrmListOneRequest().description) }
          jsonPath("$.content.created") { isNotEmpty() }
          jsonPath("$.content.updated") { isNotEmpty() }
        }
      }

      it("list 1 is found") {
        val instance = "/lists/$listOneId"
        mockMvc.get(instance) {
          contentType = MediaType.APPLICATION_JSON
        }.andExpect {
          status { isOk() }
          content { contentType(MediaType.APPLICATION_JSON) }
          jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
          jsonPath("$.method") { value(HttpMethod.GET.name().lowercase()) }
          jsonPath("$.message") { value("retrieved list id $listOneId") }
          jsonPath("$.instance") { value(instance) }
          jsonPath("$.size") { value(1) }
          jsonPath("$.content.length()") { value(6) }
          jsonPath("$.content.id") { value(listOneId) }
          jsonPath("$.content.uuid") { isNotEmpty() }
          jsonPath("$.content.name") { value(createLrmListOneRequest().name) }
          jsonPath("$.content.description") { value(createLrmListOneRequest().description) }
          jsonPath("$.content.created") { isNotEmpty() }
          jsonPath("$.content.updated") { isNotEmpty() }
          jsonPath("$.content.items") { doesNotExist() }
        }
      }

      it("list 1 is found with all lists") {
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
          jsonPath("$.size") { value(1) }
          jsonPath("$.content") { exists() }
          jsonPath("$.content") { isArray() }
          jsonPath("$.content.length()") { value(1) }
          jsonPath("$.content.[0].id") { value(listOneId) }
          jsonPath("$.content.[0].uuid") { isNotEmpty() }
          jsonPath("$.content.[0].name") { value(createLrmListOneRequest().name) }
          jsonPath("$.content.[0].description") { value(createLrmListOneRequest().description) }
          jsonPath("$.content.[0].created") { isNotEmpty() }
          jsonPath("$.content.[0].updated") { isNotEmpty() }
          jsonPath("$.content.[0].items") { doesNotExist() }
        }
      }

      it("list 1 is found and updated") {
        val instance = "/lists/$listOneId"
        mockMvc.patch(instance) {
          content = Json.encodeToString(updateLrmListOneRequest())
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
          jsonPath("$.content.id") { value(listOneId) }
          jsonPath("$.content.uuid") { isNotEmpty() }
          jsonPath("$.content.name") { value(updateLrmListOneRequest().name) }
          jsonPath("$.content.description") { value(updateLrmListOneRequest().description) }
          jsonPath("$.content.created") { isNotEmpty() }
          jsonPath("$.content.updated") { isNotEmpty() }
        }
      }

      it("list 2 is created") {
        val instance = "/lists"
        mockMvc.post(instance) {
          content = Json.encodeToString(createLrmListTwoRequest())
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
          jsonPath("$.content.id") { value(listTwoId) }
          jsonPath("$.content.uuid") { isNotEmpty() }
          jsonPath("$.content.name") { value(createLrmListTwoRequest().name) }
          jsonPath("$.content.description") { value(createLrmListTwoRequest().description) }
          jsonPath("$.content.created") { isNotEmpty() }
          jsonPath("$.content.updated") { isNotEmpty() }
        }
      }

      it("list 2 is found") {
        val instance = "/lists/$listTwoId"
        mockMvc.get(instance) {
          contentType = MediaType.APPLICATION_JSON
        }.andExpect {
          status { isOk() }
          content { contentType(MediaType.APPLICATION_JSON) }
          jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
          jsonPath("$.method") { value(HttpMethod.GET.name().lowercase()) }
          jsonPath("$.message") { value("retrieved list id $listTwoId") }
          jsonPath("$.instance") { value(instance) }
          jsonPath("$.size") { value(1) }
          jsonPath("$.content.length()") { value(6) }
          jsonPath("$.content.id") { value(listTwoId) }
          jsonPath("$.content.uuid") { isNotEmpty() }
          jsonPath("$.content.name") { value(createLrmListTwoRequest().name) }
          jsonPath("$.content.description") { value(createLrmListTwoRequest().description) }
          jsonPath("$.content.created") { isNotEmpty() }
          jsonPath("$.content.updated") { isNotEmpty() }
          jsonPath("$.content.items") { doesNotExist() }
        }
      }

      it("list 2 is found with all lists") {
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
          jsonPath("$.size") { value(2) }
          jsonPath("$.content") { exists() }
          jsonPath("$.content") { isArray() }
          jsonPath("$.content.length()") { value(2) }
          jsonPath("$.content.[1].id") { value(listTwoId) }
          jsonPath("$.content.[1].uuid") { isNotEmpty() }
          jsonPath("$.content.[1].name") { value(createLrmListTwoRequest().name) }
          jsonPath("$.content.[1].description") { value(createLrmListTwoRequest().description) }
          jsonPath("$.content.[1].created") { isNotEmpty() }
          jsonPath("$.content.[1].updated") { isNotEmpty() }
          jsonPath("$.content.[1].items") { doesNotExist() }
        }
      }

      it("list 2 is found and updated") {
        val instance = "/lists/$listTwoId"
        mockMvc.patch(instance) {
          content = Json.encodeToString(updateLrmListTwoRequest())
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
          jsonPath("$.content.id") { value(listTwoId) }
          jsonPath("$.content.uuid") { isNotEmpty() }
          jsonPath("$.content.name") { value(updateLrmListTwoRequest().name) }
          jsonPath("$.content.description") { value(updateLrmListTwoRequest().description) }
          jsonPath("$.content.name") { value(updateLrmListTwoRequest().name) }
          jsonPath("$.content.created") { isNotEmpty() }
          jsonPath("$.content.updated") { isNotEmpty() }
        }
      }
    }

    describe("list content operations") {
      it("item 1 is added to list 1") {
        val instance = "/items/$itemOneId/add-to-list"
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
        val instance = "/items/$itemTwoId/add-to-list"
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
        val instance = "/items/$itemOneId/add-to-list"
        mockMvc.post(instance) {
          content = Json.encodeToString(listOneId)
          contentType = MediaType.APPLICATION_JSON
        }.andExpect {
          status { isUnprocessableEntity() }
          content { contentType(MediaType.APPLICATION_JSON) }
          jsonPath("$.disposition") { value(DispositionOfProblem.FAILURE.nameAsLowercase()) }
          jsonPath("$.method") { value(HttpMethod.POST.name().lowercase()) }
          jsonPath("$.message") { value("Item id $itemOneId could not be added to list id $listOneId because it's already been added.") }
          jsonPath("$.instance") { value(instance) }
          jsonPath("$.size") { value(1) }
          jsonPath("$.content.length()") { value(4) }
          jsonPath("$.content.status") { value(HttpStatus.UNPROCESSABLE_ENTITY.value()) }
        }
      }

      it("item 3 is not added to list 1 because the item couldn't be found") {
        val instance = "/items/3/add-to-list"
        mockMvc.post(instance) {
          content = Json.encodeToString(listOneId)
          contentType = MediaType.APPLICATION_JSON
        }.andExpect {
          status { isNotFound() }
          content { contentType(MediaType.APPLICATION_JSON) }
          jsonPath("$.disposition") { value(DispositionOfProblem.FAILURE.nameAsLowercase()) }
          jsonPath("$.method") { value(HttpMethod.POST.name().lowercase()) }
          jsonPath("$.message") { value("Item id 3 could not be added to list id $listOneId because the item couldn't be found.") }
          jsonPath("$.instance") { value(instance) }
          jsonPath("$.size") { value(1) }
          jsonPath("$.content.length()") { value(4) }
          jsonPath("$.content.status") { value(HttpStatus.NOT_FOUND.value()) }
        }
      }

      it("item 1 is not added to list 3 because the list couldn't be found") {
        val instance = "/items/$itemOneId/add-to-list"
        mockMvc.post(instance) {
          content = Json.encodeToString(3)
          contentType = MediaType.APPLICATION_JSON
        }.andExpect {
          status { isNotFound() }
          content { contentType(MediaType.APPLICATION_JSON) }
          jsonPath("$.disposition") { value(DispositionOfProblem.FAILURE.nameAsLowercase()) }
          jsonPath("$.method") { value(HttpMethod.POST.name().lowercase()) }
          jsonPath("$.message") { value("Item id $itemOneId could not be added to list id 3 because the list couldn't be found.") }
          jsonPath("$.instance") { value(instance) }
          jsonPath("$.size") { value(1) }
          jsonPath("$.content.length()") { value(4) }
          jsonPath("$.content.status") { value(HttpStatus.NOT_FOUND.value()) }
        }
      }

      it("item 1 is moved from list 1 to list 2") {
        val instance = "/items/$itemOneId/move-to-list"
        mockMvc.post(instance) {
          content = Json.encodeToString(LrmItemMoveToListRequest(fromListId = 1, toListId = 2))
          contentType = MediaType.APPLICATION_JSON
        }.andExpect {
          status { isOk() }
          content { contentType(MediaType.APPLICATION_JSON) }
          jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
          jsonPath("$.method") { value(HttpMethod.POST.name().lowercase()) }
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
    }

    describe("item delete") {
      it("delete item with no list associations") {
        TODO()
      }

      it("delete item 2 (associated with list 1)") {
        TODO()
      }

      it("delete item associated with list 1 and list 2 (multiple list association)") {
        TODO()
      }
    }

    describe("list delete") {
      it("delete list with no item associations") {
        TODO()
      }

      it("delete list with item associations") {
        TODO()
      }
    }
  }
})
