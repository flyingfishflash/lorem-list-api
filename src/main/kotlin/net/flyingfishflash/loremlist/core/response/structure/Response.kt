package net.flyingfishflash.loremlist.core.response.structure

import jakarta.servlet.http.HttpServletRequest
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import net.flyingfishflash.loremlist.core.serialization.UUIDSerializer
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.http.server.ServerHttpRequest
import java.util.UUID

/**
 * Describes the structure of API responses sent to a client<br>
 */
@Serializable
sealed interface Response<T> {
  val id: UUID
  val disposition: Disposition
  val method: String
  val instance: String?
  val message: String
  val size: Int
  val content: T
}

@Serializable
data class ResponseProblem(
  @Serializable(with = UUIDSerializer::class) override val id: UUID = UUID.randomUUID(),
  override val disposition: DispositionOfProblem,
  override val method: String,
  override val instance: String,
  override val message: String,
  override val size: Int,
  override val content: ApiProblemDetail,
) : Response<ApiProblemDetail> {

  /** Create an API ResponseProblem from an ApiProblem
   * @return ResponseProblem
   */
  constructor(apiProblemDetail: ApiProblemDetail, responseMessage: String, request: HttpServletRequest) : this(
    disposition = DispositionOfProblem.calcDisposition(HttpStatus.valueOf(apiProblemDetail.status)),
    method = request.method.lowercase(),
    instance = request.requestURI.toString(),
    message = responseMessage,
    size = calcSize(apiProblemDetail),
    content = apiProblemDetail,
  )

  /** Create an API ResponseProblem from a Spring ProblemDetail
   * @return ResponseProblem
   */
  constructor(problemDetail: ProblemDetail, request: ServerHttpRequest) : this(
    disposition = DispositionOfProblem.calcDisposition(HttpStatus.valueOf(problemDetail.status)),
    method = request.method.name().lowercase(),
    instance = request.uri.path.lowercase(),
    message = problemDetail.detail ?: "no detail available for this problem",
    size = calcSize(problemDetail),
    content = ApiProblemDetail(problemDetail),
  )

  /** Create an API ResponseProblem from a Spring ProblemDetail
   * @return ResponseProblem
   */
  constructor(problemDetail: ProblemDetail, responseMessage: String?, request: ServerHttpRequest) : this(
    disposition = DispositionOfProblem.calcDisposition(HttpStatus.valueOf(problemDetail.status)),
    method = request.method.name().lowercase(),
    instance = request.uri.path.lowercase(),
    message = responseMessage ?: problemDetail.detail ?: "no detail available for this problem",
    size = calcSize(problemDetail),
    content = ApiProblemDetail(problemDetail),
  )
}

@Serializable
data class ResponseSuccess<T>(
  @Serializable(with = UUIDSerializer::class) override val id: UUID = UUID.randomUUID(),
  override val disposition: DispositionOfSuccess,
  override val method: String,
  override val instance: String?,
  override val message: String,
  override val size: Int,
  @Contextual override val content: T,
) : Response<T> {

  /** Create an API ResponseSuccess with content from any object type
   * @return ResponseSuccess<*>
   */
  constructor(responseContent: T, responseMessage: String, request: HttpServletRequest) : this(
    disposition = DispositionOfSuccess.SUCCESS,
    method = request.method.lowercase(),
    instance = request.requestURI,
    message = responseMessage,
    size = calcSize(responseContent),
    content = responseContent,
  )

  /** Create an API ResponseSuccess with content from any object type
   * @return ResponseSuccess<*>
   */
  constructor(responseContent: T, responseMessage: String, request: ServerHttpRequest) : this(
    disposition = DispositionOfSuccess.SUCCESS,
    method = request.method.name().lowercase(),
    instance = request.uri.path.lowercase(),
    message = responseMessage,
    size = calcSize(responseContent),
    content = responseContent,
  )
}

/** Calculate the number of items included in the content
 * return Int
 */
private fun calcSize(content: Any?): Int = when (content) {
  null -> 0
  is Collection<*> -> content.size
  is Map<*, *> -> content.size
  else -> 1
}
