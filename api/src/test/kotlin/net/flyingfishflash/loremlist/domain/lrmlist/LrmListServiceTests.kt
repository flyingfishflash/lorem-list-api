package net.flyingfishflash.loremlist.domain.lrmlist

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.validation.ConstraintViolationException
import net.flyingfishflash.loremlist.domain.lrmlist.data.LrmList
import net.flyingfishflash.loremlist.domain.lrmlist.data.LrmListMapper
import net.flyingfishflash.loremlist.domain.lrmlist.data.LrmListRepository
import net.flyingfishflash.loremlist.domain.lrmlist.exceptions.ListNotFoundException
import org.junit.jupiter.api.assertThrows

class LrmListServiceTests : FunSpec({

  val lrmListRepository = mockk<LrmListRepository>()
  val lrmListService = LrmListService(lrmListRepository, LrmListMapper())

  test("delete(): when list not found then ListNotFoundException") {
    val id = 1L
    every { lrmListRepository.deleteById(id) } returns 0
    assertThrows<ListNotFoundException> {
      lrmListService.deleteById(id)
    }
  }

  test("patch(): when list not found then ListNotFoundException") {
    val id = 1L
    every { lrmListRepository.findByIdOrNull(id) } returns null
    assertThrows<ListNotFoundException> {
      lrmListService.patch(id, mapOf("name" to "lorum ipsum"))
    }
  }

  test("patch(): update name") {
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

  test("patch(): update description") {
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

  test("patch(): when unknown list property passed then IllegalArgumentException") {
    val id = 1L
    val lrmList = LrmList(id = 0, name = "lorem list", description = "describe lorem list")
    every { lrmListRepository.findByIdOrNull(id) } returns lrmList
    assertThrows<IllegalArgumentException> {
      lrmListService.patch(id, mapOf("invalid property" to "irrelevant value"))
    }
  }

  test("patch(): when LrmListRequest constraint violation then ConstraintViolationException") {
    val id = 1L
    val lrmList = LrmList(id = 0, name = "lorem list", description = "describe lorem list")
    every { lrmListRepository.findByIdOrNull(id) } returns lrmList
    assertThrows<ConstraintViolationException> {
      lrmListService.patch(id, mapOf("description" to "  "))
    }
  }

  test("delete(): when list found then lrmListRepository.delete()") {
    val id = 1L
    every { lrmListRepository.findByIdOrNull(id) } returns LrmList(id = 0, name = "Lorem Ipsum")
    every { lrmListRepository.deleteById(ofType(Long::class)) } returns 1
    lrmListService.deleteById(id)
    verify(exactly = 1) { lrmListRepository.deleteById(any()) }
  }

  test("findByIdOrListNotFoundException(): when list found then list returned") {
    val id = 1L
    val lrmList = LrmList(id = 0, name = "Lorem Ipsum")
    every { lrmListRepository.findByIdOrNull(id) } returns lrmList
    val result = lrmListService.findByIdOrListNotFoundException(id)
    result.shouldBe(lrmList)
    verify(exactly = 1) { lrmListRepository.findByIdOrNull(id) }
  }

  test("findByIdOrListNotFoundException(): when list not found then ListNotFoundException") {
    val id = 1L
    every { lrmListRepository.findByIdOrNull(id) } returns null
    assertThrows<ListNotFoundException> {
      lrmListService.findByIdOrListNotFoundException(id)
    }
  }

  afterTest {
    clearMocks(lrmListRepository)
  }
})
