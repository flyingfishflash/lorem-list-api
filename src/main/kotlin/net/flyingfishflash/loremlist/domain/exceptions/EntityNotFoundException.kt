package net.flyingfishflash.loremlist.domain.exceptions
import net.flyingfishflash.loremlist.domain.LrmComponentType
import net.flyingfishflash.loremlist.toJsonElement
import org.springframework.http.HttpStatus
import java.util.UUID

abstract class EntityNotFoundException(idCollection: Set<UUID>, message: String? = null, lrmComponentType: LrmComponentType? = null) :
  DomainException(
    httpStatus = HTTP_STATUS,
    title = "${lrmComponentType?.name ?: "Entity"}NotFoundException",
    message = message ?: defaultMessage(idCollection, lrmComponentType),
    supplemental = mapOf("notFound" to idCollection.map { it.toString() }.toJsonElement()),
  ) {
  companion object {
    val HTTP_STATUS = HttpStatus.NOT_FOUND
    fun defaultMessage(idCollection: Set<UUID>, lrmComponentType: LrmComponentType?): String {
      val entityName = lrmComponentType?.name ?: "Entity"
      val message = if (idCollection.size > 1) {
        "${entityName}s (${idCollection.size}) could not be found."
      } else {
        "$entityName could not be found."
      }
      return message
    }
  }
}
