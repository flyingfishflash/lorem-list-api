package net.flyingfishflash.loremlist.unit.domain.lrmitem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import kotlin.Pair;
import kotlinx.datetime.Clock;
import kotlinx.datetime.Instant;
import net.flyingfishflash.loremlist.domain.ServiceResponse;
import net.flyingfishflash.loremlist.domain.exceptions.DomainException;
import net.flyingfishflash.loremlist.domain.lrmitem.ItemNotFoundException;
import net.flyingfishflash.loremlist.domain.lrmitem.LrmItem;
import net.flyingfishflash.loremlist.domain.lrmitem.LrmItemRepository;
import net.flyingfishflash.loremlist.domain.lrmitem.LrmItemServiceDefault;
import net.flyingfishflash.loremlist.domain.lrmitem.data.LrmItemCreate;
import net.flyingfishflash.loremlist.domain.lrmlist.ListNotFoundException;
import net.flyingfishflash.loremlist.domain.lrmlist.LrmList;
import net.flyingfishflash.loremlist.domain.lrmlist.LrmListService;
import net.flyingfishflash.loremlist.domain.lrmlist.LrmListSuccinct;
import net.flyingfishflash.loremlist.domain.lrmlistitem.LrmListItemService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class LrmItemServiceTests {

  private final LrmItemRepository mockLrmItemRepository = mock(LrmItemRepository.class);
  private final LrmListItemService mockLrmListItemService = mock(LrmListItemService.class);
  private final LrmListService mockLrmListService = mock(LrmListService.class);
  private final LrmItemServiceDefault lrmItemService =
      new LrmItemServiceDefault(
          mockLrmItemRepository, mockLrmListService, mockLrmListItemService, new ObjectMapper());

  private final Instant now = Clock.System.INSTANCE.now();
  private final UUID id0 = UUID.fromString("00000000-0000-4000-a000-000000000000");
  private final UUID id1 = UUID.fromString("00000000-0000-4000-a000-000000000001");
  private final LrmItemCreate lrmItemCreate =
      new LrmItemCreate("Lorem Item Name", "Lorem Item Description", 0, false);
  private final String irrelevantMessage =
      "ksADs8y96KRa1Zo4ipMdr5t8faudmFj4c564S02MjsNG6TXEO7yctC08Bb53bCB7";

  private LrmItem lrmItem() {
    return new LrmItem(
        id0,
        lrmItemCreate.getName(),
        lrmItemCreate.getDescription(),
        "Lorem Ipsum Owner",
        now,
        "Lorem Ipsum Created By",
        now,
        "Lorem Ipsum Updated By",
        Set.of());
  }

  private LrmList lrmList() {
    return new LrmList(
        id1,
        "Lorem List Name",
        "Lorem List Description",
        false,
        "Lorem Ipsum Owner",
        now,
        "Lorem Ipsum Created By",
        now,
        "Lorem Ipsum Updated By",
        Set.of());
  }

  private LrmItem lrmItemWithLists() {
    return lrmItem().withLists(Set.of(new LrmListSuccinct(id0, "Lorem List Name")));
  }

  private SQLException sqlExceptionGeneric() {
    return new SQLException("Cause of SQLException");
  }

  @Nested
  class CountByOwner {

    @Test
    void countIsReturned() {
      when(mockLrmItemRepository.countByOwner(anyString())).thenReturn(999L);
      assertThat(lrmItemService.countByOwner("lorem ipsum"))
          .isEqualTo(new ServiceResponse<>(999L, "999 items."));
      verify(mockLrmItemRepository).countByOwner(anyString());
    }

    @Test
    void itemRepositoryThrowsException() {
      when(mockLrmItemRepository.countByOwner(anyString()))
          .thenThrow(new RuntimeException("Lorem Ipsum"));
      DomainException exception =
          catchDomainException(() -> lrmItemService.countByOwner("lorem ipsum"));
      assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
      verify(mockLrmItemRepository).countByOwner(anyString());
    }
  }

  @Nested
  class DeleteAllByOwner {

    @Test
    void allItemsDeleted() {
      // the '999' values in the repository and association service are not used to calculate the
      // number of items deleted or the number of lists an item was associated with
      ServiceResponse<Pair<String, Integer>> mockAssociationServiceResponse =
          new ServiceResponse<>(new Pair<>("item name", 999), irrelevantMessage);
      when(mockLrmItemRepository.findByOwner(anyString())).thenReturn(List.of(lrmItemWithLists()));
      when(mockLrmListItemService.removeByOwnerAndItemId(any(UUID.class), anyString()))
          .thenReturn(mockAssociationServiceResponse);
      when(mockLrmItemRepository.deleteById(any())).thenReturn(999);

      var serviceResponse = lrmItemService.deleteByOwner("lorem ipsum");
      assertThat(serviceResponse.getContent().itemNames()).hasSize(1);
      assertThat(serviceResponse.getContent().associatedListNames()).hasSize(1);
      assertThat(serviceResponse.getMessage())
          .isEqualTo("Deleted all (1) of your items, and removed them from 1 lists.");
      verify(mockLrmItemRepository).findByOwner(anyString());
      verify(mockLrmListItemService).removeByOwnerAndItemId(any(UUID.class), anyString());
      verify(mockLrmItemRepository).deleteById(any());
    }

    @Test
    void noListsDeletedWhenNonePresent() {
      ServiceResponse<Pair<String, Integer>> mockAssociationServiceResponse =
          new ServiceResponse<>(new Pair<>("item name", 999), irrelevantMessage);
      when(mockLrmItemRepository.findByOwner(anyString())).thenReturn(List.of());
      when(mockLrmListItemService.removeByOwnerAndItemId(any(UUID.class), anyString()))
          .thenReturn(mockAssociationServiceResponse);
      when(mockLrmItemRepository.deleteById(any())).thenReturn(999);

      var serviceResponse = lrmItemService.deleteByOwner("lorem ipsum");
      assertThat(serviceResponse.getContent().itemNames()).isEmpty();
      assertThat(serviceResponse.getContent().associatedListNames()).isEmpty();
      assertThat(serviceResponse.getMessage())
          .isEqualTo("Deleted all (0) of your items, and removed them from 0 lists.");
      verify(mockLrmItemRepository).findByOwner(anyString());
      verify(mockLrmListItemService, never()).removeByOwnerAndItemId(any(UUID.class), anyString());
      verify(mockLrmItemRepository, never()).deleteById(any());
    }

    @Test
    void noListsDeletedWhenApiException() {
      when(mockLrmItemRepository.findByOwner(anyString()))
          .thenThrow(new RuntimeException("Lorem Ipsum"));
      DomainException exception =
          catchDomainException(() -> lrmItemService.deleteByOwner("lorem ipsum"));
      assertThat(exception.getMessage()).contains("No items were deleted");
      assertThat(exception.getMessage()).contains("Items could not be retrieved");
      verify(mockLrmItemRepository).findByOwner(anyString());
    }
  }

  @Nested
  class DeleteByIdAndOwner {

    @Test
    void itemNotFound() {
      when(mockLrmItemRepository.findByOwnerAndIdOrNull(eq(id1), anyString())).thenReturn(null);
      DomainException exception =
          catchDomainException(() -> lrmItemService.deleteByOwnerAndId(id1, "lorem ipsum", false));
      assertThat(exception.getCause()).isInstanceOf(ItemNotFoundException.class);
      assertThat(exception.getResponseMessage())
          .isEqualTo("Item id " + id1 + " could not be deleted: Item could not be found.");
      verify(mockLrmItemRepository).findByOwnerAndIdOrNull(any(UUID.class), anyString());
    }

    @Nested
    class AssociatedLists {

      @Test
      void itemIsDeletedWhenRemoveListAssociationsIsTrue() {
        ServiceResponse<Pair<String, Integer>> mockAssociationServiceResponse =
            new ServiceResponse<>(new Pair<>(lrmItem().name(), 999), irrelevantMessage);
        when(mockLrmItemRepository.findByOwnerAndIdOrNull(eq(id1), anyString()))
            .thenReturn(lrmItemWithLists());
        when(mockLrmListItemService.removeByOwnerAndItemId(eq(id1), anyString()))
            .thenReturn(mockAssociationServiceResponse);
        when(mockLrmItemRepository.deleteByOwnerAndId(eq(id1), anyString())).thenReturn(1);

        var serviceResponse = lrmItemService.deleteByOwnerAndId(id1, "lorem ipsum", true);
        assertThat(serviceResponse.getContent().itemNames())
            .isEqualTo(List.of(lrmItemWithLists().name()));
        assertThat(serviceResponse.getContent().associatedListNames())
            .isEqualTo(List.of("Lorem List Name"));
        assertThat(serviceResponse.getMessage())
            .isEqualTo(
                "Deleted item '"
                    + lrmItemWithLists().name()
                    + "', and removed it from "
                    + lrmItemWithLists().lists().size()
                    + " list.");
        verify(mockLrmItemRepository).findByOwnerAndIdOrNull(any(UUID.class), anyString());
        verify(mockLrmListItemService).removeByOwnerAndItemId(any(UUID.class), anyString());
        verify(mockLrmItemRepository).deleteByOwnerAndId(any(UUID.class), anyString());
      }

      @Test
      void itemIsNotDeletedWhenRemoveListAssociationsIsFalse() {
        when(mockLrmItemRepository.findByOwnerAndIdOrNull(eq(id1), anyString()))
            .thenReturn(lrmItemWithLists());
        DomainException exception =
            catchDomainException(
                () -> lrmItemService.deleteByOwnerAndId(id1, "lorem ipsum", false));
        assertThat(exception.getSupplemental()).isNotNull();
        assertThat(exception.getSupplemental()).hasSize(2);
        assertThat(exception.getMessage()).containsIgnoringCase(lrmItemWithLists().name());
        assertThat(exception.getMessage()).containsIgnoringCase("could not be deleted");
        assertThat(exception.getMessage()).containsIgnoringCase("is associated with");
        assertThat(exception.getResponseMessage()).containsIgnoringCase(lrmItemWithLists().name());
        assertThat(exception.getResponseMessage()).containsIgnoringCase("could not be deleted");
        assertThat(exception.getResponseMessage()).containsIgnoringCase("is associated with");
      }

      @Test
      void itemRepositoryReturnsMoreThanOneDeletedRecord() {
        ServiceResponse<Pair<String, Integer>> mockAssociationServiceResponse =
            new ServiceResponse<>(new Pair<>("item name", 999), irrelevantMessage);
        when(mockLrmItemRepository.findByOwnerAndIdOrNull(eq(id1), anyString()))
            .thenReturn(lrmItem());
        when(mockLrmListItemService.removeByOwnerAndItemId(eq(id1), anyString()))
            .thenReturn(mockAssociationServiceResponse);
        when(mockLrmItemRepository.deleteByOwnerAndId(eq(id1), anyString())).thenReturn(2);

        DomainException exception =
            catchDomainException(() -> lrmItemService.deleteByOwnerAndId(id1, "lorem ipsum", true));
        assertThat(exception.getCause()).isInstanceOf(DomainException.class);
        assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(exception.getMessage()).containsIgnoringCase(id1.toString());
        assertThat(exception.getMessage()).containsIgnoringCase("could not be deleted");
        assertThat(exception.getMessage()).containsIgnoringCase("more than one");
        assertThat(exception.getResponseMessage()).containsIgnoringCase(id1.toString());
        assertThat(exception.getResponseMessage()).containsIgnoringCase("could not be deleted");
        assertThat(exception.getResponseMessage()).containsIgnoringCase("more than one");
      }
    }

    @Nested
    class NoAssociatedLists {

      @Test
      void itemIsDeleted() {
        when(mockLrmItemRepository.findByOwnerAndIdOrNull(eq(id1), anyString()))
            .thenReturn(lrmItem());
        when(mockLrmItemRepository.deleteByOwnerAndId(eq(id1), anyString())).thenReturn(1);

        var serviceResponse = lrmItemService.deleteByOwnerAndId(id1, "lorem ipsum", false);
        assertThat(serviceResponse.getContent().itemNames()).isEqualTo(List.of(lrmItem().name()));
        assertThat(serviceResponse.getContent().associatedListNames()).isEmpty();
        assertThat(serviceResponse.getMessage())
            .isEqualTo(
                "Deleted item '"
                    + lrmItem().name()
                    + "', and removed it from "
                    + lrmItem().lists().size()
                    + " lists.");
        verify(mockLrmItemRepository).findByOwnerAndIdOrNull(any(UUID.class), anyString());
        verify(mockLrmItemRepository).deleteByOwnerAndId(eq(id1), anyString());
      }

      @Test
      void itemRepositoryReturnsMoreThanOneDeletedRecord() {
        ServiceResponse<Pair<String, Integer>> mockAssociationServiceResponse =
            new ServiceResponse<>(new Pair<>("item name", 999), irrelevantMessage);
        when(mockLrmItemRepository.findByOwnerAndIdOrNull(eq(id1), anyString()))
            .thenReturn(lrmItem());
        when(mockLrmListItemService.removeByOwnerAndItemId(eq(id1), anyString()))
            .thenReturn(mockAssociationServiceResponse);
        when(mockLrmItemRepository.deleteByOwnerAndId(eq(id1), anyString())).thenReturn(2);

        DomainException exception =
            catchDomainException(
                () -> lrmItemService.deleteByOwnerAndId(id1, "lorem ipsum", false));
        assertThat(exception.getCause()).isInstanceOf(DomainException.class);
        assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(exception.getMessage()).containsIgnoringCase(id1.toString());
        assertThat(exception.getMessage()).containsIgnoringCase("could not be deleted");
        assertThat(exception.getMessage()).containsIgnoringCase("more than one");
        assertThat(exception.getResponseMessage()).containsIgnoringCase(id1.toString());
        assertThat(exception.getResponseMessage()).containsIgnoringCase("could not be deleted");
        assertThat(exception.getResponseMessage()).containsIgnoringCase("more than one");
        verify(mockLrmItemRepository).findByOwnerAndIdOrNull(any(UUID.class), anyString());
      }
    }
  }

  @Nested
  class FindAllByOwner {

    @Test
    void allAreReturned() {
      String owner = "lorem ipsum";
      when(mockLrmItemRepository.findByOwner(anyString())).thenReturn(List.of(lrmItem()));
      var serviceResponse = lrmItemService.findByOwner(owner);
      assertThat(serviceResponse.getContent()).isEqualTo(List.of(lrmItem()));
      assertThat(serviceResponse.getMessage())
          .isEqualTo("Retrieved all items owned by " + owner + ".");
      verify(mockLrmItemRepository).findByOwner(anyString());
    }

    @Test
    void itemRepositoryThrowsException() {
      when(mockLrmItemRepository.findByOwner(anyString()))
          .thenThrow(new RuntimeException("Lorem Ipsum"));
      DomainException exception =
          catchDomainException(() -> lrmItemService.findByOwner("lorem ipsum"));
      assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
      assertThat(exception.getCause()).isInstanceOf(Exception.class);
      assertThat(exception.getMessage()).isEqualTo("Items could not be retrieved.");
      assertThat(exception.getResponseMessage()).isEqualTo("Items could not be retrieved.");
    }
  }

  @Nested
  class FindByIdAndOwner {

    @Test
    void itemAndListsAreReturned() {
      when(mockLrmItemRepository.findByOwnerAndIdOrNull(eq(id1), anyString()))
          .thenReturn(lrmItemWithLists());
      var serviceResponse = lrmItemService.findByOwnerAndId(id1, "lorem ipsum");
      assertThat(serviceResponse.getContent()).isEqualTo(lrmItemWithLists());
      assertThat(serviceResponse.getMessage())
          .isEqualTo("Retrieved item '" + lrmItemWithLists().name() + "'");
      verify(mockLrmItemRepository).findByOwnerAndIdOrNull(eq(id1), anyString());
    }

    @Test
    void itemIsNotReturned() {
      when(mockLrmItemRepository.findByOwnerAndIdOrNull(eq(id1), anyString())).thenReturn(null);
      assertThatThrownBy(() -> lrmItemService.findByOwnerAndId(id1, "lorem ipsum"))
          .isInstanceOf(ItemNotFoundException.class);
    }

    @Test
    void itemRepositoryThrowsException() {
      when(mockLrmItemRepository.findByOwnerAndIdOrNull(eq(id1), anyString()))
          .thenThrow(new RuntimeException("Lorem Ipsum"));
      DomainException exception =
          catchDomainException(() -> lrmItemService.findByOwnerAndId(id1, "lorem ipsum"));
      assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
      assertThat(exception.getCause()).isInstanceOf(Exception.class);
      assertThat(exception.getMessage()).containsIgnoringCase(id1.toString());
      assertThat(exception.getMessage()).containsIgnoringCase("could not be retrieved");
      assertThat(exception.getResponseMessage()).containsIgnoringCase(id1.toString());
      assertThat(exception.getResponseMessage()).containsIgnoringCase("could not be retrieved");
    }
  }

  @Nested
  class FindByOwnerWithNoLists {

    @Test
    void itemsAreReturned() {
      when(mockLrmItemRepository.findByOwnerAndHavingNoListAssociations(anyString()))
          .thenReturn(List.of(lrmItem()));
      var serviceResponse = lrmItemService.findByOwnerAndHavingNoListAssociations("lorem ipsum");
      assertThat(serviceResponse.getContent()).isEqualTo(List.of(lrmItem()));
      assertThat(serviceResponse.getMessage())
          .isEqualTo("Retrieved 1 items that are not a part of a list.");
      verify(mockLrmItemRepository).findByOwnerAndHavingNoListAssociations(anyString());
    }

    @Test
    void itemRepositoryThrowsException() {
      when(mockLrmItemRepository.findByOwnerAndHavingNoListAssociations(anyString()))
          .thenThrow(new RuntimeException("Lorem Ipsum"));
      DomainException exception =
          catchDomainException(
              () -> lrmItemService.findByOwnerAndHavingNoListAssociations("lorem ipsum"));
      assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
      assertThat(exception.getMessage()).containsIgnoringCase("could not be retrieved");
    }
  }

  @Nested
  class FindByOwnerAndHavingNoListAssociationsForList {

    @Test
    void eligibleItemsAreReturned() {
      when(mockLrmListService.findByOwnerAndId(eq(id1), anyString()))
          .thenReturn(new ServiceResponse<>(lrmList(), "Lorem Ipsum"));
      when(mockLrmItemRepository.findByOwnerAndHavingNoListAssociations(anyString(), eq(id1)))
          .thenReturn(List.of(lrmItem()));
      var serviceResponse =
          lrmItemService.findByOwnerAndHavingNoListAssociations("lorem ipsum", id1);
      assertThat(serviceResponse.getContent()).isEqualTo(List.of(lrmItem()));
      assertThat(serviceResponse.getMessage())
          .contains("Retrieved 1 items eligible to be added to list");
      verify(mockLrmItemRepository).findByOwnerAndHavingNoListAssociations(anyString(), eq(id1));
    }

    @Test
    void listServiceThrowsListNotFoundException() {
      when(mockLrmListService.findByOwnerAndId(eq(id1), anyString()))
          .thenThrow(new ListNotFoundException());
      DomainException exception =
          catchDomainException(
              () -> lrmItemService.findByOwnerAndHavingNoListAssociations("lorem ipsum", id1));
      assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
      assertThat(exception.getMessage()).containsIgnoringCase("could not be retrieved");
      assertThat(exception.getMessage()).containsIgnoringCase("list could not be found");
    }

    @Test
    void listServiceThrowsException() {
      when(mockLrmListService.findByOwnerAndId(eq(id1), anyString()))
          .thenThrow(new RuntimeException("Lorem Ipsum"));
      DomainException exception =
          catchDomainException(
              () -> lrmItemService.findByOwnerAndHavingNoListAssociations("lorem ipsum", id1));
      assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
      assertThat(exception.getMessage()).containsIgnoringCase("could not be retrieved");
      assertThat(exception.getMessage()).containsIgnoringCase("lorem ipsum");
    }

    @Test
    void itemRepositoryThrowsException() {
      when(mockLrmListService.findByOwnerAndId(eq(id0), anyString()))
          .thenReturn(new ServiceResponse<>(lrmList(), "Lorem Ipsum"));
      when(mockLrmItemRepository.findByOwnerAndHavingNoListAssociations(anyString(), eq(id0)))
          .thenThrow(new RuntimeException("Lorem Ipsum"));
      DomainException exception =
          catchDomainException(
              () -> lrmItemService.findByOwnerAndHavingNoListAssociations("lorem ipsum", id0));
      assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
      assertThat(exception.getMessage()).containsIgnoringCase("could not be retrieved");
      assertThat(exception.getMessage()).containsIgnoringCase("lorem ipsum");
    }
  }

  @Nested
  class PatchFields {

    @Test
    void moreThanOneItemNameUpdated() {
      when(mockLrmItemRepository.updateName(any(LrmItem.class))).thenReturn(2);
      DomainException exception = catchDomainException(() -> lrmItemService.patchName(lrmItem()));
      assertThat(exception.getMessage()).contains(lrmItem().id().toString());
    }

    @Test
    void exactlyOneItemNameUpdated() {
      when(mockLrmItemRepository.updateName(any(LrmItem.class))).thenReturn(1);
      lrmItemService.patchName(lrmItem());
      verify(mockLrmItemRepository).updateName(any(LrmItem.class));
    }

    @Test
    void lessThanOneItemNameUpdated() {
      when(mockLrmItemRepository.updateName(any(LrmItem.class))).thenReturn(0);
      DomainException exception = catchDomainException(() -> lrmItemService.patchName(lrmItem()));
      assertThat(exception.getMessage()).contains("No item affected");
    }

    @Test
    void moreThanOneItemDescriptionUpdated() {
      when(mockLrmItemRepository.updateDescription(any(LrmItem.class))).thenReturn(2);
      assertThatThrownBy(() -> lrmItemService.patchDescription(lrmItem()))
          .isInstanceOf(DomainException.class);
    }

    @Test
    void exactlyOneItemDescriptionUpdated() {
      when(mockLrmItemRepository.updateDescription(any(LrmItem.class))).thenReturn(1);
      lrmItemService.patchDescription(lrmItem());
      verify(mockLrmItemRepository).updateDescription(any(LrmItem.class));
    }

    @Test
    void lessThanOneItemDescriptionUpdated() {
      when(mockLrmItemRepository.updateDescription(any(LrmItem.class))).thenReturn(0);
      assertThatThrownBy(() -> lrmItemService.patchDescription(lrmItem()))
          .isInstanceOf(DomainException.class);
    }

    @Test
    void itemRepositoryThrowsSqlException() {
      when(mockLrmItemRepository.updateName(any(LrmItem.class)))
          .thenAnswer(
              invocation -> {
                throw sqlExceptionGeneric();
              });
      assertThatThrownBy(() -> lrmItemService.patchName(lrmItem()))
          .isInstanceOf(SQLException.class)
          .hasMessageContaining("SQLException");
      verify(mockLrmItemRepository).updateName(any(LrmItem.class));
    }
  }

  private static DomainException catchDomainException(Runnable runnable) {
    try {
      runnable.run();
    } catch (DomainException exception) {
      return exception;
    }
    throw new AssertionError("Expected DomainException to be thrown");
  }
}
