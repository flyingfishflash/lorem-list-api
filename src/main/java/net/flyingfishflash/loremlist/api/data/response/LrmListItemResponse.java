package net.flyingfishflash.loremlist.api.data.response;

import java.util.Set;
import java.util.UUID;
import kotlinx.datetime.Instant;
import net.flyingfishflash.loremlist.domain.lrmlist.LrmListSuccinct;
import net.flyingfishflash.loremlist.domain.lrmlistitem.LrmListItem;

/** Representation of a list item in the context of a list */
public record LrmListItemResponse(
    UUID id,
    String name,
    String description,
    int quantity,
    boolean isSuppressed,
    String owner,
    Instant created,
    String creator,
    Instant updated,
    String updater,
    Set<LrmListSuccinct> lists) {

  public static LrmListItemResponse fromLrmListItem(LrmListItem lrmListItem) {
    return new LrmListItemResponse(
        lrmListItem.id(),
        lrmListItem.name(),
        lrmListItem.description(),
        lrmListItem.quantity(),
        lrmListItem.isSuppressed(),
        lrmListItem.owner(),
        lrmListItem.created(),
        lrmListItem.creator(),
        lrmListItem.updated(),
        lrmListItem.updater(),
        lrmListItem.lists());
  }
}
