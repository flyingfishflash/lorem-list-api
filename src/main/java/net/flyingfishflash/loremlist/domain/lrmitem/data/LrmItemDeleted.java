package net.flyingfishflash.loremlist.domain.lrmitem.data;

import java.util.List;

public record LrmItemDeleted(List<String> itemNames, List<String> associatedListNames) {}
