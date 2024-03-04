package net.flyingfishflash.loremlist.domain.lrmitem

import net.flyingfishflash.loremlist.core.exceptions.AbstractApiException
import org.springframework.http.HttpStatus

class ItemNotFoundException(id: Long, cause: Throwable? = null) : AbstractApiException(
  httpStatus = HttpStatus.NOT_FOUND,
  title = TITLE,
  detail = "Item not found for id $id",
  cause = cause,
) {
  companion object {
    const val TITLE = "Item Not Found"
  }
}

class ItemInsertException(cause: Throwable? = null) : AbstractApiException(
  httpStatus = HttpStatus.BAD_REQUEST,
  title = TITLE,
  detail = "Problem inserting a new list",
  cause = cause,
) {
  companion object {
    const val TITLE = "Item Insert Exception"
  }
}

class ItemUpdateException(id: Long, cause: Throwable? = null) : AbstractApiException(
  httpStatus = HttpStatus.BAD_REQUEST,
  title = TITLE,
  detail = "Problem updating list id $id",
  cause = cause,
) {
  companion object {
    const val TITLE = "Item Update Exception"
  }
}

class ItemDeleteException(id: Long, cause: Throwable? = null) : AbstractApiException(
  httpStatus = HttpStatus.BAD_REQUEST,
  title = TITLE,
  detail = "Problem deleting list id $id",
  cause = cause,
) {
  companion object {
    const val TITLE = "Item Delete Exception"
  }
}
