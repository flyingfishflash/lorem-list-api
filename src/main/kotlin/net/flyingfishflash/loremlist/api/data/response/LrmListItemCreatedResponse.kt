package net.flyingfishflash.loremlist.api.data.response

import kotlinx.serialization.Serializable
import net.flyingfishflash.loremlist.domain.SuccinctLrmComponent

@Serializable
data class LrmListItemCreatedResponse(val lrmListItem: LrmListItemResponse, val associatedComponents: List<SuccinctLrmComponent>)
