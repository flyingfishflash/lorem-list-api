package net.flyingfishflash.loremlist.domain.lrmitem;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.IntSupplier;
import net.flyingfishflash.loremlist.core.exceptions.CoreException;
import net.flyingfishflash.loremlist.domain.ServiceResponse;
import net.flyingfishflash.loremlist.domain.exceptions.DomainException;
import net.flyingfishflash.loremlist.domain.lrmitem.data.LrmItemDeleted;
import net.flyingfishflash.loremlist.domain.lrmlist.LrmList;
import net.flyingfishflash.loremlist.domain.lrmlist.LrmListService;
import net.flyingfishflash.loremlist.domain.lrmlist.LrmListSuccinct;
import net.flyingfishflash.loremlist.domain.lrmlistitem.LrmListItemService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class LrmItemServiceDefault implements LrmItemService {

  private final LrmItemRepository lrmItemRepository;
  private final LrmListService lrmListService;
  private final LrmListItemService lrmListItemService;
  private final ObjectMapper objectMapper;
  private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

  public LrmItemServiceDefault(
      LrmItemRepository lrmItemRepository,
      LrmListService lrmListService,
      LrmListItemService lrmListItemService,
      ObjectMapper objectMapper) {
    this.lrmItemRepository = lrmItemRepository;
    this.lrmListService = lrmListService;
    this.lrmListItemService = lrmListItemService;
    this.objectMapper = objectMapper;
  }

  @Override
  public ServiceResponse<Long> countByOwner(String owner) {
    try {
      long repositoryResponse = lrmItemRepository.countByOwner(owner);
      String message =
          repositoryResponse == 1L ? repositoryResponse + " item." : repositoryResponse + " items.";
      return new ServiceResponse<>(repositoryResponse, message);
    } catch (Exception cause) {
      throw DomainException.builder()
          .cause(cause)
          .message("Total item count couldn't be generated.")
          .build();
    }
  }

  @Override
  public ServiceResponse<LrmItemDeleted> deleteByOwner(String owner) {
    try {
      List<LrmItem> items = findByOwner(owner).getContent();
      if (!items.isEmpty()) {
        for (LrmItem item : items) {
          if (!item.lists().isEmpty()) {
            lrmListItemService.removeByOwnerAndItemId(item.id(), owner);
          }
        }
        Set<UUID> ids =
            items.stream().map(LrmItem::id).collect(java.util.stream.Collectors.toSet());
        lrmItemRepository.deleteById(ids);
      }
      List<String> itemNames = items.stream().map(LrmItem::name).sorted().toList();
      List<String> associatedListNames =
          items.stream()
              .flatMap(item -> item.lists().stream())
              .map(LrmListSuccinct::getName)
              .sorted()
              .toList();
      LrmItemDeleted lrmItemDeleted = new LrmItemDeleted(itemNames, associatedListNames);
      return new ServiceResponse<>(
          lrmItemDeleted,
          "Deleted all ("
              + lrmItemDeleted.itemNames().size()
              + ") of your items, and removed them from "
              + lrmItemDeleted.associatedListNames().size()
              + " lists.");
    } catch (Exception cause) {
      if (cause instanceof CoreException coreException) {
        throw DomainException.builder()
            .cause(coreException)
            .httpStatus(coreException.getHttpStatus())
            .message("No items were deleted: " + coreException.getResponseMessage())
            .supplemental(coreException.getSupplemental())
            .build();
      }
      throw DomainException.builder().cause(cause).message("No items were deleted.").build();
    }
  }

  @Override
  public ServiceResponse<LrmItemDeleted> deleteByOwnerAndId(
      UUID id, String owner, boolean removeListAssociations) {
    try {
      LrmItem item = findByOwnerAndId(id, owner).getContent();
      LrmItemDeleted lrmItemDeleteResponse = createDeleteResponse(item);
      if (!lrmItemDeleteResponse.associatedListNames().isEmpty()) {
        if (removeListAssociations) {
          lrmListItemService.removeByOwnerAndItemId(id, owner);
        } else {
          throwListAssociationException(lrmItemDeleteResponse);
        }
      }
      doDeleteByOwnerAndId(id, owner);
      String noun = lrmItemDeleteResponse.associatedListNames().size() == 1 ? "list" : "lists";
      return new ServiceResponse<>(
          lrmItemDeleteResponse,
          "Deleted item '"
              + lrmItemDeleteResponse.itemNames().get(0)
              + "', and removed it from "
              + lrmItemDeleteResponse.associatedListNames().size()
              + " "
              + noun
              + ".");
    } catch (Exception cause) {
      if (cause instanceof CoreException coreException) {
        throw DomainException.builder()
            .cause(coreException)
            .httpStatus(coreException.getHttpStatus())
            .message(
                "Item id " + id + " could not be deleted: " + coreException.getResponseMessage())
            .supplemental(coreException.getSupplemental())
            .build();
      }
      throw DomainException.builder()
          .cause(cause)
          .message("Item id " + id + " could not be deleted.")
          .build();
    }
  }

  private LrmItemDeleted createDeleteResponse(LrmItem item) {
    List<String> associatedListNames =
        item.lists().stream().map(LrmListSuccinct::getName).sorted().toList();
    return new LrmItemDeleted(List.of(item.name()), associatedListNames);
  }

  private void throwListAssociationException(LrmItemDeleted lrmItemDeleteResponse) {
    String message =
        "Item '"
            + lrmItemDeleteResponse.itemNames().get(0)
            + "' is associated with "
            + lrmItemDeleteResponse.associatedListNames().size()
            + " list(s). First remove the item from each list.";
    throw DomainException.builder()
        .httpStatus(HttpStatus.UNPROCESSABLE_ENTITY)
        .supplemental(
            Map.of(
                "itemNames", objectMapper.valueToTree(lrmItemDeleteResponse.itemNames()),
                "associatedListNames",
                    objectMapper.valueToTree(lrmItemDeleteResponse.associatedListNames())))
        .message(message)
        .build();
  }

  private void doDeleteByOwnerAndId(UUID itemId, String owner) {
    int deletedCount = lrmItemRepository.deleteByOwnerAndId(itemId, owner);
    if (deletedCount != 1) {
      handleInvalidAffectedRecordCount(deletedCount, itemId);
    }
  }

  @Override
  public ServiceResponse<List<LrmItem>> findByOwner(String owner) {
    try {
      List<LrmItem> repositoryResponse = lrmItemRepository.findByOwner(owner);
      return new ServiceResponse<>(
          repositoryResponse, "Retrieved all items owned by " + owner + ".");
    } catch (Exception cause) {
      throw DomainException.builder().cause(cause).message("Items could not be retrieved.").build();
    }
  }

  @Override
  public ServiceResponse<LrmItem> findByOwnerAndId(UUID id, String owner) {
    LrmItem repositoryResponse;
    try {
      repositoryResponse = lrmItemRepository.findByOwnerAndIdOrNull(id, owner);
    } catch (Exception cause) {
      throw DomainException.builder()
          .cause(cause)
          .message("Item id " + id + " could not be retrieved.")
          .build();
    }
    if (repositoryResponse == null) {
      throw new ItemNotFoundException(id);
    }
    return new ServiceResponse<>(
        repositoryResponse, "Retrieved item '" + repositoryResponse.name() + "'");
  }

  @Override
  public ServiceResponse<List<LrmItem>> findByOwnerAndHavingNoListAssociations(String owner) {
    try {
      List<LrmItem> repositoryResponse =
          lrmItemRepository.findByOwnerAndHavingNoListAssociations(owner);
      return new ServiceResponse<>(
          repositoryResponse,
          "Retrieved " + repositoryResponse.size() + " items that are not a part of a list.");
    } catch (Exception cause) {
      throw DomainException.builder()
          .cause(cause)
          .message("Items without list associations could not be retrieved.")
          .build();
    }
  }

  @Override
  public ServiceResponse<List<LrmItem>> findByOwnerAndHavingNoListAssociations(
      String owner, UUID listId) {
    String exceptionMessage =
        "Items eligible to be added to list '" + listId + "' could not be retrieved";
    HttpStatus httpStatus = null;
    try {
      LrmList lrmList = lrmListService.findByOwnerAndId(listId, owner).getContent();
      List<LrmItem> repositoryResponse =
          lrmItemRepository.findByOwnerAndHavingNoListAssociations(owner, listId);
      return new ServiceResponse<>(
          repositoryResponse,
          "Retrieved "
              + repositoryResponse.size()
              + " items eligible to be added to list '"
              + lrmList.name()
              + "'.");
    } catch (Exception cause) {
      if (cause instanceof DomainException domainException) {
        httpStatus = domainException.getHttpStatus();
      }
      String message = exceptionMessage + ": " + cause.getMessage();
      throw DomainException.builder().cause(cause).message(message).httpStatus(httpStatus).build();
    }
  }

  @Override
  public void patchName(LrmItem patchedLrmItem) {
    patchItem(patchedLrmItem, () -> lrmItemRepository.updateName(patchedLrmItem));
  }

  @Override
  public void patchDescription(LrmItem patchedLrmItem) {
    patchItem(patchedLrmItem, () -> lrmItemRepository.updateDescription(patchedLrmItem));
  }

  private void patchItem(LrmItem patchedLrmItem, IntSupplier updateAction) {
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

  private void validateItemEntity(LrmItem lrmItem) {
    Set<ConstraintViolation<LrmItem>> violations = validator.validate(lrmItem);
    if (!violations.isEmpty()) {
      throw new ConstraintViolationException(violations);
    }
  }
}
