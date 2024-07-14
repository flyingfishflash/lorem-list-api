package net.flyingfishflash.loremlist.domain.lrmitem.data

import kotlinx.serialization.Serializable

@Serializable
data class LrmItemDeleteResponse(
  val itemNames: List<String>,
  val associatedListNames: List<String>,
) 
