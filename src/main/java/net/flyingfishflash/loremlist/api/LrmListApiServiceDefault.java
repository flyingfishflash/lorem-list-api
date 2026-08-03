package net.flyingfishflash.loremlist.api;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import kotlin.Pair;
import kotlin.Triple;
import net.flyingfishflash.loremlist.api.data.request.LrmItemCreateRequest;
import net.flyingfishflash.loremlist.api.data.request.LrmListCreateRequest;
import net.flyingfishflash.loremlist.api.data.response.ApiServiceResponse;
import net.flyingfishflash.loremlist.api.data.response.AssociationDeletedResponse;
import net.flyingfishflash.loremlist.api.data.response.AssociationsDeletedResponse;
import net.flyingfishflash.loremlist.api.data.response.LrmItemResponse;
import net.flyingfishflash.loremlist.api.data.response.LrmListDeletedResponse;
import net.flyingfishflash.loremlist.api.data.response.LrmListItemAddedResponse;
import net.flyingfishflash.loremlist.api.data.response.LrmListItemMovedResponse;
import net.flyingfishflash.loremlist.api.data.response.LrmListItemResponse;
import net.flyingfishflash.loremlist.api.data.response.LrmListResponse;
import net.flyingfishflash.loremlist.core.response.structure.ApiMessageNumeric;
import net.flyingfishflash.loremlist.domain.ServiceResponse;
import net.flyingfishflash.loremlist.domain.lrmitem.data.LrmItemCreate;
import net.flyingfishflash.loremlist.domain.lrmlist.LrmList;
import net.flyingfishflash.loremlist.domain.lrmlist.LrmListService;
import net.flyingfishflash.loremlist.domain.lrmlist.data.LrmListCreate;
import net.flyingfishflash.loremlist.domain.lrmlist.data.LrmListDeleted;
import net.flyingfishflash.loremlist.domain.lrmlistitem.LrmListItem;
import net.flyingfishflash.loremlist.domain.lrmlistitem.LrmListItemService;
import net.flyingfishflash.loremlist.domain.lrmlistitem.data.LrmListItemAdded;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class LrmListApiServiceDefault implements LrmListApiService {

  private final LrmListService lrmListService;
  private final LrmListItemService lrmListItemService;

  public LrmListApiServiceDefault(
      LrmListService lrmListService, LrmListItemService lrmListItemService) {
    this.lrmListService = lrmListService;
    this.lrmListItemService = lrmListItemService;
  }

  @Override
  public ApiServiceResponse<ApiMessageNumeric> countByOwner(String owner) {
    ServiceResponse<Long> serviceResponse = lrmListService.countByOwner(owner);
    return new ApiServiceResponse<>(
        new ApiMessageNumeric(serviceResponse.getContent()), serviceResponse.getMessage());
  }

  @Override
  public ApiServiceResponse<LrmListResponse> create(
      LrmListCreateRequest lrmListCreateRequest, String owner) {
    // TODO: evaluate where LrmItemRequest or LrmItem should be provided to domain service
    LrmListCreate lrmListCreate =
        new LrmListCreate(
            lrmListCreateRequest.getName(),
            lrmListCreateRequest.getDescription(),
            lrmListCreateRequest.getPublic());
    ServiceResponse<LrmList> serviceResponse = lrmListService.create(lrmListCreate, owner);
    return new ApiServiceResponse<>(
        LrmListResponse.fromLrmList(serviceResponse.getContent()), serviceResponse.getMessage());
  }

  @Override
  public ApiServiceResponse<LrmListDeletedResponse> deleteByOwner(String owner) {
    ServiceResponse<LrmListDeleted> serviceResponse = lrmListService.deleteByOwner(owner);
    LrmListDeleted content = serviceResponse.getContent();
    return new ApiServiceResponse<>(
        new LrmListDeletedResponse(content.listNames(), content.associatedItemNames()),
        serviceResponse.getMessage());
  }

  @Override
  public ApiServiceResponse<LrmListDeletedResponse> deleteByOwnerAndId(
      UUID id, String owner, boolean removeItemAssociations) {
    ServiceResponse<LrmListDeleted> serviceResponse =
        lrmListService.deleteByOwnerAndId(id, owner, removeItemAssociations);
    LrmListDeleted content = serviceResponse.getContent();
    return new ApiServiceResponse<>(
        new LrmListDeletedResponse(content.listNames(), content.associatedItemNames()),
        serviceResponse.getMessage());
  }

  @Override
  public ApiServiceResponse<List<LrmItemResponse>> findEligibleItemsByOwner(
      UUID listId, String owner) {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  @Override
  public ApiServiceResponse<List<LrmListResponse>> findByOwner(String owner) {
    ServiceResponse<List<LrmList>> serviceResponse = lrmListService.findByOwner(owner);
    List<LrmListResponse> content =
        serviceResponse.getContent().stream().map(LrmListResponse::fromLrmList).toList();
    return new ApiServiceResponse<>(content, serviceResponse.getMessage());
  }

  @Override
  public ApiServiceResponse<List<LrmListResponse>> findByOwnerExcludeItems(String owner) {
    ServiceResponse<List<LrmList>> serviceResponse = lrmListService.findByOwner(owner);
    List<LrmListResponse> content =
        serviceResponse.getContent().stream()
            .map(list -> LrmListResponse.fromLrmList(list.withItems(Set.of())))
            .toList();
    return new ApiServiceResponse<>(content, serviceResponse.getMessage());
  }

  @Override
  public ApiServiceResponse<List<LrmListResponse>> findByOwnerAndHavingNoItemAssociations(
      String owner) {
    ServiceResponse<List<LrmList>> serviceResponse =
        lrmListService.findByOwnerAndHavingNoItemAssociations(owner);
    List<LrmListResponse> content =
        serviceResponse.getContent().stream().map(LrmListResponse::fromLrmList).toList();
    return new ApiServiceResponse<>(content, serviceResponse.getMessage());
  }

  @Override
  public ApiServiceResponse<LrmListResponse> findByOwnerAndId(UUID id, String owner) {
    ServiceResponse<LrmList> serviceResponse = lrmListService.findByOwnerAndId(id, owner);
    return new ApiServiceResponse<>(
        LrmListResponse.fromLrmList(serviceResponse.getContent()), serviceResponse.getMessage());
  }

  @Override
  public ApiServiceResponse<LrmListResponse> findByOwnerAndIdExcludeItems(UUID id, String owner) {
    ServiceResponse<LrmList> serviceResponse = lrmListService.findByOwnerAndId(id, owner);
    return new ApiServiceResponse<>(
        LrmListResponse.fromLrmList(serviceResponse.getContent().withItems(Set.of())),
        serviceResponse.getMessage());
  }

  @Override
  public ApiServiceResponse<List<LrmListResponse>> findByPublic() {
    ServiceResponse<List<LrmList>> serviceResponse = lrmListService.findByPublic();
    List<LrmListResponse> content =
        serviceResponse.getContent().stream().map(LrmListResponse::fromLrmList).toList();
    return new ApiServiceResponse<>(content, serviceResponse.getMessage());
  }

  @Override
  public ApiServiceResponse<List<LrmListResponse>> findByPublicExcludeItems() {
    ServiceResponse<List<LrmList>> serviceResponse = lrmListService.findByPublic();
    List<LrmListResponse> content =
        serviceResponse.getContent().stream()
            .map(list -> LrmListResponse.fromLrmList(list.withItems(Set.of())))
            .toList();
    return new ApiServiceResponse<>(content, serviceResponse.getMessage());
  }

  @Override
  public ApiServiceResponse<LrmListResponse> patchByOwnerAndId(
      UUID id, String owner, Map<String, Object> patchRequest) {
    LrmList lrmList = lrmListService.findByOwnerAndId(id, owner).getContent();
    Set<String> patchedFields = new LinkedHashSet<>();
    for (Map.Entry<String, Object> entry : patchRequest.entrySet()) {
      String fieldToPatch = entry.getKey();
      Object value = entry.getValue();
      switch (fieldToPatch) {
        case "name" -> {
          if (!Objects.equals(value, lrmList.name())) {
            lrmListService.patchName(lrmList.withName((String) value));
            patchedFields.add(fieldToPatch);
          }
        }
        case "description" -> {
          if (!Objects.equals(value, lrmList.description())) {
            lrmListService.patchDescription(lrmList.withDescription((String) value));
            patchedFields.add(fieldToPatch);
          }
        }
        case "public" -> {
          if (!Objects.equals(value, lrmList.isPublic())) {
            lrmListService.patchIsPublic(lrmList.withIsPublic((Boolean) value));
            patchedFields.add(fieldToPatch);
          }
        }
        default ->
            throw new IllegalArgumentException(
                "Patch operation is not supported on field: " + fieldToPatch);
      }
    }
    LrmList patchedLrmList = lrmListService.findByOwnerAndId(id, owner).getContent();
    String message;
    if (!patchedLrmList.equals(lrmList)) {
      message =
          "List '"
              + patchedLrmList.name()
              + "' updated. Fields changed: "
              + String.join(", ", patchedFields)
              + ".";
    } else {
      message = "List '" + patchedLrmList.name() + "' not updated.";
    }
    LrmListResponse lrmItemResponse = LrmListResponse.fromLrmList(patchedLrmList);
    return new ApiServiceResponse<>(lrmItemResponse, message);
  }

  // list item context

  @Override
  public ApiServiceResponse<LrmListItemAddedResponse> addListItem(
      UUID listId, Set<UUID> itemIdCollection, String owner) {
    ServiceResponse<LrmListItemAdded> serviceResponse =
        lrmListItemService.add(listId, new java.util.ArrayList<>(itemIdCollection), owner);
    LrmListItemAdded content = serviceResponse.getContent();
    LrmListItemAddedResponse lrmListItemAddedResponse =
        new LrmListItemAddedResponse(content.listName(), content.items());
    return new ApiServiceResponse<>(lrmListItemAddedResponse, serviceResponse.getMessage());
  }

  @Override
  public ApiServiceResponse<ApiMessageNumeric> countListItems(UUID listId, String listOwner) {
    ServiceResponse<Long> serviceResponse =
        lrmListItemService.countByOwnerAndListId(listId, listOwner);
    return new ApiServiceResponse<>(
        new ApiMessageNumeric(serviceResponse.getContent()), serviceResponse.getMessage());
  }

  @Override
  public ApiServiceResponse<LrmListItemResponse> createListItem(
      UUID listId, LrmItemCreateRequest itemCreateRequest, String creator) {
    LrmItemCreate lrmItemCreate =
        new LrmItemCreate(
            itemCreateRequest.getName(),
            itemCreateRequest.getDescription(),
            itemCreateRequest.getQuantity(),
            itemCreateRequest.isSuppressed());
    ServiceResponse<LrmListItem> serviceResponse =
        lrmListItemService.create(listId, lrmItemCreate, creator);
    return new ApiServiceResponse<>(
        LrmListItemResponse.fromLrmListItem(serviceResponse.getContent()),
        serviceResponse.getMessage());
  }

  @Override
  public ApiServiceResponse<LrmListItemResponse> findListItem(
      UUID listId, UUID itemId, String listOwner) {
    ServiceResponse<LrmListItem> serviceResponse =
        lrmListItemService.findByOwnerAndItemIdAndListId(itemId, listId, listOwner);
    return new ApiServiceResponse<>(
        LrmListItemResponse.fromLrmListItem(serviceResponse.getContent()),
        serviceResponse.getMessage());
  }

  @Override
  public ApiServiceResponse<LrmListItemMovedResponse> moveListItem(
      UUID listId, UUID itemId, UUID destinationListId, String owner) {
    ServiceResponse<Triple<String, String, String>> serviceResponse =
        lrmListItemService.move(itemId, listId, destinationListId, owner);
    Triple<String, String, String> content = serviceResponse.getContent();
    LrmListItemMovedResponse lrmListItemMovedResponse =
        new LrmListItemMovedResponse(content.getFirst(), content.getSecond(), content.getThird());
    return new ApiServiceResponse<>(lrmListItemMovedResponse, serviceResponse.getMessage());
  }

  @Override
  public ApiServiceResponse<AssociationDeletedResponse> removeListItem(
      UUID listId, UUID itemId, String componentsOwner) {
    ServiceResponse<Pair<String, String>> serviceResponse =
        lrmListItemService.removeByOwnerAndListIdAndItemId(listId, itemId, componentsOwner);
    Pair<String, String> content = serviceResponse.getContent();
    AssociationDeletedResponse listItemRemovedResponse =
        new AssociationDeletedResponse(content.getFirst(), content.getSecond());
    return new ApiServiceResponse<>(listItemRemovedResponse, serviceResponse.getMessage());
  }

  @Override
  public ApiServiceResponse<AssociationsDeletedResponse> removeAllListItems(
      UUID listId, String listOwner) {
    ServiceResponse<Pair<String, Integer>> serviceResponse =
        lrmListItemService.removeByOwnerAndListId(listId, listOwner);
    Pair<String, Integer> content = serviceResponse.getContent();
    AssociationsDeletedResponse listItemsRemovedResponse =
        new AssociationsDeletedResponse(content.getFirst(), content.getSecond());
    return new ApiServiceResponse<>(listItemsRemovedResponse, serviceResponse.getMessage());
  }

  @Override
  public ApiServiceResponse<LrmListItemResponse> patchListItem(
      UUID listId, UUID itemId, String listOwner, Map<String, Object> patchRequest) {
    LrmListItem lrmListItem =
        lrmListItemService.findByOwnerAndItemIdAndListId(itemId, listId, listOwner).getContent();
    Set<String> patchedFields = new LinkedHashSet<>();
    for (Map.Entry<String, Object> entry : patchRequest.entrySet()) {
      String fieldToPatch = entry.getKey();
      Object value = entry.getValue();
      switch (fieldToPatch) {
        case "name" -> {
          if (!Objects.equals(value, lrmListItem.name())) {
            lrmListItemService.patchName(lrmListItem.withName((String) value));
            patchedFields.add(fieldToPatch);
          }
        }
        case "description" -> {
          if (!Objects.equals(value, lrmListItem.description())) {
            lrmListItemService.patchDescription(lrmListItem.withDescription((String) value));
            patchedFields.add(fieldToPatch);
          }
        }
        case "quantity" -> {
          if (!Objects.equals(value, lrmListItem.quantity())) {
            lrmListItemService.patchQuantity(lrmListItem.withQuantity((Integer) value));
            patchedFields.add(fieldToPatch);
          }
        }
        case "isSuppressed" -> {
          if (!Objects.equals(value, lrmListItem.isSuppressed())) {
            lrmListItemService.patchIsSuppressed(lrmListItem.withIsSuppressed((Boolean) value));
            patchedFields.add(fieldToPatch);
          }
        }
        default ->
            throw new IllegalArgumentException(
                "Patch operation is not supported on field: " + fieldToPatch);
      }
    }

    LrmListItem patchedLrmListItem =
        lrmListItemService.findByOwnerAndItemIdAndListId(itemId, listId, listOwner).getContent();
    String message;
    if (!patchedLrmListItem.equals(lrmListItem)) {
      message =
          "Item '"
              + patchedLrmListItem.name()
              + "' updated. Fields changed: "
              + String.join(", ", patchedFields)
              + ".";
    } else {
      message = "Item '" + patchedLrmListItem.name() + "' not updated.";
    }
    LrmListItemResponse lrmItemResponse = LrmListItemResponse.fromLrmListItem(patchedLrmListItem);
    return new ApiServiceResponse<>(lrmItemResponse, message);
  }
}
