package net.flyingfishflash.loremlist.unit.core.serialization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import kotlinx.serialization.encoding.Decoder;
import net.flyingfishflash.loremlist.core.exceptions.CoreException;
import net.flyingfishflash.loremlist.core.serialization.UuidSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class UUIDSerializerTest {

  @Test
  void deserializeThrowsCoreExceptionWithBadRequestStatusForInvalidUuidString() {
    String invalidUuidString = "not-a-uuid";
    Decoder decoder = mock(Decoder.class);
    when(decoder.decodeString()).thenReturn(invalidUuidString);

    assertThatThrownBy(() -> UuidSerializer.INSTANCE.deserialize(decoder))
        .isInstanceOf(CoreException.class)
        .satisfies(
            thrown -> {
              CoreException exception = (CoreException) thrown;
              assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
              assertThat(exception.getMessage()).isEqualTo("Error deserializing UUID.");
              assertThat(exception.getCause()).isInstanceOf(IllegalArgumentException.class);
            });

    verify(decoder).decodeString();
  }
}
