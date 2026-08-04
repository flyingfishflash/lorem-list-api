package net.flyingfishflash.loremlist.integration.domain;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.flyingfishflash.loremlist.api.data.request.LrmItemCreateRequest;
import net.flyingfishflash.loremlist.api.data.request.LrmListCreateRequest;
import net.flyingfishflash.loremlist.core.response.structure.Disposition;
import net.flyingfishflash.loremlist.core.response.structure.DispositionOfProblem;
import net.flyingfishflash.loremlist.core.response.structure.DispositionOfSuccess;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest
@ActiveProfiles("h2")
@AutoConfigureMockMvc
public abstract class AbstractIntegrationTest {

  protected static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  @Autowired protected MockMvc mockMvc;

  public ResultActions performRequest(HttpMethod method, String url) throws Exception {
    return performRequest(method, url, null);
  }

  public ResultActions performRequest(HttpMethod method, String url, String content)
      throws Exception {
    MockHttpServletRequestBuilder builder =
        request(method, url).with(jwt()).with(csrf()).contentType(MediaType.APPLICATION_JSON);
    if (content != null) {
      System.out.println("content: " + content);
      builder.content(content);
    }
    return mockMvc.perform(builder);
  }

  protected VerifyBuilder verifyRequest(HttpMethod method, String instance) {
    return new VerifyBuilder(method, instance);
  }

  /** create a list and return the resulting id */
  protected UUID createAndVerifyList(LrmListCreateRequest listRequest) throws Exception {
    ResultActions result =
        verifyRequest(HttpMethod.POST, "/lists")
            .requestBody(writeValueAsString(listRequest))
            .additionalMatchers(
                jsonPath("$.content.id").isNotEmpty(),
                jsonPath("$.content.name").value(listRequest.name()),
                jsonPath("$.content.description").value(listRequest.description()),
                jsonPath("$.content.public").value(listRequest.isPublic()),
                jsonPath("$.content.created").isNotEmpty(),
                jsonPath("$.content.updated").isNotEmpty(),
                jsonPath("$.content.items").isEmpty())
            .perform();
    return extractContentId(result);
  }

  /** create a list item and return the resulting id */
  protected UUID createAndVerifyListItem(UUID listId, LrmItemCreateRequest itemRequest)
      throws Exception {
    ResultActions result =
        verifyRequest(HttpMethod.POST, "/lists/" + listId + "/items")
            .requestBody(writeValueAsString(itemRequest))
            .additionalMatchers(
                jsonPath("$.content.id").isNotEmpty(),
                jsonPath("$.content.name").value(itemRequest.name()),
                jsonPath("$.content.description").value(itemRequest.description()),
                jsonPath("$.content.quantity").value(itemRequest.quantity()),
                jsonPath("$.content.isSuppressed").value(itemRequest.isSuppressed()),
                jsonPath("$.content.created").isNotEmpty(),
                jsonPath("$.content.updated").isNotEmpty(),
                jsonPath("$.content.lists").isNotEmpty(),
                jsonPath("$.content.lists.length()").value(1))
            .perform();
    return extractContentId(result);
  }

  protected void verifyContentSize(String url, int expectedSize) throws Exception {
    verifyRequest(HttpMethod.GET, url).expectedSize(expectedSize).perform();
  }

  protected void verifyContentValue(String url, int expectedValue) throws Exception {
    verifyRequest(HttpMethod.GET, url)
        .additionalMatchers(jsonPath("$.content.value").value(expectedValue))
        .perform();
  }

  protected void verifyContentListsLength(UUID itemId, int expectedListsCount) throws Exception {
    verifyRequest(HttpMethod.GET, "/items/" + itemId)
        .additionalMatchers(jsonPath("$.content.lists.length()").value(expectedListsCount))
        .perform();
  }

  protected void verifyContentItemsLength(UUID listId, int expectedItemsCount) throws Exception {
    verifyRequest(HttpMethod.GET, "/lists/" + listId)
        .additionalMatchers(jsonPath("$.content.items.length()").value(expectedItemsCount))
        .perform();
  }

  protected void verifyListIsFound(UUID listId) throws Exception {
    verifyRequest(HttpMethod.GET, "/lists/" + listId).perform();
  }

  protected void verifyListIsNotFound(UUID listId) throws Exception {
    verifyRequest(HttpMethod.GET, "/lists/" + listId)
        .expectedDisposition(DispositionOfProblem.FAILURE)
        .statusMatcher(status().isNotFound())
        .perform();
  }

  protected void verifyItemIsFound(UUID itemId) throws Exception {
    verifyRequest(HttpMethod.GET, "/items/" + itemId).perform();
  }

  protected void purgeDomain() throws Exception {
    verifyRequest(HttpMethod.DELETE, "/maintenance/purge").perform();
  }

  protected static String writeValueAsString(Object value) {
    try {
      return OBJECT_MAPPER.writeValueAsString(value);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static UUID extractContentId(ResultActions result) throws Exception {
    String response = result.andReturn().getResponse().getContentAsString();
    return UUID.fromString(OBJECT_MAPPER.readTree(response).path("content").path("id").asText());
  }

  /** Fluent replacement for the Kotlin version's defaulted-parameter verification helper. */
  public final class VerifyBuilder {
    private final HttpMethod method;
    private final String instance;
    private String requestBody;
    private Disposition expectedDisposition = DispositionOfSuccess.SUCCESS;
    private ResultMatcher statusMatcher = status().isOk();
    private ResultMatcher contentTypeMatcher = content().contentType(MediaType.APPLICATION_JSON);
    private int expectedSize = 1;
    private ResultMatcher[] additionalMatchers = new ResultMatcher[0];

    private VerifyBuilder(HttpMethod method, String instance) {
      this.method = method;
      this.instance = instance;
    }

    public VerifyBuilder requestBody(String requestBody) {
      this.requestBody = requestBody;
      return this;
    }

    public VerifyBuilder expectedDisposition(Disposition expectedDisposition) {
      this.expectedDisposition = expectedDisposition;
      return this;
    }

    public VerifyBuilder statusMatcher(ResultMatcher statusMatcher) {
      this.statusMatcher = statusMatcher;
      return this;
    }

    public VerifyBuilder contentTypeMatcher(ResultMatcher contentTypeMatcher) {
      this.contentTypeMatcher = contentTypeMatcher;
      return this;
    }

    public VerifyBuilder expectedSize(int expectedSize) {
      this.expectedSize = expectedSize;
      return this;
    }

    public VerifyBuilder additionalMatchers(ResultMatcher... additionalMatchers) {
      this.additionalMatchers = additionalMatchers;
      return this;
    }

    public ResultActions perform() throws Exception {
      ResultActions result = performRequest(method, instance, requestBody);
      List<ResultMatcher> matchers = new ArrayList<>();
      matchers.add(statusMatcher);
      matchers.add(contentTypeMatcher);
      matchers.add(jsonPath("$.disposition").value(expectedDisposition.nameAsLowercase()));
      matchers.add(jsonPath("$.method").value(method.name().toLowerCase()));
      int queryIndex = instance.indexOf('?');
      matchers.add(
          jsonPath("$.instance")
              .value(queryIndex < 0 ? instance : instance.substring(0, queryIndex)));
      matchers.add(jsonPath("$.size").value(expectedSize));
      matchers.addAll(List.of(additionalMatchers));
      return result.andExpectAll(matchers.toArray(new ResultMatcher[0]));
    }
  }
}
