package net.flyingfishflash.loremlist.domain.lrmlist.data

// import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import kotlinx.serialization.Serializable

@Serializable
data class LrmListRequest(
//  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  @field:NotEmpty
  val name: String,
  @field:Pattern(
    regexp = "^(?!\\s*$).+",
    message = "may be null, must not be an empty string, must not consist only of spaces",
  )
  @field:Size(max = 2048)
  val description: String? = null,
)
