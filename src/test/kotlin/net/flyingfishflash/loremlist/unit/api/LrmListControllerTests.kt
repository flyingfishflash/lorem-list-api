package net.flyingfishflash.loremlist.unit.api

import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.extensions.spring.SpringExtension
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.datetime.Clock.System.now
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.flyingfishflash.loremlist.api.LrmItemApiService
import net.flyingfishflash.loremlist.api.LrmListApiService
import net.flyingfishflash.loremlist.api.LrmListController
import net.flyingfishflash.loremlist.api.data.request.LrmItemCreateRequest
import net.flyingfishflash.loremlist.api.data.request.LrmListCreateRequest
import net.flyingfishflash.loremlist.api.data.request.LrmListItemAddRequest
import net.flyingfishflash.loremlist.api.data.response.ApiServiceResponse
import net.flyingfishflash.loremlist.api.data.response.AssociationDeletedResponse
import net.flyingfishflash.loremlist.api.data.response.AssociationsDeletedResponse
import net.flyingfishflash.loremlist.api.data.response.LrmItemResponse
import net.flyingfishflash.loremlist.api.data.response.LrmListDeletedResponse
import net.flyingfishflash.loremlist.api.data.response.LrmListItemAddedResponse
import net.flyingfishflash.loremlist.api.data.response.LrmListItemMovedResponse
import net.flyingfishflash.loremlist.api.data.response.LrmListItemResponse
import net.flyingfishflash.loremlist.api.data.response.LrmListResponse
import net.flyingfishflash.loremlist.core.configuration.SerializationConfig
import net.flyingfishflash.loremlist.core.configuration.WebSecurityConfiguration
import net.flyingfishflash.loremlist.core.response.advice.CoreExceptionHandler.Companion.VALIDATION_FAILURE_MESSAGE
import net.flyingfishflash.loremlist.core.response.structure.ApiMessageNumeric
import net.flyingfishflash.loremlist.core.response.structure.DispositionOfProblem
import net.flyingfishflash.loremlist.core.response.structure.DispositionOfSuccess
import net.flyingfishflash.loremlist.domain.exceptions.DomainException
import net.flyingfishflash.loremlist.domain.lrmitem.ItemNotFoundException
import net.flyingfishflash.loremlist.domain.lrmitem.LrmItem
import net.flyingfishflash.loremlist.domain.lrmitem.LrmItemSuccinct
import net.flyingfishflash.loremlist.domain.lrmlist.ListNotFoundException
import net.flyingfishflash.loremlist.domain.lrmlist.LrmList
import net.flyingfishflash.loremlist.domain.lrmlistitem.ListItemNotFoundException
import net.flyingfishflash.loremlist.domain.lrmlistitem.LrmListItem
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.web.bind.MethodArgumentNotValidException
import java.util.UUID

/**
 * LrmListController Unit Tests
 */
@WebMvcTest(controllers = [LrmListController::class])
@Import(SerializationConfig::class, WebSecurityConfiguration::class)
class LrmListControllerTests(mockMvc: MockMvc) : DescribeSpec() {
  override fun extensions() = listOf(SpringExtension)

  @MockkBean
  lateinit var mockLrmListApiService: LrmListApiService

  @MockkBean
  lateinit var mockLrmItemApiService: LrmItemApiService

