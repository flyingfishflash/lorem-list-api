package net.flyingfishflash.loremlist.domain.maintenance.data

import kotlinx.serialization.Serializable

@Serializable
data class PurgeResponse(
  val associationDeletedCount: Int,
  val itemDeletedCount: Int,
  val listDeletedCount: Int,
)
