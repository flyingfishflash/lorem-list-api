package net.flyingfishflash.loremlist.integration.domain.list;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.stream.Stream;
import net.flyingfishflash.loremlist.api.data.request.LrmListCreateRequest;
import net.flyingfishflash.loremlist.core.response.structure.DispositionOfProblem;
import net.flyingfishflash.loremlist.integration.domain.AbstractIntegrationTest;
import net.flyingfishflash.loremlist.integration.domain.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class ListCreateTest extends AbstractIntegrationTest {

  @BeforeEach
  void setUp() throws Exception {
    purgeDomain();
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("listCreateValidationScenarios")
  void createValidation(String description, TestData.ValidationScenario scenario) throws Exception {
    ResultMatcher[] additionalMatchers =
        scenario.expectedErrorCount() > 0
            ? new ResultMatcher[] {
              jsonPath("$.message").value(scenario.responseMessage()),
              jsonPath("$.content.validationErrors.length()").value(scenario.expectedErrorCount())
            }
            : new ResultMatcher[] {jsonPath("$.message").value(scenario.responseMessage())};
    verifyRequest(HttpMethod.POST, "/lists")
        .requestBody(scenario.postContent())
        .expectedDisposition(DispositionOfProblem.FAILURE)
        .statusMatcher(status().isBadRequest())
        .additionalMatchers(additionalMatchers)
        .perform();
  }

  static Stream<Arguments> listCreateValidationScenarios() {
    return TestData.LIST_CREATE_VALIDATION_SCENARIOS.stream()
        .map(scenario -> Arguments.of(scenario.description(), scenario));
  }

  @ParameterizedTest(name = "succeeds for ''{0}''")
  @MethodSource("listCreateRequests")
  void createSucceeds(String name, LrmListCreateRequest listRequest) throws Exception {
    createAndVerifyList(listRequest);
  }

  static Stream<Arguments> listCreateRequests() {
    return TestData.LIST_CREATE_REQUESTS.stream()
        .map(request -> Arguments.of(request.name(), request));
  }
}
