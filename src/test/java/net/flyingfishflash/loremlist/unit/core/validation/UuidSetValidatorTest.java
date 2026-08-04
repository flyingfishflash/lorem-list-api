package net.flyingfishflash.loremlist.unit.core.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.validation.ConstraintValidatorContext;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import net.flyingfishflash.loremlist.core.validation.UuidSetValidator;
import net.flyingfishflash.loremlist.core.validation.ValidUuidSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class UuidSetValidatorTest {

  private ConstraintValidatorContext.ConstraintViolationBuilder mockBuilder;
  private ConstraintValidatorContext mockContext;

  @BeforeEach
  void setUp() {
    mockBuilder = mock(ConstraintValidatorContext.ConstraintViolationBuilder.class);
    mockContext = mock(ConstraintValidatorContext.class);
    when(mockContext.buildConstraintViolationWithTemplate(org.mockito.ArgumentMatchers.any()))
        .thenReturn(mockBuilder);
  }

  @Nested
  class Initialization {

    @Test
    void setsAllowEmptyToTrueWhenSpecifiedInAnnotation() throws Exception {
      UuidSetValidator validator = new UuidSetValidator();
      ValidUuidSet annotation = mock(ValidUuidSet.class);
      when(annotation.allowEmpty()).thenReturn(true);
      validator.initialize(annotation);
      Field field = UuidSetValidator.class.getDeclaredField("allowEmpty");
      field.setAccessible(true);
      assertThat(field.getBoolean(validator)).isTrue();
    }

    @Test
    void setsAllowEmptyToFalseWhenSpecifiedInAnnotation() throws Exception {
      UuidSetValidator validator = new UuidSetValidator();
      ValidUuidSet annotation = mock(ValidUuidSet.class);
      when(annotation.allowEmpty()).thenReturn(false);
      validator.initialize(annotation);
      Field field = UuidSetValidator.class.getDeclaredField("allowEmpty");
      field.setAccessible(true);
      assertThat(field.getBoolean(validator)).isFalse();
    }
  }

  @Nested
  class IsValid {

    private final UuidSetValidator validator = new UuidSetValidator();

    @Test
    void whenValueIsNullReturnsFalseAndAddsViolationMessage() {
      boolean result = validator.isValid(null, mockContext);
      assertThat(result).isFalse();
      verify(mockContext).buildConstraintViolationWithTemplate("UUID set must not be null");
      verify(mockBuilder).addConstraintViolation();
    }

    @Test
    void whenValueIsEmptyReturnsTrueWhenAllowEmptyIsTrue() {
      ValidUuidSet annotation = mock(ValidUuidSet.class);
      when(annotation.allowEmpty()).thenReturn(true);
      validator.initialize(annotation);
      boolean result = validator.isValid(List.of(), mockContext);
      assertThat(result).isTrue();
      verify(mockContext).disableDefaultConstraintViolation();
      verify(mockBuilder, times(0)).addConstraintViolation();
    }

    @Test
    void whenValueIsEmptyReturnsFalseWhenAllowEmptyIsFalse() {
      ValidUuidSet annotation = mock(ValidUuidSet.class);
      when(annotation.allowEmpty()).thenReturn(false);
      validator.initialize(annotation);
      boolean result = validator.isValid(List.of(), mockContext);
      assertThat(result).isFalse();
      verify(mockContext).buildConstraintViolationWithTemplate("UUID set must not be empty");
      verify(mockBuilder).addConstraintViolation();
    }

    @Test
    void whenCollectionContainsNullValuesReturnsFalseWithAppropriateMessage() {
      List<UUID> uuidList = Arrays.asList(UUID.randomUUID(), null, UUID.randomUUID());
      boolean result = validator.isValid(uuidList, mockContext);
      assertThat(result).isFalse();
      verify(mockContext).disableDefaultConstraintViolation();
      verify(mockContext).buildConstraintViolationWithTemplate("Value at index 1 is null");
      verify(mockBuilder).addConstraintViolation();
    }

    @Test
    void whenCollectionContainsNonUuidObjectsReturnsFalseWithAppropriateMessage() {
      List<Object> mixedList = List.of(UUID.randomUUID(), "not-a-uuid", UUID.randomUUID());
      boolean result = validator.isValid(mixedList, mockContext);
      assertThat(result).isFalse();
      verify(mockContext).disableDefaultConstraintViolation();
      verify(mockContext)
          .buildConstraintViolationWithTemplate("Value at index 1 is not a UUID object");
      verify(mockBuilder).addConstraintViolation();
    }

    @Test
    void whenCollectionContainsInvalidUuidsReturnsFalseForUuidsNotMatchingVersion3Or4() {
      // Custom UUID that doesn't match the pattern for version 3 or 4
      UUID invalidUuid = mock(UUID.class);
      when(invalidUuid.toString()).thenReturn("00000000-0000-2000-0000-000000000000"); // Version 2
      List<UUID> uuidList = List.of(UUID.randomUUID(), invalidUuid);
      boolean result = validator.isValid(uuidList, mockContext);
      assertThat(result).isFalse();
      verify(mockContext).disableDefaultConstraintViolation();
      verify(mockContext)
          .buildConstraintViolationWithTemplate(
              "Value '00000000-0000-2000-0000-000000000000' at index 1 is not a valid version 3 or version 4 UUID");
      verify(mockBuilder).addConstraintViolation();
    }

    @Test
    void whenCollectionContainsOnlyValidUuidsReturnsTrueForValidVersion4Uuids() {
      UUID uuid1 = UUID.randomUUID();
      UUID uuid2 = UUID.randomUUID();
      List<UUID> validList = List.of(uuid1, uuid2);
      boolean result = validator.isValid(validList, mockContext);
      assertThat(result).isTrue();
      verify(mockContext).disableDefaultConstraintViolation();
      verify(mockBuilder, times(0)).addConstraintViolation();
    }

    @Test
    void whenCollectionContainsOnlyValidUuidsReturnsTrueForValidVersion3Uuids() {
      UUID v3Uuid1 = mock(UUID.class);
      UUID v3Uuid2 = mock(UUID.class);
      when(v3Uuid1.toString()).thenReturn("00000000-0000-3000-8000-000000000000");
      when(v3Uuid2.toString()).thenReturn("11111111-1111-3111-a111-111111111111");
      List<UUID> validList = List.of(v3Uuid1, v3Uuid2);
      boolean result = validator.isValid(validList, mockContext);
      assertThat(result).isTrue();
      verify(mockContext).disableDefaultConstraintViolation();
      verify(mockBuilder, times(0)).addConstraintViolation();
    }

    @Test
    void whenCollectionContainsOnlyValidUuidsReturnsTrueForMixedVersion3And4Uuids() {
      UUID v4Uuid = UUID.randomUUID();
      UUID v3Uuid = mock(UUID.class);
      when(v3Uuid.toString()).thenReturn("00000000-0000-3000-8000-000000000000");
      List<UUID> validList = List.of(v4Uuid, v3Uuid);
      boolean result = validator.isValid(validList, mockContext);
      assertThat(result).isTrue();
      verify(mockContext).disableDefaultConstraintViolation();
      verify(mockBuilder, times(0)).addConstraintViolation();
    }
  }
}
