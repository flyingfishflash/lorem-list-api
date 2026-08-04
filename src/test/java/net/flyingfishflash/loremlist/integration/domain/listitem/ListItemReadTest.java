package net.flyingfishflash.loremlist.integration.domain.listitem;

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
import net.flyingfishflash.loremlist.domain.lrmlist.ListNotFoundException;
import net.flyingfishflash.loremlist.domain.lrmlistitem.ListItemNotFoundException;
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
class ListItemReadTest extends AbstractIntegrationTest {

  private record ListItemRequestRecord(
      UUID listId, UUID itemId, LrmItemCreateRequest listItemCreateRequest) {}

  private List<ListItemRequestRecord> requests;
  private List<ListItemRequestRecord> requestsBeta;
  private UUID listWithItemsId;
  private String listWithItemsName;
  private UUID listWithNoItemsId;
  private String listWithNoItemsName;

  @BeforeAll
  void setUp() throws Exception {
    purgeDomain();

    listWithItemsId = createAndVerifyList(TestData.LIST_CREATE_REQUESTS.get(0));
    listWithItemsName = TestData.LIST_CREATE_REQUESTS.get(0).getName();
    requests = new ArrayList<>();
    for (var request : TestData.ITEM_CREATE_REQUESTS_ALPHA) {
      requests.add(
          new ListItemRequestRecord(
              listWithItemsId, createAndVerifyListItem(listWithItemsId, request), request));
    }

    listWithNoItemsId = createAndVerifyList(TestData.LIST_CREATE_REQUESTS.get(1));
    listWithNoItemsName = TestData.LIST_CREATE_REQUESTS.get(1).getName();
    requestsBeta = new ArrayList<>();
    for (var request : TestData.ITEM_CREATE_REQUESTS_BETA) {
      requestsBeta.add(
          new ListItemRequestRecord(
              listWithNoItemsId, createAndVerifyListItem(listWithNoItemsId, request), request));
    }

    verifyRequest(HttpMethod.DELETE, "/lists/" + listWithNoItemsId + "/items")
        .expectedDisposition(DispositionOfSuccess.SUCCESS)
        .perform();

    verifyContentValue("/lists/count", 2);
    verifyContentValue("/lists/" + listWithItemsId + "/items/count", requests.size());
    verifyContentValue("/lists/" + listWithNoItemsId + "/items/count", 0);
    verifyContentSize("/items/with-no-lists", requestsBeta.size());
  }

  @Test
  void allListItemsFailsWhenInvalidUuidIsProvidedForListId() throws Exception {
    verifyRequest(HttpMethod.GET, "/lists/" + TestData.INVALID_UUIDS.get(0))
        .expectedDisposition(DispositionOfProblem.FAILURE)
        .statusMatcher(status().isBadRequest())
        .additionalMatchers(
            jsonPath("$.message").value(VALIDATION_FAILURE_MESSAGE + " listId."),
            jsonPath("$.content.validationErrors.length()").value(1))
        .perform();
  }

  @Test
  void allListItemsFailsWhenListIsNotFound() throws Exception {
    UUID randomId = UUID.randomUUID();
    verifyRequest(HttpMethod.GET, "/lists/" + randomId)
        .expectedDisposition(DispositionOfProblem.FAILURE)
        .statusMatcher(status().isNotFound())
        .additionalMatchers(
            jsonPath("$.content.title").value(ListNotFoundException.class.getSimpleName()),
            jsonPath("$.content.status").value("404"),
            jsonPath("$.content.supplemental.notFound").value(randomId.toString()))
        .perform();
  }

  @Test
  void allListItemsSucceedsWhenIncludeItemsIsTrue() throws Exception {
    verifyRequest(HttpMethod.GET, "/lists/" + requests.get(0).listId() + "?includeItems=true")
        .expectedDisposition(DispositionOfSuccess.SUCCESS)
        .statusMatcher(status().isOk())
        .additionalMatchers(jsonPath("$.content.items.length()").value(requests.size()))
        .perform();
  }

