package net.flyingfishflash.loremlist.domain.lrmlistitem;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.IntSupplier;
import kotlin.Pair;
import kotlin.Triple;
import kotlinx.datetime.Clock;
import kotlinx.datetime.Instant;
import net.flyingfishflash.loremlist.core.exceptions.CoreException;
import net.flyingfishflash.loremlist.domain.LrmComponentType;
import net.flyingfishflash.loremlist.domain.ServiceResponse;
import net.flyingfishflash.loremlist.domain.SuccinctLrmComponent;
import net.flyingfishflash.loremlist.domain.exceptions.DomainException;
import net.flyingfishflash.loremlist.domain.exceptions.EntityNotFoundException;
import net.flyingfishflash.loremlist.domain.lrmitem.ItemNotFoundException;
import net.flyingfishflash.loremlist.domain.lrmitem.LrmItem;
import net.flyingfishflash.loremlist.domain.lrmitem.LrmItemRepository;
import net.flyingfishflash.loremlist.domain.lrmitem.data.LrmItemCreate;
import net.flyingfishflash.loremlist.domain.lrmlist.ListNotFoundException;
import net.flyingfishflash.loremlist.domain.lrmlist.LrmList;
import net.flyingfishflash.loremlist.domain.lrmlist.LrmListRepository;
import net.flyingfishflash.loremlist.domain.lrmlistitem.data.LrmListItemAdded;
import net.flyingfishflash.loremlist.persistence.SuccinctLrmComponentPair;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class LrmListItemServiceDefault implements LrmListItemService {

  private final LrmItemRepository lrmItemRepository;
  private final LrmListRepository lrmListRepository;
  private final LrmListItemRepository lrmListItemRepository;
  private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

  public LrmListItemServiceDefault(
      LrmItemRepository lrmItemRepository,
      LrmListRepository lrmListRepository,
      LrmListItemRepository lrmListItemRepository) {
    this.lrmItemRepository = lrmItemRepository;
    this.lrmListRepository = lrmListRepository;
    this.lrmListItemRepository = lrmListItemRepository;
  }

  @Override
  public ServiceResponse<Long> countByOwnerAndListId(UUID listId, String owner) {
    String exceptionMessage =
        "Count of items associated with list id " + listId + " could not be retrieved";
    long associations;
    try {
      if (lrmListRepository.findByOwnerAndIdOrNull(listId, owner) == null) {
        throw new ListNotFoundException(listId);
      }
      associations = lrmListItemRepository.countByOwnerAndListId(listId, owner);
    } catch (Exception exception) {
      HttpStatus httpStatus =
          exception instanceof ListNotFoundException e ? e.getHttpStatus() : null;
      String detail = exception instanceof ListNotFoundException ? exception.getMessage() : "";
      throw DomainException.builder()
          .cause(exception)
          .httpStatus(httpStatus)
          .message(exceptionMessage + ": " + detail)
          .build();
    }
    return new ServiceResponse<>(
        associations, "List is associated with " + associations + " items.");
  }

  /** Associate a single list with an arbitrary number of items */
  @Override
  public ServiceResponse<LrmListItemAdded> add(
      UUID id, List<UUID> idCollection, String componentsOwner) {
    String exceptionMessage = "Could not create a new association";
    LrmComponentType type = LrmComponentType.List;

    try {
      if (idCollection.isEmpty()) {
        throw new IllegalStateException("id collection argument must not be an empty list.");
      }
      LrmListItemAdded listItemCreated = addToList(id, componentsOwner, idCollection);

      String message;
      if (listItemCreated.items().size() <= 1) {
        message =
            "Assigned "
                + type.name().toLowerCase()
                + " '"
                + listItemCreated.listName()
                + "' to "
                + type.invert().name().toLowerCase()
                + " '"
                + listItemCreated.items().get(0).name()
                + "'";
      } else {
        message =
            "Assigned "
                + type.name().toLowerCase()
                + " '"
                + listItemCreated.listName()
                + "' to "
                + listItemCreated.items().size()
                + " "
                + type.invert().name().toLowerCase()
                + "s.";
      }

      return new ServiceResponse<>(listItemCreated, message);
    } catch (CoreException exception) {
      throw DomainException.builder()
          .cause(exception)
          .httpStatus(exception.getHttpStatus())
          .message(exceptionMessage + ": " + exception.getMessage())
          .supplemental(exception.getSupplemental())
          .build();
    } catch (Exception exception) {
      // JdbcClient translates a unique constraint violation into a DuplicateKeyException; the
      // SQLException checks below remain as a fallback for exceptions raised directly by a
      // repository (e.g. in tests).
      if (exception
          instanceof org.springframework.dao.DuplicateKeyException duplicateKeyException) {
        throw DomainException.builder()
            .cause(duplicateKeyException)
            .httpStatus(HttpStatus.UNPROCESSABLE_ENTITY)
            .message(exceptionMessage + ": It already exists.")
            .build();
      }
      if (exception instanceof SQLException sqlException) {
        String sqlMessage = sqlException.getMessage();
        if (sqlMessage != null
            && (sqlMessage.contains("duplicate key value violates unique constraint")
                || sqlMessage.contains("Unique index or primary key violation"))) {
          throw DomainException.builder()
              .cause(sqlException)
              .httpStatus(HttpStatus.UNPROCESSABLE_ENTITY)
              .message(exceptionMessage + ": It already exists.")
              .build();
        }
        throw DomainException.builder()
            .cause(sqlException)
            .message(exceptionMessage + ": Unanticipated SQL exception.")
            .build();
      }
      throw DomainException.builder().cause(exception).message(exceptionMessage + ".").build();
    }
  }

  private LrmListItemAdded addToList(UUID listId, String owner, List<UUID> itemIdCollection) {
    // ensure the list exists
    LrmList list = lrmListRepository.findByOwnerAndIdOrNull(listId, owner);
    if (list == null) {
      throw new ListNotFoundException(listId);
    }

    // ensure the items exist
    Set<UUID> notFoundItemIds = lrmItemRepository.notFoundByOwnerAndId(itemIdCollection, owner);
    if (!notFoundItemIds.isEmpty()) {
      throw new ItemNotFoundException(notFoundItemIds);
    }

    // create associations
    Set<Pair<UUID, UUID>> associationCollection = new java.util.LinkedHashSet<>();
    for (UUID itemId : itemIdCollection) {
      associationCollection.add(new Pair<>(listId, itemId));
    }
    List<SuccinctLrmComponentPair> associations =
        lrmListItemRepository.create(associationCollection);
    if (associations.size() != itemIdCollection.size()) {
      throw DomainException.builder()
          .message(
              "Mismatch in created associations count (created = "
                  + associations.size()
                  + " / requested = "
                  + itemIdCollection.size()
                  + ")")
          .build();
    }

    // return created associations with sorted item names
    List<SuccinctLrmComponent> items =
        associations.stream()
            .map(SuccinctLrmComponentPair::item)
            .sorted(java.util.Comparator.comparing(SuccinctLrmComponent::name))
            .map(item -> (SuccinctLrmComponent) item)
            .toList();
    return new LrmListItemAdded(list.name(), items);
  }

  private LrmItem createItem(LrmItemCreate lrmItemCreate, String creator) {
    try {
      Instant now = Clock.System.INSTANCE.now();
      LrmItem lrmItem =
          new LrmItem(
              UUID.randomUUID(),
              lrmItemCreate.name(),
              lrmItemCreate.description(),
              creator,
              now,
              creator,
              now,
              creator,
              Set.of());
      UUID itemId = lrmItemRepository.insert(lrmItem);
      LrmItem newLrmItem = lrmItemRepository.findByOwnerAndIdOrNull(itemId, creator);
      if (newLrmItem == null) {
        throw new ItemNotFoundException(itemId);
      }
      return newLrmItem;
    } catch (Exception cause) {
      throw DomainException.builder().cause(cause).message("Item could not be created.").build();
    }
  }

  /** Create a new item and associate it with the specified list */
  @Override
  public ServiceResponse<LrmListItem> create(
      UUID listId, LrmItemCreate lrmItemCreate, String creator) {
    LrmItem lrmItem = createItem(lrmItemCreate, creator);
    LrmListItemAdded lrmListItemAdded = addToList(listId, creator, List.of(lrmItem.id()));
    LrmListItem lrmListItem =
        findByOwnerAndItemIdAndListId(lrmItem.id(), listId, creator).getContent();
    LrmListItem tmpListItem =
        lrmListItem
            .withQuantity(lrmItemCreate.quantity())
            .withIsSuppressed(lrmItemCreate.isSuppressed());
    patchQuantity(tmpListItem);
    patchIsSuppressed(tmpListItem);
    LrmListItem patchedLrmListItem =
        findByOwnerAndItemIdAndListId(lrmItem.id(), listId, creator).getContent();
    String message =
        "Created item '"
            + lrmListItemAdded.items().get(0).name()
            + "' and assigned it to list '"
            + lrmListItemAdded.listName()
            + "'";
    return new ServiceResponse<>(patchedLrmListItem, message);
  }

  @Override
  public ServiceResponse<LrmListItem> findByOwnerAndItemIdAndListId(
      UUID itemId, UUID listId, String owner) {
    if (lrmListRepository.findByOwnerAndIdOrNull(listId, owner) == null) {
      throw new ListNotFoundException(listId);
    }
    LrmListItem listItem =
        lrmListItemRepository.findByOwnerAndItemIdAndListIdOrNull(itemId, listId, owner);
    if (listItem == null) {
      throw new ListItemNotFoundException(itemId);
    }
    return new ServiceResponse<>(listItem, "Retrieved list item '" + listItem.name() + "'");
  }

  @Override
  public ServiceResponse<Triple<String, String, String>> move(
      UUID itemId, UUID currentListId, UUID destinationListId, String owner) {
    try {
      LrmItem item = lrmItemRepository.findByOwnerAndIdOrNull(itemId, owner);
      if (item == null) {
        throw new ItemNotFoundException(itemId);
      }
      LrmList currentList = lrmListRepository.findByOwnerAndIdOrNull(currentListId, owner);
      if (currentList == null) {
        throw new ListNotFoundException(currentListId);
      }
      LrmList destinationList = lrmListRepository.findByOwnerAndIdOrNull(destinationListId, owner);
      if (destinationList == null) {
        throw new ListNotFoundException(destinationListId);
      }
      LrmListItem lrmListItem =
          lrmListItemRepository.findByOwnerAndItemIdAndListIdOrNull(itemId, currentListId, owner);
      if (lrmListItem == null) {
        throw new ListItemNotFoundException();
      }

      // TODO: should not attempt to update is source and destination list are the same
      lrmListItemRepository.updateListId(lrmListItem, destinationListId);

      Triple<String, String, String> result =
          new Triple<>(item.name(), currentList.name(), destinationList.name());
      String message =
          "Moved item '"
              + result.getFirst()
              + "' from list '"
              + result.getSecond()
              + "' to list '"
              + result.getThird()
              + "'";
      return new ServiceResponse<>(result, message);
    } catch (Exception exception) {
      HttpStatus httpStatus =
          exception instanceof EntityNotFoundException e ? e.getHttpStatus() : null;
      throw DomainException.builder()
          .cause(exception)
          .httpStatus(httpStatus)
          .message(
              "Item id "
                  + itemId
                  + " was not moved from list id "
                  + currentListId
                  + " to list id "
                  + destinationListId
                  + ".")
          .build();
    }
  }

  // TODO: Wrap in try/catch and ServiceResponse
  @Override
  public void patchQuantity(LrmListItem patchedLrmListItem) {
    patchItem(patchedLrmListItem, () -> lrmListItemRepository.updateQuantity(patchedLrmListItem));
  }

  @Override
  public void patchIsSuppressed(LrmListItem patchedLrmListItem) {
    patchItem(
        patchedLrmListItem, () -> lrmListItemRepository.updateIsItemSuppressed(patchedLrmListItem));
  }

  @Override
  public ServiceResponse<Pair<String, Integer>> removeByOwnerAndItemId(UUID itemId, String owner) {
    try {
      LrmItem item = lrmItemRepository.findByOwnerAndIdOrNull(itemId, owner);
      if (item == null) {
        throw new ItemNotFoundException(itemId);
      }
      int deletedCount = lrmListItemRepository.removeByOwnerAndItemId(itemId, owner);
      String subject = deletedCount == 1 ? "list" : "lists";
      return new ServiceResponse<>(
          new Pair<>(item.name(), deletedCount),
          "Removed '" + item.name() + "' from " + deletedCount + " " + subject + ".");
    } catch (ItemNotFoundException exception) {
      throw DomainException.builder()
          .cause(exception)
          .httpStatus(exception.getHttpStatus())
          .message(
              "Item id "
                  + itemId
                  + " could not be removed from any/all lists: "
                  + exception.getMessage())
          .build();
    } catch (Exception exception) {
      throw DomainException.builder()
          .cause(exception)
          .message("Item id " + itemId + " could not be removed from any/all lists.")
          .build();
    }
  }

  @Override
  public ServiceResponse<Pair<String, Integer>> removeByOwnerAndListId(UUID listId, String owner) {
    String exceptionMessage = "Could not remove any/all items from List id " + listId;
    try {
      LrmList list = lrmListRepository.findByOwnerAndIdOrNull(listId, owner);
      if (list == null) {
        throw new ListNotFoundException(listId);
      }
      int deletedCount = lrmListItemRepository.removeByOwnerAndListId(listId, owner);
      String subject = deletedCount == 1 ? "item" : "items";
      return new ServiceResponse<>(
          new Pair<>(list.name(), deletedCount),
          "Removed " + deletedCount + " " + subject + " from list '" + list.name() + "'");
    } catch (ListNotFoundException exception) {
      throw DomainException.builder()
          .cause(exception)
          .httpStatus(exception.getHttpStatus())
          .message(exceptionMessage + ": " + exception.getMessage())
          .build();
    } catch (Exception exception) {
      throw DomainException.builder().cause(exception).message(exceptionMessage + ".").build();
    }
  }

  @Override
  public ServiceResponse<Pair<String, String>> removeByOwnerAndListIdAndItemId(
      UUID listId, UUID itemId, String owner) {
    String exceptionMessage = "Item id " + itemId + " could not be removed from list id " + listId;

    LrmItem item;
    LrmList list;
    LrmListItem association;
    try {
      item = lrmItemRepository.findByOwnerAndIdOrNull(itemId, owner);
      if (item == null) {
        throw new ItemNotFoundException(itemId);
      }
      list = lrmListRepository.findByOwnerAndIdOrNull(listId, owner);
      if (list == null) {
        throw new ListNotFoundException(listId);
      }
      association =
          lrmListItemRepository.findByOwnerAndItemIdAndListIdOrNull(itemId, listId, owner);
      if (association == null) {
        throw new ListItemNotFoundException();
      }
    } catch (Exception exception) {
      HttpStatus httpStatus =
          exception instanceof EntityNotFoundException e ? e.getHttpStatus() : null;
      throw DomainException.builder()
          .cause(exception)
          .httpStatus(httpStatus)
          .message(exceptionMessage + ": " + exception.getMessage())
          .build();
    }

    int deletedCount;
    try {
      deletedCount = lrmListItemRepository.removeByOwnerAndListIdAndItemId(listId, itemId, owner);
    } catch (Exception cause) {
      throw DomainException.builder().cause(cause).message(exceptionMessage + ".").build();
    }

    if (deletedCount == 1) {
      return new ServiceResponse<>(
          new Pair<>(item.name(), list.name()),
          "Removed item '" + item.name() + "' from list '" + list.name() + "'");
    } else if (deletedCount < 1) {
      throw DomainException.builder()
          .message(
              exceptionMessage
                  + ": Item id "
                  + itemId
                  + " exists, list id "
                  + listId
                  + " exists, association id "
                  + association.id()
                  + " exists, but 0 records were deleted.")
          .responseMessage(
              exceptionMessage
                  + ": Item, list, and association were found, but 0 records were deleted.")
          .build();
    } else {
      throw DomainException.builder()
          .httpStatus(HttpStatus.BAD_REQUEST)
          .message(
              exceptionMessage
                  + ": Delete transaction rolled back because the count of deleted records was > 1.")
          .responseMessage(
              exceptionMessage
                  + ": Item id "
                  + itemId
                  + " is associated with list id "
                  + listId
                  + " multiple times.")
          .build();
    }
  }

  // item context

  @Override
  public ServiceResponse<Long> countByOwnerAndItemId(UUID itemId, String owner) {
    String exceptionMessage =
        "Count of lists associated with item id " + itemId + " could not be retrieved.";
    long associations;
    try {
      if (lrmItemRepository.findByOwnerAndIdOrNull(itemId, owner) == null) {
        throw new ItemNotFoundException(itemId);
      }
      associations = lrmListItemRepository.countByOwnerAndItemId(itemId, owner);
    } catch (Exception exception) {
      HttpStatus httpStatus =
          exception instanceof ItemNotFoundException e ? e.getHttpStatus() : null;
      String detail = exception instanceof ItemNotFoundException ? exception.getMessage() : "";
      throw DomainException.builder()
          .cause(exception)
          .httpStatus(httpStatus)
          .message(exceptionMessage + ": " + (detail != null ? detail : ""))
          .build();
    }
    return new ServiceResponse<>(
        associations, "Item is associated with " + associations + " lists.");
  }

  @Override
  public void patchName(LrmListItem patchedLrmListItem) {
    patchItem(patchedLrmListItem, () -> lrmListItemRepository.updateName(patchedLrmListItem));
  }

  @Override
  public void patchDescription(LrmListItem patchedLrmListItem) {
    patchItem(
        patchedLrmListItem, () -> lrmListItemRepository.updateDescription(patchedLrmListItem));
  }

  private void patchItem(LrmListItem patchedLrmItem, IntSupplier updateAction) {
    validateItemEntity(patchedLrmItem);
    int updatedCount = updateAction.getAsInt();
    if (updatedCount != 1) {
      handleInvalidAffectedRecordCount(updatedCount, patchedLrmItem.id());
    }
  }

  private void handleInvalidAffectedRecordCount(int affectedRecordCount, UUID id) {
    if (affectedRecordCount < 1) {
      throw DomainException.builder()
          .message("No item affected by the repository operation.")
          .build();
    } else if (affectedRecordCount > 1) {
      throw DomainException.builder()
          .message("More than one item with id " + id + " were found.")
          .build();
    } else {
      throw new IllegalArgumentException(
          affectedRecordCount + " should not be passed to this function");
    }
  }

  private void validateItemEntity(LrmListItem lrmItem) {
    Set<ConstraintViolation<LrmListItem>> violations = validator.validate(lrmItem);
    if (!violations.isEmpty()) {
      throw new ConstraintViolationException(violations);
    }
  }
}
