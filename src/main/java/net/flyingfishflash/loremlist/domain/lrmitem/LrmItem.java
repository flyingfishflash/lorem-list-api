package net.flyingfishflash.loremlist.domain.lrmitem;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Set;
import java.util.UUID;
import kotlinx.datetime.Instant;
import net.flyingfishflash.loremlist.core.validation.ValidUuid;
import net.flyingfishflash.loremlist.domain.lrmlist.LrmListSuccinct;

// TODO: Make more domain-like, remove id and other entity fields
public record LrmItem(
    @ValidUuid UUID id,
    @Pattern(
            regexp = "^(?!\\s*$).+",
            message = "Item name must not consist only of whitespace characters.")
        @Size(
            min = 1,
            max = 64,
            message = "Item name must have at least 1, and no more than 64 characters.")
        String name,
    @Pattern(
            regexp = "^(?!\\s*$).+",
            message = "Item description must not consist only of whitespace characters.")
        @Size(
            min = 1,
            max = 2048,
            message = "Item description must have at least 1, and no more than 2048 characters.")
        String description,
    String owner,
    Instant created,
    String creator,
    Instant updated,
    String updater,
    Set<LrmListSuccinct> lists) {

  public LrmItem withName(String newName) {
    return new LrmItem(id, newName, description, owner, created, creator, updated, updater, lists);
  }

  public LrmItem withDescription(String newDescription) {
    return new LrmItem(id, name, newDescription, owner, created, creator, updated, updater, lists);
  }

  public LrmItem withLists(Set<LrmListSuccinct> newLists) {
    return new LrmItem(id, name, description, owner, created, creator, updated, updater, newLists);
  }
}