  init {
    val now = now()
    val id = (0..3).map { UUID.fromString("00000000-0000-4000-a000-00000000000$it") }
    val apiResponseMessage = "ksADs8y96KRa1Zo4ipMdr5t8faudmFj4c564S02MjsNG6TXEO7yctC08Bb53bCB7"
    val lrmItemCreateRequest = LrmItemCreateRequest(name = "Lorem Item Name", description = "Lorem Item Description", isSuppressed = false)

    fun createLrmList(id: UUID, nameSuffix: String = "") = LrmList(
      id = id,
      name = "Lorem List Name${if (nameSuffix.isNotEmpty()) " ($nameSuffix)" else ""}",
      description = "Lorem List Description",
      public = true,
      owner = "Lorem Ipsum Owner",
      created = now,
      creator = "Lorem Ipsum Created By",
      updated = now,
      updater = "Lorem Ipsum Updated By",
    )

    fun createLrmItem(id: UUID, nameSuffix: String = "") = LrmItem(
      id = id,
      name = "Lorem Item Name${if (nameSuffix.isNotEmpty()) " ($nameSuffix)" else ""}",
      description = "Lorem Item Description",
      owner = "Lorem Ipsum Owner",
      created = now,
      creator = "Lorem Ipsum Created By",
      updated = now,
      updater = "Lorem Ipsum Updated By",
    )

    fun createLrmListItem(id: UUID, nameSuffix: String = "") = LrmListItem(
      id = id,
      listId = UUID.randomUUID(),
      name = "Lorem List Item Name${if (nameSuffix.isNotEmpty()) " ($nameSuffix)" else ""}",
      description = "Lorem List Item Description",
      isSuppressed = false,
      owner = "Lorem Ipsum Owner",
      created = now,
      creator = "Lorem Ipsum Created By",
      updated = now,
      updater = "Lorem Ipsum Updated By",
    )

    fun lrmItem(): LrmItem = LrmItem(
      id = id[0],
      name = lrmItemCreateRequest.name,
      description = lrmItemCreateRequest.description,
      owner = "Lorem Ipsum Owner",
      created = now,
      creator = "Lorem Ipsum Created By",
      updated = now,
      updater = "Lorem Ipsum Updated By",

    )

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

    afterEach { clearAllMocks() }
    afterSpec { unmockkAll() }

    describe("/lists") {
      context("delete") {
        it("lists are deleted") {
          val instance = "/lists"
          val content = LrmListDeletedResponse(
            listNames = listOf("Lorem List Name"),
            associatedItemNames = listOf("Lorem Item Name"),
          )

          every { mockLrmListApiService.deleteByOwner(ofType(String::class)) } returns
            ApiServiceResponse(content = content, message = apiResponseMessage)

          performRequest(HttpMethod.DELETE, instance).andExpectAll(
            status().isOk(),
            content().contentType(MediaType.APPLICATION_JSON),
            jsonPath("$.disposition").value(DispositionOfSuccess.SUCCESS.nameAsLowercase()),
            jsonPath("$.method").value(HttpMethod.DELETE.name().lowercase()),
            jsonPath("$.message").value(apiResponseMessage),
            jsonPath("$.instance").value(instance),
            jsonPath("$.size").value(1),
            jsonPath("$.content.listNames[0]").value(content.listNames[0]),
            jsonPath("$.content.associatedItemNames[0]").value(content.associatedItemNames[0]),
          )
        }
      }

      describe("get") {
        it("lists are found") {
          val instance = "/lists"
          val content = listOf(LrmListResponse.fromLrmList(createLrmList(id[0])))

          every { mockLrmListApiService.findByOwnerExcludeItems(ofType(String::class)) } returns
            ApiServiceResponse(content, message = apiResponseMessage)

          performRequest(HttpMethod.GET, instance).andExpectAll(
            status().isOk(),
            content().contentType(MediaType.APPLICATION_JSON),
            jsonPath("$.disposition").value(DispositionOfSuccess.SUCCESS.nameAsLowercase()),
            jsonPath("$.method").value(HttpMethod.GET.name().lowercase()),
            jsonPath("$.message").value(apiResponseMessage),
            jsonPath("$.instance").value(instance),
            jsonPath("$.size").value(1),
            jsonPath("$.content").exists(),
            jsonPath("$.content").isArray(),
            jsonPath("$.content.[0].name").value(content[0].name),
            jsonPath("$.content.[0].description").value(content[0].description),
            jsonPath("$.content.[0].items").isEmpty(),
          )
        }

        it("lists are found ?includeItems=true") {
          val instance = "/lists?includeItems=true"
          val content = listOf(LrmListResponse.fromLrmList(createLrmList(id[0])))
          every { mockLrmListApiService.findByOwner(ofType(String::class)) } returns
            ApiServiceResponse(content, message = apiResponseMessage)

          performRequest(HttpMethod.GET, instance).andExpectAll(
            status().isOk(),
            content().contentType(MediaType.APPLICATION_JSON),
            jsonPath("$.disposition").value(DispositionOfSuccess.SUCCESS.nameAsLowercase()),
            jsonPath("$.method").value(HttpMethod.GET.name().lowercase()),
            jsonPath("$.message").value(apiResponseMessage),
            jsonPath("$.instance").value(instance.substringBeforeLast("?").removeSuffix(instance)),
            jsonPath("$.size").value(content.size),
            jsonPath("$.content").isArray(),
            jsonPath("$.content.[0].name").value(createLrmList(id[0]).name),
            jsonPath("$.content.[0].description").value(createLrmList(id[0]).description),
          )
        }
      }

      describe("post") {
        it("list is created") {
          val instance = "/lists"
          val lrmListCreateRequest = LrmListCreateRequest(
            name = "Lorem List Name",
            description = "Lorem List Description",
            public = true,
          )

          every { mockLrmListApiService.create(lrmListCreateRequest, ofType(String::class)) } returns
            ApiServiceResponse(LrmListResponse.fromLrmList(createLrmList(id[0])), message = apiResponseMessage)

          performRequest(HttpMethod.POST, instance, content = Json.encodeToString(lrmListCreateRequest)).andExpectAll(
            status().isOk(),
            content().contentType(MediaType.APPLICATION_JSON),
            jsonPath("$.disposition").value(DispositionOfSuccess.SUCCESS.nameAsLowercase()),
            jsonPath("$.method").value(HttpMethod.POST.name().lowercase()),
            jsonPath("$.message").value(apiResponseMessage),
            jsonPath("$.instance").value(instance),
            jsonPath("$.size").value(1),
            jsonPath("$.content.description").value(createLrmList(id[0]).description),
            jsonPath("$.content.name").value(createLrmList(id[0]).name),
          )
        }

        it("requested list name is an empty string") {
          val instance = "/lists"
          val content = LrmListCreateRequest("", createLrmList(id[0]).description, createLrmList(id[0]).public)

          performRequest(HttpMethod.POST, instance, content = Json.encodeToString(content)).andExpectAll(
            status().isBadRequest(),
            content().contentType(MediaType.APPLICATION_JSON),
            jsonPath("$.disposition").value(DispositionOfProblem.FAILURE.nameAsLowercase()),
            jsonPath("$.method").value(HttpMethod.POST.name().lowercase()),
            jsonPath("$.message").value("$VALIDATION_FAILURE_MESSAGE name."),
            jsonPath("$.instance").value(instance),
            jsonPath("$.size").value(1),
            jsonPath("$.content.title").value(MethodArgumentNotValidException::class.java.simpleName),
            jsonPath("$.content.status").value(HttpStatus.BAD_REQUEST.value()),
          )
        }

        it("requested list description is an empty string") {
          val instance = "/lists"
          val content = LrmListCreateRequest(createLrmList(id[0]).name, "", createLrmList(id[0]).public)

          performRequest(HttpMethod.POST, instance, content = Json.encodeToString(content)).andExpectAll(
            status().isBadRequest(),
            content().contentType(MediaType.APPLICATION_JSON),
            jsonPath("$.disposition").value(DispositionOfProblem.FAILURE.nameAsLowercase()),
            jsonPath("$.method").value(HttpMethod.POST.name().lowercase()),
            jsonPath("$.message").value("$VALIDATION_FAILURE_MESSAGE description."),
            jsonPath("$.instance").value(instance),
            jsonPath("$.size").value(1),
            jsonPath("$.content.title").value(MethodArgumentNotValidException::class.java.simpleName),
            jsonPath("$.content.status").value(HttpStatus.BAD_REQUEST.value()),
          )
        }
      }
    }

    describe("/lists/with-no-items") {
      context("get") {
        it("lists with no item association are found") {
          val instance = "/lists/with-no-items"
          val content = listOf(LrmListResponse.fromLrmList(createLrmList(id[0])))

          every { mockLrmListApiService.findByOwnerAndHavingNoItemAssociations(owner = ofType(String::class)) } returns
            ApiServiceResponse(content = content, message = apiResponseMessage)

          performRequest(HttpMethod.GET, instance).andExpectAll(
            status().isOk(),
            content().contentType(MediaType.APPLICATION_JSON),
            jsonPath("$.disposition").value(DispositionOfSuccess.SUCCESS.nameAsLowercase()),
            jsonPath("$.method").value(HttpMethod.GET.name().lowercase()),
            jsonPath("$.message").value(apiResponseMessage),
            jsonPath("$.instance").value(instance),
            jsonPath("$.size").value(content.size),
            jsonPath("$.content").exists(),
            jsonPath("$.content").isArray(),
            jsonPath("$.content.[0].name").value(content[0].name),
            jsonPath("$.content.[0].description").value(content[0].description),
          )
        }
      }
    }

    describe("/lists/count") {
      context("get") {
        it("count of lists is returned") {
          val instance = "/lists/count"

          every { mockLrmListApiService.countByOwner(owner = ofType(String::class)) } returns
            ApiServiceResponse(content = ApiMessageNumeric(999), message = apiResponseMessage)

          performRequest(HttpMethod.GET, instance).andExpectAll(
            status().isOk(),
            content().contentType(MediaType.APPLICATION_JSON),
            jsonPath("$.disposition").value(DispositionOfSuccess.SUCCESS.nameAsLowercase()),
            jsonPath("$.method").value(HttpMethod.GET.name().lowercase()),
            jsonPath("$.message").value(apiResponseMessage),
            jsonPath("$.instance").value(instance),
            jsonPath("$.size").value(1),
            jsonPath("$.content.length()").value(1),
            jsonPath("$.content.value").value(999),
          )
        }
      }
    }

    describe("/lists/{list-id}") {
      context("delete") {
        it("list is deleted") {
          val instance = "/lists/${id[1]}"
          val content =
            LrmListDeletedResponse(listNames = listOf("dolor sit amet"), associatedItemNames = listOf("Lorem Ipsum"))

          // nonsensical conditioning of the delete response:
          // if the count of item to list associations is 0, then associatedListNames should be an empty list
          every {
            mockLrmListApiService.deleteByOwnerAndId(
              id = id[1],
              owner = ofType(String::class),
              removeItemAssociations = false,
            )
          } returns ApiServiceResponse(content = content, message = apiResponseMessage)

          performRequest(HttpMethod.DELETE, instance).andExpectAll(
            status().isOk(),
            content().contentType(MediaType.APPLICATION_JSON),
            jsonPath("$.disposition").value(DispositionOfSuccess.SUCCESS.nameAsLowercase()),
            jsonPath("$.method").value(HttpMethod.DELETE.name().lowercase()),
            jsonPath("$.message").value(apiResponseMessage),
            jsonPath("$.instance").value(instance),
            jsonPath("$.size").value(1),
            jsonPath("$.content.listNames.length()").value(1),
            jsonPath("$.content.listNames.[0]").value("dolor sit amet"),
            jsonPath("$.content.associatedItemNames.length()").value(1),
            jsonPath("$.content.associatedItemNames.[0]").value("Lorem Ipsum"),
          )
        }

        it("list is not found") {
          val instance = "/lists/${id[1]}"

          every {
            mockLrmListApiService.deleteByOwnerAndId(
              id = id[1],
              owner = ofType(String::class),
              removeItemAssociations = false,
            )
          } throws ListNotFoundException(id[1])

          performRequest(HttpMethod.DELETE, instance).andExpectAll(
            status().isNotFound(),
            content().contentType(MediaType.APPLICATION_JSON),
            jsonPath("$.disposition").value(DispositionOfProblem.FAILURE.nameAsLowercase()),
            jsonPath("$.method").value(HttpMethod.DELETE.name().lowercase()),
            jsonPath("$.instance").value(instance),
            jsonPath("$.size").value(1),
            jsonPath("$.content.title").value(ListNotFoundException::class.java.simpleName),
            jsonPath("$.content.status").value(HttpStatus.NOT_FOUND.value()),
          )
        }
      }

      describe("get") {
        it("list is found") {
          val instance = "/lists/${id[1]}"

          every { mockLrmListApiService.findByOwnerAndId(id = id[1], owner = ofType(String::class)) } returns
            ApiServiceResponse((LrmListResponse.fromLrmList(createLrmList(id[0]))), message = apiResponseMessage)

          performRequest(HttpMethod.GET, instance).andExpectAll(
            status().isOk(),
            content().contentType(MediaType.APPLICATION_JSON),
            jsonPath("$.disposition").value(DispositionOfSuccess.SUCCESS.nameAsLowercase()),
            jsonPath("$.method").value(HttpMethod.GET.name().lowercase()),
            jsonPath("$.message").value(apiResponseMessage),
            jsonPath("$.instance").value(instance),
            jsonPath("$.size").value(1),
            jsonPath("$.content.description").value(createLrmList(id[0]).description),
            jsonPath("$.content.name").value(createLrmList(id[0]).name),
          )
        }

        it("list is found ?includeItems=true") {
          val instance = "/lists/${id[1]}?includeItems=true"

          every { mockLrmListApiService.findByOwnerAndId(id = id[1], owner = ofType(String::class)) } returns
            ApiServiceResponse(LrmListResponse.fromLrmList(createLrmList(id[0])), message = apiResponseMessage)

          performRequest(HttpMethod.GET, instance).andExpectAll(
            status().isOk(),
            content().contentType(MediaType.APPLICATION_JSON),
            jsonPath("$.disposition").value(DispositionOfSuccess.SUCCESS.nameAsLowercase()),
            jsonPath("$.method").value(HttpMethod.GET.name().lowercase()),
            jsonPath("$.message").value(apiResponseMessage),
            jsonPath("$.instance").value(instance.substringBeforeLast("?").removeSuffix(instance)),
            jsonPath("$.size").value(1),
            jsonPath("$.content.description").value(createLrmList(id[0]).description),
            jsonPath("$.content.name").value(createLrmList(id[0]).name),
            jsonPath("$.content.items").isArray(),
          )
        }

        it("list is found ?includeItems=false") {
          val instance = "/lists/${id[1]}?includeItems=false"

          every {
            mockLrmListApiService.findByOwnerAndIdExcludeItems(
              id = id[1],
              owner = ofType(String::class),
            )
          } returns
            ApiServiceResponse(LrmListResponse.fromLrmList(createLrmList(id[0])), message = apiResponseMessage)

          performRequest(HttpMethod.GET, instance).andExpectAll(
            status().isOk(),
            content().contentType(MediaType.APPLICATION_JSON),
            jsonPath("$.disposition").value(DispositionOfSuccess.SUCCESS.nameAsLowercase()),
            jsonPath("$.method").value(HttpMethod.GET.name().lowercase()),
            jsonPath("$.message").value(apiResponseMessage),
            jsonPath("$.instance").value(instance.substringBeforeLast("?").removeSuffix(instance)),
            jsonPath("$.size").value(1),
            jsonPath("$.content.description").value(createLrmList(id[0]).description),
            jsonPath("$.content.name").value(createLrmList(id[0]).name),
            jsonPath("$.content.items").isEmpty(),
          )
        }

        it("list is not found") {
          val instance = "/lists/${id[1]}"

          every { mockLrmListApiService.findByOwnerAndId(id = id[1], owner = ofType(String::class)) } throws
            ListNotFoundException(id[1])

          performRequest(HttpMethod.GET, instance).andExpectAll(
            status().isNotFound(),
            content().contentType(MediaType.APPLICATION_JSON),
            jsonPath("$.disposition").value(DispositionOfProblem.FAILURE.nameAsLowercase()),
            jsonPath("$.method").value(HttpMethod.GET.name().lowercase()),
            jsonPath("$.instance").value(instance),
            jsonPath("$.size").value(1),
            jsonPath("$.content.title").value(ListNotFoundException::class.java.simpleName),
            jsonPath("$.content.status").value(HttpStatus.NOT_FOUND.value()),
          )
        }
      }

      describe("patch") {
        it("list is found and updated") {
          val instance = "/lists/${id[1]}"

          every { mockLrmListApiService.patchByOwnerAndId(id = id[1], owner = ofType(String::class), any()) } returns
            ApiServiceResponse(
              content = LrmListResponse.fromLrmList(createLrmList(id[0])),
              message = apiResponseMessage,
            )

          performRequest(
            HttpMethod.PATCH,
            instance,
            Json.encodeToString(mapOf("name" to createLrmList(id[0]).name)),
          ).andExpectAll(
            status().isOk(),
            content().contentType(MediaType.APPLICATION_JSON),
            jsonPath("$.disposition").value(DispositionOfSuccess.SUCCESS.nameAsLowercase()),
            jsonPath("$.method").value(HttpMethod.PATCH.name().lowercase()),
            jsonPath("$.message").value(apiResponseMessage),
            jsonPath("$.instance").value(instance),
            jsonPath("$.size").value(1),
            jsonPath("$.content.description").value(createLrmList(id[0]).description),
            jsonPath("$.content.name").value(createLrmList(id[0]).name),
            jsonPath("$.content.items").isEmpty(),
          )
        }

        it("list is found and not updated") {
          val instance = "/lists/${id[1]}"

          every { mockLrmListApiService.patchByOwnerAndId(id = id[1], owner = ofType(String::class), any()) } returns
            ApiServiceResponse(content = LrmListResponse.fromLrmList(createLrmList(id[0])), message = "not updated")

          performRequest(
            HttpMethod.PATCH,
            instance,
            Json.encodeToString(mapOf("name" to createLrmList(id[0]).name)),
          ).andExpectAll(
            status().isNoContent(),
            content().contentType(MediaType.APPLICATION_JSON),
            jsonPath("$.disposition").value(DispositionOfSuccess.SUCCESS.nameAsLowercase()),
            jsonPath("$.method").value(HttpMethod.PATCH.name().lowercase()),
            jsonPath("$.instance").value(instance),
            jsonPath("$.size").value(1),
            jsonPath("$.content.description").value(createLrmList(id[0]).description),
            jsonPath("$.content.name").value(createLrmList(id[0]).name),
            jsonPath("$.content.items").isEmpty(),
          )
        }

        it("list is not found") {
          val instance = "/lists/${id[1]}"

          every { mockLrmListApiService.patchByOwnerAndId(id = id[1], owner = ofType(String::class), any()) } throws
            ListNotFoundException(id[1])

          performRequest(
            HttpMethod.PATCH,
            instance,
            Json.encodeToString(mapOf("name" to createLrmList(id[0]).name)),
          ).andExpectAll(
            status().isNotFound(),
            content().contentType(MediaType.APPLICATION_JSON),
            jsonPath("$.disposition").value(DispositionOfProblem.FAILURE.nameAsLowercase()),
            jsonPath("$.method").value(HttpMethod.PATCH.name().lowercase()),
            jsonPath("$.instance").value(instance),
            jsonPath("$.size").value(1),
            jsonPath("$.content.title").value(ListNotFoundException::class.java.simpleName),
            jsonPath("$.content.status").value(HttpStatus.NOT_FOUND.value()),
          )
        }
      }
    }

    describe("/lists/{list-id}/items") {
      val instance = "/lists/${id[1]}/items"
      context("delete") {
        it("all items are removed from a list") {
          every {
            mockLrmListApiService.removeAllListItems(
              listId = id[1],
              listOwner = ofType(String::class),
            )
          } returns ApiServiceResponse(
            AssociationsDeletedResponse(itemName = "irrelevant", 999),
            message = apiResponseMessage,
          )

          performRequest(HttpMethod.DELETE, instance).andExpectAll(
            status().isOk(),
            content().contentType(MediaType.APPLICATION_JSON),
            jsonPath("$.disposition").value(DispositionOfSuccess.SUCCESS.nameAsLowercase()),
            jsonPath("$.method").value(HttpMethod.DELETE.name().lowercase()),
            jsonPath("$.message").value(apiResponseMessage),
            jsonPath("$.instance").value(instance),
            jsonPath("$.size").value(1),
            jsonPath("$.content.value").value(999),
          )
        }
      }

      context("post") {
        val lrmItemCreateRequest = LrmItemCreateRequest(name = "List Item Name", description = "List Item Description", quantity = 99, isSuppressed = false)

        it("list item is created and added to a list") {
          val lrmListItemResponse = LrmListItemResponse.fromLrmListItem(createLrmListItem(id[0]))

          every {
            mockLrmListApiService.createListItem(
              listId = ofType<UUID>(),
              itemCreateRequest = lrmItemCreateRequest,
              creator = ofType<String>(),
            )
          } returns
            ApiServiceResponse(content = lrmListItemResponse, message = apiResponseMessage)

          performRequest(HttpMethod.POST, instance, Json.encodeToString(lrmItemCreateRequest)).andExpectAll(
            status().isOk(),
            content().contentType(MediaType.APPLICATION_JSON),
            jsonPath("$.disposition").value(DispositionOfSuccess.SUCCESS.nameAsLowercase()),
            jsonPath("$.method").value(HttpMethod.POST.name().lowercase()),
            jsonPath("$.message").value(apiResponseMessage),
            jsonPath("$.instance").value(instance),
            jsonPath("$.size").value(1),
            jsonPath("$.content.id").value(lrmListItemResponse.id.toString()),
          )
        }

        it("list item is not created") {
          every {
            mockLrmListApiService.createListItem(
              listId = ofType<UUID>(),
              itemCreateRequest = lrmItemCreateRequest,
              creator = ofType<String>(),
            )
          } throws DomainException()

          performRequest(HttpMethod.POST, instance, Json.encodeToString(lrmItemCreateRequest)).andExpectAll(
            status().isInternalServerError(),
            content().contentType(MediaType.APPLICATION_JSON),
            jsonPath("$.disposition").value(DispositionOfProblem.ERROR.nameAsLowercase()),
            jsonPath("$.method").value(HttpMethod.POST.name().lowercase()),
            jsonPath("$.message").value(DomainException.DEFAULT_TITLE),
            jsonPath("$.instance").value(instance),
            jsonPath("$.size").value(1),
            jsonPath("$.content.detail").value(DomainException.DEFAULT_TITLE),
          )
        }
      }
    }

    describe("/lists/{list-id}/items/{item-id}") {
      context("delete") {
        it("item is removed from list") {
          val instance = "/lists/${id[1]}/items/${id[2]}"
          val lrmItemName = "58cVf5N8rSstjC6L"
          val lrmListName = "nxuS5LKlpP9TVhzV"

          every {
            mockLrmListApiService.removeListItem(
              itemId = id[2],
              listId = id[1],
              componentsOwner = ofType(String::class),
            )
          } returns
            ApiServiceResponse(
              content = AssociationDeletedResponse(itemName = lrmItemName, listName = lrmListName),
              message = apiResponseMessage,
            )

          performRequest(HttpMethod.DELETE, instance).andExpectAll(
            status().isOk(),
            content().contentType(MediaType.APPLICATION_JSON),
            jsonPath("$.disposition").value(DispositionOfSuccess.SUCCESS.nameAsLowercase()),
            jsonPath("$.method").value(HttpMethod.DELETE.name().lowercase()),
            jsonPath("$.message").value(apiResponseMessage),
            jsonPath("$.instance").value(instance),
            jsonPath("$.size").value(1),
          )
        }

        it("item is not found") {
          val instance = "/lists/${id[1]}/items/${id[2]}"

          every {
            mockLrmListApiService.removeListItem(
              itemId = id[2],
              listId = id[1],
              componentsOwner = ofType(String::class),
            )
          } throws ItemNotFoundException(id[2])

          performRequest(HttpMethod.DELETE, instance).andExpectAll(
            status().isNotFound(),
            content().contentType(MediaType.APPLICATION_JSON),
            jsonPath("$.disposition").value(DispositionOfProblem.FAILURE.nameAsLowercase()),
            jsonPath("$.method").value(HttpMethod.DELETE.name().lowercase()),
            jsonPath("$.instance").value(instance),
            jsonPath("$.size").value(1),
            jsonPath("$.content.title").value(ItemNotFoundException::class.java.simpleName),
            jsonPath("$.content.status").value(HttpStatus.NOT_FOUND.value()),
          )
        }

        it("list is not found") {
          val instance = "/lists/${id[1]}/items/${id[2]}"

          every {
            mockLrmListApiService.removeListItem(
              itemId = id[2],
              listId = id[1],
              componentsOwner = ofType(String::class),
            )
          } throws ListNotFoundException(id[1])

          performRequest(HttpMethod.DELETE, instance).andExpectAll(
            status().isNotFound(),
            content().contentType(MediaType.APPLICATION_JSON),
            jsonPath("$.disposition").value(DispositionOfProblem.FAILURE.nameAsLowercase()),
            jsonPath("$.method").value(HttpMethod.DELETE.name().lowercase()),
            jsonPath("$.instance").value(instance),
            jsonPath("$.size").value(1),
            jsonPath("$.content.title").value(ListNotFoundException::class.java.simpleName),
            jsonPath("$.content.status").value(HttpStatus.NOT_FOUND.value()),
          )
        }
      }

      context("patch") {
        it("list item is updated") {
          val instance = "/lists/${id[1]}/items/${id[2]}"
          val apiResponseContent = LrmListItemResponse.fromLrmListItem(createLrmListItem(id[3]))

          every {
            mockLrmListApiService.patchListItem(
              itemId = id[2],
              listId = id[1],
              listOwner = ofType(String::class),
              patchRequest = any(),
            )
          } returns ApiServiceResponse(
            content = apiResponseContent,
            message = apiResponseMessage,
          )

          performRequest(
            HttpMethod.PATCH,
            instance,
            Json.encodeToString(mapOf("name" to createLrmItem(id[0]).name)),
          ).andExpectAll(
            status().isOk(),
            content().contentType(MediaType.APPLICATION_JSON),
            jsonPath("$.disposition").value(DispositionOfSuccess.SUCCESS.nameAsLowercase()),
            jsonPath("$.method").value(HttpMethod.PATCH.name().lowercase()),
            jsonPath("$.message").value(apiResponseMessage),
            jsonPath("$.instance").value(instance),
            jsonPath("$.size").value(1),
            jsonPath("$.content.name").value(apiResponseContent.name),
          )
        }

        it("list item is not updated") {
          val instance = "/lists/${id[1]}/items/${id[2]}"
          val apiResponseContent = LrmListItemResponse.fromLrmListItem(createLrmListItem(id[3]))

          every {
            mockLrmListApiService.patchListItem(
              itemId = id[2],
              listId = id[1],
              listOwner = ofType(String::class),
              patchRequest = any(),
            )
          } returns ApiServiceResponse(
            content = apiResponseContent,
            message = "not updated",
          )

          performRequest(
            HttpMethod.PATCH,
            instance,
            Json.encodeToString(mapOf("name" to createLrmItem(id[0]).name)),
          ).andExpectAll(
            status().isNoContent,
            content().contentType(MediaType.APPLICATION_JSON),
            jsonPath("$.disposition").value(DispositionOfSuccess.SUCCESS.nameAsLowercase()),
            jsonPath("$.method").value(HttpMethod.PATCH.name().lowercase()),
            jsonPath("$.message").value("not updated"),
            jsonPath("$.instance").value(instance),
            jsonPath("$.size").value(1),
            jsonPath("$.content.name").value(apiResponseContent.name),
          )
        }

        it("list item is not found") {
          val instance = "/lists/${id[1]}/items/${id[2]}"

          every {
            mockLrmListApiService.patchListItem(
              itemId = id[2],
              listId = id[1],
              listOwner = ofType(String::class),
              patchRequest = any(),
            )
          } throws ListItemNotFoundException()

          performRequest(
            HttpMethod.PATCH,
            instance,
            Json.encodeToString(mapOf("name" to createLrmItem(id[0]).name)),
          ).andExpectAll(
            status().isNotFound(),
            content().contentType(MediaType.APPLICATION_JSON),
            jsonPath("$.disposition").value(DispositionOfProblem.FAILURE.nameAsLowercase()),
            jsonPath("$.method").value(HttpMethod.PATCH.name().lowercase()),
            jsonPath("$.message").value("ListItem could not be found."),
            jsonPath("$.instance").value(instance),
            jsonPath("$.size").value(1),
          )
        }
      }

      describe("put") {
        it("item is added to list") {
          val instance = "/lists/${id[1]}/items"

          val mockResponse = LrmListItemAddedResponse(
            componentName = createLrmList(id[1]).name,
            associatedComponents = listOf(LrmItemSuccinct.fromLrmItem(createLrmItem(id[2]))),
          )

          every {
            mockLrmListApiService.addListItem(
              listId = id[1],
              itemIdCollection = any(),
              owner = ofType(String::class),
            )
          } returns ApiServiceResponse(content = mockResponse, message = apiResponseMessage)

          val requestBody = LrmListItemAddRequest(itemIdCollection = setOf(UUID.randomUUID()))

          performRequest(
            HttpMethod.PUT,
            instance,
            Json.encodeToString(requestBody),
          ).andExpectAll(
            status().isOk(),
            content().contentType(MediaType.APPLICATION_JSON),
            jsonPath("$.disposition").value(DispositionOfSuccess.SUCCESS.nameAsLowercase()),
            jsonPath("$.method").value(HttpMethod.PUT.name().lowercase()),
            jsonPath("$.message").value(apiResponseMessage),
            jsonPath("$.instance").value(instance),
            jsonPath("$.size").value(1),
            jsonPath("$.content.componentName").value(mockResponse.componentName),
            jsonPath("$.content.associatedComponents.length()").value(1),
            jsonPath("$.content.associatedComponents[0].type").value("item"),
            jsonPath("$.content.associatedComponents[0].id").value(mockResponse.associatedComponents[0].id.toString()),
            jsonPath("$.content.associatedComponents[0].name").value(mockResponse.associatedComponents[0].name),
          )
        }

        it("item is added to lists") {
          val instance = "/lists/${id[1]}/items"

          val mockResponse = LrmListItemAddedResponse(
            componentName = createLrmList(id[1]).name,
            associatedComponents = listOf(
              LrmItemSuccinct.fromLrmItem(createLrmItem(id[2])),
              LrmItemSuccinct.fromLrmItem(createLrmItem(id[3])),
            ),
          )

          every {
            mockLrmListApiService.addListItem(
              listId = id[1],
              itemIdCollection = any(),
              owner = ofType(String::class),
            )
          } returns ApiServiceResponse(content = mockResponse, message = apiResponseMessage)

          val requestBody = LrmListItemAddRequest(itemIdCollection = setOf(UUID.randomUUID()))

          performRequest(
            HttpMethod.PUT,
            instance,
            Json.encodeToString(requestBody),
          ).andExpectAll(
            status().isOk(),
            content().contentType(MediaType.APPLICATION_JSON),
            jsonPath("$.disposition").value(DispositionOfSuccess.SUCCESS.nameAsLowercase()),
            jsonPath("$.method").value(HttpMethod.PUT.name().lowercase()),
            jsonPath("$.message").value(apiResponseMessage),
            jsonPath("$.instance").value(instance),
            jsonPath("$.size").value(1),
            jsonPath("$.content.componentName").value(mockResponse.componentName),
            jsonPath("$.content.associatedComponents.length()").value(2),
            jsonPath("$.content.associatedComponents[0].type").value("item"),
            jsonPath("$.content.associatedComponents[0].id").value(mockResponse.associatedComponents[0].id.toString()),
            jsonPath("$.content.associatedComponents[0].name").value(mockResponse.associatedComponents[0].name),
          )
        }

        it("list is not found") {
          val instance = "/lists/${id[1]}/items"

          every {
            mockLrmListApiService.addListItem(
              listId = id[1],
              itemIdCollection = any(),
              owner = ofType(String::class),
            )
          } throws ListNotFoundException(id[2])

          val requestBody = LrmListItemAddRequest(itemIdCollection = setOf(UUID.randomUUID()))

          performRequest(
            HttpMethod.PUT,
            instance,
            Json.encodeToString(requestBody),
          ).andExpectAll(
            status().isNotFound(),
            content().contentType(MediaType.APPLICATION_JSON),
            jsonPath("$.disposition").value(DispositionOfProblem.FAILURE.nameAsLowercase()),
            jsonPath("$.method").value(HttpMethod.PUT.name().lowercase()),
            jsonPath("$.instance").value(instance),
            jsonPath("$.size").value(1),
            jsonPath("$.content.title").value(ListNotFoundException::class.java.simpleName),
            jsonPath("$.content.status").value(HttpStatus.NOT_FOUND.value()),
          )
        }

        it("item is not found") {
          val instance = "/lists/${id[1]}/items"

          every {
            mockLrmListApiService.addListItem(
              listId = id[1],
              itemIdCollection = any(),
              owner = ofType(String::class),
            )
          } throws ItemNotFoundException(id[2])

          val requestBody = LrmListItemAddRequest(itemIdCollection = setOf(UUID.randomUUID()))

          performRequest(
            HttpMethod.PUT,
            instance,
            Json.encodeToString(requestBody),
          ).andExpectAll(
            status().isNotFound(),
            content().contentType(MediaType.APPLICATION_JSON),
            jsonPath("$.disposition").value(DispositionOfProblem.FAILURE.nameAsLowercase()),
            jsonPath("$.method").value(HttpMethod.PUT.name().lowercase()),
            jsonPath("$.instance").value(instance),
            jsonPath("$.size").value(1),
            jsonPath("$.content.title").value(ItemNotFoundException::class.java.simpleName),
            jsonPath("$.content.status").value(HttpStatus.NOT_FOUND.value()),
          )
        }
      }
    }

    describe("/lists/{list-id}/items/{item-id}/{list-id}") {
      context("patch") {
        it("list item is moved from one list to another") {
          val instance = "/lists/${id[1]}/items/${id[2]}/${id[3]}"
          val apiResponseContent = LrmListItemMovedResponse(itemName = "", currentListName = "", newListName = "")

          every {
            mockLrmListApiService.moveListItem(id[1], id[2], id[3], ofType<String>())
          } returns ApiServiceResponse(content = apiResponseContent, message = apiResponseMessage)

          performRequest(HttpMethod.PATCH, instance).andExpectAll(
            status().isOk(),
            content().contentType(MediaType.APPLICATION_JSON),
            jsonPath("$.disposition").value(DispositionOfSuccess.SUCCESS.nameAsLowercase()),
            jsonPath("$.method").value(HttpMethod.PATCH.name().lowercase()),
            jsonPath("$.message").value(apiResponseMessage),
            jsonPath("$.instance").value(instance),
            jsonPath("$.size").value(1),
            jsonPath("$.content.message").value(apiResponseMessage),
          )
        }
      }
    }

    describe("/lists/{list-id}/items/count") {
      context("get") {
        it("count of item associations is returned") {
          val instance = "/lists/${id[1]}/items/count"

          every { mockLrmListApiService.countListItems(listId = id[1], listOwner = ofType(String::class)) } returns
            ApiServiceResponse(content = ApiMessageNumeric(999), message = apiResponseMessage)

          performRequest(HttpMethod.GET, instance).andExpectAll(
            status().isOk(),
            content().contentType(MediaType.APPLICATION_JSON),
            jsonPath("$.disposition").value(DispositionOfSuccess.SUCCESS.nameAsLowercase()),
            jsonPath("$.method").value(HttpMethod.GET.name().lowercase()),
            jsonPath("$.message").value(apiResponseMessage),
            jsonPath("$.instance").value(instance),
            jsonPath("$.size").value(1),
            jsonPath("$.content.length()").value(1),
            jsonPath("$.content.value").value(999),
          )
        }

        it("item is not found") {
          val instance = "/lists/${id[1]}/items/count"

          every {
            mockLrmListApiService.countListItems(listId = id[1], listOwner = ofType(String::class))
          } throws DomainException(httpStatus = HttpStatus.NOT_FOUND)

          performRequest(HttpMethod.GET, instance).andExpectAll(
            status().isNotFound(),
            jsonPath("$.disposition").value(DispositionOfProblem.FAILURE.nameAsLowercase()),
            jsonPath("$.method").value(HttpMethod.GET.name().lowercase()),
            jsonPath("$.instance").value(instance),
            jsonPath("$.size").value(1),
            jsonPath("$.content.status").value(HttpStatus.NOT_FOUND.value()),
          )
        }
      }
    }

    describe("/lists/{list-id}/items/eligible") {
      describe("get") {
        it("items eligible for list are returned") {
          val listId = UUID.fromString("00000000-0000-4000-a000-000000000010")
          val serviceResponse = listOf(LrmItemResponse.fromLrmItem(lrmItem()))
          val mockApiServiceResponse = ApiServiceResponse(serviceResponse, "message is irrelevant")
          every {
            mockLrmItemApiService.findByOwnerAndHavingNoListAssociations(owner = ofType(String::class), listId = listId)
          } returns mockApiServiceResponse
          val instance = "/lists/$listId/items/eligible"
          mockMvc.get(instance) {
            with(jwt())
            contentType = MediaType.APPLICATION_JSON
          }.andExpect {
            status { isOk() }
            content { contentType(MediaType.APPLICATION_JSON) }
            jsonPath("$.disposition") { value(DispositionOfSuccess.SUCCESS.nameAsLowercase()) }
            jsonPath("$.method") { value(HttpMethod.GET.name().lowercase()) }
            jsonPath("$.message") { value("message is irrelevant") }
            jsonPath("$.instance") { value(instance) }
            jsonPath("$.size") { value(serviceResponse.size) }
            jsonPath("$.content") { exists() }
            jsonPath("$.content") { isArray() }
            jsonPath("$.content.[0].name") { value(serviceResponse[0].name) }
            jsonPath("$.content.[0].description") { value(serviceResponse[0].description) }
          }
          verify(exactly = 1) {
            mockLrmItemApiService.findByOwnerAndHavingNoListAssociations(owner = ofType(String::class), listId = listId)
          }
        }
      }
    }
  }
}
