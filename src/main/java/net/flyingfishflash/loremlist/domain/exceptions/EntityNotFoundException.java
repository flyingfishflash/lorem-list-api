package net.flyingfishflash.loremlist.domain.exceptions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import net.flyingfishflash.loremlist.domain.LrmComponentType;
import org.springframework.http.HttpStatus;

public abstract class EntityNotFoundException extends DomainException {

  public static final HttpStatus HTTP_STATUS = HttpStatus.NOT_FOUND;

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  protected EntityNotFoundException(
      Set<UUID> idCollection, String message, LrmComponentType lrmComponentType) {
    super(
        null,
        HTTP_STATUS,
        message != null ? message : defaultMessage(idCollection, lrmComponentType),
        null,
        (lrmComponentType != null ? lrmComponentType.name() : "Entity") + "NotFoundException",
        null,
        Map.of("notFound", toJsonElement(idCollection)));
  }

  public static String defaultMessage(Set<UUID> idCollection, LrmComponentType lrmComponentType) {
    String entityName = lrmComponentType != null ? lrmComponentType.name() : "Entity";
    if (idCollection.size() > 1) {
      return entityName + "s (" + idCollection.size() + ") could not be found.";
    }
    return entityName + " could not be found.";
  }

  private static JsonNode toJsonElement(Set<UUID> idCollection) {
    return OBJECT_MAPPER.valueToTree(
        idCollection.stream().map(UUID::toString).collect(Collectors.toList()));
  }
}
