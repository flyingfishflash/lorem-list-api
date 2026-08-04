package net.flyingfishflash.loremlist.api.data.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record LrmListCreateRequest(
    // name is non-nullable: a JSON `null` must fail request parsing, matching the strict
    // non-null enforcement kotlinx.serialization previously applied to this field.
    @JsonSetter(nulls = Nulls.FAIL)
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
    @JsonProperty("public") boolean isPublic) {}
