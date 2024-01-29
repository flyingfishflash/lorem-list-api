package net.flyingfishflash.loremlist.core.response.structure

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import java.net.URI
import java.util.Locale
import java.util.UUID

// @JsonPropertyOrder("id", "disposition", "instance", "method")

/**
 * @param <T> Type of the <i>content</i> field of the Response
 */
@Serializable
class Response<T> : ApplicationResponse<T> {
  private val id: String = UUID.randomUUID().toString()
  private val disposition: Disposition
  private val method: String
  private val message: String
  private val size: Int
  private val content: T

  @Contextual private val instance: URI?

  /**
   * @param content
   * @param message
   * @param method
   * @param instance
   */
  constructor(content: T?, message: String?, method: String?, instance: URI?) {
    if (content == null || instance == null || method == null || message == null) {
      // TODO: replace with application core constant
      throw IllegalArgumentException("Messages.Error.NULL_CONSTRUCTOR_ARG.value()")
    } else {
      require(content !is ProblemDetail) { "Content must not be of type ProblemDetail" }
    }

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
  constructor(content: T?, message: String?, method: String?) {
    if (content == null || message == null || method == null) {
      // TODO: replace with application core constant
      throw IllegalArgumentException("Messages.Error.NULL_CONSTRUCTOR_ARG.value()")
    } else if (content is ProblemDetail) {
      this.disposition = calcDisposition(content.status)
      this.message = message
      this.content = content
      this.size = calcSize()
      this.method = method
      this.instance = content.instance
    } else {
      throw IllegalArgumentException("Content must be of type ProblemDetail")
    }
  }

  override fun getId(): String? {
    return id
  }

  override fun getDisposition(): Disposition? {
    return disposition
  }

  fun getMethod(): String {
    return method.lowercase(Locale.getDefault())
  }

  override fun getMessage(): String? {
    return message
  }

  override fun getSize(): Int {
    return size
  }

  override fun getContent(): T? {
    return content
  }

  override fun getInstance(): String? {
    return instance?.path
  }

  /** Calculate the number of items included in the content  */
  private fun calcSize(): Int {
    var s = 1
    if (content is List<*>) {
      s = content.size
    }
    if (content is Map<*, *>) {
      s = content.size
    }
    return s
  }

  /** Calculate the disposition of the Api Event from the Http status  */
  private fun calcDisposition(httpStatus: Int): Disposition {
    var d = Disposition.SUCCESS
    if (HttpStatus.valueOf(httpStatus).is4xxClientError) {
      d = Disposition.FAILURE
    } else if (HttpStatus.valueOf(httpStatus).is5xxServerError) {
      d = Disposition.ERROR
    }
    return d
  }

  override fun toString(): String {
    return (
      "Response{" +
        "id='" +
        id +
        '\'' +
        ", disposition=" +
        disposition +
        ", method='" +
        method +
        '\'' +
        ", message='" +
        message +
        '\'' +
        ", size=" +
        size +
        ", content=" +
        content +
        ", instance=" +
        instance +
        '}'
    )
  }
}
