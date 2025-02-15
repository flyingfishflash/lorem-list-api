package net.flyingfishflash.loremlist.api.data.response

import kotlinx.serialization.Serializable

@Serializable
data class LrmListDeletedResponse(val listNames: List<String>, val associatedItemNames: List<String>)
