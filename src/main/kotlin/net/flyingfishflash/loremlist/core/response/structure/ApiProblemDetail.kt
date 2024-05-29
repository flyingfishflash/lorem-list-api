package net.flyingfishflash.loremlist.core.response.structure

import kotlinx.serialization.Serializable
import org.springframework.http.ProblemDetail

@Serializable
data class ApiProblemDetail(
  val type: String,
  val title: String,
  val status: Int,
  val detail: String,
  val extensions: ApiProblemDetailExtensions? = null,
) {
  /** Construct an ApiProblem from a Spring ProblemDetail */
  constructor(problemDetail: ProblemDetail) : this(
    type = problemDetail.type.toString(),
    title = problemDetail.title ?: "default title",
    status = problemDetail.status,
    detail = problemDetail.detail ?: "default detail",
  )
}

@Serializable
data class ApiProblemDetailExtensions(
  val validationErrors: List<String>? = null,
  val stackTrace: List<String>? = null,
)
