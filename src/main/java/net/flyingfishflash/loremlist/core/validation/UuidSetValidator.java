package net.flyingfishflash.loremlist.core.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Collection;
import java.util.UUID;
import java.util.regex.Pattern;

public class UuidSetValidator implements ConstraintValidator<ValidUuidSet, Collection<?>> {

  private static final Pattern PATTERN =
      Pattern.compile(
          "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[34][0-9a-fA-F]{3}-[89ab][0-9a-fA-F]{3}-[0-9a-fA-F]{12}");

  private boolean allowEmpty = true;

  @Override
  public void initialize(ValidUuidSet constraintAnnotation) {
    this.allowEmpty = constraintAnnotation.allowEmpty();
  }

  @Override
  public boolean isValid(Collection<?> value, ConstraintValidatorContext context) {
    if (value == null) {
      return buildViolation(context, "UUID set must not be null");
    }

    if (!allowEmpty && value.isEmpty()) {
      return buildViolation(context, "UUID set must not be empty");
    }

    context.disableDefaultConstraintViolation();
    boolean isValid = true;

    int index = 0;
    for (Object item : value) {
      if (item == null) {
        isValid = buildViolation(context, "Value at index " + index + " is null");
      } else if (item instanceof UUID uuid) {
        if (!PATTERN.matcher(uuid.toString()).matches()) {
          isValid =
              buildViolation(
                  context,
                  "Value '"
                      + uuid
                      + "' at index "
                      + index
                      + " is not a valid version 3 or version 4 UUID");
        }
      } else {
        isValid = buildViolation(context, "Value at index " + index + " is not a UUID object");
      }
      index++;
    }
    return isValid;
  }

  private boolean buildViolation(ConstraintValidatorContext context, String message) {
    context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
    return false;
  }
}
