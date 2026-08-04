package net.flyingfishflash.loremlist.integration;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.util.List;
import java.util.UUID;
import net.flyingfishflash.loremlist.core.response.structure.DispositionOfSuccess;
import net.flyingfishflash.loremlist.integration.domain.AbstractIntegrationTest;
import net.flyingfishflash.loremlist.integration.domain.TestData;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.http.HttpMethod;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MaintenanceTest extends AbstractIntegrationTest {

  private List<UUID> listIds;
  private List<UUID> itemIds;

  @BeforeAll
  void setUp() throws Exception {
    purgeDomain();
    listIds = new java.util.ArrayList<>();
    for (var listRequest : TestData.LIST_CREATE_REQUESTS) {
      listIds.add(createAndVerifyList(listRequest));
    }
    itemIds = new java.util.ArrayList<>();
    for (var itemRequest : TestData.ITEM_CREATE_REQUESTS_ALPHA) {
      itemIds.add(createAndVerifyListItem(listIds.get(0), itemRequest));
    }
  }

  @Test
  void purge() throws Exception {
    verifyRequest(HttpMethod.DELETE, "/maintenance/purge")
        .expectedDisposition(DispositionOfSuccess.SUCCESS)
        .additionalMatchers(
            jsonPath("$.content.length()").value(itemIds.size()),
            jsonPath("$.content.associationDeletedCount").value(itemIds.size()),
            jsonPath("$.content.itemDeletedCount").value(itemIds.size()),
            jsonPath("$.content.listDeletedCount").value(listIds.size()))
        .perform();
  }
}
