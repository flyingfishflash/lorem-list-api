package net.flyingfishflash.loremlist.unit.domain;

import static org.assertj.core.api.Assertions.assertThat;

import net.flyingfishflash.loremlist.domain.LrmComponentType;
import org.junit.jupiter.api.Test;

class LrmComponentTypeTests {

  @Test
  void inverseOfListIsItem() {
    assertThat(LrmComponentType.List.invert()).isEqualTo(LrmComponentType.Item);
  }

  @Test
  void inverseOfItemIsList() {
    assertThat(LrmComponentType.Item.invert()).isEqualTo(LrmComponentType.List);
  }

  @Test
  void inverseOfListItemIsListItem() {
    assertThat(LrmComponentType.ListItem.invert()).isEqualTo(LrmComponentType.ListItem);
  }
}
