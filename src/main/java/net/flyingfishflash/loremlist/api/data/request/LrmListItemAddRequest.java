package net.flyingfishflash.loremlist.api.data.request;

import java.util.Set;
import java.util.UUID;
import net.flyingfishflash.loremlist.core.validation.ValidUuidSet;

public record LrmListItemAddRequest(@ValidUuidSet(allowEmpty = false) Set<UUID> itemIdCollection) {}
