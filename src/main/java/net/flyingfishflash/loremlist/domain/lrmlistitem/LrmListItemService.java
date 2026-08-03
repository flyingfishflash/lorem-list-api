package net.flyingfishflash.loremlist.domain.lrmlistitem;

import java.util.List;
import java.util.UUID;
import kotlin.Pair;
import kotlin.Triple;
import net.flyingfishflash.loremlist.domain.ServiceResponse;
import net.flyingfishflash.loremlist.domain.lrmitem.data.LrmItemCreate;
import net.flyingfishflash.loremlist.domain.lrmlistitem.data.LrmListItemAdded;

public interface LrmListItemService {
  ServiceResponse<Long> countByOwnerAndListId(UUID listId, String owner);

  ServiceResponse<LrmListItemAdded> add(UUID id, List<UUID> idCollection, String componentsOwner);

  ServiceResponse<LrmListItem> create(UUID listId, LrmItemCreate lrmItemCreate, String creator);

  ServiceResponse<LrmListItem> findByOwnerAndItemIdAndListId(
      UUID itemId, UUID listId, String owner);

  ServiceResponse<Triple<String, String, String>> move(
      UUID itemId, UUID currentListId, UUID destinationListId, String owner);

  void patchQuantity(LrmListItem patchedLrmListItem);

  void patchIsSuppressed(LrmListItem patchedLrmListItem);

  ServiceResponse<Pair<String, String>> removeByOwnerAndListIdAndItemId(
      UUID listId, UUID itemId, String owner);

  ServiceResponse<Pair<String, Integer>> removeByOwnerAndItemId(UUID itemId, String owner);

  ServiceResponse<Pair<String, Integer>> removeByOwnerAndListId(UUID listId, String owner);

  // item context
  ServiceResponse<Long> countByOwnerAndItemId(UUID itemId, String owner);

  void patchName(LrmListItem patchedLrmListItem);

  void patchDescription(LrmListItem patchedLrmListItem);
}
