package net.flyingfishflash.loremlist.api.data.response

import kotlinx.serialization.Serializable
import net.flyingfishflash.loremlist.domain.SuccinctLrmComponent

@Serializable
data class LrmListItemAddedResponse(val componentName: String, val associatedComponents: List<SuccinctLrmComponent>)
