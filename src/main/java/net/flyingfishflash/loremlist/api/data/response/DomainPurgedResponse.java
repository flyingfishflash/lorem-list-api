package net.flyingfishflash.loremlist.api.data.response;

public record DomainPurgedResponse(
    int associationDeletedCount, int itemDeletedCount, int listDeletedCount) {}
