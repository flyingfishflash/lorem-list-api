package net.flyingfishflash.loremlist.domain.lrmlistitem;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Set;
import java.util.UUID;
import kotlinx.datetime.Instant;
import net.flyingfishflash.loremlist.core.validation.ValidUuid;
import net.flyingfishflash.loremlist.domain.lrmlist.LrmListSuccinct;

// TODO: Make more domain-like, remove id and other entity fields
public record LrmListItem(
    @ValidUuid UUID id,
    @ValidUuid UUID listId,
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
    @Min(value = 0, message = "Item quantity must be zero or greater.") int quantity,
    boolean isSuppressed,
    String owner,
    Instant created,
    String creator,
    Instant updated,
    String updater,
    Set<LrmListSuccinct> lists) {

  public LrmListItem withName(String newName) {
    return new LrmListItem(
        id,
        listId,
        newName,
        description,
        quantity,
        isSuppressed,
        owner,
        created,
        creator,
        updated,
        updater,
        lists);
  }

  public LrmListItem withDescription(String newDescription) {
    return new LrmListItem(
        id,
        listId,
        name,
        newDescription,
        quantity,
        isSuppressed,
        owner,
        created,
        creator,
        updated,
        updater,
        lists);
  }

  public LrmListItem withQuantity(int newQuantity) {
    return new LrmListItem(
        id,
        listId,
        name,
        description,
        newQuantity,
        isSuppressed,
        owner,
        created,
        creator,
        updated,
        updater,
        lists);
  }

  public LrmListItem withIsSuppressed(boolean newIsSuppressed) {
    return new LrmListItem(
        id,
        listId,
        name,
        description,
        quantity,
        newIsSuppressed,
        owner,
        created,
        creator,
        updated,
        updater,
        lists);
  }

  public LrmListItem withLists(Set<LrmListSuccinct> newLists) {
    return new LrmListItem(
        id,
        listId,
        name,
        description,
        quantity,
        isSuppressed,
        owner,
        created,
        creator,
        updated,
        updater,
        newLists);
  }
}
