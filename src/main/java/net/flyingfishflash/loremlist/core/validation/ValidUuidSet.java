package net.flyingfishflash.loremlist.core.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Annotation that validates all elements in a collection are valid UUIDs. */
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = UuidSetValidator.class)
public @interface ValidUuidSet {
  String message() default "The set of UUID's is invalid";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};

  boolean allowEmpty() default true;
}
