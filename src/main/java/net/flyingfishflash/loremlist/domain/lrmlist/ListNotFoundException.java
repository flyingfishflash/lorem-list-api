package net.flyingfishflash.loremlist.domain.lrmlist;

import java.util.Set;
import java.util.UUID;
import net.flyingfishflash.loremlist.domain.LrmComponentType;
import net.flyingfishflash.loremlist.domain.exceptions.EntityNotFoundException;

public class ListNotFoundException extends EntityNotFoundException {

  public ListNotFoundException(Set<UUID> idCollection, String message) {
    super(idCollection, message, LrmComponentType.List);
  }

  public ListNotFoundException(Set<UUID> idCollection) {
    this(idCollection, null);
  }

  public ListNotFoundException(UUID id, String message) {
    this(Set.of(id), message);
  }

  public ListNotFoundException(UUID id) {
    this(Set.of(id), null);
  }

  public ListNotFoundException() {
    this(Set.of(), null);
  }
}
