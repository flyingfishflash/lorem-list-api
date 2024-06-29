package net.flyingfishflash.loremlist.domain.lrmitem.data

import kotlinx.serialization.Serializable

@Serializable
data class LrmItemDeleteResponse(
  val listAssociations: Long,
  val associatedListNames: List<String>,
) 
