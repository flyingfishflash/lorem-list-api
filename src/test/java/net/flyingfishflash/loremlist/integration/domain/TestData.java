package net.flyingfishflash.loremlist.integration.domain;

import static net.flyingfishflash.loremlist.core.response.advice.CoreExceptionHandler.VALIDATION_FAILURE_MESSAGE;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import net.flyingfishflash.loremlist.api.data.request.LrmItemCreateRequest;
import net.flyingfishflash.loremlist.api.data.request.LrmListCreateRequest;

/**
 * Test data factory shared by the integration tests, mirroring the former Kotlin
 * DomainFunctionTest.TestData.
 */
public final class TestData {

  private TestData() {}

  public record ValidationScenario(
      String description, String postContent, String responseMessage, int expectedErrorCount) {}

  public record ListCreateUpdateRequestPair(
      LrmListCreateRequest createRequest, LrmListCreateRequest updateRequest) {}

  public record ListCreateItemCreateRequestPair(
      LrmListCreateRequest listCreateRequest, LrmItemCreateRequest itemCreateRequest) {}

  public static final List<UUID> INVALID_UUIDS =
      List.of(UUID.fromString("00000000-0000-a000-9000-000000000000"));

  public static final List<LrmListCreateRequest> LIST_CREATE_REQUESTS =
      IntStream.rangeClosed(1, 3).mapToObj(i -> createListRequest(i, false)).toList();

  private static final List<LrmListCreateRequest> LIST_UPDATE_REQUESTS =
      IntStream.rangeClosed(1, 3).mapToObj(i -> createListRequest(i, true)).toList();

  public static final List<LrmItemCreateRequest> ITEM_CREATE_REQUESTS_ALPHA =
      IntStream.rangeClosed(1, 3).mapToObj(i -> createListItemRequest(i, false)).toList();

  public static final List<LrmItemCreateRequest> ITEM_CREATE_REQUESTS_BETA =
      IntStream.rangeClosed(1, 10).mapToObj(i -> createListItemRequest(i, false)).toList();

  public static final List<ListCreateUpdateRequestPair> LIST_CREATE_UPDATE_REQUEST_PAIRS =
      zip(LIST_CREATE_REQUESTS, LIST_UPDATE_REQUESTS, ListCreateUpdateRequestPair::new);

  public static final List<ListCreateItemCreateRequestPair> LIST_CREATE_ITEM_CREATE_REQUEST_PAIRS =
      zip(LIST_CREATE_REQUESTS, ITEM_CREATE_REQUESTS_ALPHA, ListCreateItemCreateRequestPair::new);

  private static LrmListCreateRequest createListRequest(int index, boolean updated) {
    String name = updated ? "list " + index + " *" : "list " + index;
    String description =
        updated ? "list " + index + " description *" : "list " + index + " description";
    boolean isPublic = updated != (index % 2 == 0);
    return new LrmListCreateRequest(name, description, isPublic);
  }

  private static LrmItemCreateRequest createListItemRequest(int index, boolean updated) {
    String name = updated ? "list item " + index + " *" : "list item " + index;
    String description =
        updated ? "list item " + index + " description *" : "list item " + index + " description";
    int quantity = updated ? 1000 + index : index;
    boolean isSuppressed = updated != (index % 2 == 0);
    return new LrmItemCreateRequest(name, description, quantity, isSuppressed);
  }

  private static <A, B, R> List<R> zip(
      List<A> as, List<B> bs, java.util.function.BiFunction<A, B, R> combiner) {
    List<R> result = new ArrayList<>();
    int size = Math.min(as.size(), bs.size());
    for (int i = 0; i < size; i++) {
      result.add(combiner.apply(as.get(i), bs.get(i)));
    }
    return result;
  }

  public static final List<ValidationScenario> LIST_CREATE_VALIDATION_SCENARIOS =
      List.of(
          new ValidationScenario(
              "fails when name is null",
              "{ \"name\": null, \"description\": null }",
              "Failed to read request.",
              0),
          new ValidationScenario(
              "fails when name is only whitespace",
              AbstractIntegrationTest.writeValueAsString(
                  new LrmListCreateRequest(" ", "bLLh|Rvz.x0@W2d9G:a", true)),
              VALIDATION_FAILURE_MESSAGE + " name.",
              1),
          new ValidationScenario(
              "fails when name is empty, description is only whitespace",
              AbstractIntegrationTest.writeValueAsString(new LrmListCreateRequest("", " ", true)),
              VALIDATION_FAILURE_MESSAGE + " description, name.",
              3));

  public static final List<ValidationScenario> LIST_ITEM_CREATE_VALIDATION_SCENARIOS =
      List.of(
          new ValidationScenario(
              "fails when name is null",
              "{ \"name\": null, \"description\": null }",
              "Failed to read request.",
              0),
          new ValidationScenario(
              "fails when name is only whitespace",
              AbstractIntegrationTest.writeValueAsString(
                  new LrmItemCreateRequest(" ", "bLLh|Rvz.x0@W2d9G:a", 0, false)),
              VALIDATION_FAILURE_MESSAGE + " name.",
              1),
          new ValidationScenario(
              "fails when name is empty, description is only whitespace",
              AbstractIntegrationTest.writeValueAsString(
                  new LrmItemCreateRequest("", " ", 0, false)),
              VALIDATION_FAILURE_MESSAGE + " description, name.",
              3),
          new ValidationScenario(
              "fails when quantity is less than 0",
              AbstractIntegrationTest.writeValueAsString(
                  new LrmItemCreateRequest("S0hztGQBNl", "S0hztGQBNl", -1, false)),
              VALIDATION_FAILURE_MESSAGE + " quantity.",
              1));

  public static final List<ValidationScenario> LIST_UPDATE_VALIDATION_SCENARIOS =
      List.of(
          new ValidationScenario(
              "fails when name is null",
              "{ \"name\": null, \"description\": null }",
              VALIDATION_FAILURE_MESSAGE + " patchRequest.",
              2),
          new ValidationScenario(
              "fails when name is only whitespace",
              AbstractIntegrationTest.writeValueAsString(
                  new LrmListCreateRequest(" ", "bLLh|Rvz.x0@W2d9G:a", true)),
              VALIDATION_FAILURE_MESSAGE + " patchRequest.",
              1),
          new ValidationScenario(
              "fails when name is empty, description is only whitespace",
              AbstractIntegrationTest.writeValueAsString(new LrmListCreateRequest("", " ", true)),
              VALIDATION_FAILURE_MESSAGE + " patchRequest.",
              2));
}
