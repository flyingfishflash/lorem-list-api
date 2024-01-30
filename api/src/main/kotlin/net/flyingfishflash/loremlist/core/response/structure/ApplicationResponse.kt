package net.flyingfishflash.loremlist.core.response.structure

/**
 * Describes the structure of error responses sent to a client<br>
 *
 * <pre>
 * response.status
 * response.content.body
 * response.content.message
 * </pre>
 */
interface ApplicationResponse<T> {
  /**
   * @return Unique identifier intended for log entry reference
   */
  fun getId(): String?

  /**
   * @return Api Event Disposition
   */
  fun getDisposition(): Disposition?

  /**
   * @return Succinct contextual message describing an API event
   */
  fun getMessage(): String?

  /**
   * @return Response content
   */
  fun getContent(): T?

  /**
   * @return Number of items included in the content
   */
  fun getSize(): Int

  /**
   * @return Instance path
   */
  fun getInstance(): String?
}
