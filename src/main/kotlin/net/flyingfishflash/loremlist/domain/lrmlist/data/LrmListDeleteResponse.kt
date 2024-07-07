package net.flyingfishflash.loremlist.domain.lrmlist.data

import kotlinx.serialization.Serializable

@Serializable
data class LrmListDeleteResponse(
  val associatedItemCount: Long,
  val associatedItemNames: List<String>,
) 
