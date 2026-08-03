package net.flyingfishflash.loremlist.domain

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import net.flyingfishflash.loremlist.domain.lrmitem.LrmItemSuccinct
import net.flyingfishflash.loremlist.domain.lrmlist.LrmListSuccinct
import java.util.UUID

/**
 * The `@JsonTypeInfo`/`@JsonSubTypes` pair reproduces, for Jackson, the "type" discriminator that
 * kotlinx.serialization's `SerializersModule { polymorphic(...) }` (see SerializationConfig) added
 * for this interface's subclasses, since responses now serialize through Jackson rather than kotlinx.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
  JsonSubTypes.Type(value = LrmItemSuccinct::class, name = "item"),
  JsonSubTypes.Type(value = LrmListSuccinct::class, name = "list"),
)
interface SuccinctLrmComponent {
  val id: UUID
  val name: String
}
