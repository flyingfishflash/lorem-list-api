package net.flyingfishflash.loremlist.unit.domain.lrmlist;

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
import jakarta.validation.ConstraintViolationException;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import kotlin.Pair;
import kotlinx.datetime.Clock;
import kotlinx.datetime.Instant;
import net.flyingfishflash.loremlist.domain.ServiceResponse;
import net.flyingfishflash.loremlist.domain.exceptions.DomainException;
import net.flyingfishflash.loremlist.domain.lrmlist.ListNotFoundException;
import net.flyingfishflash.loremlist.domain.lrmlist.LrmList;
import net.flyingfishflash.loremlist.domain.lrmlist.LrmListRepository;
import net.flyingfishflash.loremlist.domain.lrmlist.LrmListServiceDefault;
import net.flyingfishflash.loremlist.domain.lrmlist.data.LrmListCreate;
import net.flyingfishflash.loremlist.domain.lrmlist.data.LrmListDeleted;
import net.flyingfishflash.loremlist.domain.lrmlistitem.LrmListItem;
import net.flyingfishflash.loremlist.domain.lrmlistitem.LrmListItemService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class LrmListServiceTests {

  private final LrmListRepository mockLrmListRepository = mock(LrmListRepository.class);
  private final LrmListItemService mockLrmListItemService = mock(LrmListItemService.class);
  private final LrmListServiceDefault lrmListService =
      new LrmListServiceDefault(mockLrmListRepository, mockLrmListItemService, new ObjectMapper());

  private final LrmListCreate lrmListCreate =
      new LrmListCreate("Lorem List Name", "Lorem List Description", true);
  private final UUID id0 = UUID.fromString("00000000-0000-4000-a000-000000000000");
  private final UUID id1 = UUID.fromString("00000000-0000-4000-a000-000000000001");
  private final UUID listId0 = UUID.fromString("00000000-0000-4000-a100-000000000000");
  private final Instant now = Clock.System.INSTANCE.now();
  private final String mockUserName = "mockUserName";
  private final String irrelevantMessage =
      "ksADs8y96KRa1Zo4ipMdr5t8faudmFj4c564S02MjsNG6TXEO7yctC08Bb53bCB7";

  private LrmList lrmList() {
    return new LrmList(
        id0,
        lrmListCreate.getName(),
        lrmListCreate.getDescription(),
        lrmListCreate.getPublic(),
        "Lorem Ipsum Owner",
        now,
        "Lorem Ipsum Created By",
        now,
        "Lorem Ipsum Updated By",
        Set.of());
  }

  private LrmList lrmListWithItems() {
    return lrmList()
        .withItems(
            Set.of(
                new LrmListItem(
                    id0,
                    listId0,
                    "Lorem Item Name",
                    "Lorem Ipsum Description",
                    0,
                    false,
                    "Lorem Ipsum Owner",
                    now,
                    "Lorem Ipsum Created By",
                    now,
                    "Lorem Ipsum Updated By",
                    Set.of())));
  }

  private SQLException sqlExceptionGeneric() {
    return new SQLException("Cause of SQLException");
  }

  private static DomainException catchDomainException(Runnable runnable) {
    try {
      runnable.run();
    } catch (DomainException exception) {
      return exception;
    }
    throw new AssertionError("Expected DomainException to be thrown");
  }

  @Nested
  class CountByOwner {

    @Test
    void countIsReturned() {
      when(mockLrmListRepository.countByOwner(anyString())).thenReturn(999L);
      var serviceResponse = lrmListService.countByOwner("lorem ipsum");
      assertThat(serviceResponse.getContent()).isEqualTo(999L);
      assertThat(serviceResponse.getMessage()).isEqualTo("999 lists.");
      verify(mockLrmListRepository).countByOwner(anyString());
    }

    @Test
    void listRepositoryThrowsException() {
      when(mockLrmListRepository.countByOwner(anyString()))
          .thenThrow(new RuntimeException("Lorem Ipsum"));
      DomainException exception =
          catchDomainException(() -> lrmListService.countByOwner("lorem ipsum"));
      assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
      verify(mockLrmListRepository).countByOwner(anyString());
    }
  }

  @Nested
  class Create {

    @Test
    void listRepositoryReturnsInsertedListId() {
      when(mockLrmListRepository.insert(any(LrmList.class))).thenReturn(id1);
      when(mockLrmListRepository.findByOwnerAndIdOrNull(eq(id1), anyString()))
          .thenReturn(lrmList());
      var serviceResponse = lrmListService.create(lrmListCreate, mockUserName);
      assertThat(serviceResponse.getContent()).isEqualTo(lrmList());
      assertThat(serviceResponse.getMessage()).isEqualTo("Created list '" + lrmList().name() + "'");
      verify(mockLrmListRepository).insert(any(LrmList.class));
      verify(mockLrmListRepository).findByOwnerAndIdOrNull(any(UUID.class), anyString());
    }

    @Test
    void listRepositoryThrowsSqlException() {
      when(mockLrmListRepository.insert(any(LrmList.class)))
          .thenAnswer(
              invocation -> {
                throw sqlExceptionGeneric();
              });
      DomainException exception =
          catchDomainException(() -> lrmListService.create(lrmListCreate, mockUserName));
      assertThat(exception.getCause()).isInstanceOf(SQLException.class);
      assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
      assertThat(exception.getMessage()).isEqualTo("List could not be created.");
      assertThat(exception.getResponseMessage()).isEqualTo("List could not be created.");
      assertThat(exception.getTitle()).isEqualTo(DomainException.class.getSimpleName());
    }
  }

  @Nested
  class DeleteAllByOwner {

    @Test
    void allListsDeleted() {
      ServiceResponse<Pair<String, Integer>> mockAssociationServiceResponse =
          new ServiceResponse<>(new Pair<>("Lorem Ipsum", 999), irrelevantMessage);
      when(mockLrmListRepository.findByOwner(anyString())).thenReturn(List.of(lrmListWithItems()));
      when(mockLrmListItemService.removeByOwnerAndListId(any(UUID.class), anyString()))
          .thenReturn(mockAssociationServiceResponse);
      when(mockLrmListRepository.deleteById(any())).thenReturn(999);

      var serviceResponse = lrmListService.deleteByOwner(mockUserName);
      assertThat(serviceResponse.getContent().listNames()).hasSize(1);
      assertThat(serviceResponse.getContent().associatedItemNames()).hasSize(1);
      assertThat(serviceResponse.getMessage())
          .isEqualTo("Deleted all (1) of your lists, and disassociated 1 items.");
      verify(mockLrmListRepository).findByOwner(anyString());
      verify(mockLrmListItemService).removeByOwnerAndListId(any(UUID.class), anyString());
      verify(mockLrmListRepository).deleteById(any());
    }

    @Test
    void noListsDeletedWhenNonePresent() {
      when(mockLrmListRepository.findByOwner(anyString())).thenReturn(List.of());
      when(mockLrmListRepository.deleteById(any())).thenReturn(0);
      var serviceResponse = lrmListService.deleteByOwner(mockUserName);
      assertThat(serviceResponse.getContent().listNames()).isEmpty();
      assertThat(serviceResponse.getContent().associatedItemNames()).isEmpty();
      assertThat(serviceResponse.getMessage())
          .isEqualTo("Deleted all (0) of your lists, and disassociated 0 items.");
      verify(mockLrmListRepository).findByOwner(anyString());
      verify(mockLrmListItemService, never()).removeByOwnerAndListId(any(UUID.class), anyString());
      verify(mockLrmListRepository, never()).deleteById(any());
    }

    @Test
    void noListsDeletedWhenApiException() {
      when(mockLrmListRepository.findByOwner(anyString()))
          .thenThrow(new RuntimeException("Lorem Ipsum"));
      DomainException exception =
          catchDomainException(() -> lrmListService.deleteByOwner(mockUserName));
      assertThat(exception.getMessage()).contains("No lists were deleted");
      assertThat(exception.getMessage()).contains("could not be retrieved");
      verify(mockLrmListRepository).findByOwner(anyString());
      verify(mockLrmListItemService, never()).removeByOwnerAndListId(any(UUID.class), anyString());
      verify(mockLrmListRepository, never()).deleteById(any());
    }
  }

  @Nested
  class DeleteByIdAndOwner {

    @Test
    void listNotFound() {
      when(mockLrmListRepository.findByOwnerAndIdOrNull(any(UUID.class), anyString()))
          .thenReturn(null);
      DomainException exception =
          catchDomainException(() -> lrmListService.deleteByOwnerAndId(id1, "lorem list", false));
      assertThat(exception.getCause()).isInstanceOf(ListNotFoundException.class);
      assertThat(exception.getResponseMessage())
          .isEqualTo("List could not be deleted: List could not be found.");
      verify(mockLrmListRepository).findByOwnerAndIdOrNull(any(UUID.class), anyString());
    }

    @Nested
    class AssociatedItems {

      @Test
      void listIsDeletedWhenRemoveItemAssociationsIsTrue() {
        ServiceResponse<Pair<String, Integer>> mockAssociationServiceResponse =
            new ServiceResponse<>(new Pair<>(lrmList().name(), 999), irrelevantMessage);
        when(mockLrmListRepository.findByOwnerAndIdOrNull(eq(id1), anyString()))
            .thenReturn(lrmListWithItems());
        when(mockLrmListItemService.removeByOwnerAndListId(any(UUID.class), anyString()))
            .thenReturn(mockAssociationServiceResponse);
        when(mockLrmListRepository.deleteByOwnerAndId(eq(id1), anyString())).thenReturn(1);

        var serviceResponse = lrmListService.deleteByOwnerAndId(id1, "lorem ipsum", true);
        assertThat(serviceResponse.getContent().listNames()).isEqualTo(List.of(lrmList().name()));
        assertThat(serviceResponse.getContent().associatedItemNames())
            .isEqualTo(lrmListWithItems().items().stream().map(LrmListItem::name).toList());
        assertThat(serviceResponse.getMessage())
            .isEqualTo("Deleted list 'Lorem List Name', and disassociated 1 item.");
        verify(mockLrmListRepository).findByOwnerAndIdOrNull(any(UUID.class), anyString());
        verify(mockLrmListItemService).removeByOwnerAndListId(any(UUID.class), anyString());
        verify(mockLrmListRepository).deleteByOwnerAndId(eq(id1), anyString());
      }

      @Test
      void listIsNotDeletedWhenRemoveItemAssociationsIsFalse() {
        when(mockLrmListRepository.findByOwnerAndIdOrNull(eq(id1), anyString()))
            .thenReturn(lrmListWithItems());
        DomainException exception =
            catchDomainException(() -> lrmListService.deleteByOwnerAndId(id1, "lorem list", false));
        assertThat(exception.getSupplemental()).hasSize(2);
        assertThat(exception.getSupplemental().get("listNames"))
            .isEqualTo(new ObjectMapper().valueToTree(List.of(lrmListWithItems().name())));
        assertThat(exception.getSupplemental().get("associatedItemNames"))
            .isEqualTo(
                new ObjectMapper()
                    .valueToTree(
                        lrmListWithItems().items().stream().map(LrmListItem::name).toList()));
        assertThat(exception.getMessage()).containsIgnoringCase(lrmList().name());
        assertThat(exception.getMessage()).containsIgnoringCase("could not be deleted");
        assertThat(exception.getMessage()).containsIgnoringCase("includes");
      }

      @Test
      void listRepositoryReturnsMoreThanOneDeletedRecord() {
        ServiceResponse<Pair<String, Integer>> mockAssociationServiceResponse =
            new ServiceResponse<>(new Pair<>("Lorem Ipsum", 999), irrelevantMessage);
        when(mockLrmListRepository.findByOwnerAndIdOrNull(eq(id1), anyString()))
            .thenReturn(lrmListWithItems());
        when(mockLrmListItemService.removeByOwnerAndListId(any(UUID.class), anyString()))
            .thenReturn(mockAssociationServiceResponse);
        when(mockLrmListRepository.deleteByOwnerAndId(eq(id1), anyString())).thenReturn(2);

        DomainException exception =
            catchDomainException(() -> lrmListService.deleteByOwnerAndId(id1, "lorem ipsum", true));
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
    class NoAssociatedItems {

      @Test
      void listIsDeleted() {
        when(mockLrmListRepository.findByOwnerAndIdOrNull(eq(id1), anyString()))
            .thenReturn(lrmList());
        when(mockLrmListRepository.deleteByOwnerAndId(eq(id1), anyString())).thenReturn(1);
        var serviceResponse = lrmListService.deleteByOwnerAndId(id1, "lorem list", false);
        assertThat(serviceResponse.getContent())
            .isEqualTo(new LrmListDeleted(List.of(lrmList().name()), List.of()));
        assertThat(serviceResponse.getMessage())
            .isEqualTo("Deleted list '" + lrmList().name() + "', and disassociated 0 items.");
        verify(mockLrmListRepository).findByOwnerAndIdOrNull(any(UUID.class), anyString());
        verify(mockLrmListRepository).deleteByOwnerAndId(eq(id1), anyString());
      }

      @Test
      void listRepositoryReturnsMoreThanOneDeletedRecord() {
        ServiceResponse<Pair<String, Integer>> mockAssociationServiceResponse =
            new ServiceResponse<>(new Pair<>("Lorem Ipsum", 999), irrelevantMessage);
        when(mockLrmListRepository.findByOwnerAndIdOrNull(eq(id1), anyString()))
            .thenReturn(lrmList());
        when(mockLrmListItemService.removeByOwnerAndListId(eq(id1), anyString()))
            .thenReturn(mockAssociationServiceResponse);
        when(mockLrmListRepository.deleteByOwnerAndId(eq(id1), anyString())).thenReturn(2);

        DomainException exception =
            catchDomainException(() -> lrmListService.deleteByOwnerAndId(id1, "lorem list", false));
        assertThat(exception.getCause()).isInstanceOf(DomainException.class);
        assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(exception.getMessage()).containsIgnoringCase(id1.toString());
        assertThat(exception.getMessage()).containsIgnoringCase("could not be deleted");
        assertThat(exception.getMessage()).containsIgnoringCase("more than one");
        assertThat(exception.getResponseMessage()).containsIgnoringCase(id1.toString());
        assertThat(exception.getResponseMessage()).containsIgnoringCase("could not be deleted");
        assertThat(exception.getResponseMessage()).containsIgnoringCase("more than one");
        verify(mockLrmListRepository).findByOwnerAndIdOrNull(any(UUID.class), anyString());
      }
    }
  }

  @Nested
  class FindAllByOwnerIncludeItems {

    @Test
    void listsAreReturned() {
      when(mockLrmListRepository.findByOwner(anyString())).thenReturn(List.of(lrmList()));
      var serviceResponse = lrmListService.findByOwner(mockUserName);
      assertThat(serviceResponse.getContent()).isEqualTo(List.of(lrmList()));
      assertThat(serviceResponse.getMessage())
          .isEqualTo("Retrieved all lists owned by '" + mockUserName + "'");
      verify(mockLrmListRepository).findByOwner(anyString());
    }

    @Test
    void listRepositoryThrowsException() {
      when(mockLrmListRepository.findByOwner(anyString()))
          .thenThrow(new RuntimeException("Lorem Ipsum"));
      DomainException exception =
          catchDomainException(() -> lrmListService.findByOwner(mockUserName));
      assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
      assertThat(exception.getCause()).isInstanceOf(Exception.class);
      assertThat(exception.getMessage())
          .isEqualTo("Lists (including associated items) could not be retrieved.");
      assertThat(exception.getResponseMessage())
          .isEqualTo("Lists (including associated items) could not be retrieved.");
    }
  }

  @Nested
  class FindByPublic {

    @Test
    void listsAreReturned() {
      when(mockLrmListRepository.findByPublic()).thenReturn(List.of(lrmList()));
      var serviceResponse = lrmListService.findByPublic();
      assertThat(serviceResponse.getContent()).isEqualTo(List.of(lrmList()));
      assertThat(serviceResponse.getMessage()).isEqualTo("Retrieved 1 public lists.");
      verify(mockLrmListRepository).findByPublic();
    }

    @Test
    void listRepositoryThrowsException() {
      when(mockLrmListRepository.findByPublic()).thenThrow(new RuntimeException("Lorem Ipsum"));
      DomainException exception = catchDomainException(lrmListService::findByPublic);
      assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
      assertThat(exception.getCause()).isInstanceOf(Exception.class);
      assertThat(exception.getMessage())
          .isEqualTo("Public lists (including associated items) could not be retrieved.");
      assertThat(exception.getResponseMessage())
          .isEqualTo("Public lists (including associated items) could not be retrieved.");
    }
  }

  @Nested
  class FindByIdAndOwnerIncludeItems {

    @Test
    void listIsFoundAndReturned() {
      when(mockLrmListRepository.findByOwnerAndIdOrNull(eq(id1), anyString()))
          .thenReturn(lrmList());
      var serviceResponse = lrmListService.findByOwnerAndId(id1, "lorem ipsum");
      assertThat(serviceResponse.getContent()).isEqualTo(lrmList());
      assertThat(serviceResponse.getMessage())
          .isEqualTo("Retrieved list '" + lrmList().name() + "'");
      verify(mockLrmListRepository).findByOwnerAndIdOrNull(eq(id1), anyString());
    }

    @Test
    void listIsNotFound() {
      when(mockLrmListRepository.findByOwnerAndIdOrNull(eq(id1), anyString())).thenReturn(null);
      assertThatThrownBy(() -> lrmListService.findByOwnerAndId(id1, "lorem ipsum"))
          .isInstanceOf(ListNotFoundException.class);
    }

    @Test
    void listRepositoryThrowsException() {
      when(mockLrmListRepository.findByOwnerAndIdOrNull(eq(id1), anyString()))
          .thenThrow(new RuntimeException("Lorem Ipsum"));
      DomainException exception =
          catchDomainException(() -> lrmListService.findByOwnerAndId(id1, "lorem ipsum"));
      assertThat(exception.getCause()).isInstanceOf(Exception.class);
      assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
      assertThat(exception.getMessage()).containsIgnoringCase(id1.toString());
      assertThat(exception.getMessage()).containsIgnoringCase("including associated items");
      assertThat(exception.getMessage()).containsIgnoringCase("could not be retrieved");
      assertThat(exception.getResponseMessage()).containsIgnoringCase(id1.toString());
      assertThat(exception.getResponseMessage()).containsIgnoringCase("including associated items");
      assertThat(exception.getResponseMessage()).containsIgnoringCase("could not be retrieved");
      assertThat(exception.getTitle()).isEqualTo(DomainException.class.getSimpleName());
    }
  }

  @Nested
  class FindByOwnerWithNoItems {

    @Test
    void itemsAreReturned() {
      when(mockLrmListRepository.findByOwnerAndHavingNoItemAssociations(anyString()))
          .thenReturn(List.of(lrmList()));
      var serviceResponse = lrmListService.findByOwnerAndHavingNoItemAssociations("lorem ipsum");
      assertThat(serviceResponse.getContent()).isEqualTo(List.of(lrmList()));
      assertThat(serviceResponse.getMessage()).isEqualTo("Retrieved 1 lists that have no items.");
      verify(mockLrmListRepository).findByOwnerAndHavingNoItemAssociations(anyString());
    }

    @Test
    void itemRepositoryThrowsException() {
      when(mockLrmListRepository.findByOwnerAndHavingNoItemAssociations(anyString()))
          .thenThrow(new RuntimeException("Lorem Ipsum"));
      DomainException exception =
          catchDomainException(
              () -> lrmListService.findByOwnerAndHavingNoItemAssociations("lorem ipsum"));
      assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
      assertThat(exception.getMessage()).containsIgnoringCase("could not be retrieved");
    }
  }

  @Nested
  class PatchFields {

    @Test
    void moreThanOneListNameUpdated() {
      when(mockLrmListRepository.updateName(any(LrmList.class))).thenReturn(2);
      DomainException exception = catchDomainException(() -> lrmListService.patchName(lrmList()));
      assertThat(exception.getMessage()).contains(lrmList().id().toString());
    }

    @Test
    void exactlyOneListNameUpdated() {
      when(mockLrmListRepository.updateName(any(LrmList.class))).thenReturn(1);
      lrmListService.patchName(lrmList());
      verify(mockLrmListRepository).updateName(any(LrmList.class));
    }

    @Test
    void lessThanOneListNameUpdated() {
      when(mockLrmListRepository.updateName(any(LrmList.class))).thenReturn(0);
      DomainException exception = catchDomainException(() -> lrmListService.patchName(lrmList()));
      assertThat(exception.getMessage()).contains("No list affected");
    }

    @Test
    void moreThanOneListDescriptionUpdated() {
      when(mockLrmListRepository.updateDescription(any(LrmList.class))).thenReturn(2);
      DomainException exception =
          catchDomainException(() -> lrmListService.patchDescription(lrmList()));
      assertThat(exception.getMessage()).contains(lrmList().id().toString());
    }

    @Test
    void exactlyOneListDescriptionUpdated() {
      when(mockLrmListRepository.updateDescription(any(LrmList.class))).thenReturn(1);
      lrmListService.patchDescription(lrmList());
      verify(mockLrmListRepository).updateDescription(any(LrmList.class));
    }

    @Test
    void lessThanOneListDescriptionUpdated() {
      when(mockLrmListRepository.updateDescription(any(LrmList.class))).thenReturn(0);
      DomainException exception =
          catchDomainException(() -> lrmListService.patchDescription(lrmList()));
      assertThat(exception.getMessage()).contains("No list affected");
    }

    @Test
    void moreThanOneListIsPublicIndicatorUpdated() {
      when(mockLrmListRepository.updateIsPublic(any(LrmList.class))).thenReturn(2);
      DomainException exception =
          catchDomainException(() -> lrmListService.patchIsPublic(lrmList()));
      assertThat(exception.getMessage()).contains(lrmList().id().toString());
    }

    @Test
    void exactlyOneListIsPublicIndicatorUpdated() {
      when(mockLrmListRepository.updateIsPublic(any(LrmList.class))).thenReturn(1);
      lrmListService.patchIsPublic(lrmList());
      verify(mockLrmListRepository).updateIsPublic(any(LrmList.class));
    }

    @Test
    void lessThanOneListIsPublicIndicatorUpdated() {
      when(mockLrmListRepository.updateIsPublic(any(LrmList.class))).thenReturn(0);
      DomainException exception =
          catchDomainException(() -> lrmListService.patchIsPublic(lrmList()));
      assertThat(exception.getMessage()).contains("No list affected");
    }

    @Test
    void updateNameToAllSpaces() {
      LrmList patchedLrmList = lrmList().withName("   ");
      assertThatThrownBy(() -> lrmListService.patchName(patchedLrmList))
          .isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void listRepositoryThrowsSqlException() {
      when(mockLrmListRepository.updateName(any(LrmList.class)))
          .thenAnswer(
              invocation -> {
                throw sqlExceptionGeneric();
              });
      assertThatThrownBy(() -> lrmListService.patchName(lrmList()))
          .isInstanceOf(SQLException.class)
          .hasMessageContaining("SQLException");
      verify(mockLrmListRepository).updateName(any(LrmList.class));
    }
  }
}
