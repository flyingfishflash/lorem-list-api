package net.flyingfishflash.loremlist.integration.domain.item;

import static net.flyingfishflash.loremlist.core.response.advice.CoreExceptionHandler.VALIDATION_FAILURE_MESSAGE;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import net.flyingfishflash.loremlist.api.data.request.LrmItemCreateRequest;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpMethod;

/**
 * Test methods here are order-dependent: this class is not @Transactional, and the final
 * delete-all-items test assumes the specific items already deleted by the preceding ones.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ItemDeleteTest extends AbstractIntegrationTest {

  private record ListItemRequestRecord(
      UUID listId, UUID itemId, LrmItemCreateRequest listItemCreateRequest) {}

  private List<ListItemRequestRecord> requestsAlpha;
  private List<ListItemRequestRecord> requestsBeta;

  @BeforeAll
  void setUp() throws Exception {
    purgeDomain();
    UUID listWithItems = createAndVerifyList(TestData.LIST_CREATE_REQUESTS.get(0));
    UUID listWithNoItems = createAndVerifyList(TestData.LIST_CREATE_REQUESTS.get(1));

    requestsAlpha = new ArrayList<>();
    for (var request : TestData.ITEM_CREATE_REQUESTS_ALPHA) {
      UUID itemId = createAndVerifyListItem(listWithItems, request);
      requestsAlpha.add(new ListItemRequestRecord(listWithItems, itemId, request));
    }

    requestsBeta = new ArrayList<>();
    for (var request : TestData.ITEM_CREATE_REQUESTS_BETA) {
      UUID itemId = createAndVerifyListItem(listWithNoItems, request);
      requestsBeta.add(new ListItemRequestRecord(listWithNoItems, itemId, request));
    }

    verifyRequest(HttpMethod.DELETE, "/lists/" + listWithNoItems + "/items")
        .expectedDisposition(DispositionOfSuccess.SUCCESS)
        .perform();

    verifyContentValue("/lists/count", 2);
    verifyContentValue("/lists/" + listWithItems + "/items/count", requestsAlpha.size());
    verifyContentValue("/lists/" + listWithNoItems + "/items/count", 0);
    verifyContentSize("/items/with-no-lists", requestsBeta.size());
  }

  @Test
  @Order(1)
  void failsWhenInvalidUuidIsProvidedForItemId() throws Exception {
    verifyRequest(HttpMethod.DELETE, "/items/" + TestData.INVALID_UUIDS.get(0))
        .expectedDisposition(DispositionOfProblem.FAILURE)
        .statusMatcher(status().isBadRequest())
        .additionalMatchers(
            jsonPath("$.message").value(VALIDATION_FAILURE_MESSAGE + " itemId."),
            jsonPath("$.content.validationErrors.length()").value(1))
        .perform();
  }

  @Test
  @Order(2)
  void failsWhenItemToDeleteIsNotFound() throws Exception {
    UUID nonExistentId = UUID.randomUUID();
    verifyRequest(HttpMethod.DELETE, "/items/" + nonExistentId)
        .expectedDisposition(DispositionOfProblem.FAILURE)
        .statusMatcher(status().isNotFound())
        .additionalMatchers(
            jsonPath("$.content.title").value(DomainException.class.getSimpleName()),
            jsonPath("$.content.status").value("404"))
        .perform();
  }

  @ParameterizedTest(name = "fails with http 422 for ''{0}''")
  @MethodSource("requestsAlpha")
  @Order(3)
  void failsWith422(String name, ListItemRequestRecord record) throws Exception {
    verifyRequest(HttpMethod.DELETE, "/items/" + record.itemId())
        .expectedDisposition(DispositionOfProblem.FAILURE)
        .statusMatcher(status().isUnprocessableEntity())
        .perform();
  }

  Stream<Arguments> requestsAlpha() {
    return requestsAlpha.stream().map(r -> Arguments.of(r.listItemCreateRequest().getName(), r));
  }

  @ParameterizedTest(name = "succeeds for ''{0}'' [no list]")
  @MethodSource("requestsBeta")
  @Order(4)
  void succeedsNoList(String name, ListItemRequestRecord record) throws Exception {
    verifyRequest(HttpMethod.DELETE, "/items/" + record.itemId())
        .expectedDisposition(DispositionOfSuccess.SUCCESS)
        .statusMatcher(status().isOk())
        .perform();
  }

  Stream<Arguments> requestsBeta() {
    return requestsBeta.stream().map(r -> Arguments.of(r.listItemCreateRequest().getName(), r));
  }

  @Test
  @Order(5)
  void deletingAllItemsSucceeds() throws Exception {
    verifyRequest(HttpMethod.DELETE, "/items")
        .expectedDisposition(DispositionOfSuccess.SUCCESS)
        .perform();
    verifyContentValue("/items/count", 0);
  }
}
