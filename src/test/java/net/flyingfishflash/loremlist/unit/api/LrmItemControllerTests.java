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
import net.flyingfishflash.loremlist.api.LrmItemController;
import net.flyingfishflash.loremlist.api.data.request.LrmItemCreateRequest;
import net.flyingfishflash.loremlist.api.data.response.ApiServiceResponse;
import net.flyingfishflash.loremlist.api.data.response.LrmItemDeletedResponse;
import net.flyingfishflash.loremlist.api.data.response.LrmItemResponse;
import net.flyingfishflash.loremlist.core.configuration.SerializationConfig;
import net.flyingfishflash.loremlist.core.response.structure.ApiMessageNumeric;
import net.flyingfishflash.loremlist.core.response.structure.DispositionOfProblem;
import net.flyingfishflash.loremlist.core.response.structure.DispositionOfSuccess;
import net.flyingfishflash.loremlist.domain.exceptions.DomainException;
import net.flyingfishflash.loremlist.domain.lrmitem.ItemNotFoundException;
import net.flyingfishflash.loremlist.domain.lrmitem.LrmItem;
import net.flyingfishflash.loremlist.domain.lrmlist.ListNotFoundException;
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

/** LrmItemController Unit Tests */
@WebMvcTest(controllers = LrmItemController.class)
@Import(SerializationConfig.class)
class LrmItemControllerTests {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private LrmItemApiService mockLrmItemApiService;

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private final Instant now = Clock.System.INSTANCE.now();
  private final UUID id1 = UUID.fromString("00000000-0000-4000-a000-000000000001");
  private final LrmItemCreateRequest lrmItemCreateRequest =
      new LrmItemCreateRequest("Lorem Item Name", "Lorem Item Description", 0, false);

