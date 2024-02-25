package net.flyingfishflash.loremlist.domain.lrmlist

import net.flyingfishflash.loremlist.core.exceptions.AbstractApiException
import org.springframework.http.HttpStatus

class ListNotFoundException(id: Long, cause: Throwable? = null) : AbstractApiException(
  httpStatus = HttpStatus.NOT_FOUND,
  title = TITLE,
  detail = "List not found for id $id",
  cause = cause,
) {
  companion object {
    const val TITLE = "List Not Found"
  }
}

class ListInsertException(cause: Throwable? = null) : AbstractApiException(
  httpStatus = HttpStatus.BAD_REQUEST,
  title = TITLE,
  detail = "Problem inserting a new list",
  cause = cause,
) {
  companion object {
    const val TITLE = "List Insert Exception"
  }
}

class ListUpdateException(id: Long, cause: Throwable? = null) : AbstractApiException(
  httpStatus = HttpStatus.BAD_REQUEST,
  title = TITLE,
  detail = "Problem updating list id $id",
  cause = cause,
) {
  companion object {
    const val TITLE = "List Update Exception"
  }
}

class ListDeleteException(id: Long, cause: Throwable? = null) : AbstractApiException(
  httpStatus = HttpStatus.BAD_REQUEST,
  title = TITLE,
  detail = "Problem deleting list id $id",
  cause = cause,
) {
  companion object {
    const val TITLE = "List Delete Exception"
  }
}
