package net.flyingfishflash.loremlist.core.response.structure;

import java.util.Collection;
import java.util.Map;

/** Calculates the number of items included in a response's content */
final class ResponseContentSize {
  private ResponseContentSize() {}

  static int calculate(Object content) {
    if (content == null) {
      return 0;
    } else if (content instanceof Collection<?> collection) {
      return collection.size();
    } else if (content instanceof Map<?, ?> map) {
      return map.size();
    } else {
      return 1;
    }
  }
}
