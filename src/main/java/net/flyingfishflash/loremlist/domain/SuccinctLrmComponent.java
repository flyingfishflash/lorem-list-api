package net.flyingfishflash.loremlist.domain;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.UUID;
import net.flyingfishflash.loremlist.domain.lrmitem.LrmItemSuccinct;
import net.flyingfishflash.loremlist.domain.lrmlist.LrmListSuccinct;

/**
 * The {@code @JsonTypeInfo}/{@code @JsonSubTypes} pair reproduces, for Jackson, the "type"
 * discriminator that kotlinx.serialization's {@code SerializersModule { polymorphic(...) }} (see
 * SerializationConfig) previously added for this interface's subclasses.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
  @JsonSubTypes.Type(value = LrmItemSuccinct.class, name = "item"),
  @JsonSubTypes.Type(value = LrmListSuccinct.class, name = "list")
})
public interface SuccinctLrmComponent {
  UUID id();

  String name();
}
