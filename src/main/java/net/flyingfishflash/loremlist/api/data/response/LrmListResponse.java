package net.flyingfishflash.loremlist.api.data.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Set;
import java.util.UUID;
import kotlinx.datetime.Instant;
import net.flyingfishflash.loremlist.domain.lrmlist.LrmList;

public record LrmListResponse(
    UUID id,
    String name,
    String description,
    // "public" is a reserved word in Java and cannot be a record component identifier.
    @JsonProperty("public") boolean isPublic,
    String owner,
    Instant created,
    String creator,
    Instant updated,
    String updater,
    Set<LrmListItemResponse> items) {

  public static LrmListResponse fromLrmList(LrmList lrmList) {
    Set<LrmListItemResponse> items =
        lrmList.items().stream()
            .map(LrmListItemResponse::fromLrmListItem)
            .collect(java.util.stream.Collectors.toSet());
    return new LrmListResponse(
        lrmList.id(),
        lrmList.name(),
        lrmList.description(),
        lrmList.isPublic(),
        lrmList.owner(),
        lrmList.created(),
        lrmList.creator(),
        lrmList.updated(),
        lrmList.updater(),
        items);
  }
}
