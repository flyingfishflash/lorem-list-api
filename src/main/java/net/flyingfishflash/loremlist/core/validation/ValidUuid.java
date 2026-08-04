package net.flyingfishflash.loremlist.core.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = UuidValidator.class)
public @interface ValidUuid {
  String message() default "Must be a valid UUID";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
