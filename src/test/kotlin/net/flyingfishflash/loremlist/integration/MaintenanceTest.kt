package net.flyingfishflash.loremlist.integration

import net.flyingfishflash.loremlist.integration.domain.DomainFunctionTest
import org.springframework.http.HttpMethod
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import java.util.*

class MaintenanceTest(mockMvc: MockMvc) :
  DomainFunctionTest(mockMvc = mockMvc, body = {
    var listIds: List<UUID>
    var itemIds: List<UUID> = emptyList()

    beforeSpec {
      listIds = TestData.listCreateRequests.map { createAndVerifyList(it) }
      itemIds = TestData.itemCreateRequestsAlpha.map { createAndVerifyListItem(listId = listIds[0], itemRequest = it) }
    }

    describe("maintenance functions") {
      it("purge") {
        performRequestAndVerifyResponse(
          method = HttpMethod.DELETE,
          instance = "/maintenance/purge",
          expectSuccess = true,
          additionalMatchers = arrayOf(
            jsonPath("$.content.length()").value(itemIds.size),
            jsonPath("$.content.associationDeletedCount").value(itemIds.size),
            jsonPath("$.content.itemDeletedCount").value(itemIds.size),
            jsonPath("$.content.listDeletedCount").value(itemIds.size),
          ),
        )
      }
    }
  })
