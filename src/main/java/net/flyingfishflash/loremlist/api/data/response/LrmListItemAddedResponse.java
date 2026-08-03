package net.flyingfishflash.loremlist.api.data.response;

import java.util.List;
import net.flyingfishflash.loremlist.domain.SuccinctLrmComponent;

public record LrmListItemAddedResponse(
    String componentName, List<SuccinctLrmComponent> associatedComponents) {}