  @Test
  void listItemFailsWhenInvalidUuidsAreProvided() throws Exception {
    UUID invalid = TestData.INVALID_UUIDS.get(0);
    verifyRequest(HttpMethod.GET, "/lists/" + invalid + "/items/" + invalid)
        .expectedDisposition(DispositionOfProblem.FAILURE)
        .statusMatcher(status().isBadRequest())
        .additionalMatchers(
            jsonPath("$.message").value(VALIDATION_FAILURE_MESSAGE + " itemId, listId."),
            jsonPath("$.content.validationErrors.length()").value(1))
        .perform();
  }

  @Test
  void listItemFailsWhenListIsNotFound() throws Exception {
    UUID randomId = UUID.randomUUID();
    verifyRequest(HttpMethod.GET, "/lists/" + randomId + "/items/" + requests.get(0).itemId())
        .expectedDisposition(DispositionOfProblem.FAILURE)
        .statusMatcher(status().isNotFound())
        .additionalMatchers(
            jsonPath("$.content.title").value(ListNotFoundException.class.getSimpleName()),
            jsonPath("$.content.status").value("404"),
            jsonPath("$.content.supplemental.notFound").value(randomId.toString()))
        .perform();
  }

  @Test
  void listItemFailsWhenListItemIsNotFound() throws Exception {
    UUID randomId = UUID.randomUUID();
    verifyRequest(HttpMethod.GET, "/lists/" + requests.get(0).listId() + "/items/" + randomId)
        .expectedDisposition(DispositionOfProblem.FAILURE)
        .statusMatcher(status().isNotFound())
        .additionalMatchers(
            jsonPath("$.content.title").value(ListItemNotFoundException.class.getSimpleName()),
            jsonPath("$.content.status").value("404"),
            jsonPath("$.content.supplemental.notFound").value(randomId.toString()))
        .perform();
  }

  @ParameterizedTest(name = "succeeds for ''{0}''")
  @MethodSource("requests")
  void listItemSucceeds(String name, ListItemRequestRecord record) throws Exception {
    verifyRequest(HttpMethod.GET, "/lists/" + record.listId() + "/items/" + record.itemId())
        .expectedDisposition(DispositionOfSuccess.SUCCESS)
        .statusMatcher(status().isOk())
        .additionalMatchers(jsonPath("$.content.lists.length()").value(1))
        .perform();
  }

  Stream<Arguments> requests() {
    return requests.stream().map(r -> Arguments.of(r.listItemCreateRequest().getName(), r));
  }

  private record EligibleScenario(String listName, UUID listId, int expectedSize) {}

  @ParameterizedTest(name = "succeeds for ''{0}'' with {2} items")
  @MethodSource("eligibleScenarios")
  void eligibleItemsSucceeds(String listName, UUID listId, int expectedSize) throws Exception {
    verifyContentSize("/lists/" + listId + "/items/eligible", expectedSize);
  }

  Stream<Arguments> eligibleScenarios() {
    return Stream.of(
        // eligible items will not include requestsAlpha because those items are already in this
        // list
        Arguments.of(listWithItemsName, listWithItemsId, requestsBeta.size()),
        // eligible items will include all items since none are in this list
        Arguments.of(
            listWithNoItemsName, listWithNoItemsId, requests.size() + requestsBeta.size()));
  }

  @Test
  void eligibleItemsFailsWhenListIsNotFound() throws Exception {
    UUID randomId = UUID.randomUUID();
    verifyRequest(HttpMethod.GET, "/lists/" + randomId + "/items/eligible")
        .expectedDisposition(DispositionOfProblem.FAILURE)
        .statusMatcher(status().isNotFound())
        .additionalMatchers(
            jsonPath("$.content.title").value(DomainException.class.getSimpleName()),
            jsonPath("$.content.status").value("404"),
            jsonPath("$.content.cause.name").value(ListNotFoundException.class.getSimpleName()))
        .perform();
  }

  @Test
  void eligibleItemsFailsWhenInvalidUuidIsProvidedForListId() throws Exception {
    verifyRequest(HttpMethod.GET, "/lists/" + TestData.INVALID_UUIDS.get(0) + "/items/eligible")
        .expectedDisposition(DispositionOfProblem.FAILURE)
        .statusMatcher(status().isBadRequest())
        .additionalMatchers(
            jsonPath("$.message").value(VALIDATION_FAILURE_MESSAGE + " listId."),
            jsonPath("$.content.validationErrors.length()").value(1))
        .perform();
  }
}
