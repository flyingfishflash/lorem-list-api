package net.flyingfishflash.loremlist.core.response.structure

import com.fasterxml.jackson.annotation.JsonInclude
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import org.springframework.http.ProblemDetail

/** RFC 9457 - Problem Details for HTTP APIs **/
@Serializable
@JsonInclude(JsonInclude.Include.NON_EMPTY)
data class ApiProblemDetail(
  val type: String,
  val title: String,
  val status: Int,
  val detail: String?,
  val cause: ExceptionCauseDetail? = null,
  val validationErrors: List<String>? = null,
  val supplemental: Map<String, JsonElement>? = null,
  val stackTrace: List<String>? = null,
) {
  /** Construct an ApiProblemDetail from a Spring ProblemDetail */
  constructor(problemDetail: ProblemDetail) : this(
    type = problemDetail.type.toString(),
    title = problemDetail.title ?: "default title",
    status = problemDetail.status,
    detail = problemDetail.detail ?: "default detail",
  )
}

/** Root cause of the exception, if applicable */
@Serializable
data class ExceptionCauseDetail(
  val name: String,
  val message: String?,
)
