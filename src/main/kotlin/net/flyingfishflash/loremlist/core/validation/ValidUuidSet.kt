package net.flyingfishflash.loremlist.core.validation

import jakarta.validation.Constraint
import jakarta.validation.Payload
import kotlin.reflect.KClass

/**
 * Annotation that validates all elements in a collection are valid UUIDs.
 */
@Target(
  AnnotationTarget.FIELD,
  AnnotationTarget.PROPERTY_GETTER,
  AnnotationTarget.VALUE_PARAMETER,
  AnnotationTarget.PROPERTY,
  AnnotationTarget.TYPE,
)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@Constraint(validatedBy = [UuidSetValidator::class])
annotation class ValidUuidSet(
  val message: String = "The set of UUID's is invalid",
  val groups: Array<KClass<*>> = [],
  val payload: Array<KClass<out Payload>> = [],
  val allowEmpty: Boolean = true,
)
