package net.flyingfishflash.loremlist.domain.lrmlistitem;

import java.util.Set;
import java.util.UUID;
import net.flyingfishflash.loremlist.domain.LrmComponentType;
import net.flyingfishflash.loremlist.domain.exceptions.EntityNotFoundException;

public class ListItemNotFoundException extends EntityNotFoundException {

  public ListItemNotFoundException(Set<UUID> idCollection, String message) {
    super(idCollection, message, LrmComponentType.ListItem);
  }

  public ListItemNotFoundException(Set<UUID> idCollection) {
    this(idCollection, null);
  }

  public ListItemNotFoundException(UUID id, String message) {
    this(Set.of(id), message);
  }

  public ListItemNotFoundException(UUID id) {
    this(Set.of(id), null);
  }

  public ListItemNotFoundException() {
    this(Set.of(), null);
  }
}
