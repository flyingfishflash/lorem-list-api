package net.flyingfishflash.loremlist.unit.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import kotlin.Pair;
import kotlin.Triple;
import kotlinx.datetime.Clock;
import kotlinx.datetime.Instant;
import net.flyingfishflash.loremlist.api.LrmListApiServiceDefault;
import net.flyingfishflash.loremlist.api.data.request.LrmItemCreateRequest;
import net.flyingfishflash.loremlist.api.data.request.LrmListCreateRequest;
import net.flyingfishflash.loremlist.api.data.response.ApiServiceResponse;
import net.flyingfishflash.loremlist.api.data.response.AssociationsDeletedResponse;
import net.flyingfishflash.loremlist.api.data.response.LrmListDeletedResponse;
import net.flyingfishflash.loremlist.api.data.response.LrmListResponse;
import net.flyingfishflash.loremlist.core.response.structure.ApiMessageNumeric;
import net.flyingfishflash.loremlist.domain.ServiceResponse;
import net.flyingfishflash.loremlist.domain.lrmitem.LrmItemSuccinct;
import net.flyingfishflash.loremlist.domain.lrmitem.data.LrmItemCreate;
import net.flyingfishflash.loremlist.domain.lrmlist.LrmList;
import net.flyingfishflash.loremlist.domain.lrmlist.LrmListService;
import net.flyingfishflash.loremlist.domain.lrmlist.data.LrmListCreate;
import net.flyingfishflash.loremlist.domain.lrmlist.data.LrmListDeleted;
import net.flyingfishflash.loremlist.domain.lrmlistitem.LrmListItem;
import net.flyingfishflash.loremlist.domain.lrmlistitem.LrmListItemService;
import net.flyingfishflash.loremlist.domain.lrmlistitem.data.LrmListItemAdded;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class LrmListApiServiceDefaultTests {

  private final LrmListService mockLrmListService = mock(LrmListService.class);
  private final LrmListItemService mockLrmListItemService = mock(LrmListItemService.class);
  private final LrmList mockLrmList = mock(LrmList.class);
  private final LrmListItem mockLrmListItem = mock(LrmListItem.class);
  private final LrmListApiServiceDefault lrmListApiService =
      new LrmListApiServiceDefault(mockLrmListService, mockLrmListItemService);

  private final UUID id0 = UUID.fromString("00000000-0000-4000-a000-000000000000");
  private final UUID id1 = UUID.fromString("00000000-0000-4000-a000-000000000001");
  private final String serviceResponseMessage =
      "ksADs8y96KRa1Zo4ipMdr5t8faudmFj4c564S02MjsNG6TXEO7yctC08Bb53bCB7";
  private final String owner = "owner";
  private final Instant now = Clock.System.INSTANCE.now();

  private LrmList createLrmList(UUID id, String nameSuffix, Set<LrmListItem> items) {
    String name = "Lorem List Name" + (nameSuffix.isEmpty() ? "" : " (" + nameSuffix + ")");
    return new LrmList(
        id,
        name,
        "Lorem List Description",
        true,
        "Lorem Ipsum Owner",
        now,
        "Lorem Ipsum Created By",
        now,
        "Lorem Ipsum Updated By",
        items);
  }

  private LrmList createLrmList(UUID id) {
    return createLrmList(id, "", Set.of());
  }

  /** Map.of(...) has no guaranteed iteration order; some patch tests assert on field order. */
  private static Map<String, Object> orderedMap(Object... keyValuePairs) {
    Map<String, Object> map = new java.util.LinkedHashMap<>();
    for (int i = 0; i < keyValuePairs.length; i += 2) {
      map.put((String) keyValuePairs[i], keyValuePairs[i + 1]);
    }
    return map;
  }

  @Nested
  class ListOperations {

    @Test
    void countListsByOwner() {
      ServiceResponse<Long> serviceResponse = new ServiceResponse<>(10L, serviceResponseMessage);
      ApiServiceResponse<ApiMessageNumeric> apiServiceResponse =
          new ApiServiceResponse<>(
              new ApiMessageNumeric(serviceResponse.getContent()), serviceResponse.getMessage());
      when(mockLrmListService.countByOwner(owner)).thenReturn(serviceResponse);
      assertThat(lrmListApiService.countByOwner(owner)).isEqualTo(apiServiceResponse);
    }

    @Test
    void createANewList() {
      ServiceResponse<LrmList> serviceResponse =
          new ServiceResponse<>(mockLrmList, serviceResponseMessage);
      ApiServiceResponse<LrmListResponse> apiServiceResponse =
          new ApiServiceResponse<>(
              LrmListResponse.fromLrmList(serviceResponse.getContent()),
              serviceResponse.getMessage());
      when(mockLrmListService.create(any(LrmListCreate.class), eq(owner)))
          .thenReturn(serviceResponse);
      LrmListCreateRequest request =
          new LrmListCreateRequest("Lorem List Name", "Lorem List Description", true);
      assertThat(lrmListApiService.create(request, owner)).isEqualTo(apiServiceResponse);
    }

    @Test
    void deleteListsByOwner() {
      LrmListDeleted mockDeleted = mock(LrmListDeleted.class);
      ServiceResponse<LrmListDeleted> serviceResponse =
          new ServiceResponse<>(mockDeleted, serviceResponseMessage);
      LrmListDeletedResponse listDeletedResponse =
          new LrmListDeletedResponse(mockDeleted.listNames(), mockDeleted.associatedItemNames());
      ApiServiceResponse<LrmListDeletedResponse> apiServiceResponse =
          new ApiServiceResponse<>(listDeletedResponse, serviceResponse.getMessage());
      when(mockLrmListService.deleteByOwner(owner)).thenReturn(serviceResponse);
      assertThat(lrmListApiService.deleteByOwner(owner)).isEqualTo(apiServiceResponse);
    }

    @Test
    void deleteListByOwnerAndId() {
      LrmListDeleted mockDeleted = mock(LrmListDeleted.class);
      ServiceResponse<LrmListDeleted> serviceResponse =
          new ServiceResponse<>(mockDeleted, serviceResponseMessage);
      LrmListDeletedResponse listDeletedResponse =
          new LrmListDeletedResponse(mockDeleted.listNames(), mockDeleted.associatedItemNames());
      ApiServiceResponse<LrmListDeletedResponse> apiServiceResponse =
          new ApiServiceResponse<>(listDeletedResponse, serviceResponse.getMessage());
      when(mockLrmListService.deleteByOwnerAndId(id0, owner, false)).thenReturn(serviceResponse);
      assertThat(lrmListApiService.deleteByOwnerAndId(id0, owner, false))
          .isEqualTo(apiServiceResponse);
    }

    @Test
    void findListsByOwner() {
      ServiceResponse<List<LrmList>> serviceResponse =
          new ServiceResponse<>(List.of(mockLrmList), serviceResponseMessage);
      List<LrmListResponse> apiServiceResponseContent =
          serviceResponse.getContent().stream().map(LrmListResponse::fromLrmList).toList();
      ApiServiceResponse<List<LrmListResponse>> apiServiceResponse =
          new ApiServiceResponse<>(apiServiceResponseContent, serviceResponse.getMessage());
      when(mockLrmListService.findByOwner(owner)).thenReturn(serviceResponse);
      assertThat(lrmListApiService.findByOwner(owner)).isEqualTo(apiServiceResponse);
    }

    @Test
    void findListsByOwnerAndExcludeItems() {
      LrmList listWithItems = createLrmList(id0, "", Set.of(mockLrmListItem));
      ServiceResponse<List<LrmList>> serviceResponse =
          new ServiceResponse<>(List.of(listWithItems), serviceResponseMessage);
      assertThat(serviceResponse.getContent()).hasSize(1);
      assertThat(serviceResponse.getContent().get(0).items()).isNotEmpty();
      List<LrmListResponse> apiServiceResponseContent =
          serviceResponse.getContent().stream()
              .map(list -> LrmListResponse.fromLrmList(list.withItems(Set.of())))
              .toList();
      ApiServiceResponse<List<LrmListResponse>> apiServiceResponse =
          new ApiServiceResponse<>(apiServiceResponseContent, serviceResponse.getMessage());
      when(mockLrmListService.findByOwner(owner)).thenReturn(serviceResponse);
      assertThat(lrmListApiService.findByOwnerExcludeItems(owner)).isEqualTo(apiServiceResponse);
    }

    @Test
    void findListsByOwnerAndHavingNoItemAssociations() {
      ServiceResponse<List<LrmList>> serviceResponse =
          new ServiceResponse<>(List.of(mockLrmList), serviceResponseMessage);
      List<LrmListResponse> apiServiceResponseContent =
          serviceResponse.getContent().stream().map(LrmListResponse::fromLrmList).toList();
      ApiServiceResponse<List<LrmListResponse>> apiServiceResponse =
          new ApiServiceResponse<>(apiServiceResponseContent, serviceResponse.getMessage());
      when(mockLrmListService.findByOwnerAndHavingNoItemAssociations(owner))
          .thenReturn(serviceResponse);
      assertThat(lrmListApiService.findByOwnerAndHavingNoItemAssociations(owner))
          .isEqualTo(apiServiceResponse);
    }

    @Test
    void findListByOwnerAndId() {
      ServiceResponse<LrmList> serviceResponse =
          new ServiceResponse<>(mockLrmList, serviceResponseMessage);
      LrmListResponse apiServiceResponseContent =
          LrmListResponse.fromLrmList(serviceResponse.getContent());
      ApiServiceResponse<LrmListResponse> apiServiceResponse =
          new ApiServiceResponse<>(apiServiceResponseContent, serviceResponse.getMessage());
      when(mockLrmListService.findByOwnerAndId(id0, owner)).thenReturn(serviceResponse);
      assertThat(lrmListApiService.findByOwnerAndId(id0, owner)).isEqualTo(apiServiceResponse);
    }

    @Test
    void findListByOwnerAndIdAndExcludeItems() {
      LrmList serviceResponseContent = createLrmList(id0, "", Set.of(mockLrmListItem));
      ServiceResponse<LrmList> serviceResponse =
          new ServiceResponse<>(serviceResponseContent, serviceResponseMessage);
      LrmListResponse apiServiceResponseContent =
          LrmListResponse.fromLrmList(serviceResponseContent.withItems(Set.of()));
      ApiServiceResponse<LrmListResponse> apiServiceResponse =
          new ApiServiceResponse<>(apiServiceResponseContent, serviceResponse.getMessage());
      when(mockLrmListService.findByOwnerAndId(id0, owner)).thenReturn(serviceResponse);
      assertThat(lrmListApiService.findByOwnerAndIdExcludeItems(id0, owner))
          .isEqualTo(apiServiceResponse);
    }

    @Test
    void findListsByPublicIndicator() {
      ServiceResponse<List<LrmList>> serviceResponse =
          new ServiceResponse<>(List.of(mockLrmList), serviceResponseMessage);
      List<LrmListResponse> apiServiceResponseContent =
          serviceResponse.getContent().stream().map(LrmListResponse::fromLrmList).toList();
      ApiServiceResponse<List<LrmListResponse>> apiServiceResponse =
          new ApiServiceResponse<>(apiServiceResponseContent, serviceResponse.getMessage());
      when(mockLrmListService.findByPublic()).thenReturn(serviceResponse);
      assertThat(lrmListApiService.findByPublic()).isEqualTo(apiServiceResponse);
    }

    @Test
    void findListsByPublicIndicatorAndExcludeItems() {
      LrmList listWithItems = createLrmList(id0, "", Set.of(mockLrmListItem));
      ServiceResponse<List<LrmList>> serviceResponse =
          new ServiceResponse<>(List.of(listWithItems), serviceResponseMessage);
      List<LrmListResponse> apiServiceResponseContent =
          serviceResponse.getContent().stream()
              .map(list -> LrmListResponse.fromLrmList(list.withItems(Set.of())))
              .toList();
      ApiServiceResponse<List<LrmListResponse>> apiServiceResponse =
          new ApiServiceResponse<>(apiServiceResponseContent, serviceResponse.getMessage());
      when(mockLrmListService.findByPublic()).thenReturn(serviceResponse);
      assertThat(lrmListApiService.findByPublicExcludeItems()).isEqualTo(apiServiceResponse);
    }

    @Nested
    class PatchListByIdAndOwner {

      @Test
      void listIsUpdated() {
        Map<String, Object> patchRequest =
            Map.of("name", "Updated Name", "description", "Updated Description", "public", true);
        LrmList updatedList =
            createLrmList(id0)
                .withName("Updated Name")
                .withDescription("Updated Description")
                .withIsPublic(true);
        when(mockLrmListService.findByOwnerAndId(id0, owner))
            .thenReturn(new ServiceResponse<>(mockLrmList, serviceResponseMessage))
            .thenReturn(new ServiceResponse<>(updatedList, serviceResponseMessage));
        var result = lrmListApiService.patchByOwnerAndId(id0, owner, patchRequest);
        assertThat(result.getContent()).isEqualTo(LrmListResponse.fromLrmList(updatedList));
      }

      @Test
      void listIsNotUpdatedWhenPatchRequestContainsUpToDateValues() {
        Map<String, Object> patchRequest =
            Map.of("name", "Updated Name", "description", "Updated Description", "public", true);
        ServiceResponse<LrmList> serviceResponse =
            new ServiceResponse<>(mockLrmList, serviceResponseMessage);
        when(mockLrmListService.findByOwnerAndId(id0, owner)).thenReturn(serviceResponse);
        var result = lrmListApiService.patchByOwnerAndId(id0, owner, patchRequest);
        assertThat(result.getContent()).isEqualTo(LrmListResponse.fromLrmList(mockLrmList));
        assertThat(result.getMessage()).contains("not updated");
      }

      @Test
      void throwExceptionForUnsupportedPatchField() {
        Map<String, Object> patchRequest = Map.of("unsupportedField", "value");
        when(mockLrmListService.findByOwnerAndId(id0, owner))
            .thenReturn(new ServiceResponse<>(mockLrmList, serviceResponseMessage));
        assertThatThrownBy(() -> lrmListApiService.patchByOwnerAndId(id0, owner, patchRequest))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Patch operation is not supported on field: unsupportedField");
      }
    }
  }

  @Nested
  class ListItemOperations {

    @Test
    void countListItemsByListIdAndOwner() {
      when(mockLrmListItemService.countByOwnerAndListId(id0, owner))
          .thenReturn(new ServiceResponse<>(99L, serviceResponseMessage));
      lrmListApiService.countListItems(id0, owner);
    }

    @Test
    void addListItem() {
      LrmListItemAdded serviceResponseContent =
          new LrmListItemAdded("Lorem Ipsum", List.of(new LrmItemSuccinct(id0, "Lorem Item")));
      ServiceResponse<LrmListItemAdded> serviceResponse =
          new ServiceResponse<>(serviceResponseContent, serviceResponseMessage);
      when(mockLrmListItemService.add(id0, List.of(id0), owner)).thenReturn(serviceResponse);
      lrmListApiService.addListItem(id0, Set.of(id0), owner);
    }

    @Test
    void createListItem() {
      LrmItemCreateRequest itemCreateRequest =
          new LrmItemCreateRequest("Lorem Item Name", "Lorem Item Description", 0, false);
      ServiceResponse<LrmListItem> serviceResponse =
          new ServiceResponse<>(mockLrmListItem, serviceResponseMessage);
      when(mockLrmListItemService.create(eq(id0), any(LrmItemCreate.class), eq(owner)))
          .thenReturn(serviceResponse);
      lrmListApiService.createListItem(id0, itemCreateRequest, owner);
    }

    @Test
    void removeListItemByItemIdAndListId() {
      when(mockLrmListItemService.removeByOwnerAndListIdAndItemId(id1, id0, owner))
          .thenReturn(new ServiceResponse<>(new Pair<>("Lorem", "Ipsum"), serviceResponseMessage));
      lrmListApiService.removeListItem(id1, id0, owner);
    }

    @Test
    void removeAllListItems() {
      ServiceResponse<Pair<String, Integer>> serviceResponse =
          new ServiceResponse<>(new Pair<>("", 9), serviceResponseMessage);
      AssociationsDeletedResponse apiResponseContent =
          new AssociationsDeletedResponse(
              serviceResponse.getContent().getFirst(), serviceResponse.getContent().getSecond());
      ApiServiceResponse<AssociationsDeletedResponse> apiServiceResponse =
          new ApiServiceResponse<>(apiResponseContent, serviceResponse.getMessage());
      when(mockLrmListItemService.removeByOwnerAndListId(id0, owner)).thenReturn(serviceResponse);
      assertThat(lrmListApiService.removeAllListItems(id0, owner)).isEqualTo(apiServiceResponse);
    }

    @Test
    void moveListItem() {
      UUID listId2 = UUID.fromString("00000000-0000-4000-a000-000000000002");
      ServiceResponse<Triple<String, String, String>> serviceResponse =
          new ServiceResponse<>(new Triple<>("Item 1", "List A", "List B"), serviceResponseMessage);
      when(mockLrmListItemService.move(id0, id1, listId2, owner)).thenReturn(serviceResponse);
      var result = lrmListApiService.moveListItem(id1, id0, listId2, owner);
      assertThat(result.getContent().itemName()).isEqualTo(serviceResponse.getContent().getFirst());
      assertThat(result.getContent().currentListName())
          .isEqualTo(serviceResponse.getContent().getSecond());
      assertThat(result.getContent().newListName())
          .isEqualTo(serviceResponse.getContent().getThird());
      assertThat(result.getMessage()).isEqualTo(serviceResponse.getMessage());
    }

    @Nested
    class PatchListItemByListIdItemIdAndOwner {

      @Test
      void listItemIsUpdated() {
        LrmListItem mockPatchedLrmListItem = mock(LrmListItem.class);
        Map<String, Object> patchRequest =
            orderedMap(
                "name",
                "Updated Name",
                "description",
                "Updated Description",
                "quantity",
                7,
                "isSuppressed",
                true);
        when(mockLrmListItem.name()).thenReturn("Item Name");
        when(mockPatchedLrmListItem.name()).thenReturn("Updated Item Name");
        when(mockLrmListItemService.findByOwnerAndItemIdAndListId(id0, id1, owner))
            .thenReturn(new ServiceResponse<>(mockLrmListItem, serviceResponseMessage))
            .thenReturn(new ServiceResponse<>(mockPatchedLrmListItem, serviceResponseMessage));
        var response = lrmListApiService.patchListItem(id1, id0, owner, patchRequest);
        assertThat(response.getMessage())
            .isEqualTo(
                "Item 'Updated Item Name' updated. Fields changed: name, description, quantity, isSuppressed.");
      }

      @Test
      void listItemIsNotUpdatedWhenPatchRequestIsEmpty() {
        when(mockLrmListItem.name()).thenReturn("Item Name");
        when(mockLrmListItemService.findByOwnerAndItemIdAndListId(id0, id1, owner))
            .thenReturn(new ServiceResponse<>(mockLrmListItem, serviceResponseMessage));
        var response = lrmListApiService.patchListItem(id1, id0, owner, Map.of());
        assertThat(response.getMessage()).isEqualTo("Item 'Item Name' not updated.");
      }

      @Test
      void throwExceptionForUnsupportedPatchField() {
        Map<String, Object> patchRequest = Map.of("unsupportedField", "value");
        ServiceResponse<LrmListItem> serviceResponse =
            new ServiceResponse<>(mockLrmListItem, serviceResponseMessage);
        when(mockLrmListItemService.findByOwnerAndItemIdAndListId(id0, id1, owner))
            .thenReturn(serviceResponse);
        assertThatThrownBy(() -> lrmListApiService.patchListItem(id1, id0, owner, patchRequest))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Patch operation is not supported on field: unsupportedField");
      }
    }
  }
}
