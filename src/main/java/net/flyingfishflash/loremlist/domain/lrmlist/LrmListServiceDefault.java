package net.flyingfishflash.loremlist.domain.lrmlist;

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
import kotlinx.datetime.Clock;
import kotlinx.datetime.Instant;
import net.flyingfishflash.loremlist.core.exceptions.CoreException;
import net.flyingfishflash.loremlist.domain.ServiceResponse;
import net.flyingfishflash.loremlist.domain.exceptions.DomainException;
import net.flyingfishflash.loremlist.domain.lrmitem.LrmItem;
import net.flyingfishflash.loremlist.domain.lrmlist.data.LrmListCreate;
import net.flyingfishflash.loremlist.domain.lrmlist.data.LrmListDeleted;
import net.flyingfishflash.loremlist.domain.lrmlistitem.LrmListItemService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class LrmListServiceDefault implements LrmListService {

  private final LrmListRepository lrmListRepository;
  private final LrmListItemService lrmListItemService;
  private final ObjectMapper objectMapper;
  private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

  public LrmListServiceDefault(
      LrmListRepository lrmListRepository,
      LrmListItemService lrmListItemService,
      ObjectMapper objectMapper) {
    this.lrmListRepository = lrmListRepository;
    this.lrmListItemService = lrmListItemService;
    this.objectMapper = objectMapper;
  }

  @Override
  public ServiceResponse<Long> countByOwner(String owner) {
    try {
      long repositoryResponse = lrmListRepository.countByOwner(owner);
      String message =
          repositoryResponse == 1L ? repositoryResponse + " list." : repositoryResponse + " lists.";
      return new ServiceResponse<>(repositoryResponse, message);
    } catch (Exception cause) {
      throw DomainException.builder()
          .cause(cause)
          .message("Total list count couldn't be generated.")
          .build();
    }
  }

  @Override
  public ServiceResponse<LrmList> create(LrmListCreate lrmListCreate, String creator) {
    try {
      Instant now = Clock.System.INSTANCE.now();
      LrmList lrmList =
          new LrmList(
              UUID.randomUUID(),
              lrmListCreate.getName(),
              lrmListCreate.getDescription(),
              lrmListCreate.getPublic(),
              creator,
              now,
              creator,
              now,
              creator,
              Set.of());
      UUID id = lrmListRepository.insert(lrmList);
      LrmList createdLrmList = findByOwnerAndId(id, creator).getContent();
      return new ServiceResponse<>(createdLrmList, "Created list '" + createdLrmList.name() + "'");
    } catch (Exception cause) {
      throw DomainException.builder().cause(cause).message("List could not be created.").build();
    }
  }

  @Override
  public ServiceResponse<LrmListDeleted> deleteByOwner(String owner) {
    try {
      List<LrmList> lists = findByOwner(owner).getContent();
      if (!lists.isEmpty()) {
        for (LrmList list : lists) {
          if (!list.items().isEmpty()) {
            lrmListItemService.removeByOwnerAndListId(list.id(), owner);
          }
        }
        Set<UUID> ids =
            lists.stream().map(LrmList::id).collect(java.util.stream.Collectors.toSet());
        lrmListRepository.deleteById(ids);
      }
      List<String> listNames = lists.stream().map(LrmList::name).sorted().toList();
      List<String> associatedItemNames =
          lists.stream()
              .flatMap(list -> list.items().stream())
              .map(item -> item.name())
              .sorted()
              .toList();
      LrmListDeleted lrmListDeleteResponse = new LrmListDeleted(listNames, associatedItemNames);
      return new ServiceResponse<>(
          lrmListDeleteResponse,
          "Deleted all ("
              + lrmListDeleteResponse.listNames().size()
              + ") of your lists, and disassociated "
              + lrmListDeleteResponse.associatedItemNames().size()
              + " items.");
    } catch (Exception cause) {
      if (cause instanceof CoreException coreException) {
        throw DomainException.builder()
            .cause(coreException)
            .httpStatus(coreException.getHttpStatus())
            .message("No lists were deleted: " + coreException.getResponseMessage())
            .supplemental(coreException.getSupplemental())
            .build();
      }
      throw DomainException.builder().cause(cause).message("No lists were deleted.").build();
    }
  }

  @Override
  public ServiceResponse<LrmListDeleted> deleteByOwnerAndId(
      UUID id, String owner, boolean removeItemAssociations) {
    try {
      LrmList list = findByOwnerAndId(id, owner).getContent();
      LrmListDeleted deleteResponse = createDeleteResponse(list);
      if (deleteResponse.associatedItemNames().isEmpty()) {
        doDeleteByOwnerAndId(id, owner);
      } else {
        if (removeItemAssociations) {
          deleteItemAssociations(id, owner);
        } else {
          throwItemAssociationException(deleteResponse, list.name());
        }
      }
      String noun = deleteResponse.associatedItemNames().size() == 1 ? "item" : "items";
      return new ServiceResponse<>(
          deleteResponse,
          "Deleted list '"
              + deleteResponse.listNames().get(0)
              + "', and disassociated "
              + deleteResponse.associatedItemNames().size()
              + " "
              + noun
              + ".");
    } catch (Exception cause) {
      if (cause instanceof CoreException coreException) {
        throw DomainException.builder()
            .cause(coreException)
            .httpStatus(coreException.getHttpStatus())
            .message("List could not be deleted: " + coreException.getResponseMessage())
            .supplemental(coreException.getSupplemental())
            .build();
      }
      throw DomainException.builder()
          .cause(cause)
          .message("List id " + id + " could not be deleted.")
          .build();
    }
  }

  @Override
  public ServiceResponse<List<LrmItem>> findEligibleItemsByOwner(UUID id, String owner) {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  private LrmListDeleted createDeleteResponse(LrmList list) {
    List<String> associatedItemNames =
        list.items().stream().map(item -> item.name()).sorted().toList();
    return new LrmListDeleted(List.of(list.name()), associatedItemNames);
  }

  private void deleteItemAssociations(UUID id, String owner) {
    lrmListItemService.removeByOwnerAndListId(id, owner);
    int deletedCount = lrmListRepository.deleteByOwnerAndId(id, owner);
    if (deletedCount > 1) {
      handleInvalidAffectedRecordCount(deletedCount, id);
    }
  }

  private void throwItemAssociationException(LrmListDeleted deleteResponse, String listName) {
    String message =
        "List '"
            + listName
            + "' is associated with "
            + deleteResponse.associatedItemNames().size()
            + " item(s). First remove each item from the list.";
    throw DomainException.builder()
        .httpStatus(HttpStatus.UNPROCESSABLE_ENTITY)
        .supplemental(
            Map.of(
                "listNames", objectMapper.valueToTree(deleteResponse.listNames()),
                "associatedItemNames",
                    objectMapper.valueToTree(deleteResponse.associatedItemNames())))
        .responseMessage(
            "'"
                + listName
                + "' includes "
                + deleteResponse.associatedItemNames().size()
                + " item(s).")
        .message(message)
        .build();
  }

  private void doDeleteByOwnerAndId(UUID id, String owner) {
    int deletedCount = lrmListRepository.deleteByOwnerAndId(id, owner);
    if (deletedCount > 1) {
      handleInvalidAffectedRecordCount(deletedCount, id);
    }
  }

  @Override
  public ServiceResponse<List<LrmList>> findByOwner(String owner) {
    String exceptionMessage = "Lists (including associated items) could not be retrieved.";
    try {
      List<LrmList> repositoryResponse = lrmListRepository.findByOwner(owner);
      return new ServiceResponse<>(
          repositoryResponse, "Retrieved all lists owned by '" + owner + "'");
    } catch (Exception cause) {
      throw DomainException.builder().cause(cause).message(exceptionMessage).build();
    }
  }

  @Override
  public ServiceResponse<LrmList> findByOwnerAndId(UUID id, String owner) {
    String exceptionMessage =
        "List id " + id + " (including associated items) could not be retrieved.";
    LrmList list;
    try {
      list = lrmListRepository.findByOwnerAndIdOrNull(id, owner);
    } catch (Exception cause) {
      throw DomainException.builder().cause(cause).message(exceptionMessage).build();
    }
    if (list == null) {
      throw new ListNotFoundException(id);
    }
    return new ServiceResponse<>(list, "Retrieved list '" + list.name() + "'");
  }

  @Override
  public ServiceResponse<List<LrmList>> findByOwnerAndHavingNoItemAssociations(String owner) {
    String exceptionMessage = "Lists without item associations could not be retrieved.";
    try {
      List<LrmList> repositoryResponse =
          lrmListRepository.findByOwnerAndHavingNoItemAssociations(owner);
      return new ServiceResponse<>(
          repositoryResponse,
          "Retrieved " + repositoryResponse.size() + " lists that have no items.");
    } catch (Exception cause) {
      throw DomainException.builder().cause(cause).message(exceptionMessage).build();
    }
  }

  @Override
  public ServiceResponse<List<LrmList>> findByPublic() {
    try {
      List<LrmList> repositoryResponse = lrmListRepository.findByPublic();
      return new ServiceResponse<>(
          repositoryResponse, "Retrieved " + repositoryResponse.size() + " public lists.");
    } catch (Exception cause) {
      throw DomainException.builder()
          .cause(cause)
          .message("Public lists (including associated items) could not be retrieved.")
          .build();
    }
  }

  @Override
  public void patchName(LrmList patchedLrmList) {
    patchList(patchedLrmList, () -> lrmListRepository.updateName(patchedLrmList));
  }

  @Override
  public void patchDescription(LrmList patchedLrmList) {
    patchList(patchedLrmList, () -> lrmListRepository.updateDescription(patchedLrmList));
  }

  @Override
  public void patchIsPublic(LrmList patchedLrmList) {
    patchList(patchedLrmList, () -> lrmListRepository.updateIsPublic(patchedLrmList));
  }

  private void patchList(LrmList patchedLrmList, IntSupplier updateAction) {
    validateListEntity(patchedLrmList);
    int updatedCount = updateAction.getAsInt();
    if (updatedCount != 1) {
      handleInvalidAffectedRecordCount(updatedCount, patchedLrmList.id());
    }
  }

  private void handleInvalidAffectedRecordCount(int affectedRecordCount, UUID id) {
    if (affectedRecordCount < 1) {
      throw DomainException.builder()
          .message("No list affected by the repository operation.")
          .build();
    } else if (affectedRecordCount > 1) {
      throw DomainException.builder()
          .message("More than one list with id " + id + " found.")
          .build();
    } else {
      throw new IllegalArgumentException(
          affectedRecordCount + " should not be passed to this function");
    }
  }

  private void validateListEntity(LrmList lrmList) {
    Set<ConstraintViolation<LrmList>> violations = validator.validate(lrmList);
    if (!violations.isEmpty()) {
      throw new ConstraintViolationException(violations);
    }
  }
}
