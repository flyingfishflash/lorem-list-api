package net.flyingfishflash.loremlist.unit.domain.lrmlistitem;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.UUID;
import net.flyingfishflash.loremlist.domain.lrmlistitem.ListItemNotFoundException;
import org.junit.jupiter.api.Test;

class ListItemNotFoundTests {

  @Test
  void idIsNull() {
    ListItemNotFoundException exception = new ListItemNotFoundException();
    assertThat(exception.getResponseMessage()).isEqualTo("ListItem could not be found.");
  }

  @Test
  void idIsNotNull() {
    UUID id = UUID.randomUUID();
    ListItemNotFoundException exception = new ListItemNotFoundException(id);
    assertThat(exception.getResponseMessage()).isEqualTo("ListItem could not be found.");
  }

  @Test
  void customMessage() {
    ListItemNotFoundException exception = new ListItemNotFoundException(Set.of(), "Lorem Ipsum");
    assertThat(exception.getMessage()).isEqualTo("Lorem Ipsum");
    assertThat(exception.getResponseMessage()).isEqualTo("Lorem Ipsum");
  }
}
