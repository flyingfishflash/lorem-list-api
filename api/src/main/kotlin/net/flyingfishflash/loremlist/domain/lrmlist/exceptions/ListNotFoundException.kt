package net.flyingfishflash.loremlist.domain.lrmlist.exceptions

import org.springframework.http.HttpStatus
import org.springframework.web.ErrorResponseException

class ListNotFoundException(cause: Throwable? = null) : ErrorResponseException(HttpStatus.NOT_FOUND, cause)
