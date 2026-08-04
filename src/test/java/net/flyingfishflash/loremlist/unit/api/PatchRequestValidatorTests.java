package net.flyingfishflash.loremlist.unit.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.ConstraintValidatorContext.ConstraintViolationBuilder;
import java.util.Map;
import net.flyingfishflash.loremlist.api.validation.PatchRequestValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PatchRequestValidatorTests {

  private PatchRequestValidator validator;
  private ConstraintValidatorContext context;
  private ConstraintViolationBuilder violationBuilder;

  @BeforeEach
  void setUp() {
    validator = new PatchRequestValidator();
    context = mock(ConstraintValidatorContext.class);
    violationBuilder = mock(ConstraintViolationBuilder.class);
    when(context.buildConstraintViolationWithTemplate(any())).thenReturn(violationBuilder);
  }

  @Test
  void shouldReturnFalseIfInputIsNull() {
    boolean result = validator.isValid(null, context);
    assertThat(result).isFalse();
    verify(context).buildConstraintViolationWithTemplate("patch request cannot be null");
  }

  @Test
  void shouldReturnFalseIfKeyIsLongerThan64Characters() {
    String longKey = "k".repeat(65);
    boolean result = validator.isValid(Map.of(longKey, "value"), context);
    assertThat(result).isFalse();
    verify(context)
        .buildConstraintViolationWithTemplate(
            "Key '" + longKey + "' must be 1 to 64 characters long");
  }

  @Test
  void shouldReturnFalseIfValueIsNotAStringOrBoolean() {
    Map<String, Object> data = new java.util.HashMap<>();
    data.put("name", 123);
    boolean result = validator.isValid(data, context);
    assertThat(result).isFalse();
    verify(context)
        .buildConstraintViolationWithTemplate("Value for key 'name' must be a boolean or string");
  }

  @Test
  void shouldReturnFalseForBlankStringValue() {
    boolean result = validator.isValid(Map.of("name", "   "), context);
    assertThat(result).isFalse();
    verify(context)
        .buildConstraintViolationWithTemplate(
            "String value for key 'name' must not be blank or whitespace");
  }

  @Test
  void shouldReturnFalseIfStringIsTooLongForNameField() {
    String longValue = "a".repeat(65);
    boolean result = validator.isValid(Map.of("name", longValue), context);
    assertThat(result).isFalse();
    verify(context)
        .buildConstraintViolationWithTemplate(
            "Value for field 'name' must be a string from 1 to 64 characters long");
  }

  @Test
  void shouldReturnFalseIfStringIsTooLongForDescriptionField() {
    String longDesc = "a".repeat(2049);
    boolean result = validator.isValid(Map.of("description", longDesc), context);
    assertThat(result).isFalse();
    verify(context)
        .buildConstraintViolationWithTemplate(
            "Value for field 'description' must be a string from 1 to 2048 characters long");
  }

  @Test
  void shouldReturnTrueForValidBooleanValue() {
    boolean result = validator.isValid(Map.of("public", true), context);
    assertThat(result).isTrue();
  }

  @Test
  void shouldReturnTrueForValidStringFields() {
    Map<String, Object> validData =
        Map.of(
            "name", "Valid Name",
            "description", "A valid description",
            "customField", "custom value");

    boolean result = validator.isValid(validData, context);
    assertThat(result).isTrue();
  }
}
