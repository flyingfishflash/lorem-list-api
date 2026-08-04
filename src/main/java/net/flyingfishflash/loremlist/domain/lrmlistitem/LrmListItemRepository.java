package net.flyingfishflash.loremlist.domain.lrmlistitem;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import kotlin.Pair;
import net.flyingfishflash.loremlist.persistence.SuccinctLrmComponentPair;

public interface LrmListItemRepository {
  long countByOwnerAndListId(UUID listId, String listOwner);

  long countByOwnerAndItemId(UUID itemId, String itemOwner);

  void create(UUID listId, UUID itemId);

  List<SuccinctLrmComponentPair> create(Set<Pair<UUID, UUID>> associationCollection);

  LrmListItem findByOwnerAndItemIdAndListIdOrNull(UUID itemId, UUID listId, String owner);

  int removeByOwnerAndItemId(UUID itemId, String owner);

  int removeByOwnerAndListId(UUID listId, String owner);

  int removeByOwnerAndListIdAndItemId(UUID listId, UUID itemId, String owner);

  int updateListId(LrmListItem lrmListItem, UUID destinationListId);

  int updateName(LrmListItem lrmListItem);

  int updateDescription(LrmListItem lrmListItem);

  int updateQuantity(LrmListItem lrmListItem);

  int updateIsItemSuppressed(LrmListItem lrmListItem);
}
