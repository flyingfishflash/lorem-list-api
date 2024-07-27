package net.flyingfishflash.loremlist.domain

enum class LrmComponentType {
  Item,
  List,
  ;

  private lateinit var opposite: LrmComponentType

  fun invert(): LrmComponentType {
    return opposite
  }

  companion object {
    init {
      Item.opposite = List
      List.opposite = Item
    }
  }
}
