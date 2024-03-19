package net.flyingfishflash.loremlist.domain.lrmitem

import net.flyingfishflash.loremlist.core.exceptions.AbstractApiException
import org.springframework.http.HttpStatus

class ItemNotFoundException(id: Long, cause: Throwable? = null) : AbstractApiException(
  httpStatus = HttpStatus.NOT_FOUND,
  title = TITLE,
  message = "Item not found for id $id",
  responseMessage = "Item not found for id $id",
  cause = cause,
) {
  companion object {
    const val TITLE = "Item Not Found"
  }
}

class ItemInsertException(cause: Throwable? = null) : AbstractApiException(
  httpStatus = HttpStatus.BAD_REQUEST,
  title = TITLE,
  message = "Problem inserting a new list",
  cause = cause,
  responseMessage = "Problem inserting a new list",
) {
  companion object {
    const val TITLE = "Item Insert Exception"
  }
}

class ItemUpdateException(id: Long, cause: Throwable? = null) : AbstractApiException(
  httpStatus = HttpStatus.BAD_REQUEST,
  title = TITLE,
  message = "Problem updating list id $id",
  cause = cause,
  responseMessage = "Problem updating list id $id",
) {
  companion object {
    const val TITLE = "Item Update Exception"
  }
}

class ItemDeleteException(id: Long, cause: Throwable? = null) : AbstractApiException(
  httpStatus = HttpStatus.BAD_REQUEST,
  title = TITLE,
  // TODO: Fix this detail
  message = "Problem deleting list id $id",
  cause = cause,
  responseMessage = "Problem deleting list id $id",
) {
  companion object {
    const val TITLE = "Item Delete Exception"
  }
}

class ItemAddToListException(id: Long, cause: Throwable? = null) : AbstractApiException(
  httpStatus = HttpStatus.BAD_REQUEST,
  title = TITLE,
  message = "Problem adding item id $id to a list",
  cause = cause,
  responseMessage = "Problem adding item id $id to a list",
) {
  companion object {
    const val TITLE = "Item Add to List Exception"
  }
}
