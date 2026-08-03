package net.flyingfishflash.loremlist.api.data.response;

public record LrmListItemMovedResponse(
    String itemName, String currentListName, String newListName) {}
