package net.flyingfishflash.loremlist.unit.domain.lrmlist;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.UUID;
import net.flyingfishflash.loremlist.domain.lrmlist.ListNotFoundException;
import org.junit.jupiter.api.Test;

class LrmListNotFoundTests {

  @Test
  void primaryConstructorDefaultMessage() {
    UUID id = UUID.randomUUID();
    ListNotFoundException exception = new ListNotFoundException(id);
    assertThat(exception.getMessage()).isEqualTo("List could not be found.");
  }

  @Test
  void primaryConstructorNonDefaultMessage() {
    ListNotFoundException exception = new ListNotFoundException(UUID.randomUUID(), "Lorem Ipsum");
    assertThat(exception.getMessage()).isEqualTo("Lorem Ipsum");
  }

  @Test
  void secondaryConstructorDefaultMessageSetSizeOne() {
    Set<UUID> ids = Set.of(UUID.randomUUID());
    ListNotFoundException exception = new ListNotFoundException(ids);
    assertThat(exception.getMessage()).isEqualTo("List could not be found.");
  }

  @Test
  void secondaryConstructorDefaultMessageSetSizeGreaterThanOne() {
    Set<UUID> ids = Set.of(UUID.randomUUID(), UUID.randomUUID());
    ListNotFoundException exception = new ListNotFoundException(ids);
    assertThat(exception.getMessage()).isEqualTo("Lists (2) could not be found.");
  }

  @Test
  void secondaryConstructorNonDefaultMessage() {
    ListNotFoundException exception =
        new ListNotFoundException(Set.of(UUID.randomUUID()), "Lorem Ipsum");
    assertThat(exception.getMessage()).isEqualTo("Lorem Ipsum");
  }
}
