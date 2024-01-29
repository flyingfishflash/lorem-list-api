package net.flyingfishflash.loremlist.domain.lrmlist

import net.flyingfishflash.loremlist.core.exceptions.AbstractApiException
import org.springframework.http.HttpStatus
import org.springframework.web.ErrorResponseException

class ListNotFoundException(id: Long, cause: Throwable? = null) : AbstractApiException(
  HttpStatus.NOT_FOUND,
  TITLE,
  "List not found for id $id",
  cause,
) {
  companion object {
    const val TITLE = "List Not Found"
  }
}

class ListInsertException(cause: Throwable? = null) : ErrorResponseException(HttpStatus.BAD_REQUEST, cause)

class ListUpdateException(cause: Throwable? = null) : ErrorResponseException(HttpStatus.BAD_REQUEST, cause)

class ListDeleteException(cause: Throwable? = null) : ErrorResponseException(HttpStatus.BAD_REQUEST, cause)
