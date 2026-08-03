package net.flyingfishflash.loremlist.domain;

public enum LrmComponentType {
  ListItem,
  Item,
  List;

  private LrmComponentType opposite;

  static {
    Item.opposite = List;
    List.opposite = Item;
    ListItem.opposite = ListItem;
  }

  public LrmComponentType invert() {
    return opposite;
  }
}
