package net.flyingfishflash.loremlist.core.response.structure

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.springframework.http.ProblemDetail

/** Required members as defined in RFC 9457 - Problem Details for HTTP APIs: 3.1 **/
@Serializable
@JsonInclude(JsonInclude.Include.NON_EMPTY)
data class ApiProblemDetail(
  val type: String,
  val title: String,
  val status: Int,
  val detail: String?,
  val extensions: ApiProblemDetailExtensions? = null,
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

/** Extension members as permitted by RFC 9457 - Problem Details for HTTP APIs: 3.2 **/
@Serializable
@JsonIgnoreProperties(value = ["eachExtensionNull"])
data class ApiProblemDetailExtensions(
  val cause: ExceptionCauseDetail? = null,
  val stackTrace: List<String>? = null,
  val validationErrors: List<String>? = null,
  @Transient
  val isEachExtensionNull: Boolean = (cause == null && validationErrors == null && stackTrace == null),
)
