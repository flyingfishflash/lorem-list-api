package net.flyingfishflash.loremlist.domain.lrmlist.data;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record LrmListCreate(
    @Pattern(
            regexp = "^(?!\\s*$).+",
            message = "List name must not consist only of whitespace characters.")
        @Size(
            min = 1,
            max = 64,
            message = "List name must have at least 1, and no more than 64 characters.")
        String name,
    @Pattern(
            regexp = "^(?!\\s*$).+",
            message = "List description must not consist only of whitespace characters.")
        @Size(
            min = 1,
            max = 2048,
            message = "List description must have at least 1, and no more than 2048 characters.")
        String description,
    // "public" is a reserved word in Java and cannot be a record component identifier.
    boolean isPublic) {}
