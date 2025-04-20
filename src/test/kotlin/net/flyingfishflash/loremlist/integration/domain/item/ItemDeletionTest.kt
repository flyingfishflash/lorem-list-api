package net.flyingfishflash.loremlist.integration.domain.item

import net.flyingfishflash.loremlist.integration.domain.DomainFunctionTest
import org.springframework.http.HttpMethod
import org.springframework.test.web.servlet.MockMvc
import java.util.*

class ItemDeletionTest(mockMvc: MockMvc) :
  DomainFunctionTest(mockMvc = mockMvc, body = {
    var itemIds = listOf<UUID>()

    beforeSpec {
      purgeDomain()

      // Create a test list
      val listId = createAndVerifyList(TestData.listCreateRequests[0])
      // Create items in the list
      itemIds = TestData.itemCreateRequestsAlpha.map { createAndVerifyListItem(listId = listId, it) }
      // Delete the list but keep items
      performRequestAndVerifyResponse(
        method = HttpMethod.DELETE,
        instance = "/lists/$listId?removeItemAssociations=true",
        expectSuccess = true,
      )
    }

    describe("item deletion") {
      it("delete one item") {
        performRequestAndVerifyResponse(
          method = HttpMethod.DELETE,
          instance = "/items/${itemIds[0]}",
          expectSuccess = true,
        )

        // Verify the item count
        verifyContentValue(url = "/items/count", expectedValue = itemIds.size - 1)
      }

      it("delete all remaining items") {
        performRequestAndVerifyResponse(
          method = HttpMethod.DELETE,
          instance = "/items",
          expectSuccess = true,
        )

        // Verify no items remain
        verifyContentValue(url = "/items/count", expectedValue = 0)
      }
    }
  })
