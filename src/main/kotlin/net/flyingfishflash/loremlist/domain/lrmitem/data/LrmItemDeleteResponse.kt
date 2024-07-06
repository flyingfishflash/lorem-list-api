package net.flyingfishflash.loremlist.domain.lrmitem.data

import kotlinx.serialization.Serializable

@Serializable
data class LrmItemDeleteResponse(
  val associatedListCount: Long,
  val associatedListNames: List<String>,
) 
