package net.flyingfishflash.loremlist.domain.lrmlist

import net.flyingfishflash.loremlist.core.exceptions.AbstractApiException
import org.springframework.http.HttpStatus

class ListNotFoundException(id: Long, cause: Throwable? = null) : AbstractApiException(
  httpStatus = HttpStatus.NOT_FOUND,
  title = TITLE,
  message = "List not found for id $id",
  cause = cause,
  responseMessage = "List not found for id $id",
) {
  companion object {
    const val TITLE = "List Not Found"
  }
}

class ListInsertException(cause: Throwable? = null) : AbstractApiException(
  httpStatus = HttpStatus.BAD_REQUEST,
  title = TITLE,
  message = "Problem inserting a new list",
  cause = cause,
  responseMessage = "Problem inserting a new list",
) {
  companion object {
    const val TITLE = "List Insert Exception"
  }
}

class ListUpdateException(id: Long, cause: Throwable? = null) : AbstractApiException(
  httpStatus = HttpStatus.BAD_REQUEST,
  title = TITLE,
  message = "Problem updating list id $id",
  cause = cause,
  responseMessage = "Problem updating list id $id",
) {
  companion object {
    const val TITLE = "List Update Exception"
  }
}

class ListDeleteException(id: Long, cause: Throwable? = null) : AbstractApiException(
  httpStatus = HttpStatus.BAD_REQUEST,
  title = TITLE,
  message = "Problem deleting list id $id",
  cause = cause,
  responseMessage = "Problem deleting list id $id",
) {
  companion object {
    const val TITLE = "List Delete Exception"
  }
}
