package net.flyingfishflash.loremlist.domain.lrmlist;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface LrmListRepository {
  long countByOwner(String owner);

  int delete();

  int deleteById(Set<UUID> ids);

  int deleteByOwnerAndId(UUID id, String owner);

  List<LrmList> findByOwner(String owner);

  LrmList findByOwnerAndIdOrNull(UUID id, String owner);

  List<LrmList> findByOwnerAndHavingNoItemAssociations(String owner);

  List<LrmList> findByPublic();

  List<UUID> findIdsByOwnerAndIds(List<UUID> listIdCollection, String owner);

  Set<UUID> notFoundByOwnerAndId(List<UUID> listIdCollection, String owner);

  UUID insert(LrmList lrmList);

  int update(LrmList lrmList);

  int updateName(LrmList lrmList);

  int updateDescription(LrmList lrmList);

  int updateIsPublic(LrmList lrmList);
}
