package net.flyingfishflash.loremlist.core.response.structure

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import org.springframework.http.ProblemDetail

@Serializable
data class ApiProblemDetail(
  val type: String,
  val title: String,
  val status: Int,
  val detail: String,
  val extensions: JsonElement? = null,
) {
  /** Construct an ApiProblem from a Spring ProblemDetail */
  constructor(problemDetail: ProblemDetail) : this(
    type = problemDetail.type.toString(),
    title = problemDetail.title ?: "default title",
    status = problemDetail.status,
    detail = problemDetail.detail ?: "default detail",
  )
}
