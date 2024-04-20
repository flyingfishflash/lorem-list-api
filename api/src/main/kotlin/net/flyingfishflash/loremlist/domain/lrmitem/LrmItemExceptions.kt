package net.flyingfishflash.loremlist.domain.lrmitem

import net.flyingfishflash.loremlist.core.exceptions.AbstractApiException
import org.springframework.http.HttpStatus

class ItemNotFoundException(
  id: Long,
  cause: Throwable? = null,
  message: String? = null,
  responseMessage: String? = null,
) : AbstractApiException(
  cause = cause,
  httpStatus = HttpStatus.NOT_FOUND,
  detail = message ?: defaultMessage(id),
  responseMessage = responseMessage ?: defaultMessage(id),
  title = TITLE,
) {
  companion object {
    const val TITLE = "Item Not Found Exception"
    fun defaultMessage(id: Long) = "Item id $id could not be found."
  }
}

class ItemDeleteException(
  id: Long,
  cause: Throwable? = null,
  message: String? = null,
  responseMessage: String? = null,
) : AbstractApiException(
  cause = cause,
  httpStatus = HttpStatus.BAD_REQUEST,
  detail = message ?: defaultMessage(id),
  responseMessage = responseMessage ?: defaultMessage(id),
  title = TITLE,
) {
  companion object {
    const val TITLE = "Item Delete Exception"
    fun defaultMessage(id: Long) = "Item id $id could not be deleted."
  }
}

class ItemRemoveFromListException(
  itemId: Long,
  listId: Long,
  cause: Throwable? = null,
  message: String? = null,
  responseMessage: String? = null,
) : AbstractApiException(
  cause = cause,
  httpStatus = HttpStatus.BAD_REQUEST,
  detail = message ?: buildMessage(itemId, listId, cause),
  responseMessage = responseMessage ?: buildMessage(itemId, listId, cause),
  title = TITLE,
) {
  companion object {
    const val TITLE = "Item Remove From List Exception"
    fun buildMessage(itemId: Long, listId: Long, cause: Throwable?): String {
      val default = "Item id $itemId could not be removed from list id $listId."
      return when (cause) {
        is AbstractApiException -> "$default ${cause.message}"
        else -> default
      }
    }
  }
}
