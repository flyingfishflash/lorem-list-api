package net.flyingfishflash.loremlist.domain.lrmlist

import org.springframework.http.HttpStatus
import org.springframework.web.ErrorResponseException

class ListNotFoundException(cause: Throwable? = null) : ErrorResponseException(HttpStatus.NOT_FOUND, cause)

class ListInsertException(cause: Throwable? = null) : ErrorResponseException(HttpStatus.BAD_REQUEST, cause)

class ListUpdateException(cause: Throwable? = null) : ErrorResponseException(HttpStatus.BAD_REQUEST, cause)

class ListDeleteException(cause: Throwable? = null) : ErrorResponseException(HttpStatus.BAD_REQUEST, cause)