  private LrmItem lrmItem() {
    return new LrmItem(
        UUID.fromString("00000000-0000-4000-a000-000000000000"),
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
  class Items {

    @Test
    void itemsAreDeleted() throws Exception {
      LrmItemDeletedResponse serviceResponse =
          new LrmItemDeletedResponse(
              List.of("Deleted Lorem Item Name"), List.of("Associated Lorem List Name"));
      String message =
          "Deleted all ("
              + serviceResponse.itemNames().size()
              + ") of your items from "
              + serviceResponse.associatedListNames().size()
              + " lists.";
      ApiServiceResponse<LrmItemDeletedResponse> mockReturn =
          new ApiServiceResponse<>(serviceResponse, message);
      when(mockLrmItemApiService.deleteByOwner(anyString())).thenReturn(mockReturn);
      String instance = "/items";

      mockMvc
          .perform(
              delete(instance).with(jwt()).with(csrf()).contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(
              jsonPath("$.disposition").value(DispositionOfSuccess.SUCCESS.nameAsLowercase()))
          .andExpect(jsonPath("$.method").value(HttpMethod.DELETE.name().toLowerCase()))
          .andExpect(jsonPath("$.message").value(message))
          .andExpect(jsonPath("$.instance").value(instance))
          .andExpect(jsonPath("$.size").value(1))
          .andExpect(
              jsonPath("$.content.itemNames[0]").value(mockReturn.getContent().itemNames().get(0)))
          .andExpect(
              jsonPath("$.content.associatedListNames[0]")
                  .value(mockReturn.getContent().associatedListNames().get(0)));
    }

    @Test
    void itemsAreFound() throws Exception {
      List<LrmItemResponse> mockServiceResponse = List.of(LrmItemResponse.fromLrmItem(lrmItem()));
      ApiServiceResponse<List<LrmItemResponse>> mockApiServiceResponse =
          new ApiServiceResponse<>(mockServiceResponse, "message is irrelevant");
      when(mockLrmItemApiService.findByOwner(anyString())).thenReturn(mockApiServiceResponse);
      String instance = "/items";

      mockMvc
          .perform(get(instance).with(jwt()).contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(
              jsonPath("$.disposition").value(DispositionOfSuccess.SUCCESS.nameAsLowercase()))
          .andExpect(jsonPath("$.method").value(HttpMethod.GET.name().toLowerCase()))
          .andExpect(jsonPath("$.message").value("message is irrelevant"))
          .andExpect(jsonPath("$.instance").value(instance))
          .andExpect(jsonPath("$.size").value(mockServiceResponse.size()))
          .andExpect(jsonPath("$.content").exists())
          .andExpect(jsonPath("$.content").isArray())
          .andExpect(jsonPath("$.content.[0].name").value(mockServiceResponse.get(0).name()))
          .andExpect(
              jsonPath("$.content.[0].description").value(mockServiceResponse.get(0).description()))
          .andExpect(jsonPath("$.content.[0].items").doesNotExist());

      verify(mockLrmItemApiService, times(1)).findByOwner(anyString());
    }
  }

  @Nested
  class ItemById {

    @Test
    void itemIsDeleted() throws Exception {
      LrmItemDeletedResponse mockServiceResponse =
          new LrmItemDeletedResponse(List.of("dolor sit amet"), List.of("Lorem Ipsum"));
      ApiServiceResponse<LrmItemDeletedResponse> mockApiServiceResponse =
          new ApiServiceResponse<>(mockServiceResponse, "message is irrelevant");
      when(mockLrmItemApiService.deleteByOwnerAndId(eq(id1), anyString(), eq(false)))
          .thenReturn(mockApiServiceResponse);
      String instance = "/items/" + id1;

      mockMvc
          .perform(delete(instance).with(jwt()).with(csrf()))
          .andExpect(status().isOk())
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(
              jsonPath("$.disposition").value(DispositionOfSuccess.SUCCESS.nameAsLowercase()))
          .andExpect(jsonPath("$.method").value(HttpMethod.DELETE.name().toLowerCase()))
          .andExpect(jsonPath("$.message").value(mockApiServiceResponse.getMessage()))
          .andExpect(jsonPath("$.instance").value(instance))
          .andExpect(jsonPath("$.size").value(1))
          .andExpect(jsonPath("$.content.itemNames.length()").value(1))
          .andExpect(
              jsonPath("$.content.itemNames.[0]")
                  .value(mockApiServiceResponse.getContent().itemNames().get(0)))
          .andExpect(
              jsonPath("$.content.associatedListNames.[0]")
                  .value(mockApiServiceResponse.getContent().associatedListNames().get(0)));

      verify(mockLrmItemApiService, times(1))
          .deleteByOwnerAndId(any(UUID.class), anyString(), anyBoolean());
    }

    @Test
    void itemIsNotFoundOnDelete() throws Exception {
      when(mockLrmItemApiService.deleteByOwnerAndId(eq(id1), anyString(), eq(false)))
          .thenThrow(new ItemNotFoundException(id1));
      String instance = "/items/" + id1;

      mockMvc
          .perform(delete(instance).with(jwt()).with(csrf()))
          .andExpect(status().isNotFound())
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(
              jsonPath("$.disposition").value(DispositionOfProblem.FAILURE.nameAsLowercase()))
          .andExpect(jsonPath("$.method").value(HttpMethod.DELETE.name().toLowerCase()))
          .andExpect(jsonPath("$.instance").value(instance))
          .andExpect(jsonPath("$.size").value(1))
          .andExpect(jsonPath("$.content.title").value(ItemNotFoundException.class.getSimpleName()))
          .andExpect(jsonPath("$.content.status").value(HttpStatus.NOT_FOUND.value()));

      verify(mockLrmItemApiService, times(1))
          .deleteByOwnerAndId(any(UUID.class), anyString(), anyBoolean());
    }

    @Test
    void itemIsFound() throws Exception {
      LrmItemResponse mockServiceResponse = LrmItemResponse.fromLrmItem(lrmItem());
      ApiServiceResponse<LrmItemResponse> mockApiServiceResponse =
          new ApiServiceResponse<>(mockServiceResponse, "message is irrelevant");
      when(mockLrmItemApiService.findByOwnerAndId(eq(id1), anyString()))
          .thenReturn(mockApiServiceResponse);
      String instance = "/items/" + id1;

      mockMvc
          .perform(get(instance).with(jwt()))
          .andExpect(status().isOk())
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(
              jsonPath("$.disposition").value(DispositionOfSuccess.SUCCESS.nameAsLowercase()))
          .andExpect(jsonPath("$.method").value(HttpMethod.GET.name().toLowerCase()))
          .andExpect(jsonPath("$.message").value("message is irrelevant"))
          .andExpect(jsonPath("$.instance").value(instance))
          .andExpect(jsonPath("$.size").value(1))
          .andExpect(jsonPath("$.content.description").value(lrmItem().description()))
          .andExpect(jsonPath("$.content.name").value(lrmItem().name()));

      verify(mockLrmItemApiService, times(1)).findByOwnerAndId(eq(id1), anyString());
    }

    @Test
    void itemIsNotFoundOnGet() throws Exception {
      when(mockLrmItemApiService.findByOwnerAndId(eq(id1), anyString()))
          .thenThrow(new ItemNotFoundException(id1));
      String instance = "/items/" + id1;

      mockMvc
          .perform(get(instance).with(jwt()))
          .andExpect(status().isNotFound())
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(
              jsonPath("$.disposition").value(DispositionOfProblem.FAILURE.nameAsLowercase()))
          .andExpect(jsonPath("$.method").value(HttpMethod.GET.name().toLowerCase()))
          .andExpect(jsonPath("$.instance").value(instance))
          .andExpect(jsonPath("$.size").value(1))
          .andExpect(jsonPath("$.content.title").value(ItemNotFoundException.class.getSimpleName()))
          .andExpect(jsonPath("$.content.status").value(HttpStatus.NOT_FOUND.value()));

      verify(mockLrmItemApiService, times(1)).findByOwnerAndId(any(UUID.class), anyString());
    }

    @Test
    void itemIsFoundAndUpdated() throws Exception {
      ApiServiceResponse<LrmItemResponse> mockApiServiceResponse =
          new ApiServiceResponse<>(LrmItemResponse.fromLrmItem(lrmItem()), "message is irrelevant");
      when(mockLrmItemApiService.patchByOwnerAndId(eq(id1), anyString(), any()))
          .thenReturn(mockApiServiceResponse);
      String instance = "/items/" + id1;
      Map<String, Object> patchBody = Map.of("name", lrmItem().name());

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
          .andExpect(jsonPath("$.message").value(mockApiServiceResponse.getMessage()))
          .andExpect(jsonPath("$.instance").value(instance))
          .andExpect(jsonPath("$.size").value(1))
          .andExpect(
              jsonPath("$.content.description")
                  .value(mockApiServiceResponse.getContent().description()))
          .andExpect(jsonPath("$.content.name").value(mockApiServiceResponse.getContent().name()));

      verify(mockLrmItemApiService, times(1))
          .patchByOwnerAndId(any(UUID.class), anyString(), eq(patchBody));
    }

    @Test
    void itemIsNotFoundOnPatch() throws Exception {
      when(mockLrmItemApiService.patchByOwnerAndId(eq(id1), anyString(), any()))
          .thenThrow(new ListNotFoundException(id1));
      String instance = "/items/" + id1;
      Map<String, Object> patchBody = Map.of("name", lrmItem().name());

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

      verify(mockLrmItemApiService, times(1))
          .patchByOwnerAndId(any(UUID.class), anyString(), eq(patchBody));
    }
  }

  @Nested
  class ItemByIdListsCount {

    @Test
    void countOfListAssociationsIsReturned() throws Exception {
      ApiServiceResponse<ApiMessageNumeric> mockApiServiceResponse =
          new ApiServiceResponse<>(new ApiMessageNumeric(999), "message is irrelevant");
      when(mockLrmItemApiService.countListAssociationsByItemIdAndItemOwner(eq(id1), anyString()))
          .thenReturn(mockApiServiceResponse);
      String instance = "/items/" + id1 + "/lists/count";

      mockMvc
          .perform(get(instance).with(jwt()))
          .andExpect(status().isOk())
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(
              jsonPath("$.disposition").value(DispositionOfSuccess.SUCCESS.nameAsLowercase()))
          .andExpect(jsonPath("$.method").value(HttpMethod.GET.name().toLowerCase()))
          .andExpect(jsonPath("$.message").value(mockApiServiceResponse.getMessage()))
          .andExpect(jsonPath("$.instance").value(instance))
          .andExpect(jsonPath("$.size").value(1))
          .andExpect(jsonPath("$.content.length()").value(1))
          .andExpect(
              jsonPath("$.content.value").value(mockApiServiceResponse.getContent().value()));

      verify(mockLrmItemApiService, times(1))
          .countListAssociationsByItemIdAndItemOwner(any(UUID.class), anyString());
    }

    @Test
    void itemIsNotFound() throws Exception {
      when(mockLrmItemApiService.countListAssociationsByItemIdAndItemOwner(eq(id1), anyString()))
          .thenThrow(DomainException.builder().httpStatus(HttpStatus.NOT_FOUND).build());
      String instance = "/items/" + id1 + "/lists/count";

      mockMvc
          .perform(get(instance).with(jwt()))
          .andExpect(status().isNotFound())
          .andExpect(
              jsonPath("$.disposition").value(DispositionOfProblem.FAILURE.nameAsLowercase()))
          .andExpect(jsonPath("$.method").value(HttpMethod.GET.name().toLowerCase()))
          .andExpect(jsonPath("$.instance").value(instance))
          .andExpect(jsonPath("$.size").value(1))
          .andExpect(jsonPath("$.content.status").value(HttpStatus.NOT_FOUND.value()));

      verify(mockLrmItemApiService, times(1))
          .countListAssociationsByItemIdAndItemOwner(any(UUID.class), anyString());
    }
  }

  @Nested
  class ItemsCount {

    @Test
    void countOfItemsIsReturned() throws Exception {
      when(mockLrmItemApiService.countByOwner(anyString()))
          .thenReturn(new ApiServiceResponse<>(new ApiMessageNumeric(999L), "999 items."));
      String instance = "/items/count";

      mockMvc
          .perform(get(instance).with(jwt()))
          .andExpect(status().isOk())
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(
              jsonPath("$.disposition").value(DispositionOfSuccess.SUCCESS.nameAsLowercase()))
          .andExpect(jsonPath("$.method").value(HttpMethod.GET.name().toLowerCase()))
          .andExpect(jsonPath("$.message").value("999 items."))
          .andExpect(jsonPath("$.instance").value(instance))
          .andExpect(jsonPath("$.size").value(1))
          .andExpect(jsonPath("$.content.value").value(999));
    }
  }

  @Nested
  class ItemsWithNoLists {

    @Test
    void itemsWithNoListAssociationAreFound() throws Exception {
      ApiServiceResponse<List<LrmItemResponse>> mockApiServiceResponse =
          new ApiServiceResponse<>(
              List.of(LrmItemResponse.fromLrmItem(lrmItem())), "message is irrelevant");
      when(mockLrmItemApiService.findByOwnerAndHavingNoListAssociations(anyString()))
          .thenReturn(mockApiServiceResponse);
      String instance = "/items/with-no-lists";

      mockMvc
          .perform(get(instance).with(jwt()).contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(
              jsonPath("$.disposition").value(DispositionOfSuccess.SUCCESS.nameAsLowercase()))
          .andExpect(jsonPath("$.method").value(HttpMethod.GET.name().toLowerCase()))
          .andExpect(jsonPath("$.message").value(mockApiServiceResponse.getMessage()))
          .andExpect(jsonPath("$.instance").value(instance))
          .andExpect(jsonPath("$.size").value(mockApiServiceResponse.getContent().size()))
          .andExpect(jsonPath("$.content").exists())
          .andExpect(jsonPath("$.content").isArray())
          .andExpect(
              jsonPath("$.content.[0].name")
                  .value(mockApiServiceResponse.getContent().get(0).name()))
          .andExpect(
              jsonPath("$.content.[0].description")
                  .value(mockApiServiceResponse.getContent().get(0).description()));

      verify(mockLrmItemApiService, times(1)).findByOwnerAndHavingNoListAssociations(anyString());
    }
  }
}
