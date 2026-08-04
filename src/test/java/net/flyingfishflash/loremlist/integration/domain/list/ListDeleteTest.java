package net.flyingfishflash.loremlist.integration.domain.list;

import static net.flyingfishflash.loremlist.core.response.advice.CoreExceptionHandler.VALIDATION_FAILURE_MESSAGE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.flyingfishflash.loremlist.core.response.structure.DispositionOfProblem;
import net.flyingfishflash.loremlist.core.response.structure.DispositionOfSuccess;
import net.flyingfishflash.loremlist.domain.exceptions.DomainException;
import net.flyingfishflash.loremlist.integration.domain.AbstractIntegrationTest;
import net.flyingfishflash.loremlist.integration.domain.TestData;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.http.HttpMethod;

/**
 * Test methods here are order-dependent: this class is not @Transactional, so each test's writes
 * persist into the next, and later tests (e.g. the final all-lists count) assume the specific set
 * of lists deleted by the preceding tests. Method order is pinned to match.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ListDeleteTest extends AbstractIntegrationTest {

  private List<UUID> listsWithItemsIds;
  private List<UUID> emptyListsAlphaIds;
  private List<UUID> emptyListsBetaIds;
  private List<UUID> itemIdsAlpha;
  private List<UUID> itemIdsBeta;
  private UUID listWithItemsAlpha;
  private UUID listWithItemsBeta;

  @BeforeAll
  void setUp() throws Exception {
    purgeDomain();

    listsWithItemsIds = new ArrayList<>();
    for (var request : TestData.LIST_CREATE_REQUESTS) {
      listsWithItemsIds.add(createAndVerifyList(request));
    }
    emptyListsAlphaIds = new ArrayList<>();
    for (var request : TestData.LIST_CREATE_REQUESTS) {
      emptyListsAlphaIds.add(createAndVerifyList(request));
    }
    emptyListsBetaIds = new ArrayList<>();
    for (var request : TestData.LIST_CREATE_REQUESTS) {
      emptyListsBetaIds.add(createAndVerifyList(request));
    }

    assertThat(listsWithItemsIds.size()).isGreaterThanOrEqualTo(2);
    assertThat(emptyListsAlphaIds.size()).isGreaterThanOrEqualTo(1);
    assertThat(emptyListsBetaIds.size()).isGreaterThanOrEqualTo(3);

    listWithItemsAlpha = listsWithItemsIds.get(0);
    listWithItemsBeta = listsWithItemsIds.get(listsWithItemsIds.size() - 1);

    itemIdsAlpha = new ArrayList<>();
    for (var request : TestData.ITEM_CREATE_REQUESTS_ALPHA) {
      itemIdsAlpha.add(createAndVerifyListItem(listWithItemsAlpha, request));
    }
    itemIdsBeta = new ArrayList<>();
    for (var request : TestData.ITEM_CREATE_REQUESTS_BETA) {
      itemIdsBeta.add(createAndVerifyListItem(listWithItemsBeta, request));
    }

    verifyContentValue(
        "/lists/count",
        listsWithItemsIds.size() + emptyListsAlphaIds.size() + emptyListsBetaIds.size());
    verifyContentValue("/items/count", itemIdsAlpha.size() + itemIdsBeta.size());
  }

  @Test
  @Order(1)
  void failsWhenInvalidUuidIsProvidedForListId() throws Exception {
    verifyRequest(HttpMethod.DELETE, "/lists/" + TestData.INVALID_UUIDS.get(0))
        .expectedDisposition(DispositionOfProblem.FAILURE)
        .statusMatcher(status().isBadRequest())
        .additionalMatchers(
            jsonPath("$.message").value(VALIDATION_FAILURE_MESSAGE + " listId."),
            jsonPath("$.content.validationErrors.length()").value(1))
        .perform();
  }

  @Test
  @Order(2)
  void failsWhenListToDeleteIsNotFound() throws Exception {
    UUID nonExistentId = UUID.randomUUID();
    verifyRequest(HttpMethod.DELETE, "/lists/" + nonExistentId)
        .expectedDisposition(DispositionOfProblem.FAILURE)
        .statusMatcher(status().isNotFound())
        .additionalMatchers(
            jsonPath("$.content.title").value(DomainException.class.getSimpleName()),
            jsonPath("$.content.status").value("404"))
        .perform();
  }

  @Test
  @Order(3)
  void deletingListWithItemsFailsWhenRemoveItemAssociationsIsOmitted() throws Exception {
    verifyRequest(HttpMethod.DELETE, "/lists/" + listWithItemsAlpha)
        .expectedDisposition(DispositionOfProblem.FAILURE)
        .statusMatcher(status().isUnprocessableEntity())
        .additionalMatchers(jsonPath("$.message").exists())
        .perform();

    verifyListIsFound(listWithItemsAlpha);
    for (UUID itemId : itemIdsAlpha) {
      verifyContentListsLength(itemId, 1);
    }
  }

  @Test
  @Order(4)
  void deletingListWithItemsFailsWhenRemoveItemAssociationsIsFalse() throws Exception {
    verifyRequest(
            HttpMethod.DELETE, "/lists/" + listWithItemsAlpha + "?removeItemAssociations=false")
        .expectedDisposition(DispositionOfProblem.FAILURE)
        .statusMatcher(status().isUnprocessableEntity())
        .additionalMatchers(jsonPath("$.message").exists())
        .perform();

    verifyListIsFound(listWithItemsAlpha);
    for (UUID itemId : itemIdsAlpha) {
      verifyContentListsLength(itemId, 1);
    }
  }

  @Test
  @Order(5)
  void deletingListWithItemsSucceedsWhenRemoveItemAssociationsIsTrue() throws Exception {
    verifyRequest(
            HttpMethod.DELETE, "/lists/" + listWithItemsAlpha + "?removeItemAssociations=true")
        .expectedDisposition(DispositionOfSuccess.SUCCESS)
        .additionalMatchers(
            jsonPath("$.message").exists(),
            jsonPath("$.content.associatedItemNames.length()").value(itemIdsAlpha.size()))
        .perform();

    verifyListIsNotFound(listWithItemsAlpha);
    for (UUID itemId : itemIdsAlpha) {
      verifyContentListsLength(itemId, 0);
    }
  }

  @Test
  @Order(6)
  void deletingListWithoutItemsSucceedsWhenRemoveItemAssociationsIsOmitted() throws Exception {
    UUID emptyList0 = emptyListsBetaIds.get(0);
    verifyRequest(HttpMethod.DELETE, "/lists/" + emptyList0)
        .expectedDisposition(DispositionOfSuccess.SUCCESS)
        .additionalMatchers(jsonPath("$.content").exists())
        .perform();
    verifyListIsNotFound(emptyList0);
  }

  @Test
  @Order(7)
  void deletingListWithoutItemsSucceedsWhenRemoveItemAssociationsIsTrue() throws Exception {
    UUID emptyList1 = emptyListsBetaIds.get(1);
    verifyRequest(HttpMethod.DELETE, "/lists/" + emptyList1 + "?removeItemAssociations=true")
        .expectedDisposition(DispositionOfSuccess.SUCCESS)
        .additionalMatchers(jsonPath("$.content.associatedItemNames.length()").value(0))
        .perform();
    verifyListIsNotFound(emptyList1);
  }

  @Test
  @Order(8)
  void deletingListWithoutItemsSucceedsWhenRemoveItemAssociationsIsFalse() throws Exception {
    UUID emptyList2 = emptyListsBetaIds.get(2);
    verifyRequest(HttpMethod.DELETE, "/lists/" + emptyList2 + "?removeItemAssociations=false")
        .expectedDisposition(DispositionOfSuccess.SUCCESS)
        .additionalMatchers(jsonPath("$.content.associatedItemNames.length()").value(0))
        .perform();
    verifyListIsNotFound(emptyList2);
  }

  @Test
  @Order(9)
  void deletingAllListsSucceedsAndAllItemsRemain() throws Exception {
    verifyContentValue("/lists/count", 5);
    verifyContentValue("/items/count", itemIdsAlpha.size() + itemIdsBeta.size());

    verifyRequest(HttpMethod.DELETE, "/lists")
        .expectedDisposition(DispositionOfSuccess.SUCCESS)
        .perform();

    verifyContentValue("/lists/count", 0);
    verifyContentValue("/items/count", itemIdsAlpha.size() + itemIdsBeta.size());
  }
}
