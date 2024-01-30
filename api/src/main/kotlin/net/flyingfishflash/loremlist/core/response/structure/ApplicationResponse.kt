package net.flyingfishflash.loremlist.core.response.structure

import java.net.URI

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
  val id: String
  val disposition: Disposition
  val method: String
  val message: String
  val size: Int
  val instance: URI?
  val content: T
}
