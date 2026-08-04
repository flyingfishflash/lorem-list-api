package net.flyingfishflash.loremlist.unit.domain.lrmitem;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.UUID;
import net.flyingfishflash.loremlist.domain.lrmitem.ItemNotFoundException;
import org.junit.jupiter.api.Test;

class LrmItemNotFoundTests {

  @Test
  void primaryConstructorDefaultMessage() {
    UUID id = UUID.randomUUID();
    ItemNotFoundException exception = new ItemNotFoundException(id);
    assertThat(exception.getMessage()).isEqualTo("Item could not be found.");
  }

  @Test
  void primaryConstructorNonDefaultMessage() {
    ItemNotFoundException exception = new ItemNotFoundException(UUID.randomUUID(), "Lorem Ipsum");
    assertThat(exception.getMessage()).isEqualTo("Lorem Ipsum");
  }

  @Test
  void secondaryConstructorDefaultMessageSetSizeOne() {
    Set<UUID> ids = Set.of(UUID.randomUUID());
    ItemNotFoundException exception = new ItemNotFoundException(ids);
    assertThat(exception.getMessage()).isEqualTo("Item could not be found.");
  }

  @Test
  void secondaryConstructorDefaultMessageSetSizeGreaterThanOne() {
    Set<UUID> ids = Set.of(UUID.randomUUID(), UUID.randomUUID());
    ItemNotFoundException exception = new ItemNotFoundException(ids);
    assertThat(exception.getMessage()).isEqualTo("Items (2) could not be found.");
  }

  @Test
  void secondaryConstructorNonDefaultMessage() {
    ItemNotFoundException exception =
        new ItemNotFoundException(Set.of(UUID.randomUUID()), "Lorem Ipsum");
    assertThat(exception.getMessage()).isEqualTo("Lorem Ipsum");
  }
}
