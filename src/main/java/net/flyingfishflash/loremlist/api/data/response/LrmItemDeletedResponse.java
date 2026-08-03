package net.flyingfishflash.loremlist.api.data.response;

import java.util.List;

public record LrmItemDeletedResponse(List<String> itemNames, List<String> associatedListNames) {}
