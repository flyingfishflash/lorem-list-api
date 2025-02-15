package net.flyingfishflash.loremlist.api.data.response

import kotlinx.serialization.Serializable

@Serializable
data class LrmItemDeletedResponse(val itemNames: List<String>, val associatedListNames: List<String>)
