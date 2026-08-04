package net.flyingfishflash.loremlist.unit.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import kotlinx.datetime.Clock;
import kotlinx.datetime.Instant;
import net.flyingfishflash.loremlist.api.LrmItemApiService;
import net.flyingfishflash.loremlist.api.LrmListApiService;
import net.flyingfishflash.loremlist.api.LrmListController;
import net.flyingfishflash.loremlist.api.data.request.LrmItemCreateRequest;
import net.flyingfishflash.loremlist.api.data.request.LrmListCreateRequest;
import net.flyingfishflash.loremlist.api.data.request.LrmListItemAddRequest;
import net.flyingfishflash.loremlist.api.data.response.ApiServiceResponse;
import net.flyingfishflash.loremlist.api.data.response.AssociationDeletedResponse;
import net.flyingfishflash.loremlist.api.data.response.AssociationsDeletedResponse;
import net.flyingfishflash.loremlist.api.data.response.LrmItemResponse;
import net.flyingfishflash.loremlist.api.data.response.LrmListDeletedResponse;
import net.flyingfishflash.loremlist.api.data.response.LrmListItemAddedResponse;
import net.flyingfishflash.loremlist.api.data.response.LrmListItemMovedResponse;
import net.flyingfishflash.loremlist.api.data.response.LrmListItemResponse;
import net.flyingfishflash.loremlist.api.data.response.LrmListResponse;
import net.flyingfishflash.loremlist.core.configuration.SerializationConfig;
import net.flyingfishflash.loremlist.core.configuration.WebSecurityConfiguration;
import net.flyingfishflash.loremlist.core.response.advice.CoreExceptionHandler;
import net.flyingfishflash.loremlist.core.response.structure.ApiMessageNumeric;
import net.flyingfishflash.loremlist.core.response.structure.DispositionOfProblem;
import net.flyingfishflash.loremlist.core.response.structure.DispositionOfSuccess;
import net.flyingfishflash.loremlist.domain.exceptions.DomainException;
import net.flyingfishflash.loremlist.domain.lrmitem.ItemNotFoundException;
import net.flyingfishflash.loremlist.domain.lrmitem.LrmItem;
import net.flyingfishflash.loremlist.domain.lrmitem.LrmItemSuccinct;
import net.flyingfishflash.loremlist.domain.lrmlist.ListNotFoundException;
import net.flyingfishflash.loremlist.domain.lrmlist.LrmList;
import net.flyingfishflash.loremlist.domain.lrmlistitem.ListItemNotFoundException;
import net.flyingfishflash.loremlist.domain.lrmlistitem.LrmListItem;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** LrmListController Unit Tests */
@WebMvcTest(controllers = LrmListController.class)
@Import({SerializationConfig.class, WebSecurityConfiguration.class})
class LrmListControllerTests {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private LrmListApiService mockLrmListApiService;

  @MockitoBean private LrmItemApiService mockLrmItemApiService;

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private final Instant now = Clock.System.INSTANCE.now();
  private final UUID id0 = UUID.fromString("00000000-0000-4000-a000-000000000000");
  private final UUID id1 = UUID.fromString("00000000-0000-4000-a000-000000000001");
  private final UUID id2 = UUID.fromString("00000000-0000-4000-a000-000000000002");
  private final UUID id3 = UUID.fromString("00000000-0000-4000-a000-000000000003");
  private final String apiResponseMessage =
      "ksADs8y96KRa1Zo4ipMdr5t8faudmFj4c564S02MjsNG6TXEO7yctC08Bb53bCB7";
  private final LrmItemCreateRequest lrmItemCreateRequest =
      new LrmItemCreateRequest("Lorem Item Name", "Lorem Item Description", 0, false);

  private LrmList createLrmList(UUID id) {
    return new LrmList(
        id,
        "Lorem List Name",
        "Lorem List Description",
        true,
        "Lorem Ipsum Owner",
        now,
        "Lorem Ipsum Created By",
        now,
        "Lorem Ipsum Updated By",
        Set.of());
  }

  private LrmItem createLrmItem(UUID id) {
    return new LrmItem(
        id,
        "Lorem Item Name",
        "Lorem Item Description",
        "Lorem Ipsum Owner",
        now,
        "Lorem Ipsum Created By",
        now,
        "Lorem Ipsum Updated By",
        Set.of());
  }

  private LrmListItem createLrmListItem(UUID id) {
    return new LrmListItem(
        id,
        UUID.randomUUID(),
        "Lorem List Item Name",
        "Lorem List Item Description",
        0,
        false,
        "Lorem Ipsum Owner",
        now,
        "Lorem Ipsum Created By",
        now,
        "Lorem Ipsum Updated By",
        Set.of());
  }

  private LrmItem lrmItem() {
    return new LrmItem(
        id0,
        lrmItemCreateRequest.name(),
        lrmItemCreateRequest.description(),
        "Lorem Ipsum Owner",
        now,
        "Lorem Ipsum Created By",
        now,
        "Lorem Ipsum Updated By",
        Set.of());
  }

  private static String writeValueAsString(Object value) throws Exception {
    return OBJECT_MAPPER.writeValueAsString(value);
  }

  @Nested
  class Lists {

