package net.flyingfishflash.loremlist.api;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.flyingfishflash.loremlist.api.data.response.ApiServiceResponse;
import net.flyingfishflash.loremlist.api.data.response.LrmItemDeletedResponse;
import net.flyingfishflash.loremlist.api.data.response.LrmItemResponse;
import net.flyingfishflash.loremlist.core.response.structure.ApiMessageNumeric;

public interface LrmItemApiService {
  // item context scoped by owner
  ApiServiceResponse<ApiMessageNumeric> countByOwner(String owner);

  ApiServiceResponse<LrmItemDeletedResponse> deleteByOwner(String owner);

  ApiServiceResponse<LrmItemDeletedResponse> deleteByOwnerAndId(
      UUID id, String owner, boolean removeListAssociations);

  ApiServiceResponse<List<LrmItemResponse>> findByOwner(String owner);

  ApiServiceResponse<LrmItemResponse> findByOwnerAndId(UUID id, String owner);

  ApiServiceResponse<List<LrmItemResponse>> findByOwnerAndHavingNoListAssociations(String owner);

  ApiServiceResponse<List<LrmItemResponse>> findByOwnerAndHavingNoListAssociations(
      String owner, UUID listId);

  ApiServiceResponse<LrmItemResponse> patchByOwnerAndId(
      UUID id, String owner, Map<String, Object> patchRequest);

  // list item context scoped by owner
  ApiServiceResponse<ApiMessageNumeric> countListAssociationsByItemIdAndItemOwner(
      UUID itemId, String itemOwner);
}
