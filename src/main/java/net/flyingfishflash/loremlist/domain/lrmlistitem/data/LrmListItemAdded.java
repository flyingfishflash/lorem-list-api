package net.flyingfishflash.loremlist.domain.lrmlistitem.data;

import java.util.List;
import net.flyingfishflash.loremlist.domain.SuccinctLrmComponent;

public record LrmListItemAdded(String listName, List<SuccinctLrmComponent> items) {}
