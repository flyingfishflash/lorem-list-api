package net.flyingfishflash.loremlist.unit.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import kotlinx.datetime.Clock;
import kotlinx.datetime.Instant;
import net.flyingfishflash.loremlist.api.LrmItemApiServiceDefault;
import net.flyingfishflash.loremlist.api.data.request.LrmItemCreateRequest;
import net.flyingfishflash.loremlist.api.data.response.ApiServiceResponse;
import net.flyingfishflash.loremlist.api.data.response.LrmItemDeletedResponse;
import net.flyingfishflash.loremlist.api.data.response.LrmItemResponse;
import net.flyingfishflash.loremlist.core.response.structure.ApiMessageNumeric;
import net.flyingfishflash.loremlist.domain.ServiceResponse;
import net.flyingfishflash.loremlist.domain.lrmitem.LrmItem;
import net.flyingfishflash.loremlist.domain.lrmitem.LrmItemService;
import net.flyingfishflash.loremlist.domain.lrmitem.data.LrmItemDeleted;
import net.flyingfishflash.loremlist.domain.lrmlistitem.LrmListItemService;
import org.junit.jupiter.api.Test;

class LrmItemApiServiceDefaultTests {

  private final LrmItemService mockLrmItemService = mock(LrmItemService.class);
  private final LrmListItemService mockLrmListItemService = mock(LrmListItemService.class);
  private final LrmItemApiServiceDefault lrmItemApiService =
      new LrmItemApiServiceDefault(mockLrmItemService, mockLrmListItemService);

  private final Instant now = Clock.System.INSTANCE.now();
  private final UUID id0 = UUID.fromString("00000000-0000-4000-a000-000000000000");
  private final LrmItemCreateRequest lrmItemCreateRequest =
      new LrmItemCreateRequest("Lorem Item Name", "Lorem Item Description", 0, false);
  private final String irrelevantMessage =
      "ksADs8y96KRa1Zo4ipMdr5t8faudmFj4c564S02MjsNG6TXEO7yctC08Bb53bCB7";

  private LrmItem lrmItem() {
    return new LrmItem(
        id0,
        lrmItemCreateRequest.getName(),
        lrmItemCreateRequest.getDescription(),
        "Lorem Ipsum Owner",
        now,
        "Lorem Ipsum Created By",
        now,
        "Lorem Ipsum Updated By",
        Set.of());
  }

  /** Map.of(...) has no guaranteed iteration order; the patch tests assert on field order. */
  private static Map<String, Object> orderedMap(Object... keyValuePairs) {
    Map<String, Object> map = new LinkedHashMap<>();
    for (int i = 0; i < keyValuePairs.length; i += 2) {
      map.put((String) keyValuePairs[i], keyValuePairs[i + 1]);
    }
    return map;
  }

  @Test
  void countItemsByOwner() {
    String owner = "test_owner";
    long expectedCount = 10L;
    ServiceResponse<Long> serviceResponse = new ServiceResponse<>(expectedCount, irrelevantMessage);
    ApiServiceResponse<ApiMessageNumeric> apiServiceResponse =
        new ApiServiceResponse<>(
            new ApiMessageNumeric(serviceResponse.getContent()), serviceResponse.getMessage());
    when(mockLrmItemService.countByOwner(owner)).thenReturn(serviceResponse);
    assertThat(lrmItemApiService.countByOwner(owner)).isEqualTo(apiServiceResponse);
    verify(mockLrmItemService).countByOwner(owner);
  }

