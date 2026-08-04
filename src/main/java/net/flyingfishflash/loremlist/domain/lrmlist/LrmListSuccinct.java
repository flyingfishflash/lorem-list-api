package net.flyingfishflash.loremlist.domain.lrmlist;

import java.util.UUID;
import net.flyingfishflash.loremlist.domain.SuccinctLrmComponent;

public record LrmListSuccinct(UUID id, String name) implements SuccinctLrmComponent {

  public static LrmListSuccinct fromLrmList(LrmList lrmList) {
    return new LrmListSuccinct(lrmList.id(), lrmList.name());
  }
}
