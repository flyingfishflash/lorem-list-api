package net.flyingfishflash.loremlist.domain.lrmlist.data.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size

data class LrmListRecord(
  @Size(max = 2048)
  @NotEmpty
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  val name: String,
)
