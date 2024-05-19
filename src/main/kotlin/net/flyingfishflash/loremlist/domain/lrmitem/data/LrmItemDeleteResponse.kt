package net.flyingfishflash.loremlist.domain.lrmitem.data

import kotlinx.serialization.Serializable

@Serializable
data class LrmItemDeleteResponse(
  val countItemToListAssociations: Long,
  val associatedListNames: List<String>,
) 
