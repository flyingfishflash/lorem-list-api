package net.flyingfishflash.loremlist.core.response.structure

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import org.springframework.http.ProblemDetail

@Serializable
data class ApiProblemDetail(
  val type: String,
  val title: String? = null,
  val status: Int,
  val detail: String? = null,
  val extensions: JsonElement? = null,
) {
  constructor(problemDetail: ProblemDetail) : this(
    type = problemDetail.type.toString(),
    title = problemDetail.title,
    status = problemDetail.status,
    detail = problemDetail.detail,
  ) {
    // ProblemDetailUtility.setCustomPropertiesFromThrowable(problemDetail, o)
  }
}
