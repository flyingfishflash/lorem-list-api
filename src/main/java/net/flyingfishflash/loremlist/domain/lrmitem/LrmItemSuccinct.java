package net.flyingfishflash.loremlist.domain.lrmitem;

import java.util.UUID;
import net.flyingfishflash.loremlist.domain.SuccinctLrmComponent;

public record LrmItemSuccinct(UUID id, String name) implements SuccinctLrmComponent {

  public static LrmItemSuccinct fromLrmItem(LrmItem lrmItem) {
    return new LrmItemSuccinct(lrmItem.id(), lrmItem.name());
  }
}
