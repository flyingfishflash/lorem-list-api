package net.flyingfishflash.loremlist.api.data.response;

import java.util.List;

public record LrmListDeletedResponse(List<String> listNames, List<String> associatedItemNames) {}
