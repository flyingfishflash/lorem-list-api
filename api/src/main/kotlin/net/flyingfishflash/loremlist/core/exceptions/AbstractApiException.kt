package net.flyingfishflash.loremlist.core.exceptions

import org.springframework.http.HttpStatus
import java.net.URI

abstract class AbstractApiException(
  override val cause: Throwable? = null,
  val detail: String,
  val httpStatus: HttpStatus,
  val responseMessage: String,
  val title: String,
  val type: URI = URI.create("about:config"),
) : Exception(detail, cause)
