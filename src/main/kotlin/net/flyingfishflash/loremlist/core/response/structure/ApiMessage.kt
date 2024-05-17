package net.flyingfishflash.loremlist.core.response.structure

import kotlinx.serialization.Serializable

@Serializable
data class ApiMessage(
  val message: String,
)

@Serializable
data class ApiMessageNumeric(
  val value: Long,
)
