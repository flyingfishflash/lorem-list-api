package net.flyingfishflash.loremlist.domain.lrmitem;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface LrmItemRepository {
  long countByOwner(String owner);

  int delete();

  int deleteById(Set<UUID> ids);

  int deleteByOwnerAndId(UUID id, String owner);

  List<LrmItem> findByOwner(String owner);

  LrmItem findByOwnerAndIdOrNull(UUID id, String owner);

  List<LrmItem> findByOwnerAndHavingNoListAssociations(String owner);

  List<LrmItem> findByOwnerAndHavingNoListAssociations(String owner, UUID listId);

  List<UUID> findIdsByOwnerAndIds(List<UUID> itemIdCollection, String owner);

  Set<UUID> notFoundByOwnerAndId(List<UUID> itemIdCollection, String owner);

  UUID insert(LrmItem lrmItem);

  int update(LrmItem lrmItem);

  int updateName(LrmItem lrmItem);

  int updateDescription(LrmItem lrmItem);
}
