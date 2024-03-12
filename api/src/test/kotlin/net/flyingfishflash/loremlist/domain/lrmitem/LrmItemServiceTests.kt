package net.flyingfishflash.loremlist.domain.lrmitem

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import net.flyingfishflash.loremlist.domain.lrmitem.data.LrmItem
import net.flyingfishflash.loremlist.domain.lrmitem.data.LrmItemRepository
import net.flyingfishflash.loremlist.domain.lrmitem.data.LrmItemRequest
import org.junit.jupiter.api.assertThrows

class LrmItemServiceTests : DescribeSpec({
  val lrmItemRepository = mockk<LrmItemRepository>()
  val lrmItemService = LrmItemService(lrmItemRepository)

  val lrmItemName = "Lorem Item Name"
  val lrmItemDescription = "Lorem Item Description"
  val lrmItemMockResponse = LrmItem(id = 0, name = lrmItemName, description = lrmItemDescription)
  val lrmItemRequest = LrmItemRequest(lrmItemName, lrmItemDescription)
  val id = 1L

  describe("create()") {
    it("repository returns inserted item") {
      every { lrmItemRepository.insert(ofType(LrmItemRequest::class)) } returns lrmItemMockResponse
      lrmItemService.create(lrmItemRequest)
      verify(exactly = 1) { lrmItemRepository.insert(lrmItemRequest) }
    }
  }

  describe("deleteSingleById()") {
    it("item repository returns 0 deleted records") {
      every { lrmItemRepository.deleteById(id) } returns 0
      assertThrows<ItemDeleteException> {
        lrmItemService.deleteSingleById(id)
      }.cause.shouldBeInstanceOf<ItemNotFoundException>()
    }

    it("item repository returns > 1 deleted records") {
      every { lrmItemRepository.deleteById(id) } returns 2
      assertThrows<ItemDeleteException> {
        lrmItemService.deleteSingleById(id)
      }.cause.shouldBeNull()
    }

    it("item repository returns 1 deleted record") {
      every { lrmItemRepository.findByIdOrNull(id) } returns lrmItemMockResponse
      every { lrmItemRepository.deleteById(id) } returns 1
      lrmItemService.deleteSingleById(id)
      verify(exactly = 1) { lrmItemRepository.deleteById(id) }
    }
  }

  describe("findAll()") {
    it("items are returned") {
      every { lrmItemRepository.findAll() } returns listOf(lrmItemMockResponse)
      lrmItemService.findAll()
      verify(exactly = 1) { lrmItemRepository.findAll() }
    }
  }

  describe("findAllAndLists()") {
    it("items are returned") {
      every { lrmItemRepository.findAllAndLists() } returns listOf(lrmItemMockResponse)
      lrmItemService.findAllAndLists()
      verify(exactly = 1) { lrmItemRepository.findAllAndLists() }
    }
  }

  afterTest {
    clearMocks(lrmItemRepository)
  }
})
