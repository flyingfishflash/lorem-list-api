package net.flyingfishflash.loremlist.integration.domain.list;

import static net.flyingfishflash.loremlist.core.response.advice.CoreExceptionHandler.VALIDATION_FAILURE_MESSAGE;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ListReadTest extends AbstractIntegrationTest {

  private Map<UUID, LrmListCreateRequest> listIdMap;
  private int itemCount;

  @BeforeAll
  void setUp() throws Exception {
    purgeDomain();

    listIdMap = new LinkedHashMap<>();
    for (var request : TestData.LIST_CREATE_REQUESTS) {
      listIdMap.put(createAndVerifyList(request), request);
    }
    UUID firstListId = listIdMap.keySet().iterator().next();
    itemCount = 0;
    for (var request : TestData.ITEM_CREATE_REQUESTS_ALPHA) {
      createAndVerifyListItem(firstListId, request);
      itemCount++;
    }

    verifyContentValue("/lists/count", listIdMap.size());
  }

  @Test
  void allListsSucceedsWhenIncludeItemsIsTrue() throws Exception {
    verifyRequest(HttpMethod.GET, "/lists?includeItems=true")
        .expectedDisposition(DispositionOfSuccess.SUCCESS)
        .expectedSize(listIdMap.size())
        .additionalMatchers(
            jsonPath("$.content.length()").value(listIdMap.size()),
            jsonPath("$..items.length()").value(itemCount))
        .perform();
  }

  @Test
  void allListsSucceedsWhenIncludeItemsIsFalse() throws Exception {
    verifyRequest(HttpMethod.GET, "/lists?includeItems=false")
        .expectedDisposition(DispositionOfSuccess.SUCCESS)
        .expectedSize(listIdMap.size())
        .additionalMatchers(
            jsonPath("$.content.length()").value(listIdMap.size()),
            jsonPath("$..items.length()").value(0))
        .perform();
  }

  @Test
  void allListsSucceedsWhenIncludeItemsIsOmitted() throws Exception {
    verifyRequest(HttpMethod.GET, "/lists")
        .expectedDisposition(DispositionOfSuccess.SUCCESS)
        .expectedSize(listIdMap.size())
        .additionalMatchers(
            jsonPath("$.content.length()").value(listIdMap.size()),
            jsonPath("$..items.length()").value(0))
        .perform();
  }

  @Test
  void retrievingListFailsWhenInvalidUuidIsProvided() throws Exception {
    verifyRequest(HttpMethod.GET, "/lists/" + TestData.INVALID_UUIDS.get(0))
        .expectedDisposition(DispositionOfProblem.FAILURE)
        .statusMatcher(status().isBadRequest())
        .additionalMatchers(
            jsonPath("$.message").value(VALIDATION_FAILURE_MESSAGE + " listId."),
            jsonPath("$.content.validationErrors.length()").value(1))
        .perform();
  }

  @Test
  void retrievingListFailsWhenNotFound() throws Exception {
    UUID listId = UUID.randomUUID();
    verifyRequest(HttpMethod.GET, "/lists/" + listId)
        .expectedDisposition(DispositionOfProblem.FAILURE)
        .statusMatcher(status().isNotFound())
        .additionalMatchers(
            jsonPath("$.content.title").value(ListNotFoundException.class.getSimpleName()),
            jsonPath("$.content.status").value("404"),
            jsonPath("$.content.supplemental.notFound").value(listId.toString()))
        .perform();
  }

  @ParameterizedTest(name = "succeeds when uuid is {0}")
  @MethodSource("listEntries")
  void retrievingListSucceedsWhenIncludeItemsIsTrue(UUID uuid, LrmListCreateRequest request)
      throws Exception {
    UUID firstListId = listIdMap.keySet().iterator().next();
    List<ResultMatcher> matchers =
        new ArrayList<>(
            List.of(
                jsonPath("$.content.id").value(uuid.toString()),
                jsonPath("$.content.name").value(request.name()),
                jsonPath("$.content.description").value(request.description()),
                jsonPath("$.content.public").value(request.isPublic())));
    matchers.add(jsonPath("$..items.length()").value(uuid.equals(firstListId) ? itemCount : 0));
    verifyRequest(HttpMethod.GET, "/lists/" + uuid + "?includeItems=true")
        .expectedDisposition(DispositionOfSuccess.SUCCESS)
        .additionalMatchers(matchers.toArray(new ResultMatcher[0]))
        .perform();
  }

  @ParameterizedTest(name = "succeeds when uuid is {0}")
  @MethodSource("listEntries")
  void retrievingListSucceedsWhenIncludeItemsIsFalse(UUID uuid, LrmListCreateRequest request)
      throws Exception {
    verifyRequest(HttpMethod.GET, "/lists/" + uuid + "?includeItems=false")
        .expectedDisposition(DispositionOfSuccess.SUCCESS)
        .additionalMatchers(
            jsonPath("$.content.id").value(uuid.toString()),
            jsonPath("$.content.name").value(request.name()),
            jsonPath("$.content.description").value(request.description()),
            jsonPath("$.content.public").value(request.isPublic()))
        .perform();
  }

  @ParameterizedTest(name = "succeeds when uuid is {0}")
  @MethodSource("listEntries")
  void retrievingListSucceedsWhenIncludeItemsIsOmitted(UUID uuid, LrmListCreateRequest request)
      throws Exception {
    verifyRequest(HttpMethod.GET, "/lists/" + uuid)
        .expectedDisposition(DispositionOfSuccess.SUCCESS)
        .additionalMatchers(
            jsonPath("$.content.id").value(uuid.toString()),
            jsonPath("$.content.name").value(request.name()),
            jsonPath("$.content.description").value(request.description()),
            jsonPath("$.content.public").value(request.isPublic()))
        .perform();
  }

  Stream<Arguments> listEntries() {
    return listIdMap.entrySet().stream().map(e -> Arguments.of(e.getKey(), e.getValue()));
  }
}
