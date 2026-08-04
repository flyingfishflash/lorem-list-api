package net.flyingfishflash.loremlist.core.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.UUID;
import java.util.regex.Pattern;

public class UuidValidator implements ConstraintValidator<ValidUuid, UUID> {

  private static final Pattern PATTERN =
      Pattern.compile(
          "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[34][0-9a-fA-F]{3}-[89ab][0-9a-fA-F]{3}-[0-9a-fA-F]{12}");

  @Override
  public boolean isValid(UUID uuid, ConstraintValidatorContext context) {
    return PATTERN.matcher(uuid.toString()).matches();
  }
}