  @Test
  void deleteItemsByOwner() {
    String owner = "test_owner";
    ServiceResponse<LrmItemDeleted> serviceResponse =
        new ServiceResponse<>(
            new LrmItemDeleted(List.of("test_item_name"), List.of("test_list_name")),
            irrelevantMessage);
    ApiServiceResponse<LrmItemDeletedResponse> apiServiceResponse =
        new ApiServiceResponse<>(
            new LrmItemDeletedResponse(
                serviceResponse.getContent().itemNames(),
                serviceResponse.getContent().associatedListNames()),
            serviceResponse.getMessage());
    when(mockLrmItemService.deleteByOwner(owner)).thenReturn(serviceResponse);
    assertThat(lrmItemApiService.deleteByOwner(owner)).isEqualTo(apiServiceResponse);
    verify(mockLrmItemService).deleteByOwner(owner);
  }

  @Test
  void deleteItemByOwnerAndId() {
    UUID id = UUID.randomUUID();
    String owner = "test_owner";
    String listName = "Lorem List Name";
    boolean removeListAssociations = true;
    ServiceResponse<LrmItemDeleted> serviceResponse =
        new ServiceResponse<>(
            new LrmItemDeleted(List.of(lrmItem().name()), List.of(listName)), irrelevantMessage);
    ApiServiceResponse<LrmItemDeletedResponse> apiServiceResponse =
        new ApiServiceResponse<>(
            new LrmItemDeletedResponse(
                serviceResponse.getContent().itemNames(),
                serviceResponse.getContent().associatedListNames()),
            serviceResponse.getMessage());
    when(mockLrmItemService.deleteByOwnerAndId(id, owner, removeListAssociations))
        .thenReturn(serviceResponse);
    assertThat(lrmItemApiService.deleteByOwnerAndId(id, owner, removeListAssociations))
        .isEqualTo(apiServiceResponse);
    verify(mockLrmItemService).deleteByOwnerAndId(id, owner, removeListAssociations);
  }

  @Test
  void findItemsByOwner() {
    String owner = "test_owner";
    ServiceResponse<List<LrmItem>> serviceResponse =
        new ServiceResponse<>(List.of(lrmItem()), irrelevantMessage);
    ApiServiceResponse<List<LrmItemResponse>> apiServiceResponse =
        new ApiServiceResponse<>(
            serviceResponse.getContent().stream().map(LrmItemResponse::fromLrmItem).toList(),
            serviceResponse.getMessage());
    when(mockLrmItemService.findByOwner(owner)).thenReturn(serviceResponse);
    assertThat(lrmItemApiService.findByOwner(owner)).isEqualTo(apiServiceResponse);
    verify(mockLrmItemService).findByOwner(owner);
  }

  @Test
  void findItemByOwnerAndId() {
    String owner = "test_owner";
    ServiceResponse<LrmItem> serviceResponse = new ServiceResponse<>(lrmItem(), irrelevantMessage);
    ApiServiceResponse<LrmItemResponse> apiServiceResponse =
        new ApiServiceResponse<>(
            LrmItemResponse.fromLrmItem(serviceResponse.getContent()),
            serviceResponse.getMessage());
    when(mockLrmItemService.findByOwnerAndId(id0, owner)).thenReturn(serviceResponse);
    assertThat(lrmItemApiService.findByOwnerAndId(id0, owner)).isEqualTo(apiServiceResponse);
    verify(mockLrmItemService).findByOwnerAndId(id0, owner);
  }

  @Test
  void findItemsByOwnerHavingNoListAssociations() {
    String owner = "test_owner";
    ServiceResponse<List<LrmItem>> serviceResponse =
        new ServiceResponse<>(List.of(lrmItem()), irrelevantMessage);
    ApiServiceResponse<List<LrmItemResponse>> apiServiceResponse =
        new ApiServiceResponse<>(
            serviceResponse.getContent().stream()
                .map(item -> LrmItemResponse.fromLrmItem(lrmItem()))
                .toList(),
            serviceResponse.getMessage());
    when(mockLrmItemService.findByOwnerAndHavingNoListAssociations(owner))
        .thenReturn(serviceResponse);
    assertThat(lrmItemApiService.findByOwnerAndHavingNoListAssociations(owner))
        .isEqualTo(apiServiceResponse);
    verify(mockLrmItemService).findByOwnerAndHavingNoListAssociations(owner);
  }

