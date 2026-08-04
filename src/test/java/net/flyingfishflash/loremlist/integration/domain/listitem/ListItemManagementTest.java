package net.flyingfishflash.loremlist.integration.domain.listitem;

import static net.flyingfishflash.loremlist.core.response.advice.CoreExceptionHandler.VALIDATION_FAILURE_MESSAGE;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.flyingfishflash.loremlist.api.data.request.LrmListItemAddRequest;
import net.flyingfishflash.loremlist.core.response.structure.DispositionOfProblem;
import net.flyingfishflash.loremlist.core.response.structure.DispositionOfSuccess;
import net.flyingfishflash.loremlist.domain.exceptions.DomainException;
import net.flyingfishflash.loremlist.domain.lrmitem.ItemNotFoundException;
import net.flyingfishflash.loremlist.domain.lrmlist.ListNotFoundException;
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
 * Test methods here are order-dependent: this class is not @Transactional, and later tests (move,
 * then remove, then add, then remove-all) build on state left behind by earlier ones.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ListItemManagementTest extends AbstractIntegrationTest {

  private List<UUID> listIds;
  private List<UUID> itemIds;

  @BeforeAll
  void setUp() throws Exception {
    purgeDomain();

    listIds = new ArrayList<>();
    for (var request : TestData.LIST_CREATE_REQUESTS) {
      listIds.add(createAndVerifyList(request));
    }
    itemIds = new ArrayList<>();
    for (var request : TestData.ITEM_CREATE_REQUESTS_ALPHA) {
      itemIds.add(createAndVerifyListItem(listIds.get(0), request));
    }

    verifyContentValue("/lists/count", TestData.LIST_CREATE_REQUESTS.size());
    verifyContentValue("/items/count", TestData.ITEM_CREATE_REQUESTS_ALPHA.size());
    verifyContentValue("/lists/" + listIds.get(0) + "/items/count", itemIds.size());
    verifyContentValue("/lists/" + listIds.get(1) + "/items/count", 0);
    verifyContentValue("/lists/" + listIds.get(2) + "/items/count", 0);
  }

  @Test
  @Order(1)
  void moveFailsWhenInvalidUuidsAreProvided() throws Exception {
    UUID invalid = TestData.INVALID_UUIDS.get(0);
    verifyRequest(HttpMethod.PATCH, "/lists/" + invalid + "/items/" + invalid + "/" + invalid)
        .expectedDisposition(DispositionOfProblem.FAILURE)
        .statusMatcher(status().isBadRequest())
        .additionalMatchers(
            jsonPath("$.message")
                .value(VALIDATION_FAILURE_MESSAGE + " destinationListId, itemId, listId."),
            jsonPath("$.content.validationErrors.length()").value(1))
        .perform();
  }

  @Test
  @Order(2)
  void moveFailsWhenSourceListIsNotFound() throws Exception {
    verifyRequest(
            HttpMethod.PATCH,
            "/lists/" + UUID.randomUUID() + "/items/" + itemIds.get(0) + "/" + listIds.get(1))
        .expectedDisposition(DispositionOfProblem.FAILURE)
        .statusMatcher(status().isNotFound())
        .additionalMatchers(
            jsonPath("$.content.title").value(DomainException.class.getSimpleName()),
            jsonPath("$.content.cause.name").value(ListNotFoundException.class.getSimpleName()))
        .perform();
  }

  @Test
  @Order(3)
  void moveFailsWhenDestinationListIsNotFound() throws Exception {
    verifyRequest(
            HttpMethod.PATCH,
            "/lists/" + listIds.get(0) + "/items/" + itemIds.get(0) + "/" + UUID.randomUUID())
        .expectedDisposition(DispositionOfProblem.FAILURE)
        .statusMatcher(status().isNotFound())
        .additionalMatchers(
            jsonPath("$.content.title").value(DomainException.class.getSimpleName()),
            jsonPath("$.content.cause.name").value(ListNotFoundException.class.getSimpleName()))
        .perform();
  }

  @Test
  @Order(4)
  void moveFailsWhenItemIsNotFound() throws Exception {
    verifyRequest(
            HttpMethod.PATCH,
            "/lists/" + listIds.get(0) + "/items/" + UUID.randomUUID() + "/" + listIds.get(1))
        .expectedDisposition(DispositionOfProblem.FAILURE)
        .statusMatcher(status().isNotFound())
        .additionalMatchers(
            jsonPath("$.content.title").value(DomainException.class.getSimpleName()),
            jsonPath("$.content.cause.name").value(ItemNotFoundException.class.getSimpleName()))
        .perform();
  }

  @Test
  @Order(5)
  void moveSucceedsFromFirstListToSecondList() throws Exception {
    verifyRequest(
            HttpMethod.PATCH,
            "/lists/" + listIds.get(0) + "/items/" + itemIds.get(0) + "/" + listIds.get(1))
        .expectedDisposition(DispositionOfSuccess.SUCCESS)
        .additionalMatchers(jsonPath("$.content.message").isNotEmpty())
        .perform();

    verifyContentValue("/lists/" + listIds.get(0) + "/items/count", itemIds.size() - 1);
    verifyContentValue("/lists/" + listIds.get(1) + "/items/count", 1);
    verifyContentListsLength(itemIds.get(0), 1);
  }

  @Test
  @Order(6)
  void removeFailsWhenInvalidUuidsAreProvided() throws Exception {
    UUID invalid = TestData.INVALID_UUIDS.get(0);
    verifyRequest(HttpMethod.DELETE, "/lists/" + invalid + "/items/" + invalid)
        .expectedDisposition(DispositionOfProblem.FAILURE)
        .statusMatcher(status().isBadRequest())
        .additionalMatchers(
            jsonPath("$.message").value(VALIDATION_FAILURE_MESSAGE + " itemId, listId."),
            jsonPath("$.content.validationErrors.length()").value(1))
        .perform();
  }

  @Test
  @Order(7)
  void removeFailsWhenListIsNotFound() throws Exception {
    verifyRequest(HttpMethod.DELETE, "/lists/" + UUID.randomUUID() + "/items/" + itemIds.get(0))
        .expectedDisposition(DispositionOfProblem.FAILURE)
        .statusMatcher(status().isNotFound())
        .additionalMatchers(
            jsonPath("$.content.title").value(DomainException.class.getSimpleName()),
            jsonPath("$.content.cause.name").value(ListNotFoundException.class.getSimpleName()))
        .perform();
  }

  @Test
  @Order(8)
  void removeFailsWhenItemIsNotFound() throws Exception {
    verifyRequest(HttpMethod.DELETE, "/lists/" + listIds.get(0) + "/items/" + UUID.randomUUID())
        .expectedDisposition(DispositionOfProblem.FAILURE)
        .statusMatcher(status().isNotFound())
        .additionalMatchers(
            jsonPath("$.content.title").value(DomainException.class.getSimpleName()),
            jsonPath("$.content.cause.name").value(ItemNotFoundException.class.getSimpleName()))
        .perform();
  }

  @Test
  @Order(9)
  void removeSucceedsFromSecondList() throws Exception {
    verifyRequest(HttpMethod.DELETE, "/lists/" + listIds.get(1) + "/items/" + itemIds.get(0))
        .expectedDisposition(DispositionOfSuccess.SUCCESS)
        .perform();

    verifyContentValue("/lists/" + listIds.get(1) + "/items/count", 0);
    verifyContentListsLength(itemIds.get(0), 0);
    verifyContentSize("/items/with-no-lists", 1);
  }

  @Test
  @Order(10)
  void addFailsWhenInvalidUuidIsProvidedForListId() throws Exception {
    verifyRequest(HttpMethod.PUT, "/lists/" + TestData.INVALID_UUIDS.get(0) + "/items")
        .requestBody(
            writeValueAsString(new LrmListItemAddRequest(Set.of(itemIds.get(1), itemIds.get(2)))))
        .expectedDisposition(DispositionOfProblem.FAILURE)
        .statusMatcher(status().isBadRequest())
        .additionalMatchers(
            jsonPath("$.message").value(VALIDATION_FAILURE_MESSAGE + " listId."),
            jsonPath("$.content.validationErrors.length()").value(1))
        .perform();
  }

  @Test
  @Order(11)
  void addFailsDeserializationWhenItemIdCollectionContainsNonUuid() throws Exception {
    verifyRequest(HttpMethod.PUT, "/lists/" + listIds.get(1) + "/items")
        .requestBody("{\"itemIdCollection\":[\"000000000000a000900000000000000\"]}")
        .expectedDisposition(DispositionOfProblem.FAILURE)
        .statusMatcher(status().isBadRequest())
        .perform();
  }

  @Test
  @Order(12)
  void addFailsValidationWhenItemCollectionContainsInvalidUuidVersion() throws Exception {
    verifyRequest(HttpMethod.PUT, "/lists/" + listIds.get(1) + "/items")
        .requestBody(
            writeValueAsString(new LrmListItemAddRequest(Set.of(TestData.INVALID_UUIDS.get(0)))))
        .expectedDisposition(DispositionOfProblem.FAILURE)
        .statusMatcher(status().isBadRequest())
        .perform();
  }

  @Test
  @Order(13)
  void addFailsValidationWhenRequestBodyContainsEmptyItemIdCollection() throws Exception {
    verifyRequest(HttpMethod.PUT, "/lists/" + listIds.get(1) + "/items")
        .requestBody("{\"itemIdCollection\":[]}")
        .expectedDisposition(DispositionOfProblem.FAILURE)
        .statusMatcher(status().isBadRequest())
        .perform();
  }

  @Test
  @Order(14)
  void addFailsWhenListIsNotFound() throws Exception {
    verifyRequest(HttpMethod.PUT, "/lists/" + UUID.randomUUID() + "/items")
        .requestBody(
            writeValueAsString(new LrmListItemAddRequest(Set.of(itemIds.get(1), itemIds.get(2)))))
        .expectedDisposition(DispositionOfProblem.FAILURE)
        .statusMatcher(status().isNotFound())
        .additionalMatchers(
            jsonPath("$.content.title").value(DomainException.class.getSimpleName()),
            jsonPath("$.content.cause.name").value(ListNotFoundException.class.getSimpleName()))
        .perform();
  }

  @Test
  @Order(15)
  void addSucceedsForSecondAndThirdItemsToSecondList() throws Exception {
    verifyRequest(HttpMethod.PUT, "/lists/" + listIds.get(1) + "/items")
        .requestBody(
            writeValueAsString(new LrmListItemAddRequest(Set.of(itemIds.get(1), itemIds.get(2)))))
        .expectedDisposition(DispositionOfSuccess.SUCCESS)
        .perform();

    verifyContentValue("/lists/" + listIds.get(1) + "/items/count", 2);
    verifyContentListsLength(itemIds.get(1), 2);
    verifyContentListsLength(itemIds.get(2), 2);
  }

  @Test
  @Order(16)
  void removeAllFailsWhenInvalidUuidIsProvidedForListId() throws Exception {
    verifyRequest(HttpMethod.DELETE, "/lists/" + TestData.INVALID_UUIDS.get(0) + "/items")
        .expectedDisposition(DispositionOfProblem.FAILURE)
        .statusMatcher(status().isBadRequest())
        .additionalMatchers(
            jsonPath("$.message").value(VALIDATION_FAILURE_MESSAGE + " listId."),
            jsonPath("$.content.validationErrors.length()").value(1))
        .perform();
  }

  @Test
  @Order(17)
  void removeAllFailsWhenListIsNotFound() throws Exception {
    verifyRequest(HttpMethod.DELETE, "/lists/" + UUID.randomUUID() + "/items")
        .expectedDisposition(DispositionOfProblem.FAILURE)
        .statusMatcher(status().isNotFound())
        .additionalMatchers(
            jsonPath("$.content.title").value(DomainException.class.getSimpleName()),
            jsonPath("$.content.cause.name").value(ListNotFoundException.class.getSimpleName()))
        .perform();
  }

  @Test
  @Order(18)
  void removeAllSucceedsFromSecondList() throws Exception {
    verifyRequest(HttpMethod.DELETE, "/lists/" + listIds.get(1) + "/items")
        .expectedDisposition(DispositionOfSuccess.SUCCESS)
        .perform();

    verifyContentValue("/lists/" + listIds.get(1) + "/items/count", 0);
    verifyContentListsLength(itemIds.get(1), 1);
    verifyContentListsLength(itemIds.get(2), 1);
  }
}
