package net.flyingfishflash.loremlist.unit.domain.lrmlistitem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import kotlin.Pair;
import kotlin.Triple;
import net.flyingfishflash.loremlist.domain.exceptions.DomainException;
import net.flyingfishflash.loremlist.domain.lrmitem.ItemNotFoundException;
import net.flyingfishflash.loremlist.domain.lrmitem.LrmItem;
import net.flyingfishflash.loremlist.domain.lrmitem.LrmItemRepository;
import net.flyingfishflash.loremlist.domain.lrmitem.LrmItemSuccinct;
import net.flyingfishflash.loremlist.domain.lrmitem.data.LrmItemCreate;
import net.flyingfishflash.loremlist.domain.lrmlist.ListNotFoundException;
import net.flyingfishflash.loremlist.domain.lrmlist.LrmList;
import net.flyingfishflash.loremlist.domain.lrmlist.LrmListRepository;
import net.flyingfishflash.loremlist.domain.lrmlist.LrmListSuccinct;
import net.flyingfishflash.loremlist.domain.lrmlistitem.ListItemNotFoundException;
import net.flyingfishflash.loremlist.domain.lrmlistitem.LrmListItem;
import net.flyingfishflash.loremlist.domain.lrmlistitem.LrmListItemRepository;
import net.flyingfishflash.loremlist.domain.lrmlistitem.LrmListItemServiceDefault;
import net.flyingfishflash.loremlist.persistence.SuccinctLrmComponentPair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class LrmListItemServiceTests {

  private final LrmListItemRepository mockLrmListItemRepository = mock(LrmListItemRepository.class);
  private final LrmItemRepository mockLrmItemRepository = mock(LrmItemRepository.class);
  private final LrmListRepository mockLrmListRepository = mock(LrmListRepository.class);
  private final LrmList mockLrmList = mock(LrmList.class);
  private final LrmItem mockLrmItem1 = mock(LrmItem.class);
  private final LrmItem mockLrmItem2 = mock(LrmItem.class);
  private final LrmListItem mockLrmListItem = mock(LrmListItem.class);

  private final LrmListItemServiceDefault lrmListItemService =
      new LrmListItemServiceDefault(
          mockLrmItemRepository, mockLrmListRepository, mockLrmListItemRepository);
  private final String owner = "lorem ipsum";

  private static DomainException catchDomainException(Runnable runnable) {
    try {
      runnable.run();
    } catch (DomainException exception) {
      return exception;
    }
    throw new AssertionError("Expected DomainException to be thrown");
  }

  @Nested
  class CountByOwnerAndListId {

    private final UUID listId = UUID.randomUUID();

    @Test
    void returnExpectedCountWhenListFound() {
      when(mockLrmListRepository.findByOwnerAndIdOrNull(eq(listId), eq(owner)))
          .thenReturn(mockLrmList);
      when(mockLrmListItemRepository.countByOwnerAndListId(eq(listId), eq(owner))).thenReturn(1L);
      var response = lrmListItemService.countByOwnerAndListId(listId, owner);
      assertThat(response.getContent()).isEqualTo(1L);
      assertThat(response.getMessage()).isEqualTo("List is associated with 1 items.");
    }

    @Test
    void throwExceptionWhenListNotFound() {
      when(mockLrmListRepository.findByOwnerAndIdOrNull(eq(listId), eq(owner))).thenReturn(null);
      DomainException exception =
          catchDomainException(() -> lrmListItemService.countByOwnerAndListId(listId, owner));
      assertThat(exception.getCause()).isInstanceOf(ListNotFoundException.class);
      assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void throwExceptionWhenListRepositoryThrowsAnException() {
      when(mockLrmListRepository.findByOwnerAndIdOrNull(eq(listId), eq("lorem ipsum")))
          .thenThrow(new RuntimeException("Error"));
      DomainException exception =
          catchDomainException(
              () -> lrmListItemService.countByOwnerAndListId(listId, "lorem ipsum"));
      assertThat(exception.getCause()).isInstanceOf(RuntimeException.class);
      assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @Nested
  class FindByOwnerAndItemIdAndListId {

    private final UUID listId = UUID.randomUUID();
    private final UUID itemId = UUID.randomUUID();

    @Test
    void returnCorrectListItemWhenValidItemIsFound() {
      when(mockLrmListItem.name()).thenReturn("Lorem List Item");
      when(mockLrmListRepository.findByOwnerAndIdOrNull(eq(listId), eq(owner)))
          .thenReturn(mockLrmList);
      when(mockLrmListItemRepository.findByOwnerAndItemIdAndListIdOrNull(
              eq(itemId), eq(listId), eq(owner)))
          .thenReturn(mockLrmListItem);
      var response = lrmListItemService.findByOwnerAndItemIdAndListId(itemId, listId, owner);
      assertThat(response.getContent()).isEqualTo(mockLrmListItem);
      assertThat(response.getMessage()).isEqualTo("Retrieved list item 'Lorem List Item'");
    }

    @Test
    void throwListNotFoundExceptionWhenListIsNotFound() {
      when(mockLrmListRepository.findByOwnerAndIdOrNull(eq(listId), eq(owner))).thenReturn(null);
      assertThatThrownBy(
              () -> lrmListItemService.findByOwnerAndItemIdAndListId(itemId, listId, owner))
          .isInstanceOf(ListNotFoundException.class);
    }

    @Test
    void throwItemNotFoundExceptionWhenItemIsNotFound() {
      when(mockLrmListRepository.findByOwnerAndIdOrNull(eq(listId), eq(owner)))
          .thenReturn(mockLrmList);
      when(mockLrmListItemRepository.findByOwnerAndItemIdAndListIdOrNull(
              eq(itemId), eq(listId), eq(owner)))
          .thenReturn(null);
      assertThatThrownBy(
              () -> lrmListItemService.findByOwnerAndItemIdAndListId(itemId, listId, owner))
          .isInstanceOf(ListItemNotFoundException.class);
    }
  }

  @Nested
  class Add {

    private final UUID listId = UUID.randomUUID();
    private final UUID itemId1 = UUID.randomUUID();
    private final UUID itemId2 = UUID.randomUUID();

    @Test
    void catchRethrowIllegalStateExceptionWhenItemIdsAreEmpty() {
      DomainException exception =
          catchDomainException(() -> lrmListItemService.add(listId, List.of(), owner));
      assertThat(exception.getCause()).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void catchRethrowListNotFoundExceptionWhenListNotFound() {
      when(mockLrmListRepository.findByOwnerAndIdOrNull(eq(listId), eq(owner))).thenReturn(null);
      DomainException exception =
          catchDomainException(() -> lrmListItemService.add(listId, List.of(itemId1), owner));
      assertThat(exception.getCause()).isInstanceOf(ListNotFoundException.class);
    }

    @Test
    void catchRethrowItemNotFoundExceptionWhenItemNotFound() {
      when(mockLrmListRepository.findByOwnerAndIdOrNull(eq(listId), eq(owner)))
          .thenReturn(mockLrmList);
      when(mockLrmItemRepository.notFoundByOwnerAndId(eq(List.of(itemId1)), eq(owner)))
          .thenReturn(Set.of(itemId1));
      DomainException exception =
          catchDomainException(() -> lrmListItemService.add(listId, List.of(itemId1), owner));
      assertThat(exception.getCause()).isInstanceOf(ItemNotFoundException.class);
    }

    @Nested
    class CreateAssociationsSuccessfully {

      @BeforeEach
      void setUp() {
        when(mockLrmListRepository.findByOwnerAndIdOrNull(eq(listId), eq(owner)))
            .thenReturn(mockLrmList);
        when(mockLrmItemRepository.findByOwnerAndIdOrNull(eq(itemId1), eq(owner)))
            .thenReturn(mockLrmItem1);
        when(mockLrmItem1.id()).thenReturn(itemId1);
        when(mockLrmItem1.name()).thenReturn("Item1");
        when(mockLrmItem2.id()).thenReturn(itemId2);
        when(mockLrmItem2.name()).thenReturn("Item2");
        when(mockLrmList.id()).thenReturn(listId);
        when(mockLrmList.name()).thenReturn("List1");
      }

      @Test
      void whenASingleItemIdIsProvided() {
        when(mockLrmItemRepository.notFoundByOwnerAndId(eq(List.of(itemId1)), eq(owner)))
            .thenReturn(Set.of());
        List<SuccinctLrmComponentPair> createdAssociations =
            List.of(
                new SuccinctLrmComponentPair(
                    LrmListSuccinct.Companion.fromLrmList(mockLrmList),
                    LrmItemSuccinct.Companion.fromLrmItem(mockLrmItem1)));
        when(mockLrmListItemRepository.create(any())).thenReturn(createdAssociations);
        var response = lrmListItemService.add(listId, List.of(itemId1), owner);
        assertThat(response.getMessage()).contains("to item");
        assertThat(response.getContent().items())
            .isEqualTo(List.of(LrmItemSuccinct.Companion.fromLrmItem(mockLrmItem1)));
      }

      @Test
      void whenMultipleItemIdsAreProvided() {
        when(mockLrmItemRepository.notFoundByOwnerAndId(eq(List.of(itemId1, itemId2)), eq(owner)))
            .thenReturn(Set.of());
        List<SuccinctLrmComponentPair> createdAssociations =
            List.of(
                new SuccinctLrmComponentPair(
                    LrmListSuccinct.Companion.fromLrmList(mockLrmList),
                    LrmItemSuccinct.Companion.fromLrmItem(mockLrmItem1)),
                new SuccinctLrmComponentPair(
                    LrmListSuccinct.Companion.fromLrmList(mockLrmList),
                    LrmItemSuccinct.Companion.fromLrmItem(mockLrmItem2)));
        when(mockLrmListItemRepository.create(any())).thenReturn(createdAssociations);
        var response = lrmListItemService.add(listId, List.of(itemId1, itemId2), owner);
        assertThat(response.getMessage()).contains("2 items");
        assertThat(response.getContent().items())
            .isEqualTo(
                List.of(
                    LrmItemSuccinct.Companion.fromLrmItem(mockLrmItem1),
                    LrmItemSuccinct.Companion.fromLrmItem(mockLrmItem2)));
      }
    }

    @Test
    void throwDomainExceptionWhenCreatedAssociationCountNotEqualToRequestedAssociationCount() {
      when(mockLrmList.id()).thenReturn(listId);
      when(mockLrmList.name()).thenReturn("LrmList");
      when(mockLrmItem1.id()).thenReturn(itemId1);
      when(mockLrmItem1.name()).thenReturn("LrmItem");
      when(mockLrmListRepository.findByOwnerAndIdOrNull(eq(listId), eq(owner)))
          .thenReturn(mockLrmList);
      when(mockLrmItemRepository.notFoundByOwnerAndId(eq(List.of(itemId1, itemId2)), eq(owner)))
          .thenReturn(Set.of());
      List<SuccinctLrmComponentPair> createdAssociations =
          List.of(
              new SuccinctLrmComponentPair(
                  LrmListSuccinct.Companion.fromLrmList(mockLrmList),
                  LrmItemSuccinct.Companion.fromLrmItem(mockLrmItem1)));
      when(mockLrmListItemRepository.create(any())).thenReturn(createdAssociations);
      DomainException exception =
          catchDomainException(
              () -> lrmListItemService.add(listId, List.of(itemId1, itemId2), owner));
      assertThat(exception.getMessage()).contains("created = 1 / requested = 2");
    }

    @Nested
    class CatchRethrowDomainExceptionWhenRepositoryThrowsSqlException {

      @BeforeEach
      void setUp() {
        when(mockLrmListRepository.findByOwnerAndIdOrNull(eq(listId), eq(owner)))
            .thenReturn(mockLrmList);
        when(mockLrmItemRepository.notFoundByOwnerAndId(eq(List.of(itemId1)), eq(owner)))
            .thenReturn(Set.of());
      }

      @Test
      void h2UniqueIndexOrPrimaryKeyViolation() {
        when(mockLrmListItemRepository.create(any()))
            .thenAnswer(
                invocation -> {
                  throw new SQLException("Unique index or primary key violation");
                });
        DomainException exception =
            catchDomainException(() -> lrmListItemService.add(listId, List.of(itemId1), owner));
        assertThat(exception.getCause()).isInstanceOf(SQLException.class);
        assertThat(exception.getResponseMessage()).contains("It already exists.");
      }

      @Test
      void pgDuplicateKeyValueViolatesUniqueConstraint() {
        when(mockLrmListItemRepository.create(any()))
            .thenAnswer(
                invocation -> {
                  throw new SQLException("duplicate key value violates unique constraint");
                });
        DomainException exception =
            catchDomainException(
                () -> lrmListItemService.add(listId, List.of(itemId1), "lorem ipsum"));
        assertThat(exception.getCause()).isInstanceOf(SQLException.class);
        assertThat(exception.getResponseMessage()).contains("It already exists.");
      }

      @Test
      void otherSqlException() {
        when(mockLrmListItemRepository.create(any()))
            .thenAnswer(
                invocation -> {
                  throw new SQLException("other sql exception");
                });
        DomainException exception =
            catchDomainException(
                () -> lrmListItemService.add(listId, List.of(itemId1), "lorem ipsum"));
        assertThat(exception.getCause()).isInstanceOf(SQLException.class);
        assertThat(exception.getResponseMessage()).contains("Unanticipated SQL exception");
      }
    }

    @Test
    void catchRethrowDomainExceptionWhenRepositoryThrowsRuntimeException() {
      when(mockLrmListItemRepository.create(any()))
          .thenThrow(new RuntimeException("Repository Exception"));
      DomainException exception =
          catchDomainException(
              () -> lrmListItemService.add(listId, List.of(itemId1), "lorem ipsum"));
      assertThat(exception.getCause()).isInstanceOf(RuntimeException.class);
      assertThat(exception.getResponseMessage()).contains("Could not create a new association");
    }
  }

  @Nested
  class Create {

    private final UUID listId = UUID.randomUUID();
    private final UUID itemId = UUID.randomUUID();
    private final SuccinctLrmComponentPair mockSuccinctLrmComponentPair =
        mock(SuccinctLrmComponentPair.class);
    private final LrmItemCreate lrmItemCreate = new LrmItemCreate("UZ4p3ClnTd", null, 0, false);
    private final String mockSuccinctItemName = "mock succinct item component name";
    private final String mockListName = "mock lorem list name";

    @BeforeEach
    void setUp() {
      when(mockLrmItemRepository.insert(any(LrmItem.class))).thenReturn(itemId);
    }

    @Test
    void createItemAndAssignItToAList() {
      LrmListItem realLrmListItem =
          new LrmListItem(
              itemId,
              listId,
              "Zmi22CTcJ4",
              "Lorem List Item Description",
              0,
              false,
              owner,
              kotlinx.datetime.Clock.System.INSTANCE.now(),
              owner,
              kotlinx.datetime.Clock.System.INSTANCE.now(),
              owner,
              Set.of());
      when(mockLrmItem1.id()).thenReturn(itemId);
      when(mockLrmItem1.name()).thenReturn(mockSuccinctItemName);
      when(mockLrmList.name()).thenReturn(mockListName);
      when(mockLrmItemRepository.findByOwnerAndIdOrNull(eq(itemId), eq(owner)))
          .thenReturn(mockLrmItem1);
      when(mockLrmListRepository.findByOwnerAndIdOrNull(eq(listId), eq(owner)))
          .thenReturn(mockLrmList);
      when(mockLrmItemRepository.notFoundByOwnerAndId(eq(List.of(itemId)), eq(owner)))
          .thenReturn(Set.of());
      when(mockLrmListItemRepository.create(Set.of(new Pair<>(listId, itemId))))
          .thenReturn(List.of(mockSuccinctLrmComponentPair));
      LrmItemSuccinct itemSuccinct = LrmItemSuccinct.Companion.fromLrmItem(mockLrmItem1);
      when(mockSuccinctLrmComponentPair.item()).thenReturn(itemSuccinct);
      when(mockLrmListItemRepository.findByOwnerAndItemIdAndListIdOrNull(
              eq(itemId), eq(listId), eq(owner)))
          .thenReturn(realLrmListItem);
      when(mockLrmListItemRepository.updateQuantity(any(LrmListItem.class))).thenReturn(1);
      when(mockLrmListItemRepository.updateIsItemSuppressed(any(LrmListItem.class))).thenReturn(1);

      var response = lrmListItemService.create(listId, lrmItemCreate, owner);
      assertThat(response.getMessage())
          .isEqualTo(
              "Created item '"
                  + mockSuccinctItemName
                  + "' and assigned it to list '"
                  + mockListName
                  + "'");
      assertThat(response.getContent()).isEqualTo(realLrmListItem);
    }

    @Test
    void itemCannotBeCreated() {
      when(mockLrmItemRepository.findByOwnerAndIdOrNull(eq(itemId), eq(owner))).thenReturn(null);
      DomainException exception =
          catchDomainException(() -> lrmListItemService.create(listId, lrmItemCreate, owner));
      assertThat(exception.getCause()).isInstanceOf(ItemNotFoundException.class);
    }
  }

  @Nested
  class RemoveByOwnerAndListIdAndItemId {

    private final UUID itemId = UUID.randomUUID();
    private final UUID listId = UUID.randomUUID();
    private final UUID listItemId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
      when(mockLrmItem1.name()).thenReturn("Item1");
      when(mockLrmList.name()).thenReturn("List1");
    }

    @Test
    void removeAnItemSuccessfullyFromTheList() {
      when(mockLrmItemRepository.findByOwnerAndIdOrNull(eq(itemId), eq(owner)))
          .thenReturn(mockLrmItem1);
      when(mockLrmListRepository.findByOwnerAndIdOrNull(eq(listId), eq(owner)))
          .thenReturn(mockLrmList);
      when(mockLrmListItemRepository.findByOwnerAndItemIdAndListIdOrNull(
              eq(itemId), eq(listId), eq(owner)))
          .thenReturn(mockLrmListItem);
      when(mockLrmListItemRepository.removeByOwnerAndListIdAndItemId(
              eq(listId), eq(itemId), eq(owner)))
          .thenReturn(1);
      var response = lrmListItemService.removeByOwnerAndListIdAndItemId(listId, itemId, owner);
      assertThat(response.getContent()).isEqualTo(new Pair<>("Item1", "List1"));
      assertThat(response.getMessage()).isEqualTo("Removed item 'Item1' from list 'List1'");
    }

    @Test
    void throwAnExceptionWhenTheItemIsNotFound() {
      when(mockLrmItemRepository.findByOwnerAndIdOrNull(eq(itemId), eq(owner))).thenReturn(null);
      DomainException exception =
          catchDomainException(
              () -> lrmListItemService.removeByOwnerAndListIdAndItemId(listId, itemId, owner));
      assertThat(exception.getCause()).isInstanceOf(ItemNotFoundException.class);
    }

    @Test
    void throwAnExceptionWhenTheListIsNotFound() {
      when(mockLrmItemRepository.findByOwnerAndIdOrNull(eq(itemId), eq(owner)))
          .thenReturn(mockLrmItem1);
      when(mockLrmListRepository.findByOwnerAndIdOrNull(eq(listId), eq(owner))).thenReturn(null);
      DomainException exception =
          catchDomainException(
              () -> lrmListItemService.removeByOwnerAndListIdAndItemId(listId, itemId, owner));
      assertThat(exception.getCause()).isInstanceOf(ListNotFoundException.class);
    }

    @Test
    void throwAnExceptionWhenNoAssociationIsFound() {
      when(mockLrmItemRepository.findByOwnerAndIdOrNull(eq(itemId), eq(owner)))
          .thenReturn(mockLrmItem1);
      when(mockLrmListRepository.findByOwnerAndIdOrNull(eq(listId), eq(owner)))
          .thenReturn(mockLrmList);
      when(mockLrmListItemRepository.findByOwnerAndItemIdAndListIdOrNull(
              eq(itemId), eq(listId), eq(owner)))
          .thenReturn(null);
      DomainException exception =
          catchDomainException(
              () -> lrmListItemService.removeByOwnerAndListIdAndItemId(listId, itemId, owner));
      assertThat(exception.getCause()).isInstanceOf(ListItemNotFoundException.class);
    }

    @Test
    void throwAnExceptionWhenARepositoryExceptionOccurs() {
      when(mockLrmListItem.id()).thenReturn(listItemId);
      when(mockLrmItemRepository.findByOwnerAndIdOrNull(eq(itemId), eq(owner)))
          .thenReturn(mockLrmItem1);
      when(mockLrmListRepository.findByOwnerAndIdOrNull(eq(listId), eq(owner)))
          .thenReturn(mockLrmList);
      when(mockLrmListItemRepository.findByOwnerAndItemIdAndListIdOrNull(
              eq(itemId), eq(listId), eq(owner)))
          .thenReturn(mockLrmListItem);
      when(mockLrmListItemRepository.removeByOwnerAndListIdAndItemId(
              eq(listId), eq(itemId), eq(owner)))
          .thenThrow(new RuntimeException("Repository Exception"));
      DomainException exception =
          catchDomainException(
              () -> lrmListItemService.removeByOwnerAndListIdAndItemId(listId, itemId, owner));
      assertThat(exception.getCause()).isInstanceOf(RuntimeException.class);
      assertThat(exception.getCause().getMessage()).isEqualTo("Repository Exception");
    }

    @Test
    void throwAnExceptionWhenNoRecordsAreDeleted() {
      when(mockLrmListItem.id()).thenReturn(listItemId);
      when(mockLrmItemRepository.findByOwnerAndIdOrNull(eq(itemId), eq(owner)))
          .thenReturn(mockLrmItem1);
      when(mockLrmListRepository.findByOwnerAndIdOrNull(eq(listId), eq(owner)))
          .thenReturn(mockLrmList);
      when(mockLrmListItemRepository.findByOwnerAndItemIdAndListIdOrNull(
              eq(itemId), eq(listId), eq(owner)))
          .thenReturn(mockLrmListItem);
      when(mockLrmListItemRepository.removeByOwnerAndListIdAndItemId(
              eq(listId), eq(itemId), eq(owner)))
          .thenReturn(0);
      assertThatThrownBy(
              () -> lrmListItemService.removeByOwnerAndListIdAndItemId(listId, itemId, owner))
          .isInstanceOf(DomainException.class);
    }

    @Test
    void throwAnExceptionWhenMoreThanOneRecordIsDeleted() {
      when(mockLrmItemRepository.findByOwnerAndIdOrNull(eq(itemId), eq(owner)))
          .thenReturn(mockLrmItem1);
      when(mockLrmListRepository.findByOwnerAndIdOrNull(eq(listId), eq(owner)))
          .thenReturn(mockLrmList);
      when(mockLrmListItemRepository.findByOwnerAndItemIdAndListIdOrNull(
              eq(itemId), eq(listId), eq(owner)))
          .thenReturn(mockLrmListItem);
      when(mockLrmListItemRepository.removeByOwnerAndListIdAndItemId(
              eq(listId), eq(itemId), eq(owner)))
          .thenReturn(2);
      assertThatThrownBy(
              () -> lrmListItemService.removeByOwnerAndListIdAndItemId(listId, itemId, owner))
          .isInstanceOf(DomainException.class);
    }
  }

  @Nested
  class RemoveByOwnerAndItemId {

    private final UUID itemId = UUID.randomUUID();

    @Test
    void removeItemsSuccessfullyFromTheList() {
      when(mockLrmItem1.name()).thenReturn("Item1");
      when(mockLrmItemRepository.findByOwnerAndIdOrNull(eq(itemId), eq(owner)))
          .thenReturn(mockLrmItem1);
      when(mockLrmListItemRepository.removeByOwnerAndItemId(eq(itemId), eq(owner))).thenReturn(3);
      var response = lrmListItemService.removeByOwnerAndItemId(itemId, owner);
      assertThat(response.getContent()).isEqualTo(new Pair<>("Item1", 3));
      assertThat(response.getMessage()).isEqualTo("Removed 'Item1' from 3 lists.");
    }

    @Test
    void returnCorrectResponseWhenOneItemIsRemovedFromMultipleLists() {
      when(mockLrmItem1.name()).thenReturn("Item1");
      when(mockLrmItemRepository.findByOwnerAndIdOrNull(eq(itemId), eq(owner)))
          .thenReturn(mockLrmItem1);
      when(mockLrmListItemRepository.removeByOwnerAndItemId(eq(itemId), eq(owner))).thenReturn(1);
      var response = lrmListItemService.removeByOwnerAndItemId(itemId, owner);
      assertThat(response.getContent()).isEqualTo(new Pair<>("Item1", 1));
      assertThat(response.getMessage()).isEqualTo("Removed 'Item1' from 1 list.");
    }

    @Test
    void throwAnExceptionIfTheItemIsNotFound() {
      when(mockLrmItemRepository.findByOwnerAndIdOrNull(eq(itemId), eq(owner))).thenReturn(null);
      DomainException exception =
          catchDomainException(() -> lrmListItemService.removeByOwnerAndItemId(itemId, owner));
      assertThat(exception.getCause()).isInstanceOf(ItemNotFoundException.class);
    }

    @Test
    void throwADomainExceptionWhenAnUnknownErrorOccurs() {
      when(mockLrmItemRepository.findByOwnerAndIdOrNull(eq(itemId), eq(owner)))
          .thenReturn(mockLrmItem1);
      when(mockLrmListItemRepository.removeByOwnerAndItemId(eq(itemId), eq(owner)))
          .thenThrow(new RuntimeException("Unknown error"));
      assertThatThrownBy(() -> lrmListItemService.removeByOwnerAndItemId(itemId, owner))
          .isInstanceOf(DomainException.class);
    }
  }

  @Nested
  class RemoveByOwnerAndListId {

    private final UUID listId = UUID.randomUUID();

    @Test
    void removeItemsSuccessfullyFromTheList() {
      when(mockLrmList.name()).thenReturn("List1");
      when(mockLrmListRepository.findByOwnerAndIdOrNull(eq(listId), eq(owner)))
          .thenReturn(mockLrmList);
      when(mockLrmListItemRepository.removeByOwnerAndListId(eq(listId), eq(owner))).thenReturn(3);
      var response = lrmListItemService.removeByOwnerAndListId(listId, owner);
      assertThat(response.getContent()).isEqualTo(new Pair<>("List1", 3));
      assertThat(response.getMessage()).isEqualTo("Removed 3 items from list 'List1'");
    }

    @Test
    void returnCorrectResponseWhenOneItemIsRemoved() {
      when(mockLrmList.name()).thenReturn("List1");
      when(mockLrmListRepository.findByOwnerAndIdOrNull(eq(listId), eq(owner)))
          .thenReturn(mockLrmList);
      when(mockLrmListItemRepository.removeByOwnerAndListId(eq(listId), eq(owner))).thenReturn(1);
      var response = lrmListItemService.removeByOwnerAndListId(listId, owner);
      assertThat(response.getContent()).isEqualTo(new Pair<>("List1", 1));
      assertThat(response.getMessage()).isEqualTo("Removed 1 item from list 'List1'");
    }

    @Test
    void throwAnExceptionIfTheListIsNotFound() {
      when(mockLrmListRepository.findByOwnerAndIdOrNull(eq(listId), eq(owner))).thenReturn(null);
      DomainException exception =
          catchDomainException(() -> lrmListItemService.removeByOwnerAndListId(listId, owner));
      assertThat(exception.getCause()).isInstanceOf(ListNotFoundException.class);
    }

    @Test
    void throwADomainExceptionWhenAnUnknownErrorOccurs() {
      when(mockLrmListRepository.findByOwnerAndIdOrNull(eq(listId), eq(owner)))
          .thenReturn(mockLrmList);
      when(mockLrmListItemRepository.removeByOwnerAndListId(eq(listId), eq(owner)))
          .thenThrow(new RuntimeException("Unknown error"));
      assertThatThrownBy(() -> lrmListItemService.removeByOwnerAndListId(listId, owner))
          .isInstanceOf(DomainException.class);
    }
  }

  @Nested
  class Move {

    private final UUID itemId = UUID.randomUUID();
    private final UUID currentListId = UUID.randomUUID();
    private final UUID destinationListId = UUID.randomUUID();
    private final LrmList mockCurrentList = mock(LrmList.class);
    private final LrmList mockDestinationList = mock(LrmList.class);

    @Test
    void moveTheItemFromTheCurrentListToTheDestinationListSuccessfully() {
      when(mockLrmItem1.name()).thenReturn("Item1");
      when(mockCurrentList.name()).thenReturn("CurrentList");
      when(mockDestinationList.name()).thenReturn("DestinationList");
      when(mockLrmItemRepository.findByOwnerAndIdOrNull(eq(itemId), eq(owner)))
          .thenReturn(mockLrmItem1);
      when(mockLrmListRepository.findByOwnerAndIdOrNull(eq(currentListId), eq(owner)))
          .thenReturn(mockCurrentList);
      when(mockLrmListRepository.findByOwnerAndIdOrNull(eq(destinationListId), eq(owner)))
          .thenReturn(mockDestinationList);
      when(mockLrmListItemRepository.findByOwnerAndItemIdAndListIdOrNull(
              eq(itemId), eq(currentListId), eq(owner)))
          .thenReturn(mockLrmListItem);
      when(mockLrmListItemRepository.updateListId(eq(mockLrmListItem), eq(destinationListId)))
          .thenReturn(1);
      var response = lrmListItemService.move(itemId, currentListId, destinationListId, owner);
      assertThat(response.getContent())
          .isEqualTo(new Triple<>("Item1", "CurrentList", "DestinationList"));
      assertThat(response.getMessage())
          .isEqualTo("Moved item 'Item1' from list 'CurrentList' to list 'DestinationList'");
    }

    @Test
    void throwAnExceptionWhenTheItemIsNotFound() {
      when(mockLrmItemRepository.findByOwnerAndIdOrNull(eq(itemId), eq(owner))).thenReturn(null);
      DomainException exception =
          catchDomainException(
              () -> lrmListItemService.move(itemId, currentListId, destinationListId, owner));
      assertThat(exception.getCause()).isInstanceOf(ItemNotFoundException.class);
    }

    @Test
    void throwAnExceptionWhenTheCurrentListIsNotFound() {
      when(mockLrmItemRepository.findByOwnerAndIdOrNull(eq(itemId), eq(owner)))
          .thenReturn(mockLrmItem1);
      when(mockLrmListRepository.findByOwnerAndIdOrNull(eq(currentListId), eq(owner)))
          .thenReturn(null);
      DomainException exception =
          catchDomainException(
              () -> lrmListItemService.move(itemId, currentListId, destinationListId, owner));
      assertThat(exception.getCause()).isInstanceOf(ListNotFoundException.class);
    }

    @Test
    void throwAnExceptionWhenTheDestinationListIsNotFound() {
      when(mockLrmItemRepository.findByOwnerAndIdOrNull(eq(itemId), eq(owner)))
          .thenReturn(mockLrmItem1);
      when(mockLrmListRepository.findByOwnerAndIdOrNull(eq(currentListId), eq(owner)))
          .thenReturn(mockCurrentList);
      when(mockLrmListRepository.findByOwnerAndIdOrNull(eq(destinationListId), eq(owner)))
          .thenReturn(null);
      DomainException exception =
          catchDomainException(
              () -> lrmListItemService.move(itemId, currentListId, destinationListId, owner));
      assertThat(exception.getCause()).isInstanceOf(ListNotFoundException.class);
    }

    @Test
    void throwAnExceptionWhenNoAssociationIsFoundBetweenTheItemAndTheCurrentList() {
      when(mockLrmItemRepository.findByOwnerAndIdOrNull(eq(itemId), eq(owner)))
          .thenReturn(mockLrmItem1);
      when(mockLrmListRepository.findByOwnerAndIdOrNull(eq(currentListId), eq(owner)))
          .thenReturn(mockCurrentList);
      when(mockLrmListRepository.findByOwnerAndIdOrNull(eq(destinationListId), eq(owner)))
          .thenReturn(mockDestinationList);
      when(mockLrmListItemRepository.findByOwnerAndItemIdAndListIdOrNull(
              eq(itemId), eq(currentListId), eq(owner)))
          .thenReturn(null);
      DomainException exception =
          catchDomainException(
              () -> lrmListItemService.move(itemId, currentListId, destinationListId, owner));
      assertThat(exception.getCause()).isInstanceOf(ListItemNotFoundException.class);
    }

    @Test
    void throwAnExceptionWhenAnUnexpectedErrorOccurs() {
      when(mockLrmItemRepository.findByOwnerAndIdOrNull(eq(itemId), eq(owner)))
          .thenReturn(mockLrmItem1);
      when(mockLrmListRepository.findByOwnerAndIdOrNull(eq(currentListId), eq(owner)))
          .thenReturn(mockCurrentList);
      when(mockLrmListRepository.findByOwnerAndIdOrNull(eq(destinationListId), eq(owner)))
          .thenReturn(mockDestinationList);
      when(mockLrmListItemRepository.findByOwnerAndItemIdAndListIdOrNull(
              eq(itemId), eq(currentListId), eq(owner)))
          .thenReturn(mockLrmListItem);
      when(mockLrmListItemRepository.updateListId(eq(mockLrmListItem), eq(destinationListId)))
          .thenThrow(new RuntimeException("Unknown error"));
      DomainException exception =
          catchDomainException(
              () -> lrmListItemService.move(itemId, currentListId, destinationListId, owner));
      assertThat(exception.getCause()).isInstanceOf(RuntimeException.class);
    }
  }

  @Nested
  class CountByOwnerAndItemId {

    private final UUID itemId = UUID.randomUUID();

    @Test
    void returnTheExpectedCountOfListAssociationsWhenItemIsFound() {
      long expectedCount = 5L;
      when(mockLrmItemRepository.findByOwnerAndIdOrNull(eq(itemId), eq(owner)))
          .thenReturn(mockLrmItem1);
      when(mockLrmListItemRepository.countByOwnerAndItemId(eq(itemId), eq(owner)))
          .thenReturn(expectedCount);
      var response = lrmListItemService.countByOwnerAndItemId(itemId, owner);
      assertThat(response.getContent()).isEqualTo(expectedCount);
      assertThat(response.getMessage())
          .isEqualTo("Item is associated with " + expectedCount + " lists.");
    }

    @Test
    void throwExceptionWhenItemNotFound() {
      when(mockLrmItemRepository.findByOwnerAndIdOrNull(eq(itemId), eq(owner))).thenReturn(null);
      DomainException exception =
          catchDomainException(() -> lrmListItemService.countByOwnerAndItemId(itemId, owner));
      assertThat(exception.getCause()).isInstanceOf(ItemNotFoundException.class);
      assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void throwExceptionWhenItemRepositoryThrowsAnException() {
      when(mockLrmListRepository.findByOwnerAndIdOrNull(eq(itemId), eq("lorem ipsum")))
          .thenThrow(new RuntimeException("Error"));
      DomainException exception =
          catchDomainException(
              () -> lrmListItemService.countByOwnerAndListId(itemId, "lorem ipsum"));
      assertThat(exception.getCause()).isInstanceOf(RuntimeException.class);
      assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }
}
