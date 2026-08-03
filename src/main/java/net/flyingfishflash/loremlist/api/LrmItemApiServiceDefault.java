package net.flyingfishflash.loremlist.api;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import net.flyingfishflash.loremlist.api.data.response.ApiServiceResponse;
import net.flyingfishflash.loremlist.api.data.response.LrmItemDeletedResponse;
import net.flyingfishflash.loremlist.api.data.response.LrmItemResponse;
import net.flyingfishflash.loremlist.core.response.structure.ApiMessageNumeric;
import net.flyingfishflash.loremlist.domain.ServiceResponse;
import net.flyingfishflash.loremlist.domain.lrmitem.LrmItem;
import net.flyingfishflash.loremlist.domain.lrmitem.LrmItemService;
import net.flyingfishflash.loremlist.domain.lrmitem.data.LrmItemDeleted;
import net.flyingfishflash.loremlist.domain.lrmlistitem.LrmListItemService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class LrmItemApiServiceDefault implements LrmItemApiService {

  private final LrmItemService lrmItemService;
  private final LrmListItemService lrmListItemService;

  public LrmItemApiServiceDefault(
      LrmItemService lrmItemService, LrmListItemService lrmListItemService) {
    this.lrmItemService = lrmItemService;
    this.lrmListItemService = lrmListItemService;
  }

  @Override
  public ApiServiceResponse<ApiMessageNumeric> countByOwner(String owner) {
    ServiceResponse<Long> serviceResponse = lrmItemService.countByOwner(owner);
    return new ApiServiceResponse<>(
        new ApiMessageNumeric(serviceResponse.getContent()), serviceResponse.getMessage());
  }

  @Override
  public ApiServiceResponse<LrmItemDeletedResponse> deleteByOwner(String owner) {
    ServiceResponse<LrmItemDeleted> serviceResponse = lrmItemService.deleteByOwner(owner);
    LrmItemDeleted content = serviceResponse.getContent();
    return new ApiServiceResponse<>(
        new LrmItemDeletedResponse(content.itemNames(), content.associatedListNames()),
        serviceResponse.getMessage());
  }

  @Override
  public ApiServiceResponse<LrmItemDeletedResponse> deleteByOwnerAndId(
      UUID id, String owner, boolean removeListAssociations) {
    ServiceResponse<LrmItemDeleted> serviceResponse =
        lrmItemService.deleteByOwnerAndId(id, owner, removeListAssociations);
    LrmItemDeleted content = serviceResponse.getContent();
    return new ApiServiceResponse<>(
        new LrmItemDeletedResponse(content.itemNames(), content.associatedListNames()),
        serviceResponse.getMessage());
  }

  @Override
  public ApiServiceResponse<List<LrmItemResponse>> findByOwner(String owner) {
    ServiceResponse<List<LrmItem>> serviceResponse = lrmItemService.findByOwner(owner);
    List<LrmItemResponse> content =
        serviceResponse.getContent().stream().map(LrmItemResponse::fromLrmItem).toList();
    return new ApiServiceResponse<>(content, serviceResponse.getMessage());
  }

  @Override
  public ApiServiceResponse<LrmItemResponse> findByOwnerAndId(UUID id, String owner) {
    ServiceResponse<LrmItem> serviceResponse = lrmItemService.findByOwnerAndId(id, owner);
    return new ApiServiceResponse<>(
        LrmItemResponse.fromLrmItem(serviceResponse.getContent()), serviceResponse.getMessage());
  }

  @Override
  public ApiServiceResponse<List<LrmItemResponse>> findByOwnerAndHavingNoListAssociations(
      String owner) {
    ServiceResponse<List<LrmItem>> serviceResponse =
        lrmItemService.findByOwnerAndHavingNoListAssociations(owner);
    List<LrmItemResponse> content =
        serviceResponse.getContent().stream().map(LrmItemResponse::fromLrmItem).toList();
    return new ApiServiceResponse<>(content, serviceResponse.getMessage());
  }

  @Override
  public ApiServiceResponse<List<LrmItemResponse>> findByOwnerAndHavingNoListAssociations(
      String owner, UUID listId) {
    ServiceResponse<List<LrmItem>> serviceResponse =
        lrmItemService.findByOwnerAndHavingNoListAssociations(owner, listId);
    List<LrmItemResponse> content =
        serviceResponse.getContent().stream().map(LrmItemResponse::fromLrmItem).toList();
    return new ApiServiceResponse<>(content, serviceResponse.getMessage());
  }

  @Override
  public ApiServiceResponse<LrmItemResponse> patchByOwnerAndId(
      UUID id, String owner, Map<String, Object> patchRequest) {
    LrmItem lrmItem = lrmItemService.findByOwnerAndId(id, owner).getContent();
    Set<String> patchedFields = new LinkedHashSet<>();
    for (Map.Entry<String, Object> entry : patchRequest.entrySet()) {
      String fieldToPatch = entry.getKey();
      Object value = entry.getValue();
      switch (fieldToPatch) {
        case "name" -> {
          if (!Objects.equals(value, lrmItem.name())) {
            lrmItemService.patchName(lrmItem.withName((String) value));
            patchedFields.add(fieldToPatch);
          }
        }
        case "description" -> {
          if (!Objects.equals(value, lrmItem.description())) {
            lrmItemService.patchDescription(lrmItem.withDescription((String) value));
            patchedFields.add(fieldToPatch);
          }
        }
        default ->
            throw new IllegalArgumentException(
                "Patch operation is not supported on field: " + fieldToPatch);
      }
    }

    LrmItem patchedLrmItem = lrmItemService.findByOwnerAndId(id, owner).getContent();
    String message;
    if (!patchedLrmItem.equals(lrmItem)) {
      message =
          "Item '"
              + patchedLrmItem.name()
              + "' updated. Fields changed: "
              + String.join(", ", patchedFields)
              + ".";
    } else {
      message = "Item '" + patchedLrmItem.name() + "' not updated.";
    }
    LrmItemResponse lrmItemResponse = LrmItemResponse.fromLrmItem(patchedLrmItem);
    return new ApiServiceResponse<>(lrmItemResponse, message);
  }

  @Override
  public ApiServiceResponse<ApiMessageNumeric> countListAssociationsByItemIdAndItemOwner(
      UUID itemId, String itemOwner) {
    ServiceResponse<Long> serviceResponse =
        lrmListItemService.countByOwnerAndItemId(itemId, itemOwner);
    return new ApiServiceResponse<>(
        new ApiMessageNumeric(serviceResponse.getContent()), serviceResponse.getMessage());
  }
}
