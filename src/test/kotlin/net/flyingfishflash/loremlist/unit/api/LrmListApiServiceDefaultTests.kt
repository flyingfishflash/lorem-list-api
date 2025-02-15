package net.flyingfishflash.loremlist.unit.api

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.datetime.Clock.System.now
import net.flyingfishflash.loremlist.api.LrmListApiServiceDefault
import net.flyingfishflash.loremlist.api.data.request.LrmListCreateRequest
import net.flyingfishflash.loremlist.api.data.response.ApiServiceResponse
import net.flyingfishflash.loremlist.api.data.response.LrmListDeletedResponse
import net.flyingfishflash.loremlist.api.data.response.LrmListResponse
import net.flyingfishflash.loremlist.core.response.structure.ApiMessageNumeric
import net.flyingfishflash.loremlist.domain.LrmComponentType
import net.flyingfishflash.loremlist.domain.ServiceResponse
import net.flyingfishflash.loremlist.domain.association.AssociationService
import net.flyingfishflash.loremlist.domain.association.data.AssociationCreated
import net.flyingfishflash.loremlist.domain.lrmitem.LrmItem
import net.flyingfishflash.loremlist.domain.lrmitem.LrmItemSuccinct
import net.flyingfishflash.loremlist.domain.lrmlist.LrmList
import net.flyingfishflash.loremlist.domain.lrmlist.LrmListServiceDefault
import net.flyingfishflash.loremlist.domain.lrmlist.data.LrmListCreate
import net.flyingfishflash.loremlist.domain.lrmlist.data.LrmListDeleted
import java.util.*

