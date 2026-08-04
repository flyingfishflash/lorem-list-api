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
import net.flyingfishflash.loremlist.domain.lrmitem.ItemNotFoundException;
import net.flyingfishflash.loremlist.integration.domain.AbstractIntegrationTest;
import net.flyingfishflash.loremlist.integration.domain.TestData;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpMethod;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ItemReadTest extends AbstractIntegrationTest {

  private record ListItemRequestRecord(
      UUID listId, UUID itemId, LrmItemCreateRequest listItemCreateRequest) {}

  private List<ListItemRequestRecord> requestsAlpha;
  private List<ListItemRequestRecord> requestsBeta;
  private UUID listWithItems;
  private UUID listWithNoItems;

  @BeforeAll
  void setUp() throws Exception {
    purgeDomain();
    listWithItems = createAndVerifyList(TestData.LIST_CREATE_REQUESTS.get(0));
    listWithNoItems = createAndVerifyList(TestData.LIST_CREATE_REQUESTS.get(1));

    requestsAlpha = new ArrayList<>();
    for (var request : TestData.ITEM_CREATE_REQUESTS_ALPHA) {
      requestsAlpha.add(
          new ListItemRequestRecord(
              listWithItems, createAndVerifyListItem(listWithItems, request), request));
    }
    requestsBeta = new ArrayList<>();
    for (var request : TestData.ITEM_CREATE_REQUESTS_BETA) {
      requestsBeta.add(
          new ListItemRequestRecord(
              listWithNoItems, createAndVerifyListItem(listWithNoItems, request), request));
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
  void countingItemsSucceeds() throws Exception {
    verifyContentValue("/items/count", requestsAlpha.size() + requestsBeta.size());
  }

  @Test
  void retrievingAllItemsSucceeds() throws Exception {
    verifyRequest(HttpMethod.GET, "/items")
        .expectedDisposition(DispositionOfSuccess.SUCCESS)
        .expectedSize(requestsAlpha.size() + requestsBeta.size())
        .statusMatcher(status().isOk())
        .additionalMatchers(
            jsonPath("$.content.length()").value(requestsAlpha.size() + requestsBeta.size()))
        .perform();
  }

  @Test
  void retrievingItemsWithNoListAssociationsSucceeds() throws Exception {
    verifyContentSize("/items/with-no-lists", requestsBeta.size());
  }

  @Test
  void retrievingItemFailsWhenInvalidUuidIsProvided() throws Exception {
    verifyRequest(HttpMethod.GET, "/items/" + TestData.INVALID_UUIDS.get(0))
        .expectedDisposition(DispositionOfProblem.FAILURE)
        .statusMatcher(status().isBadRequest())
        .additionalMatchers(
            jsonPath("$.message").value(VALIDATION_FAILURE_MESSAGE + " itemId."),
            jsonPath("$.content.validationErrors.length()").value(1))
        .perform();
  }

  @Test
  void retrievingItemFailsWhenNotFound() throws Exception {
    UUID randomId = UUID.randomUUID();
    verifyRequest(HttpMethod.GET, "/items/" + randomId)
        .expectedDisposition(DispositionOfProblem.FAILURE)
        .statusMatcher(status().isNotFound())
        .additionalMatchers(
            jsonPath("$.content.title").value(ItemNotFoundException.class.getSimpleName()),
            jsonPath("$.content.status").value("404"),
            jsonPath("$.content.supplemental.notFound").value(randomId.toString()))
        .perform();
  }

  @ParameterizedTest(name = "succeeds for ''{0}''")
  @MethodSource("requestsAlpha")
  void retrievingItemSucceeds(String name, ListItemRequestRecord record) throws Exception {
    verifyRequest(HttpMethod.GET, "/items/" + record.itemId())
        .expectedDisposition(DispositionOfSuccess.SUCCESS)
        .statusMatcher(status().isOk())
        .additionalMatchers(jsonPath("$.content.lists.length()").value(1))
        .perform();
  }

  Stream<Arguments> requestsAlpha() {
    return requestsAlpha.stream().map(r -> Arguments.of(r.listItemCreateRequest().name(), r));
  }

  @ParameterizedTest(name = "succeeds for ''{0}'' [no lists]")
  @MethodSource("requestsBeta")
  void retrievingItemSucceedsNoLists(String name, ListItemRequestRecord record) throws Exception {
    verifyRequest(HttpMethod.GET, "/items/" + record.itemId())
        .expectedDisposition(DispositionOfSuccess.SUCCESS)
        .statusMatcher(status().isOk())
        .additionalMatchers(jsonPath("$.content.lists.length()").value(0))
        .perform();
  }

  Stream<Arguments> requestsBeta() {
    return requestsBeta.stream().map(r -> Arguments.of(r.listItemCreateRequest().name(), r));
  }

  @Test
  void listCountFailsWhenInvalidUuidIsProvided() throws Exception {
    verifyRequest(HttpMethod.GET, "/items/" + TestData.INVALID_UUIDS.get(0) + "/lists/count")
        .expectedDisposition(DispositionOfProblem.FAILURE)
        .statusMatcher(status().isBadRequest())
        .additionalMatchers(
            jsonPath("$.message").value(VALIDATION_FAILURE_MESSAGE + " itemId."),
            jsonPath("$.content.validationErrors.length()").value(1))
        .perform();
  }

  @Test
  void listCountFailsWhenItemIsNotFound() throws Exception {
    UUID randomId = UUID.randomUUID();
    verifyRequest(HttpMethod.GET, "/items/" + randomId + "/lists/count")
        .expectedDisposition(DispositionOfProblem.FAILURE)
        .statusMatcher(status().isNotFound())
        .additionalMatchers(
            jsonPath("$.content.title").value(DomainException.class.getSimpleName()),
            jsonPath("$.content.status").value("404"),
            jsonPath("$.content.cause.name").value(ItemNotFoundException.class.getSimpleName()))
        .perform();
  }

  @ParameterizedTest(name = "succeeds for ''{0}''")
  @MethodSource("requestsAlpha")
  void listCountSucceeds(String name, ListItemRequestRecord record) throws Exception {
    verifyContentValue("/items/" + record.itemId() + "/lists/count", 1);
  }

  @ParameterizedTest(name = "succeeds for ''{0}'' [no lists]")
  @MethodSource("requestsBeta")
  void listCountSucceedsNoLists(String name, ListItemRequestRecord record) throws Exception {
    verifyContentValue("/items/" + record.itemId() + "/lists/count", 0);
  }
}
