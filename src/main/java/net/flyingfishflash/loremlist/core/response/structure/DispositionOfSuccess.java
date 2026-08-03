package net.flyingfishflash.loremlist.core.response.structure;

import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Locale;

public enum DispositionOfSuccess implements Disposition {
  /** Client request success */
  SUCCESS;

  /** Returns an enum constant name() in lowercase */
  @Override
  @JsonValue
  public String nameAsLowercase() {
    return name().toLowerCase(Locale.getDefault());
  }
}
