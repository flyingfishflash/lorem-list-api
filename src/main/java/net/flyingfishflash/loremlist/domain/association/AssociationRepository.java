package net.flyingfishflash.loremlist.domain.association;

import java.util.UUID;

public interface AssociationRepository {
  boolean listIsConsistent(UUID listId);

  int delete();
}
