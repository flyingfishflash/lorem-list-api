package net.flyingfishflash.loremlist.domain.maintenance.data;

public record DomainPurged(
    int associationDeletedCount, int itemDeletedCount, int listDeletedCount) {}
