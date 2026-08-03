package net.flyingfishflash.loremlist.domain.lrmlist;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Set;
import java.util.UUID;
import kotlinx.datetime.Instant;
import net.flyingfishflash.loremlist.core.validation.ValidUuid;
import net.flyingfishflash.loremlist.domain.lrmlistitem.LrmListItem;

public record LrmList(
    @ValidUuid UUID id,
    @Pattern(
            regexp = "^(?!\\s*$).+",
            message = "List name must not consist only of whitespace characters.")
        @Size(
            min = 1,
            max = 64,
            message = "List name must have at least 1, and no more than 64 characters.")
        String name,
    @Pattern(
            regexp = "^(?!\\s*$).+",
            message = "List description must not consist only of whitespace characters.")
        @Size(
            min = 1,
            max = 2048,
            message = "List description must have at least 1, and no more than 2048 characters.")
        String description,
    // "public" is a reserved word in Java and cannot be a record component identifier.
    boolean isPublic,
    String owner,
    Instant created,
    String creator,
    Instant updated,
    String updater,
    Set<LrmListItem> items) {

  public LrmList withName(String newName) {
    return new LrmList(
        id, newName, description, isPublic, owner, created, creator, updated, updater, items);
  }

  public LrmList withDescription(String newDescription) {
    return new LrmList(
        id, name, newDescription, isPublic, owner, created, creator, updated, updater, items);
  }

  public LrmList withIsPublic(boolean newIsPublic) {
    return new LrmList(
        id, name, description, newIsPublic, owner, created, creator, updated, updater, items);
  }

  public LrmList withItems(Set<LrmListItem> newItems) {
    return new LrmList(
        id, name, description, isPublic, owner, created, creator, updated, updater, newItems);
  }
}
