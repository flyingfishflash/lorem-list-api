package net.flyingfishflash.loremlist.domain.lrmitem;

import java.util.List;
import java.util.UUID;
import net.flyingfishflash.loremlist.domain.ServiceResponse;
import net.flyingfishflash.loremlist.domain.lrmitem.data.LrmItemDeleted;

public interface LrmItemService {
  ServiceResponse<Long> countByOwner(String owner);

  ServiceResponse<LrmItemDeleted> deleteByOwner(String owner);

  ServiceResponse<LrmItemDeleted> deleteByOwnerAndId(
      UUID id, String owner, boolean removeListAssociations);

  ServiceResponse<List<LrmItem>> findByOwner(String owner);

  ServiceResponse<LrmItem> findByOwnerAndId(UUID id, String owner);

  ServiceResponse<List<LrmItem>> findByOwnerAndHavingNoListAssociations(String owner);

  ServiceResponse<List<LrmItem>> findByOwnerAndHavingNoListAssociations(String owner, UUID listId);

  void patchName(LrmItem patchedLrmItem);

  void patchDescription(LrmItem patchedLrmItem);
}
