package net.flyingfishflash.loremlist.api;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
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

public interface LrmListApiService {
  // list context scoped by owner
  ApiServiceResponse<ApiMessageNumeric> countByOwner(String owner);

  ApiServiceResponse<LrmListResponse> create(
      LrmListCreateRequest lrmListCreateRequest, String owner);

  ApiServiceResponse<LrmListDeletedResponse> deleteByOwner(String owner);

  ApiServiceResponse<LrmListDeletedResponse> deleteByOwnerAndId(
      UUID id, String owner, boolean removeItemAssociations);

  ApiServiceResponse<List<LrmItemResponse>> findEligibleItemsByOwner(UUID listId, String owner);

  ApiServiceResponse<List<LrmListResponse>> findByOwner(String owner);

  ApiServiceResponse<List<LrmListResponse>> findByOwnerExcludeItems(String owner);

  ApiServiceResponse<List<LrmListResponse>> findByOwnerAndHavingNoItemAssociations(String owner);

  ApiServiceResponse<LrmListResponse> findByOwnerAndId(UUID id, String owner);

  ApiServiceResponse<LrmListResponse> findByOwnerAndIdExcludeItems(UUID id, String owner);

  ApiServiceResponse<List<LrmListResponse>> findByPublic();

  ApiServiceResponse<List<LrmListResponse>> findByPublicExcludeItems();

  ApiServiceResponse<LrmListResponse> patchByOwnerAndId(
      UUID id, String owner, Map<String, Object> patchRequest);

  // list item context scoped by owner
  ApiServiceResponse<LrmListItemAddedResponse> addListItem(
      UUID listId, Set<UUID> itemIdCollection, String owner);

  ApiServiceResponse<ApiMessageNumeric> countListItems(UUID listId, String listOwner);

  ApiServiceResponse<LrmListItemResponse> createListItem(
      UUID listId, LrmItemCreateRequest itemCreateRequest, String creator);

  ApiServiceResponse<LrmListItemResponse> findListItem(UUID listId, UUID itemId, String listOwner);

  ApiServiceResponse<LrmListItemMovedResponse> moveListItem(
      UUID listId, UUID itemId, UUID destinationListId, String owner);

  ApiServiceResponse<AssociationDeletedResponse> removeListItem(
      UUID listId, UUID itemId, String componentsOwner);

  ApiServiceResponse<AssociationsDeletedResponse> removeAllListItems(UUID listId, String listOwner);

  ApiServiceResponse<LrmListItemResponse> patchListItem(
      UUID listId, UUID itemId, String listOwner, Map<String, Object> patchRequest);
}
