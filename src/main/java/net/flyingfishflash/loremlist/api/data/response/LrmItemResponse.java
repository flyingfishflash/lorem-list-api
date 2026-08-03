package net.flyingfishflash.loremlist.api.data.response;

import java.util.Set;
import java.util.UUID;
import kotlinx.datetime.Instant;
import net.flyingfishflash.loremlist.domain.lrmitem.LrmItem;
import net.flyingfishflash.loremlist.domain.lrmlist.LrmListSuccinct;

/** Representation of a list item outside the context of a list association */
public record LrmItemResponse(
    UUID id,
    String name,
    String description,
    int quantity,
    String owner,
    Instant created,
    String creator,
    Instant updated,
    String updater,
    Set<LrmListSuccinct> lists) {

  public static LrmItemResponse fromLrmItem(LrmItem lrmItem) {
    return new LrmItemResponse(
        lrmItem.id(),
        lrmItem.name(),
        lrmItem.description(),
        0,
        lrmItem.owner(),
        lrmItem.created(),
        lrmItem.creator(),
        lrmItem.updated(),
        lrmItem.updater(),
        lrmItem.lists());
  }
}
