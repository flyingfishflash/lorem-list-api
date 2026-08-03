package net.flyingfishflash.loremlist.domain.lrmitem;

import java.util.Set;
import java.util.UUID;
import net.flyingfishflash.loremlist.domain.LrmComponentType;
import net.flyingfishflash.loremlist.domain.exceptions.EntityNotFoundException;

public class ItemNotFoundException extends EntityNotFoundException {

  public ItemNotFoundException(Set<UUID> idCollection, String message) {
    super(idCollection, message, LrmComponentType.Item);
  }

  public ItemNotFoundException(Set<UUID> idCollection) {
    this(idCollection, null);
  }

  public ItemNotFoundException(UUID id, String message) {
    this(Set.of(id), message);
  }

  public ItemNotFoundException(UUID id) {
    this(Set.of(id), null);
  }
}
