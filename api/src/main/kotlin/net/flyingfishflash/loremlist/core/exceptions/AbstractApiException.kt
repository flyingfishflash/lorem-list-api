package net.flyingfishflash.loremlist.core.exceptions

import org.springframework.http.HttpStatus
import java.net.URI

abstract class AbstractApiException(
  override val cause: Throwable? = null,
  override val message: String,
  val httpStatus: HttpStatus,
  val responseMessage: String,
  val title: String,
  val type: URI = URI.create("about:config"),
) : Exception(message, cause)

// abstract class AbstractApiException protected constructor(
//  val type: URI = URI.create("about:config"),
//  val httpStatus: HttpStatus,
//  val title: String,
//  val detail: String,
//  cause: Throwable? = null,
// ) : Exception(cause)