class LrmListApiServiceDefaultTests :
  DescribeSpec({
    val mockLrmListService = mockk<LrmListServiceDefault>()
    val mockAssociationService = mockk<AssociationService>()
    val lrmListApiService = LrmListApiServiceDefault(mockLrmListService, mockAssociationService)

    val id0 = UUID.fromString("00000000-0000-4000-a000-000000000000")
    val now = now()
    val lrmListCreateRequest = LrmListCreateRequest(
      name = "Lorem List Name",
      description = "Lorem List Description",
      public = true,
    )
    val irrelevantMessage = "ksADs8y96KRa1Zo4ipMdr5t8faudmFj4c564S02MjsNG6TXEO7yctC08Bb53bCB7"

    fun lrmItem(): LrmItem = LrmItem(
      id = id0,
      name = "Lorem Item Name",
      description = "Lorem Item Description",
      quantity = 0,
      created = now,
      createdBy = "Lorem Ipsum Created By",
      updated = now,
      updatedBy = "Lorem Ipsum Updated By",
    )

    fun lrmList(): LrmList = LrmList(
      id = id0,
      name = lrmListCreateRequest.name,
      description = lrmListCreateRequest.description,
      public = lrmListCreateRequest.public,
      items = setOf(lrmItem()),
      created = now,
      createdBy = "Lorem Ipsum Created By",
      updated = now,
      updatedBy = "Lorem Ipsum Updated By",
    )

    afterEach { clearAllMocks() }
    afterSpec { unmockkAll() }

    describe("LrmListApiServiceDefault") {
      it("count lists by owner") {
        val owner = "test_owner"
        val serviceResponse = ServiceResponse(10L, message = irrelevantMessage)
        val apiServiceResponse = ApiServiceResponse(content = ApiMessageNumeric(serviceResponse.content), message = serviceResponse.message)
        every { mockLrmListService.countByOwner(owner) } returns serviceResponse
        lrmListApiService.countByOwner(owner) shouldBe apiServiceResponse
        verify { mockLrmListService.countByOwner(owner) }
      }

      it("create a new list") {
        val lrmList = lrmList()
        val owner = "test_owner"
        val serviceResponse = ServiceResponse(content = lrmList, message = irrelevantMessage)
        val apiServiceResponse = ApiServiceResponse(
          content = LrmListResponse.fromLrmList(serviceResponse.content),
          message = serviceResponse.message,
        )
        every { mockLrmListService.create(ofType(LrmListCreate::class), owner) } returns serviceResponse
        lrmListApiService.create(mockk<LrmListCreateRequest>(relaxed = true), owner) shouldBe
          apiServiceResponse
        verify { mockLrmListService.create(ofType(LrmListCreate::class), owner) }
      }

      it("delete lists by owner") {
        // TODO: evaluate the veracity of this test
        val owner = "test_owner"
        val serviceResponse = ServiceResponse(
          LrmListDeleted(listOf("test_list_name"), listOf("test_list_name")),
          message = irrelevantMessage,
        )
        val apiServiceResponse =
          ApiServiceResponse(
            content = LrmListDeletedResponse(
              listNames = serviceResponse.content.listNames,
              associatedItemNames = serviceResponse.content.associatedItemNames,
            ),
            message = serviceResponse.message,
          )
        every { mockLrmListService.deleteByOwner(owner) } returns serviceResponse
        lrmListApiService.deleteByOwner(owner) shouldBe apiServiceResponse
        verify { mockLrmListService.deleteByOwner(owner) }
      }

      it("delete list by owner and id") {
        val id = UUID.randomUUID()
        val owner = "test_owner"
        val removeListAssociations = true
        val serviceResponse = ServiceResponse(
          LrmListDeleted(listOf("test_list_name"), listOf("test_list_name")),
          message = irrelevantMessage,
        )
        val apiServiceResponse = ApiServiceResponse(
          content = LrmListDeletedResponse(
            listNames = serviceResponse.content.listNames,
            associatedItemNames = serviceResponse.content.associatedItemNames,
          ),
          message = serviceResponse.message,
        )
        every { mockLrmListService.deleteByOwnerAndId(id, owner, removeListAssociations) } returns serviceResponse
        lrmListApiService.deleteByOwnerAndId(id, owner, removeListAssociations) shouldBe apiServiceResponse
        verify { mockLrmListService.deleteByOwnerAndId(id, owner, removeListAssociations) }
      }

      it("find lists by owner") {
        val owner = "test_owner"
        val serviceResponse = ServiceResponse(content = listOf(lrmList()), message = irrelevantMessage)
        val apiServiceResponse = ApiServiceResponse(
          content = serviceResponse.content.map { LrmListResponse.fromLrmList(it) },
          message = serviceResponse.message,
        )
        every { mockLrmListService.findByOwner(owner) } returns serviceResponse
        lrmListApiService.findByOwner(owner) shouldBe apiServiceResponse
        verify { mockLrmListService.findByOwner(owner) }
      }

      it("find list by owner removing any items from response") {
        val owner = "test_owner"
        val serviceResponse = ServiceResponse(listOf(lrmList()), message = irrelevantMessage)
        val apiServiceResponse =
          ApiServiceResponse(
            content = serviceResponse.content.map { LrmListResponse.fromLrmList(it.copy(items = emptySet())) },
            message = serviceResponse.message,
          )
        every { mockLrmListService.findByOwner(owner) } returns serviceResponse
        lrmListApiService.findByOwnerExcludeItems(owner) shouldBe apiServiceResponse
        verify { mockLrmListService.findByOwner(owner) }
      }

      it("find list by owner and id") {
        val id = UUID.randomUUID()
        val owner = "test_owner"
        val serviceResponse = ServiceResponse(lrmList(), message = irrelevantMessage)
        val apiServiceResponse = ApiServiceResponse(content = LrmListResponse.fromLrmList(serviceResponse.content), serviceResponse.message)
        every { mockLrmListService.findByOwnerAndId(id, owner) } returns serviceResponse
        lrmListApiService.findByOwnerAndId(id, owner) shouldBe apiServiceResponse
        verify { mockLrmListService.findByOwnerAndId(id, owner) }
      }

      it("find list by owner and id removing any items from response") {
        val id = UUID.randomUUID()
        val owner = "test_owner"
        val serviceResponse = ServiceResponse(lrmList(), message = irrelevantMessage)
        val apiServiceResponse =
          ApiServiceResponse(
            content = LrmListResponse.fromLrmList(serviceResponse.content.copy(items = emptySet())),
            message = serviceResponse.message,
          )
        every { mockLrmListService.findByOwnerAndId(id, owner) } returns serviceResponse
        lrmListApiService.findByOwnerAndIdExcludeItems(id, owner) shouldBe apiServiceResponse
        verify { mockLrmListService.findByOwnerAndId(id, owner) }
      }

      it("find list by public indicator") {
        val serviceResponse = ServiceResponse(listOf(lrmList()), message = irrelevantMessage)
        val apiServiceResponse =
          ApiServiceResponse(content = serviceResponse.content.map { LrmListResponse.fromLrmList(it) }, message = serviceResponse.message)
        every { mockLrmListService.findByPublic() } returns serviceResponse
        lrmListApiService.findByPublic() shouldBe apiServiceResponse
        verify { mockLrmListService.findByPublic() }
      }

      it("find list by public indicator removing any items from response") {
        val serviceResponse = ServiceResponse(listOf(lrmList()), message = irrelevantMessage)
        val apiServiceResponse =
          ApiServiceResponse(
            content = serviceResponse.content.map { LrmListResponse.fromLrmList(it.copy(items = emptySet())) },
            message = serviceResponse.message,
          )
        every { mockLrmListService.findByPublic() } returns serviceResponse
        lrmListApiService.findByPublicExcludeItems() shouldBe apiServiceResponse
        verify { mockLrmListService.findByPublic() }
      }

      it("find lists by owner having no list associations") {
        val owner = "test_owner"
        val lrmLists = listOf(lrmList())
        val serviceResponse = ServiceResponse(lrmLists, message = irrelevantMessage)
        val apiServiceResponse =
          ApiServiceResponse(
            content = serviceResponse.content.map { LrmListResponse.fromLrmList(lrmList()) },
            message = serviceResponse.message,
          )
        every { mockLrmListService.findByOwnerAndHavingNoItemAssociations(owner) } returns serviceResponse
        lrmListApiService.findByOwnerAndHavingNoItemAssociations(owner) shouldBe apiServiceResponse
        verify { mockLrmListService.findByOwnerAndHavingNoItemAssociations(owner) }
      }

      it("patch a list by owner and id") {
        val id = UUID.randomUUID()
        val owner = "test_owner"
        val updatedListName = "Updated List Name"
        val updatedListDescription = "Updated List Description"
        val updatedListIsPublic = false
        val patchRequest = mapOf("name" to updatedListName, "description" to updatedListDescription, "public" to updatedListIsPublic)
        val originalLrmList = lrmList()
        val updatedLrmList = lrmList().copy(name = updatedListName, description = updatedListDescription, public = updatedListIsPublic)
        every {
          mockLrmListService.findByOwnerAndId(id, owner)
        } returns ServiceResponse(originalLrmList, message = irrelevantMessage) andThen
          ServiceResponse(updatedLrmList, message = irrelevantMessage)
        every { mockLrmListService.patchName(any()) } returns Unit
        every { mockLrmListService.patchDescription(any()) } returns Unit
        every { mockLrmListService.patchIsPublic(any()) } returns Unit
        val result = lrmListApiService.patchByOwnerAndId(id, owner, patchRequest)
        result.content shouldBe LrmListResponse.fromLrmList(updatedLrmList)
//        result.second shouldBe true
        verify { mockLrmListService.patchName(any()) }
        verify { mockLrmListService.patchDescription(any()) }
        verify { mockLrmListService.patchIsPublic(any()) }
        verify { mockLrmListService.findByOwnerAndId(id, owner) }
      }

      it("not patch a list by owner and id") {
        val id = UUID.randomUUID()
        val owner = "test_owner"
        val updatedListName = lrmList().name
        val updatedListDescription = lrmList().description.toString()
        val updatedListIsPublic = lrmList().public
        val patchRequest = mapOf("name" to updatedListName, "description" to updatedListDescription, "public" to updatedListIsPublic)
        val originalLrmList = lrmList()
        val updatedLrmList = lrmList().copy()
        every {
          mockLrmListService.findByOwnerAndId(id, owner)
        } returns ServiceResponse(originalLrmList, message = irrelevantMessage) andThen
          ServiceResponse(updatedLrmList, message = irrelevantMessage)
        every { mockLrmListService.patchName(any()) } returns Unit
        every { mockLrmListService.patchDescription(any()) } returns Unit
        every { mockLrmListService.patchIsPublic(any()) } returns Unit
        val result = lrmListApiService.patchByOwnerAndId(id, owner, patchRequest)
        result.content shouldBe LrmListResponse.fromLrmList(updatedLrmList)
//        result.second shouldBe false
        verify(exactly = 0) { mockLrmListService.patchName(any()) }
        verify(exactly = 0) { mockLrmListService.patchDescription(any()) }
        verify(exactly = 0) { mockLrmListService.patchIsPublic(any()) }
        verify(exactly = 2) { mockLrmListService.findByOwnerAndId(id, owner) }
      }

      it("throw exception for unsupported patch field") {
        val owner = "test_owner"
        val patchRequest = mapOf("unsupportedField" to "value")
        every { mockLrmListService.findByOwnerAndId(id0, owner) } returns ServiceResponse(lrmList(), message = "message is irrelevant")
        val exception = shouldThrow<IllegalArgumentException> {
          lrmListApiService.patchByOwnerAndId(id0, owner, patchRequest)
        }
        exception.message shouldBe "Patch operation is not supported on field: unsupportedField"
      }

      it("count item associations by list id and list owner") {
        every { mockAssociationService.countByOwnerForList(listId = ofType(UUID::class), listOwner = ofType(String::class)) } returns
          ServiceResponse(99L, message = "message is irrelevant")
        lrmListApiService.countItemAssociationsByListIdAndListOwner(id0, "test_owner")
        verify(exactly = 1) { mockAssociationService.countByOwnerForList(listId = ofType(UUID::class), listOwner = ofType(String::class)) }
      }

      it("create item associations") {
        val associationCreated = AssociationCreated(
          componentName = "Lorem Ipsum",
          associatedComponents = listOf(LrmItemSuccinct(name = "Lorem Ipsum", id = id0)),
        )

        every {
          mockAssociationService.create(
            id = id0,
            idCollection = listOf(id0),
            LrmComponentType.List,
            componentsOwner = ofType(String::class),
          )
        } returns ServiceResponse(associationCreated, message = "message is irrelevant")

        lrmListApiService.createItemAssociations(id0, setOf(id0), "test_owner")
        verify(exactly = 1) {
          mockAssociationService.create(
            id = ofType(UUID::class),
            idCollection = listOf(id0),
            LrmComponentType.List,
            componentsOwner = ofType(String::class),
          )
        }
      }

      it("delete item association by item id and list id and components owner") {
        every {
          mockAssociationService.deleteByItemIdAndListId(
            itemId = ofType(UUID::class),
            listId = ofType(UUID::class),
            componentsOwner = ofType(String::class),
          )
        } returns ServiceResponse(Pair("Lorem", "Ipsum"), message = "message is irrelevant")

        lrmListApiService.deleteItemAssociationByItemIdAndListIdAndComponentsOwner(id0, id0, "test_owner")

        verify(exactly = 1) {
          mockAssociationService.deleteByItemIdAndListId(
            itemId = ofType(UUID::class),
            listId = ofType(UUID::class),
            componentsOwner = ofType(String::class),
          )
        }
      }

      it("delete item associations by list id and list owner") {
        every {
          mockAssociationService.deleteByListOwnerAndListId(listId = ofType(UUID::class), listOwner = ofType(String::class))
        } returns ServiceResponse(Pair("Lorem Ipsum", 99), message = "message is irrelevant")

        lrmListApiService.deleteItemAssociationsByListIdAndListOwner(listId = id0, listOwner = "test_owner")

        verify(exactly = 1) {
          mockAssociationService.deleteByListOwnerAndListId(listId = ofType(UUID::class), listOwner = ofType(String::class))
        }
      }
    }
  })
