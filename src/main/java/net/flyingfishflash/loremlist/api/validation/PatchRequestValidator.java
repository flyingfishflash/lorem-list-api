package net.flyingfishflash.loremlist.api.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Map;

public class PatchRequestValidator
    implements ConstraintValidator<ValidPatchRequest, Map<String, Object>> {

  @Override
  public boolean isValid(Map<String, Object> value, ConstraintValidatorContext context) {
    if (value == null) {
      return buildViolation(context, "patch request cannot be null");
    }

    context.disableDefaultConstraintViolation();
    boolean isValid = true;

    for (Map.Entry<String, Object> entry : value.entrySet()) {
      if (!validKey(entry.getKey(), context)) {
        isValid = false;
      }
      if (!validValue(entry.getKey(), entry.getValue(), context)) {
        isValid = false;
      }
    }
    return isValid;
  }

  private boolean validKey(String key, ConstraintValidatorContext context) {
    if (key.length() < 1 || key.length() > 64) {
      return buildViolation(context, "Key '" + key + "' must be 1 to 64 characters long");
    }
    return true;
  }

  private boolean validValue(String key, Object value, ConstraintValidatorContext context) {
    if (value == null) {
      return buildViolation(context, "Value for key '" + key + "' must not be null");
    }

    if (value instanceof Boolean) {
      return true;
    }
    if (value instanceof String stringValue) {
      return validStringValue(key, stringValue, context);
    }
    return buildViolation(context, "Value for key '" + key + "' must be a boolean or string");
  }

  private boolean validStringValue(String key, String value, ConstraintValidatorContext context) {
    if (value.isBlank()) {
      return buildViolation(
          context, "String value for key '" + key + "' must not be blank or whitespace");
    }

    int maxLength = "description".equals(key) ? 2048 : 64;

    if (value.length() < 1 || value.length() > maxLength) {
      return buildViolation(
          context,
          "Value for field '"
              + key
              + "' must be a string from 1 to "
              + maxLength
              + " characters long");
    }

    return true;
  }

  private boolean buildViolation(ConstraintValidatorContext context, String message) {
    context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
    return false;
  }
}
