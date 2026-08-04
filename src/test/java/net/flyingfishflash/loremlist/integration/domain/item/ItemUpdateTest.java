package net.flyingfishflash.loremlist.integration.domain.item;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import net.flyingfishflash.loremlist.api.data.request.LrmItemCreateRequest;
import net.flyingfishflash.loremlist.core.response.structure.DispositionOfProblem;
import net.flyingfishflash.loremlist.core.response.structure.DispositionOfSuccess;
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
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ItemUpdateTest extends AbstractIntegrationTest {

  private record ListItemRequestRecord(
      UUID listId, UUID itemId, LrmItemCreateRequest listItemCreateRequest) {}

  private List<ListItemRequestRecord> requestsAlpha;

  @BeforeAll
  void setUp() throws Exception {
    purgeDomain();
    UUID listWithItems = createAndVerifyList(TestData.LIST_CREATE_REQUESTS.get(0));

    requestsAlpha = new ArrayList<>();
    for (var request : TestData.ITEM_CREATE_REQUESTS_ALPHA) {
      requestsAlpha.add(
          new ListItemRequestRecord(
              listWithItems, createAndVerifyListItem(listWithItems, request), request));
    }

    verifyContentValue("/lists/count", 1);
    verifyContentValue("/lists/" + listWithItems + "/items/count", requestsAlpha.size());
  }

  @Test
  void failsWhenItemToUpdateIsNotFound() throws Exception {
    UUID nonExistentUuid = UUID.randomUUID();
    verifyRequest(HttpMethod.PATCH, "/items/" + nonExistentUuid)
        .requestBody("{ \"name\": null, \"description\": null }")
        .expectedDisposition(DispositionOfProblem.FAILURE)
        .statusMatcher(status().isNotFound())
        .additionalMatchers(
            jsonPath("$.content.title").value(ItemNotFoundException.class.getSimpleName()),
            jsonPath("$.content.status").value("404"))
        .perform();
  }

  @Test
  void failsWhenRequestBodyIncludesUnsupportedField() throws Exception {
    verifyRequest(HttpMethod.PATCH, "/items/" + requestsAlpha.get(0).itemId())
        .requestBody(
            "{ \"name\": \"lorem ipsum\", \"description\": \"lorem ipsum\", \"zzz\": \"blah\" }")
        .expectedDisposition(DispositionOfProblem.ERROR)
        .statusMatcher(status().isInternalServerError())
        .additionalMatchers(
            jsonPath("$.content.title").value(IllegalArgumentException.class.getSimpleName()),
            jsonPath("$.content.status").value(HttpStatus.INTERNAL_SERVER_ERROR.value()))
        .perform();
  }

  @ParameterizedTest(name = "succeeds for all supported fields of ''{0}''")
  @MethodSource("requestsAlpha")
  void succeedsForAllSupportedFields(String name, ListItemRequestRecord record) throws Exception {
    String newName = record.listItemCreateRequest().getName() + " *";
    String newDescription = record.listItemCreateRequest().getDescription() + " *";
    verifyRequest(HttpMethod.PATCH, "/items/" + record.itemId())
        .requestBody(
            "{ \"name\": \"" + newName + "\", \"description\": \"" + newDescription + "\" }")
        .expectedDisposition(DispositionOfSuccess.SUCCESS)
        .additionalMatchers(
            jsonPath("$.content.id").value(record.itemId().toString()),
            jsonPath("$.content.name").value(newName),
            jsonPath("$.content.description").value(newDescription),
            jsonPath("$.content.created").isNotEmpty(),
            jsonPath("$.content.updated").isNotEmpty())
        .perform();

    verifyRequest(HttpMethod.GET, "/items/" + record.itemId())
        .expectedDisposition(DispositionOfSuccess.SUCCESS)
        .additionalMatchers(
            jsonPath("$.content.name").value(newName),
            jsonPath("$.content.description").value(newDescription))
        .perform();
  }

  Stream<Arguments> requestsAlpha() {
    return requestsAlpha.stream().map(r -> Arguments.of(r.listItemCreateRequest().getName(), r));
  }
}
