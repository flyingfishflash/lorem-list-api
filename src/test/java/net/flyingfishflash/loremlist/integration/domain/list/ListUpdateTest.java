package net.flyingfishflash.loremlist.integration.domain.list;

import static net.flyingfishflash.loremlist.core.response.advice.CoreExceptionHandler.VALIDATION_FAILURE_MESSAGE;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import net.flyingfishflash.loremlist.api.data.request.LrmListCreateRequest;
import net.flyingfishflash.loremlist.core.response.structure.DispositionOfProblem;
import net.flyingfishflash.loremlist.core.response.structure.DispositionOfSuccess;
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
import org.springframework.transaction.annotation.Transactional;

/** Integration tests for list update operations */
@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ListUpdateTest extends AbstractIntegrationTest {

  private record ListRequestRecord(
      UUID uuid, LrmListCreateRequest createRequest, LrmListCreateRequest updateRequest) {}

  private List<ListRequestRecord> requestRecords;

  @BeforeAll
  void setUp() throws Exception {
    purgeDomain();

    requestRecords = new ArrayList<>();
    for (var pair : TestData.LIST_CREATE_UPDATE_REQUEST_PAIRS) {
      UUID uuid = createAndVerifyList(pair.createRequest());
      requestRecords.add(new ListRequestRecord(uuid, pair.createRequest(), pair.updateRequest()));
    }
  }

  @Test
  void failsWhenInvalidUuidIsProvidedForListId() throws Exception {
    verifyRequest(HttpMethod.PATCH, "/lists/" + TestData.INVALID_UUIDS.get(0))
        .requestBody(
            writeValueAsString(TestData.LIST_CREATE_UPDATE_REQUEST_PAIRS.get(0).updateRequest()))
        .expectedDisposition(DispositionOfProblem.FAILURE)
        .statusMatcher(status().isBadRequest())
        .additionalMatchers(
            jsonPath("$.message").value(VALIDATION_FAILURE_MESSAGE + " listId."),
            jsonPath("$.content.validationErrors.length()").value(1))
        .perform();
  }

  @Test
  void failsWhenListToUpdateIsNotFound() throws Exception {
    UUID nonExistentUuid = UUID.randomUUID();
    var sampleUpdate = TestData.LIST_CREATE_UPDATE_REQUEST_PAIRS.get(0).updateRequest();

    verifyRequest(HttpMethod.PATCH, "/lists/" + nonExistentUuid)
        .requestBody(writeValueAsString(sampleUpdate))
        .expectedDisposition(DispositionOfProblem.FAILURE)
        .statusMatcher(status().isNotFound())
        .additionalMatchers(
            jsonPath("$.content.title").value(ListNotFoundException.class.getSimpleName()),
            jsonPath("$.content.status").value("404"))
        .perform();
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("updateValidationScenarios")
  void updateValidation(String description, TestData.ValidationScenario scenario) throws Exception {
    ResultMatcher[] additionalMatchers =
        scenario.expectedErrorCount() > 0
            ? new ResultMatcher[] {
              jsonPath("$.message").value(scenario.responseMessage()),
              jsonPath("$.content.validationErrors.length()").value(scenario.expectedErrorCount())
            }
            : new ResultMatcher[] {jsonPath("$.message").value(scenario.responseMessage())};
    verifyRequest(HttpMethod.PATCH, "/lists/" + requestRecords.get(0).uuid())
        .requestBody(scenario.postContent())
        .expectedDisposition(DispositionOfProblem.FAILURE)
        .statusMatcher(status().isBadRequest())
        .additionalMatchers(additionalMatchers)
        .perform();
  }

  static Stream<Arguments> updateValidationScenarios() {
    return TestData.LIST_UPDATE_VALIDATION_SCENARIOS.stream()
        .map(scenario -> Arguments.of(scenario.description(), scenario));
  }

  @ParameterizedTest(name = "succeeds for all fields of ''{0}''")
  @MethodSource("requestRecords")
  void succeedsForAllFields(String name, ListRequestRecord record) throws Exception {
    verifyRequest(HttpMethod.PATCH, "/lists/" + record.uuid())
        .requestBody(writeValueAsString(record.updateRequest()))
        .expectedDisposition(DispositionOfSuccess.SUCCESS)
        .additionalMatchers(
            jsonPath("$.content.id").value(record.uuid().toString()),
            jsonPath("$.content.name").value(record.updateRequest().getName()),
            jsonPath("$.content.description").value(record.updateRequest().getDescription()),
            jsonPath("$.content.public").value(record.updateRequest().getPublic()),
            jsonPath("$.content.created").isNotEmpty(),
            jsonPath("$.content.updated").isNotEmpty(),
            jsonPath("$.content.items").isEmpty())
        .perform();

    verifyRequest(HttpMethod.GET, "/lists/" + record.uuid())
        .expectedDisposition(DispositionOfSuccess.SUCCESS)
        .additionalMatchers(
            jsonPath("$.content.name").value(record.updateRequest().getName()),
            jsonPath("$.content.description").value(record.updateRequest().getDescription()),
            jsonPath("$.content.public").value(record.updateRequest().getPublic()))
        .perform();
  }

  Stream<Arguments> requestRecords() {
    return requestRecords.stream().map(r -> Arguments.of(r.createRequest().getName(), r));
  }
}
