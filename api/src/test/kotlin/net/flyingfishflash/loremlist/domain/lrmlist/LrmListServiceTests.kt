package net.flyingfishflash.loremlist.domain.lrmlist

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.validation.ConstraintViolationException
import net.flyingfishflash.loremlist.domain.lrmlist.data.LrmList
import net.flyingfishflash.loremlist.domain.lrmlist.data.LrmListRepository
import net.flyingfishflash.loremlist.domain.lrmlist.data.dto.LrmListRequest
import org.junit.jupiter.api.assertThrows

class LrmListServiceTests : DescribeSpec({

  val lrmListRepository = mockk<LrmListRepository>()
  val lrmListService = LrmListService(lrmListRepository)

  describe("create()") {
    it("repository returns inserted list") {
      every { lrmListRepository.insert(ofType(LrmListRequest::class)) } returns LrmList(id = 0, name = "Lorem Ipsum")
      lrmListService.create(LrmListRequest(name = "Lorem List Name", description = "Lorem List Description"))
      verify(exactly = 1) { lrmListRepository.insert(ofType(LrmListRequest::class)) }
    }
  }

  describe("delete()") {
    it("list repository returns 0 deleted records") {
      val id = 1L
      every { lrmListRepository.deleteById(id) } returns 0
      assertThrows<ListDeleteException> {
        lrmListService.deleteSingleById(id)
      }.cause.shouldBeInstanceOf<ListNotFoundException>()
    }

    it("list repository returns > 1 deleted records") {
      val id = 1L
      every { lrmListRepository.deleteById(id) } returns 2
      assertThrows<ListDeleteException> {
        lrmListService.deleteSingleById(id)
      }.cause.shouldBeNull()
    }

    it("list repository returns 1 deleted record") {
      val id = 1L
      every { lrmListRepository.findByIdOrNull(id) } returns LrmList(id = 0, name = "Lorem Ipsum")
      every { lrmListRepository.deleteById(ofType(Long::class)) } returns 1
      lrmListService.deleteSingleById(id)
      verify(exactly = 1) { lrmListRepository.deleteById(any()) }
    }
  }

  describe("patch()") {
    it("list is not found") {
      val id = 1L
      every { lrmListRepository.findByIdOrNull(id) } returns null
      assertThrows<ListNotFoundException> {
        lrmListService.patch(id, mapOf("name" to "lorum ipsum"))
      }
    }

    it("update name") {
      val id = 1L
      val expectedName = "patched lorem list"
      val lrmList = LrmList(id = 0, name = "lorem list", description = "describe lorem list")
      every { lrmListRepository.findByIdOrNull(id) } returns lrmList
      every { lrmListRepository.update(ofType(LrmList::class)) } returns lrmList
      val patchedLrmList = lrmListService.patch(id, mapOf("name" to expectedName)).first
      patchedLrmList.name.shouldBe(expectedName)
      verify(exactly = 1) { lrmListRepository.findByIdOrNull(id) }
      verify(exactly = 1) { lrmListRepository.update(ofType(LrmList::class)) }
    }

    it("update description") {
      val id = 1L
      val expectedDescription = "patched lorem list description"
      val lrmList = LrmList(id = 0, name = "lorem list", description = "describe lorem list")
      every { lrmListRepository.findByIdOrNull(id) } returns lrmList
      every { lrmListRepository.update(ofType(LrmList::class)) } returns lrmList
      val patchedLrmList = lrmListService.patch(id, mapOf("description" to expectedDescription)).first
      patchedLrmList.description.shouldBe(expectedDescription)
      verify(exactly = 1) { lrmListRepository.findByIdOrNull(id) }
      verify(exactly = 1) { lrmListRepository.update(ofType(LrmList::class)) }
    }

    it("update an undefined list property") {
      val id = 1L
      val lrmList = LrmList(id = 0, name = "lorem list", description = "describe lorem list")
      every { lrmListRepository.findByIdOrNull(id) } returns lrmList
      assertThrows<IllegalArgumentException> {
        lrmListService.patch(id, mapOf("undefined property" to "irrelevant value"))
      }
    }

    it("update description to '  '") {
      val id = 1L
      val lrmList = LrmList(id = 0, name = "lorem list", description = "describe lorem list")
      every { lrmListRepository.findByIdOrNull(id) } returns lrmList
      assertThrows<ConstraintViolationException> {
        lrmListService.patch(id, mapOf("description" to "  "))
      }
    }
  }

  describe("findByIdOrListNotFoundException()") {
    it("list is found and returned") {
      val id = 1L
      val lrmList = LrmList(id = 0, name = "Lorem Ipsum")
      every { lrmListRepository.findByIdOrNull(id) } returns lrmList
      val result = lrmListService.findByIdOrListNotFoundException(id)
      result.shouldBe(lrmList)
      verify(exactly = 1) { lrmListRepository.findByIdOrNull(id) }
    }

    it("list is not found") {
      val id = 1L
      every { lrmListRepository.findByIdOrNull(id) } returns null
      assertThrows<ListNotFoundException> {
        lrmListService.findByIdOrListNotFoundException(id)
      }
    }
  }

  afterTest {
    clearMocks(lrmListRepository)
  }
})
