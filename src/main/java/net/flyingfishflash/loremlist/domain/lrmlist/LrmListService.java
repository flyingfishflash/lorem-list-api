package net.flyingfishflash.loremlist.domain.lrmlist;

import java.util.List;
import java.util.UUID;
import net.flyingfishflash.loremlist.domain.ServiceResponse;
import net.flyingfishflash.loremlist.domain.lrmitem.LrmItem;
import net.flyingfishflash.loremlist.domain.lrmlist.data.LrmListCreate;
import net.flyingfishflash.loremlist.domain.lrmlist.data.LrmListDeleted;

public interface LrmListService {
  ServiceResponse<Long> countByOwner(String owner);

  ServiceResponse<LrmList> create(LrmListCreate lrmListCreate, String creator);

  ServiceResponse<LrmListDeleted> deleteByOwner(String owner);

  ServiceResponse<LrmListDeleted> deleteByOwnerAndId(
      UUID id, String owner, boolean removeItemAssociations);

  ServiceResponse<List<LrmItem>> findEligibleItemsByOwner(UUID id, String owner);

  ServiceResponse<List<LrmList>> findByOwner(String owner);

  ServiceResponse<LrmList> findByOwnerAndId(UUID id, String owner);

  ServiceResponse<List<LrmList>> findByOwnerAndHavingNoItemAssociations(String owner);

  ServiceResponse<List<LrmList>> findByPublic();

  void patchName(LrmList patchedLrmList);

  void patchDescription(LrmList patchedLrmList);

  void patchIsPublic(LrmList patchedLrmList);
}