    @Test
    void listsAreDeleted() throws Exception {
      LrmListDeletedResponse content =
          new LrmListDeletedResponse(List.of("Lorem List Name"), List.of("Lorem Item Name"));
      when(mockLrmListApiService.deleteByOwner(anyString()))
          .thenReturn(new ApiServiceResponse<>(content, apiResponseMessage));
      String instance = "/lists";

      mockMvc
          .perform(
              delete(instance).with(jwt()).with(csrf()).contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(
              jsonPath("$.disposition").value(DispositionOfSuccess.SUCCESS.nameAsLowercase()))
          .andExpect(jsonPath("$.method").value(HttpMethod.DELETE.name().toLowerCase()))
          .andExpect(jsonPath("$.message").value(apiResponseMessage))
          .andExpect(jsonPath("$.instance").value(instance))
          .andExpect(jsonPath("$.size").value(1))
          .andExpect(jsonPath("$.content.listNames[0]").value(content.listNames().get(0)))
          .andExpect(
              jsonPath("$.content.associatedItemNames[0]")
                  .value(content.associatedItemNames().get(0)));
    }

    @Test
    void listsAreFound() throws Exception {
      List<LrmListResponse> content = List.of(LrmListResponse.fromLrmList(createLrmList(id0)));
      when(mockLrmListApiService.findByOwnerExcludeItems(anyString()))
          .thenReturn(new ApiServiceResponse<>(content, apiResponseMessage));
      String instance = "/lists";

      mockMvc
          .perform(get(instance).with(jwt()).contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(
              jsonPath("$.disposition").value(DispositionOfSuccess.SUCCESS.nameAsLowercase()))
          .andExpect(jsonPath("$.method").value(HttpMethod.GET.name().toLowerCase()))
          .andExpect(jsonPath("$.message").value(apiResponseMessage))
          .andExpect(jsonPath("$.instance").value(instance))
          .andExpect(jsonPath("$.size").value(1))
          .andExpect(jsonPath("$.content").exists())
          .andExpect(jsonPath("$.content").isArray())
          .andExpect(jsonPath("$.content.[0].name").value(content.get(0).name()))
          .andExpect(jsonPath("$.content.[0].description").value(content.get(0).description()))
          .andExpect(jsonPath("$.content.[0].items").isEmpty());
    }

    @Test
    void listsAreFoundIncludeItemsTrue() throws Exception {
      List<LrmListResponse> content = List.of(LrmListResponse.fromLrmList(createLrmList(id0)));
      when(mockLrmListApiService.findByOwner(anyString()))
          .thenReturn(new ApiServiceResponse<>(content, apiResponseMessage));
      String instance = "/lists?includeItems=true";
      String path = "/lists";

      mockMvc
          .perform(get(instance).with(jwt()).contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(
              jsonPath("$.disposition").value(DispositionOfSuccess.SUCCESS.nameAsLowercase()))
          .andExpect(jsonPath("$.method").value(HttpMethod.GET.name().toLowerCase()))
          .andExpect(jsonPath("$.message").value(apiResponseMessage))
          .andExpect(jsonPath("$.instance").value(path))
          .andExpect(jsonPath("$.size").value(content.size()))
          .andExpect(jsonPath("$.content").isArray())
          .andExpect(jsonPath("$.content.[0].name").value(createLrmList(id0).name()))
          .andExpect(jsonPath("$.content.[0].description").value(createLrmList(id0).description()));
    }

    @Test
    void listIsCreated() throws Exception {
      LrmListCreateRequest lrmListCreateRequest =
          new LrmListCreateRequest("Lorem List Name", "Lorem List Description", true);
      when(mockLrmListApiService.create(eq(lrmListCreateRequest), anyString()))
          .thenReturn(
              new ApiServiceResponse<>(
                  LrmListResponse.fromLrmList(createLrmList(id0)), apiResponseMessage));
      String instance = "/lists";

      mockMvc
          .perform(
              post(instance)
                  .with(jwt())
                  .with(csrf())
                  .content(writeValueAsString(lrmListCreateRequest))
                  .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(
              jsonPath("$.disposition").value(DispositionOfSuccess.SUCCESS.nameAsLowercase()))
          .andExpect(jsonPath("$.method").value(HttpMethod.POST.name().toLowerCase()))
          .andExpect(jsonPath("$.message").value(apiResponseMessage))
          .andExpect(jsonPath("$.instance").value(instance))
          .andExpect(jsonPath("$.size").value(1))
          .andExpect(jsonPath("$.content.description").value(createLrmList(id0).description()))
          .andExpect(jsonPath("$.content.name").value(createLrmList(id0).name()));
    }

    @Test
    void requestedListNameIsAnEmptyString() throws Exception {
      LrmListCreateRequest requestBody =
          new LrmListCreateRequest(
              "", createLrmList(id0).description(), createLrmList(id0).isPublic());
      String instance = "/lists";

      mockMvc
          .perform(
              post(instance)
                  .with(jwt())
                  .with(csrf())
                  .content(writeValueAsString(requestBody))
                  .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isBadRequest())
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(
              jsonPath("$.disposition").value(DispositionOfProblem.FAILURE.nameAsLowercase()))
          .andExpect(jsonPath("$.method").value(HttpMethod.POST.name().toLowerCase()))
          .andExpect(
              jsonPath("$.message")
                  .value(CoreExceptionHandler.VALIDATION_FAILURE_MESSAGE + " name."))
          .andExpect(jsonPath("$.instance").value(instance))
          .andExpect(jsonPath("$.size").value(1))
          .andExpect(
              jsonPath("$.content.title")
                  .value(
                      org.springframework.web.bind.MethodArgumentNotValidException.class
                          .getSimpleName()))
          .andExpect(jsonPath("$.content.status").value(HttpStatus.BAD_REQUEST.value()));
    }

    @Test
    void requestedListDescriptionIsAnEmptyString() throws Exception {
      LrmListCreateRequest requestBody =
          new LrmListCreateRequest(createLrmList(id0).name(), "", createLrmList(id0).isPublic());
      String instance = "/lists";

      mockMvc
          .perform(
              post(instance)
                  .with(jwt())
                  .with(csrf())
                  .content(writeValueAsString(requestBody))
                  .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isBadRequest())
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(
              jsonPath("$.disposition").value(DispositionOfProblem.FAILURE.nameAsLowercase()))
          .andExpect(jsonPath("$.method").value(HttpMethod.POST.name().toLowerCase()))
          .andExpect(
              jsonPath("$.message")
                  .value(CoreExceptionHandler.VALIDATION_FAILURE_MESSAGE + " description."))
          .andExpect(jsonPath("$.instance").value(instance))
          .andExpect(jsonPath("$.size").value(1))
          .andExpect(
              jsonPath("$.content.title")
                  .value(
                      org.springframework.web.bind.MethodArgumentNotValidException.class
                          .getSimpleName()))
          .andExpect(jsonPath("$.content.status").value(HttpStatus.BAD_REQUEST.value()));
    }
  }

  @Nested
  class ListsWithNoItems {

    @Test
    void listsWithNoItemAssociationAreFound() throws Exception {
      List<LrmListResponse> content = List.of(LrmListResponse.fromLrmList(createLrmList(id0)));
      when(mockLrmListApiService.findByOwnerAndHavingNoItemAssociations(anyString()))
          .thenReturn(new ApiServiceResponse<>(content, apiResponseMessage));
      String instance = "/lists/with-no-items";

      mockMvc
          .perform(get(instance).with(jwt()).contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(
              jsonPath("$.disposition").value(DispositionOfSuccess.SUCCESS.nameAsLowercase()))
          .andExpect(jsonPath("$.method").value(HttpMethod.GET.name().toLowerCase()))
          .andExpect(jsonPath("$.message").value(apiResponseMessage))
          .andExpect(jsonPath("$.instance").value(instance))
          .andExpect(jsonPath("$.size").value(content.size()))
          .andExpect(jsonPath("$.content").exists())
          .andExpect(jsonPath("$.content").isArray())
          .andExpect(jsonPath("$.content.[0].name").value(content.get(0).name()))
          .andExpect(jsonPath("$.content.[0].description").value(content.get(0).description()));
    }
  }

  @Nested
  class ListsCount {

    @Test
    void countOfListsIsReturned() throws Exception {
      when(mockLrmListApiService.countByOwner(anyString()))
          .thenReturn(new ApiServiceResponse<>(new ApiMessageNumeric(999), apiResponseMessage));
      String instance = "/lists/count";

      mockMvc
          .perform(get(instance).with(jwt()).contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(
              jsonPath("$.disposition").value(DispositionOfSuccess.SUCCESS.nameAsLowercase()))
          .andExpect(jsonPath("$.method").value(HttpMethod.GET.name().toLowerCase()))
          .andExpect(jsonPath("$.message").value(apiResponseMessage))
          .andExpect(jsonPath("$.instance").value(instance))
          .andExpect(jsonPath("$.size").value(1))
          .andExpect(jsonPath("$.content.length()").value(1))
          .andExpect(jsonPath("$.content.value").value(999));
    }
  }

  @Nested
  class ListById {

    @Test
    void listIsDeleted() throws Exception {
      String instance = "/lists/" + id1;
      LrmListDeletedResponse content =
          new LrmListDeletedResponse(List.of("dolor sit amet"), List.of("Lorem Ipsum"));
      when(mockLrmListApiService.deleteByOwnerAndId(eq(id1), anyString(), eq(false)))
          .thenReturn(new ApiServiceResponse<>(content, apiResponseMessage));

      mockMvc
          .perform(
              delete(instance).with(jwt()).with(csrf()).contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(
              jsonPath("$.disposition").value(DispositionOfSuccess.SUCCESS.nameAsLowercase()))
          .andExpect(jsonPath("$.method").value(HttpMethod.DELETE.name().toLowerCase()))
          .andExpect(jsonPath("$.message").value(apiResponseMessage))
          .andExpect(jsonPath("$.instance").value(instance))
          .andExpect(jsonPath("$.size").value(1))
          .andExpect(jsonPath("$.content.listNames.length()").value(1))
          .andExpect(jsonPath("$.content.listNames.[0]").value("dolor sit amet"))
          .andExpect(jsonPath("$.content.associatedItemNames.length()").value(1))
          .andExpect(jsonPath("$.content.associatedItemNames.[0]").value("Lorem Ipsum"));

      verify(mockLrmListApiService, times(1))
          .deleteByOwnerAndId(any(UUID.class), anyString(), anyBoolean());
    }

    @Test
    void listIsNotFoundOnDelete() throws Exception {
      String instance = "/lists/" + id1;
      when(mockLrmListApiService.deleteByOwnerAndId(eq(id1), anyString(), eq(false)))
          .thenThrow(new ListNotFoundException(id1));

      mockMvc
          .perform(
              delete(instance).with(jwt()).with(csrf()).contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isNotFound())
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(
              jsonPath("$.disposition").value(DispositionOfProblem.FAILURE.nameAsLowercase()))
          .andExpect(jsonPath("$.method").value(HttpMethod.DELETE.name().toLowerCase()))
          .andExpect(jsonPath("$.instance").value(instance))
          .andExpect(jsonPath("$.size").value(1))
          .andExpect(jsonPath("$.content.title").value(ListNotFoundException.class.getSimpleName()))
          .andExpect(jsonPath("$.content.status").value(HttpStatus.NOT_FOUND.value()));
    }

    @Test
    void listIsFound() throws Exception {
      String instance = "/lists/" + id1;
      when(mockLrmListApiService.findByOwnerAndId(eq(id1), anyString()))
          .thenReturn(
              new ApiServiceResponse<>(
                  LrmListResponse.fromLrmList(createLrmList(id0)), apiResponseMessage));

      mockMvc
          .perform(get(instance).with(jwt()).contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(
              jsonPath("$.disposition").value(DispositionOfSuccess.SUCCESS.nameAsLowercase()))
          .andExpect(jsonPath("$.method").value(HttpMethod.GET.name().toLowerCase()))
          .andExpect(jsonPath("$.message").value(apiResponseMessage))
          .andExpect(jsonPath("$.instance").value(instance))
          .andExpect(jsonPath("$.size").value(1))
          .andExpect(jsonPath("$.content.description").value(createLrmList(id0).description()))
          .andExpect(jsonPath("$.content.name").value(createLrmList(id0).name()));
    }

    @Test
    void listIsFoundIncludeItemsTrue() throws Exception {
      String instance = "/lists/" + id1 + "?includeItems=true";
      String path = "/lists/" + id1;
      when(mockLrmListApiService.findByOwnerAndId(eq(id1), anyString()))
          .thenReturn(
              new ApiServiceResponse<>(
                  LrmListResponse.fromLrmList(createLrmList(id0)), apiResponseMessage));

      mockMvc
          .perform(get(instance).with(jwt()).contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(
              jsonPath("$.disposition").value(DispositionOfSuccess.SUCCESS.nameAsLowercase()))
          .andExpect(jsonPath("$.method").value(HttpMethod.GET.name().toLowerCase()))
          .andExpect(jsonPath("$.message").value(apiResponseMessage))
          .andExpect(jsonPath("$.instance").value(path))
          .andExpect(jsonPath("$.size").value(1))
          .andExpect(jsonPath("$.content.description").value(createLrmList(id0).description()))
          .andExpect(jsonPath("$.content.name").value(createLrmList(id0).name()))
          .andExpect(jsonPath("$.content.items").isArray());
    }

    @Test
    void listIsFoundIncludeItemsFalse() throws Exception {
      String instance = "/lists/" + id1 + "?includeItems=false";
      String path = "/lists/" + id1;
      when(mockLrmListApiService.findByOwnerAndIdExcludeItems(eq(id1), anyString()))
          .thenReturn(
              new ApiServiceResponse<>(
                  LrmListResponse.fromLrmList(createLrmList(id0)), apiResponseMessage));

      mockMvc
          .perform(get(instance).with(jwt()).contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(
              jsonPath("$.disposition").value(DispositionOfSuccess.SUCCESS.nameAsLowercase()))
          .andExpect(jsonPath("$.method").value(HttpMethod.GET.name().toLowerCase()))
          .andExpect(jsonPath("$.message").value(apiResponseMessage))
          .andExpect(jsonPath("$.instance").value(path))
          .andExpect(jsonPath("$.size").value(1))
          .andExpect(jsonPath("$.content.description").value(createLrmList(id0).description()))
          .andExpect(jsonPath("$.content.name").value(createLrmList(id0).name()))
          .andExpect(jsonPath("$.content.items").isEmpty());
    }

    @Test
    void listIsNotFoundOnGet() throws Exception {
      String instance = "/lists/" + id1;
      when(mockLrmListApiService.findByOwnerAndId(eq(id1), anyString()))
          .thenThrow(new ListNotFoundException(id1));

      mockMvc
          .perform(get(instance).with(jwt()).contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isNotFound())
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(
              jsonPath("$.disposition").value(DispositionOfProblem.FAILURE.nameAsLowercase()))
          .andExpect(jsonPath("$.method").value(HttpMethod.GET.name().toLowerCase()))
          .andExpect(jsonPath("$.instance").value(instance))
          .andExpect(jsonPath("$.size").value(1))
          .andExpect(jsonPath("$.content.title").value(ListNotFoundException.class.getSimpleName()))
          .andExpect(jsonPath("$.content.status").value(HttpStatus.NOT_FOUND.value()));
    }

    @Test
    void listIsFoundAndUpdated() throws Exception {
      String instance = "/lists/" + id1;
      when(mockLrmListApiService.patchByOwnerAndId(eq(id1), anyString(), any()))
          .thenReturn(
              new ApiServiceResponse<>(
                  LrmListResponse.fromLrmList(createLrmList(id0)), apiResponseMessage));
      Map<String, Object> patchBody = Map.of("name", createLrmList(id0).name());

      mockMvc
          .perform(
              patch(instance)
                  .with(jwt())
                  .with(csrf())
                  .content(writeValueAsString(patchBody))
                  .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(
              jsonPath("$.disposition").value(DispositionOfSuccess.SUCCESS.nameAsLowercase()))
          .andExpect(jsonPath("$.method").value(HttpMethod.PATCH.name().toLowerCase()))
          .andExpect(jsonPath("$.message").value(apiResponseMessage))
          .andExpect(jsonPath("$.instance").value(instance))
          .andExpect(jsonPath("$.size").value(1))
          .andExpect(jsonPath("$.content.description").value(createLrmList(id0).description()))
          .andExpect(jsonPath("$.content.name").value(createLrmList(id0).name()))
          .andExpect(jsonPath("$.content.items").isEmpty());
    }

    @Test
    void listIsFoundAndNotUpdated() throws Exception {
      String instance = "/lists/" + id1;
      when(mockLrmListApiService.patchByOwnerAndId(eq(id1), anyString(), any()))
          .thenReturn(
              new ApiServiceResponse<>(
                  LrmListResponse.fromLrmList(createLrmList(id0)), "not updated"));
      Map<String, Object> patchBody = Map.of("name", createLrmList(id0).name());

      mockMvc
          .perform(
              patch(instance)
                  .with(jwt())
                  .with(csrf())
                  .content(writeValueAsString(patchBody))
                  .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isNoContent())
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(
              jsonPath("$.disposition").value(DispositionOfSuccess.SUCCESS.nameAsLowercase()))
          .andExpect(jsonPath("$.method").value(HttpMethod.PATCH.name().toLowerCase()))
          .andExpect(jsonPath("$.instance").value(instance))
          .andExpect(jsonPath("$.size").value(1))
          .andExpect(jsonPath("$.content.description").value(createLrmList(id0).description()))
          .andExpect(jsonPath("$.content.name").value(createLrmList(id0).name()))
          .andExpect(jsonPath("$.content.items").isEmpty());
    }

    @Test
    void listIsNotFoundOnPatch() throws Exception {
      String instance = "/lists/" + id1;
      when(mockLrmListApiService.patchByOwnerAndId(eq(id1), anyString(), any()))
          .thenThrow(new ListNotFoundException(id1));
      Map<String, Object> patchBody = Map.of("name", createLrmList(id0).name());

      mockMvc
          .perform(
              patch(instance)
                  .with(jwt())
                  .with(csrf())
                  .content(writeValueAsString(patchBody))
                  .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isNotFound())
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(
              jsonPath("$.disposition").value(DispositionOfProblem.FAILURE.nameAsLowercase()))
          .andExpect(jsonPath("$.method").value(HttpMethod.PATCH.name().toLowerCase()))
          .andExpect(jsonPath("$.instance").value(instance))
          .andExpect(jsonPath("$.size").value(1))
          .andExpect(jsonPath("$.content.title").value(ListNotFoundException.class.getSimpleName()))
          .andExpect(jsonPath("$.content.status").value(HttpStatus.NOT_FOUND.value()));
    }
  }

  @Nested
  class ListItems {

    @Test
    void allItemsAreRemovedFromAList() throws Exception {
      String instance = "/lists/" + id1 + "/items";
      when(mockLrmListApiService.removeAllListItems(eq(id1), anyString()))
          .thenReturn(
              new ApiServiceResponse<>(
                  new AssociationsDeletedResponse("irrelevant", 999), apiResponseMessage));

      mockMvc
          .perform(
              delete(instance).with(jwt()).with(csrf()).contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(
              jsonPath("$.disposition").value(DispositionOfSuccess.SUCCESS.nameAsLowercase()))
          .andExpect(jsonPath("$.method").value(HttpMethod.DELETE.name().toLowerCase()))
          .andExpect(jsonPath("$.message").value(apiResponseMessage))
          .andExpect(jsonPath("$.instance").value(instance))
          .andExpect(jsonPath("$.size").value(1))
          .andExpect(jsonPath("$.content.value").value(999));
    }

    @Test
    void listItemIsCreatedAndAddedToAList() throws Exception {
      String instance = "/lists/" + id1 + "/items";
      LrmItemCreateRequest requestBody =
          new LrmItemCreateRequest("List Item Name", "List Item Description", 99, false);
      LrmListItemResponse lrmListItemResponse =
          LrmListItemResponse.fromLrmListItem(createLrmListItem(id0));
      when(mockLrmListApiService.createListItem(eq(id1), eq(requestBody), anyString()))
          .thenReturn(new ApiServiceResponse<>(lrmListItemResponse, apiResponseMessage));

      mockMvc
          .perform(
              post(instance)
                  .with(jwt())
                  .with(csrf())
                  .content(writeValueAsString(requestBody))
                  .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(
              jsonPath("$.disposition").value(DispositionOfSuccess.SUCCESS.nameAsLowercase()))
          .andExpect(jsonPath("$.method").value(HttpMethod.POST.name().toLowerCase()))
          .andExpect(jsonPath("$.message").value(apiResponseMessage))
          .andExpect(jsonPath("$.instance").value(instance))
          .andExpect(jsonPath("$.size").value(1))
          .andExpect(jsonPath("$.content.id").value(lrmListItemResponse.id().toString()));
    }

    @Test
    void listItemIsNotCreated() throws Exception {
      String instance = "/lists/" + id1 + "/items";
      LrmItemCreateRequest requestBody =
          new LrmItemCreateRequest("List Item Name", "List Item Description", 99, false);
      when(mockLrmListApiService.createListItem(eq(id1), eq(requestBody), anyString()))
          .thenThrow(DomainException.builder().build());

      mockMvc
          .perform(
              post(instance)
                  .with(jwt())
                  .with(csrf())
                  .content(writeValueAsString(requestBody))
                  .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isInternalServerError())
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(jsonPath("$.disposition").value(DispositionOfProblem.ERROR.nameAsLowercase()))
          .andExpect(jsonPath("$.method").value(HttpMethod.POST.name().toLowerCase()))
          .andExpect(jsonPath("$.message").value(DomainException.DEFAULT_TITLE))
          .andExpect(jsonPath("$.instance").value(instance))
          .andExpect(jsonPath("$.size").value(1))
          .andExpect(jsonPath("$.content.detail").value(DomainException.DEFAULT_TITLE));
    }

    @Test
    void itemIsAddedToList() throws Exception {
      String instance = "/lists/" + id1 + "/items";
      LrmListItemAddedResponse mockResponse =
          new LrmListItemAddedResponse(
              createLrmList(id1).name(), List.of(LrmItemSuccinct.fromLrmItem(createLrmItem(id2))));
      when(mockLrmListApiService.addListItem(eq(id1), any(), anyString()))
          .thenReturn(new ApiServiceResponse<>(mockResponse, apiResponseMessage));
      LrmListItemAddRequest requestBody = new LrmListItemAddRequest(Set.of(UUID.randomUUID()));

      mockMvc
          .perform(
              put(instance)
                  .with(jwt())
                  .with(csrf())
                  .content(writeValueAsString(requestBody))
                  .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(
              jsonPath("$.disposition").value(DispositionOfSuccess.SUCCESS.nameAsLowercase()))
          .andExpect(jsonPath("$.method").value(HttpMethod.PUT.name().toLowerCase()))
          .andExpect(jsonPath("$.message").value(apiResponseMessage))
          .andExpect(jsonPath("$.instance").value(instance))
          .andExpect(jsonPath("$.size").value(1))
          .andExpect(jsonPath("$.content.componentName").value(mockResponse.componentName()))
          .andExpect(jsonPath("$.content.associatedComponents.length()").value(1))
          .andExpect(jsonPath("$.content.associatedComponents[0].type").value("item"))
          .andExpect(
              jsonPath("$.content.associatedComponents[0].id")
                  .value(mockResponse.associatedComponents().get(0).id().toString()))
          .andExpect(
              jsonPath("$.content.associatedComponents[0].name")
                  .value(mockResponse.associatedComponents().get(0).name()));
    }

    @Test
    void itemIsAddedToLists() throws Exception {
      String instance = "/lists/" + id1 + "/items";
      LrmListItemAddedResponse mockResponse =
          new LrmListItemAddedResponse(
              createLrmList(id1).name(),
              List.of(
                  LrmItemSuccinct.fromLrmItem(createLrmItem(id2)),
                  LrmItemSuccinct.fromLrmItem(createLrmItem(id3))));
      when(mockLrmListApiService.addListItem(eq(id1), any(), anyString()))
          .thenReturn(new ApiServiceResponse<>(mockResponse, apiResponseMessage));
      LrmListItemAddRequest requestBody = new LrmListItemAddRequest(Set.of(UUID.randomUUID()));

      mockMvc
          .perform(
              put(instance)
                  .with(jwt())
                  .with(csrf())
                  .content(writeValueAsString(requestBody))
                  .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(
              jsonPath("$.disposition").value(DispositionOfSuccess.SUCCESS.nameAsLowercase()))
          .andExpect(jsonPath("$.method").value(HttpMethod.PUT.name().toLowerCase()))
          .andExpect(jsonPath("$.message").value(apiResponseMessage))
          .andExpect(jsonPath("$.instance").value(instance))
          .andExpect(jsonPath("$.size").value(1))
          .andExpect(jsonPath("$.content.componentName").value(mockResponse.componentName()))
          .andExpect(jsonPath("$.content.associatedComponents.length()").value(2))
          .andExpect(jsonPath("$.content.associatedComponents[0].type").value("item"))
          .andExpect(
              jsonPath("$.content.associatedComponents[0].id")
                  .value(mockResponse.associatedComponents().get(0).id().toString()))
          .andExpect(
              jsonPath("$.content.associatedComponents[0].name")
                  .value(mockResponse.associatedComponents().get(0).name()));
    }

    @Test
    void listIsNotFoundOnAddListItem() throws Exception {
      String instance = "/lists/" + id1 + "/items";
      when(mockLrmListApiService.addListItem(eq(id1), any(), anyString()))
          .thenThrow(new ListNotFoundException(id2));
      LrmListItemAddRequest requestBody = new LrmListItemAddRequest(Set.of(UUID.randomUUID()));

      mockMvc
          .perform(
              put(instance)
                  .with(jwt())
                  .with(csrf())
                  .content(writeValueAsString(requestBody))
                  .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isNotFound())
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(
              jsonPath("$.disposition").value(DispositionOfProblem.FAILURE.nameAsLowercase()))
          .andExpect(jsonPath("$.method").value(HttpMethod.PUT.name().toLowerCase()))
          .andExpect(jsonPath("$.instance").value(instance))
          .andExpect(jsonPath("$.size").value(1))
          .andExpect(jsonPath("$.content.title").value(ListNotFoundException.class.getSimpleName()))
          .andExpect(jsonPath("$.content.status").value(HttpStatus.NOT_FOUND.value()));
    }

    @Test
    void itemIsNotFoundOnAddListItem() throws Exception {
      String instance = "/lists/" + id1 + "/items";
      when(mockLrmListApiService.addListItem(eq(id1), any(), anyString()))
          .thenThrow(new ItemNotFoundException(id2));
      LrmListItemAddRequest requestBody = new LrmListItemAddRequest(Set.of(UUID.randomUUID()));

      mockMvc
          .perform(
              put(instance)
                  .with(jwt())
                  .with(csrf())
                  .content(writeValueAsString(requestBody))
                  .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isNotFound())
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(
              jsonPath("$.disposition").value(DispositionOfProblem.FAILURE.nameAsLowercase()))
          .andExpect(jsonPath("$.method").value(HttpMethod.PUT.name().toLowerCase()))
          .andExpect(jsonPath("$.instance").value(instance))
          .andExpect(jsonPath("$.size").value(1))
          .andExpect(jsonPath("$.content.title").value(ItemNotFoundException.class.getSimpleName()))
          .andExpect(jsonPath("$.content.status").value(HttpStatus.NOT_FOUND.value()));
    }
  }

  @Nested
  class ListItemById {

    @Test
    void itemIsRemovedFromList() throws Exception {
      String instance = "/lists/" + id1 + "/items/" + id2;
      String lrmItemName = "58cVf5N8rSstjC6L";
      String lrmListName = "nxuS5LKlpP9TVhzV";
      when(mockLrmListApiService.removeListItem(eq(id1), eq(id2), anyString()))
          .thenReturn(
              new ApiServiceResponse<>(
                  new AssociationDeletedResponse(lrmItemName, lrmListName), apiResponseMessage));

      mockMvc
          .perform(
              delete(instance).with(jwt()).with(csrf()).contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(
              jsonPath("$.disposition").value(DispositionOfSuccess.SUCCESS.nameAsLowercase()))
          .andExpect(jsonPath("$.method").value(HttpMethod.DELETE.name().toLowerCase()))
          .andExpect(jsonPath("$.message").value(apiResponseMessage))
          .andExpect(jsonPath("$.instance").value(instance))
          .andExpect(jsonPath("$.size").value(1));
    }

    @Test
    void itemIsNotFoundOnRemoveListItem() throws Exception {
      String instance = "/lists/" + id1 + "/items/" + id2;
      when(mockLrmListApiService.removeListItem(eq(id1), eq(id2), anyString()))
          .thenThrow(new ItemNotFoundException(id2));

      mockMvc
          .perform(
              delete(instance).with(jwt()).with(csrf()).contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isNotFound())
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(
              jsonPath("$.disposition").value(DispositionOfProblem.FAILURE.nameAsLowercase()))
          .andExpect(jsonPath("$.method").value(HttpMethod.DELETE.name().toLowerCase()))
          .andExpect(jsonPath("$.instance").value(instance))
          .andExpect(jsonPath("$.size").value(1))
          .andExpect(jsonPath("$.content.title").value(ItemNotFoundException.class.getSimpleName()))
          .andExpect(jsonPath("$.content.status").value(HttpStatus.NOT_FOUND.value()));
    }

    @Test
    void listIsNotFoundOnRemoveListItem() throws Exception {
      String instance = "/lists/" + id1 + "/items/" + id2;
      when(mockLrmListApiService.removeListItem(eq(id1), eq(id2), anyString()))
          .thenThrow(new ListNotFoundException(id1));

      mockMvc
          .perform(
              delete(instance).with(jwt()).with(csrf()).contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isNotFound())
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(
              jsonPath("$.disposition").value(DispositionOfProblem.FAILURE.nameAsLowercase()))
          .andExpect(jsonPath("$.method").value(HttpMethod.DELETE.name().toLowerCase()))
          .andExpect(jsonPath("$.instance").value(instance))
          .andExpect(jsonPath("$.size").value(1))
          .andExpect(jsonPath("$.content.title").value(ListNotFoundException.class.getSimpleName()))
          .andExpect(jsonPath("$.content.status").value(HttpStatus.NOT_FOUND.value()));
    }

    @Test
    void listItemIsUpdated() throws Exception {
      String instance = "/lists/" + id1 + "/items/" + id2;
      LrmListItemResponse apiResponseContent =
          LrmListItemResponse.fromLrmListItem(createLrmListItem(id3));
      when(mockLrmListApiService.patchListItem(eq(id1), eq(id2), anyString(), any()))
          .thenReturn(new ApiServiceResponse<>(apiResponseContent, apiResponseMessage));
      Map<String, Object> patchBody = Map.of("name", createLrmItem(id0).name());

      mockMvc
          .perform(
              patch(instance)
                  .with(jwt())
                  .with(csrf())
                  .content(writeValueAsString(patchBody))
                  .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(
              jsonPath("$.disposition").value(DispositionOfSuccess.SUCCESS.nameAsLowercase()))
          .andExpect(jsonPath("$.method").value(HttpMethod.PATCH.name().toLowerCase()))
          .andExpect(jsonPath("$.message").value(apiResponseMessage))
          .andExpect(jsonPath("$.instance").value(instance))
          .andExpect(jsonPath("$.size").value(1))
          .andExpect(jsonPath("$.content.name").value(apiResponseContent.name()));
    }

    @Test
    void listItemIsNotUpdated() throws Exception {
      String instance = "/lists/" + id1 + "/items/" + id2;
      LrmListItemResponse apiResponseContent =
          LrmListItemResponse.fromLrmListItem(createLrmListItem(id3));
      when(mockLrmListApiService.patchListItem(eq(id1), eq(id2), anyString(), any()))
          .thenReturn(new ApiServiceResponse<>(apiResponseContent, "not updated"));
      Map<String, Object> patchBody = Map.of("name", createLrmItem(id0).name());

      mockMvc
          .perform(
              patch(instance)
                  .with(jwt())
                  .with(csrf())
                  .content(writeValueAsString(patchBody))
                  .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isNoContent())
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(
              jsonPath("$.disposition").value(DispositionOfSuccess.SUCCESS.nameAsLowercase()))
          .andExpect(jsonPath("$.method").value(HttpMethod.PATCH.name().toLowerCase()))
          .andExpect(jsonPath("$.message").value("not updated"))
          .andExpect(jsonPath("$.instance").value(instance))
          .andExpect(jsonPath("$.size").value(1))
          .andExpect(jsonPath("$.content.name").value(apiResponseContent.name()));
    }

    @Test
    void listItemIsNotFound() throws Exception {
      String instance = "/lists/" + id1 + "/items/" + id2;
      when(mockLrmListApiService.patchListItem(eq(id1), eq(id2), anyString(), any()))
          .thenThrow(new ListItemNotFoundException());
      Map<String, Object> patchBody = Map.of("name", createLrmItem(id0).name());

      mockMvc
          .perform(
              patch(instance)
                  .with(jwt())
                  .with(csrf())
                  .content(writeValueAsString(patchBody))
                  .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isNotFound())
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(
              jsonPath("$.disposition").value(DispositionOfProblem.FAILURE.nameAsLowercase()))
          .andExpect(jsonPath("$.method").value(HttpMethod.PATCH.name().toLowerCase()))
          .andExpect(jsonPath("$.message").value("ListItem could not be found."))
          .andExpect(jsonPath("$.instance").value(instance))
          .andExpect(jsonPath("$.size").value(1));
    }
  }

  @Nested
  class ListItemMove {

    @Test
    void listItemIsMovedFromOneListToAnother() throws Exception {
      String instance = "/lists/" + id1 + "/items/" + id2 + "/" + id3;
      LrmListItemMovedResponse apiResponseContent = new LrmListItemMovedResponse("", "", "");
      when(mockLrmListApiService.moveListItem(eq(id1), eq(id2), eq(id3), anyString()))
          .thenReturn(new ApiServiceResponse<>(apiResponseContent, apiResponseMessage));

      mockMvc
          .perform(patch(instance).with(jwt()).with(csrf()).contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(
              jsonPath("$.disposition").value(DispositionOfSuccess.SUCCESS.nameAsLowercase()))
          .andExpect(jsonPath("$.method").value(HttpMethod.PATCH.name().toLowerCase()))
          .andExpect(jsonPath("$.message").value(apiResponseMessage))
          .andExpect(jsonPath("$.instance").value(instance))
          .andExpect(jsonPath("$.size").value(1))
          .andExpect(jsonPath("$.content.message").value(apiResponseMessage));
    }
  }

  @Nested
  class ListItemsCount {

    @Test
    void countOfItemAssociationsIsReturned() throws Exception {
      String instance = "/lists/" + id1 + "/items/count";
      when(mockLrmListApiService.countListItems(eq(id1), anyString()))
          .thenReturn(new ApiServiceResponse<>(new ApiMessageNumeric(999), apiResponseMessage));

      mockMvc
          .perform(get(instance).with(jwt()).contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(
              jsonPath("$.disposition").value(DispositionOfSuccess.SUCCESS.nameAsLowercase()))
          .andExpect(jsonPath("$.method").value(HttpMethod.GET.name().toLowerCase()))
          .andExpect(jsonPath("$.message").value(apiResponseMessage))
          .andExpect(jsonPath("$.instance").value(instance))
          .andExpect(jsonPath("$.size").value(1))
          .andExpect(jsonPath("$.content.length()").value(1))
          .andExpect(jsonPath("$.content.value").value(999));
    }

    @Test
    void itemIsNotFound() throws Exception {
      String instance = "/lists/" + id1 + "/items/count";
      when(mockLrmListApiService.countListItems(eq(id1), anyString()))
          .thenThrow(DomainException.builder().httpStatus(HttpStatus.NOT_FOUND).build());

      mockMvc
          .perform(get(instance).with(jwt()).contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isNotFound())
          .andExpect(
              jsonPath("$.disposition").value(DispositionOfProblem.FAILURE.nameAsLowercase()))
          .andExpect(jsonPath("$.method").value(HttpMethod.GET.name().toLowerCase()))
          .andExpect(jsonPath("$.instance").value(instance))
          .andExpect(jsonPath("$.size").value(1))
          .andExpect(jsonPath("$.content.status").value(HttpStatus.NOT_FOUND.value()));
    }
  }

  @Nested
  class ListItemsEligible {

    @Test
    void itemsEligibleForListAreReturned() throws Exception {
      UUID listId = UUID.fromString("00000000-0000-4000-a000-000000000010");
      List<LrmItemResponse> serviceResponse = List.of(LrmItemResponse.fromLrmItem(lrmItem()));
      ApiServiceResponse<List<LrmItemResponse>> mockApiServiceResponse =
          new ApiServiceResponse<>(serviceResponse, "message is irrelevant");
      when(mockLrmItemApiService.findByOwnerAndHavingNoListAssociations(anyString(), eq(listId)))
          .thenReturn(mockApiServiceResponse);
      String instance = "/lists/" + listId + "/items/eligible";

      mockMvc
          .perform(get(instance).with(jwt()).contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(
              jsonPath("$.disposition").value(DispositionOfSuccess.SUCCESS.nameAsLowercase()))
          .andExpect(jsonPath("$.method").value(HttpMethod.GET.name().toLowerCase()))
          .andExpect(jsonPath("$.message").value("message is irrelevant"))
          .andExpect(jsonPath("$.instance").value(instance))
          .andExpect(jsonPath("$.size").value(serviceResponse.size()))
          .andExpect(jsonPath("$.content").exists())
          .andExpect(jsonPath("$.content").isArray())
          .andExpect(jsonPath("$.content.[0].name").value(serviceResponse.get(0).name()))
          .andExpect(
              jsonPath("$.content.[0].description").value(serviceResponse.get(0).description()));

      verify(mockLrmItemApiService, times(1))
          .findByOwnerAndHavingNoListAssociations(anyString(), eq(listId));
    }
  }
}
