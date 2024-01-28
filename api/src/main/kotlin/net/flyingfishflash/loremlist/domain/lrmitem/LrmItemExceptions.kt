package net.flyingfishflash.loremlist.domain.lrmitem

import org.springframework.http.HttpStatus
import org.springframework.web.ErrorResponseException

class ItemNotFoundException(cause: Throwable? = null) : ErrorResponseException(HttpStatus.NOT_FOUND, cause)

class ItemInsertException(cause: Throwable? = null) : ErrorResponseException(HttpStatus.BAD_REQUEST, cause)

class ItemUpdateException(cause: Throwable? = null) : ErrorResponseException(HttpStatus.BAD_REQUEST, cause)

class ItemDeleteException(cause: Throwable? = null) : ErrorResponseException(HttpStatus.BAD_REQUEST, cause)
