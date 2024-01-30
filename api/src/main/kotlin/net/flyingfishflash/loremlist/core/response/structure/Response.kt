package net.flyingfishflash.loremlist.core.response.structure

import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import java.net.URI
import java.util.Locale
import java.util.UUID

/**
 * @param <T> Type of the <i>content</i> field of the Response
 */
open class Response<T> : ApplicationResponse<T> {
  override val id: String = UUID.randomUUID().toString()
  final override val disposition: Disposition
  final override val instance: URI?
  final override val method: String
    get() = field.lowercase(Locale.getDefault())
  final override val message: String
  final override val size: Int
  final override val content: T

  /**
   * @param content
   * @param message
   * @param method
   * @param instance
   */
  constructor(content: T, message: String, method: String, instance: URI) {
    require(content !is ProblemDetail) { "Content must not be of type ProblemDetail" }
    this.disposition = calcDisposition(HttpStatus.OK.value())
    this.message = message
    this.content = content
    this.size = calcSize()
    this.method = method
    this.instance = instance
  }

  /**
   * @param content
   * @param message
   * @param method
   */
  constructor(content: T, message: String, method: String) {
    require(content is ProblemDetail) { "Content must be of type ProblemDetail" }
    this.disposition = calcDisposition(content.status)
    this.message = message
    this.content = content
    this.size = calcSize()
    this.method = method
    this.instance = content.instance
  }

  /** Calculate the number of items included in the content  */
  private fun calcSize(): Int =
    when (content) {
      is List<*> -> content.size
      is Map<*, *> -> content.size
      else -> 1
    }

  /** Calculate the disposition of the Api Event from the Http status  */
  private fun calcDisposition(httpStatus: Int): Disposition =
    when {
      HttpStatus.valueOf(httpStatus).is4xxClientError -> Disposition.FAILURE
      HttpStatus.valueOf(httpStatus).is5xxServerError -> Disposition.ERROR
      else -> Disposition.SUCCESS
    }

  final override fun toString(): String {
    return "Response(id='$id', " +
      "disposition=$disposition, " +
      "method='$method', " +
      "message='$message', " +
      "size=$size, " +
      "content=$content, " +
      "instance=$instance)"
  }
}