  @Test
  void patchAnItemByOwnerAndId() {
    String owner = "test_owner";
    String updatedItemName = "Updated Item Name";
    String updatedItemDescription = "Updated Item Description";
    Map<String, Object> patchRequest =
        orderedMap("name", updatedItemName, "description", updatedItemDescription);
    LrmItem originalLrmItem = lrmItem();
    LrmItem updatedLrmItem =
        lrmItem().withName(updatedItemName).withDescription(updatedItemDescription);
    when(mockLrmItemService.findByOwnerAndId(id0, owner))
        .thenReturn(new ServiceResponse<>(originalLrmItem, irrelevantMessage))
        .thenReturn(new ServiceResponse<>(updatedLrmItem, irrelevantMessage));

    var apiServiceResponse = lrmItemApiService.patchByOwnerAndId(id0, owner, patchRequest);
    assertThat(apiServiceResponse.getContent())
        .isEqualTo(LrmItemResponse.fromLrmItem(updatedLrmItem));
    assertThat(apiServiceResponse.getMessage())
        .isEqualTo("Item 'Updated Item Name' updated. Fields changed: name, description.");
    verify(mockLrmItemService).patchName(any());
    verify(mockLrmItemService).patchDescription(any());
    verify(mockLrmItemService, times(2)).findByOwnerAndId(id0, owner);
  }

  @Test
  void notPatchAnItemByOwnerAndId() {
    String owner = "test_owner";
    String updatedItemName = lrmItem().name();
    String updatedItemDescription = lrmItem().description();
    Map<String, Object> patchRequest =
        orderedMap("name", updatedItemName, "description", updatedItemDescription);
    LrmItem originalLrmItem = lrmItem();
    LrmItem updatedLrmItem = lrmItem();
    when(mockLrmItemService.findByOwnerAndId(id0, owner))
        .thenReturn(new ServiceResponse<>(originalLrmItem, irrelevantMessage))
        .thenReturn(new ServiceResponse<>(updatedLrmItem, irrelevantMessage));

    var apiServiceResponse = lrmItemApiService.patchByOwnerAndId(id0, owner, patchRequest);
    assertThat(apiServiceResponse.getContent())
        .isEqualTo(LrmItemResponse.fromLrmItem(updatedLrmItem));
    assertThat(apiServiceResponse.getMessage())
        .isEqualTo("Item '" + updatedLrmItem.name() + "' not updated.");
    verify(mockLrmItemService, times(0)).patchName(any());
    verify(mockLrmItemService, times(0)).patchDescription(any());
    verify(mockLrmItemService, times(2)).findByOwnerAndId(id0, owner);
  }

  @Test
  void throwExceptionForUnsupportedPatchField() {
    String owner = "test_owner";
    Map<String, Object> patchRequest = Map.of("unsupportedField", "value");
    when(mockLrmItemService.findByOwnerAndId(id0, owner))
        .thenReturn(new ServiceResponse<>(lrmItem(), irrelevantMessage));
    assertThatThrownBy(() -> lrmItemApiService.patchByOwnerAndId(id0, owner, patchRequest))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Patch operation is not supported on field: unsupportedField");
  }

  @Test
  void countListAssociationsByItemIdAndItemOwner() {
    long itemCount = 999L;
    ServiceResponse<Long> serviceResponse = new ServiceResponse<>(itemCount, irrelevantMessage);
    when(mockLrmListItemService.countByOwnerAndItemId(any(UUID.class), anyString()))
        .thenReturn(serviceResponse);
    var apiServiceResponse =
        lrmItemApiService.countListAssociationsByItemIdAndItemOwner(id0, "test_owner");
    assertThat(apiServiceResponse.getContent().value()).isEqualTo(itemCount);
    assertThat(apiServiceResponse.getMessage()).isEqualTo(irrelevantMessage);
  }
}
