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
import net.flyingfishflash.loremlist.domain.lrmlist.ListNotFoundException;
import net.flyingfishflash.loremlist.integration.domain.AbstractIntegrationTest;
import net.flyingfishflash.loremlist.integration.domain.TestData;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.ResultMatcher;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ListItemCreateTest extends AbstractIntegrationTest {

  private record ListItemRequestRecord(UUID listId, LrmItemCreateRequest listItemCreateRequest) {}

  private List<ListItemRequestRecord> requestRecords;

  @BeforeAll
  void setUp() throws Exception {
    purgeDomain();

    // create each list and associate its uuid with a corresponding list item create request;
    // this results in three lists, each with one list item, once the creation tests run
    requestRecords = new ArrayList<>();
    for (var pair : TestData.LIST_CREATE_ITEM_CREATE_REQUEST_PAIRS) {
      UUID listId = createAndVerifyList(pair.listCreateRequest());
      requestRecords.add(new ListItemRequestRecord(listId, pair.itemCreateRequest()));
    }

    verifyContentValue("/lists/count", requestRecords.size());
    verifyContentValue("/items/count", 0);
    for (var record : requestRecords) {
      verifyContentValue("/lists/" + record.listId() + "/items/count", 0);
    }
  }

  @Test
  void failsWhenInvalidUuidIsProvidedForListId() throws Exception {
    verifyRequest(HttpMethod.POST, "/lists/" + TestData.INVALID_UUIDS.get(0) + "/items")
        .requestBody(
            writeValueAsString(
                TestData.LIST_CREATE_ITEM_CREATE_REQUEST_PAIRS.get(0).itemCreateRequest()))
        .expectedDisposition(DispositionOfProblem.FAILURE)
        .statusMatcher(status().isBadRequest())
        .additionalMatchers(
            jsonPath("$.message").value(VALIDATION_FAILURE_MESSAGE + " listId."),
            jsonPath("$.content.validationErrors.length()").value(1))
        .perform();
  }

  @Test
  void failsWhenListToCreateNewItemForIsNotFound() throws Exception {
    UUID randomUuid = UUID.randomUUID();
    var requestBody = TestData.LIST_CREATE_ITEM_CREATE_REQUEST_PAIRS.get(0).itemCreateRequest();

    verifyRequest(HttpMethod.POST, "/lists/" + randomUuid + "/items")
        .requestBody(writeValueAsString(requestBody))
        .expectedDisposition(DispositionOfProblem.FAILURE)
        .statusMatcher(status().isNotFound())
        .additionalMatchers(
            jsonPath("$.content.title").value(ListNotFoundException.class.getSimpleName()),
            jsonPath("$.content.status").value("404"))
        .perform();
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("createValidationScenarios")
  void createValidation(String description, TestData.ValidationScenario scenario) throws Exception {
    ResultMatcher[] additionalMatchers =
        scenario.expectedErrorCount() > 0
            ? new ResultMatcher[] {
              jsonPath("$.message").value(scenario.responseMessage()),
              jsonPath("$.content.validationErrors.length()").value(scenario.expectedErrorCount())
            }
            : new ResultMatcher[] {jsonPath("$.message").value(scenario.responseMessage())};
    verifyRequest(HttpMethod.POST, "/lists/" + requestRecords.get(0).listId() + "/items")
        .requestBody(scenario.postContent())
        .expectedDisposition(DispositionOfProblem.FAILURE)
        .statusMatcher(status().isBadRequest())
        .additionalMatchers(additionalMatchers)
        .perform();
  }

  static Stream<Arguments> createValidationScenarios() {
    return TestData.LIST_ITEM_CREATE_VALIDATION_SCENARIOS.stream()
        .map(scenario -> Arguments.of(scenario.description(), scenario));
  }

  @ParameterizedTest(name = "succeeds for ''{0}''")
  @MethodSource("requestRecords")
  void createSucceeds(String name, ListItemRequestRecord record) throws Exception {
    createAndVerifyListItem(record.listId(), record.listItemCreateRequest());
  }

  Stream<Arguments> requestRecords() {
    return requestRecords.stream().map(r -> Arguments.of(r.listItemCreateRequest().name(), r));
  }
}
