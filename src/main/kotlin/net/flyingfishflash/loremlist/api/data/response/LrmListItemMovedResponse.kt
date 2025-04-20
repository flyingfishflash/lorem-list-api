package net.flyingfishflash.loremlist.api.data.response

import kotlinx.serialization.Serializable

@Serializable
data class LrmListItemMovedResponse(val itemName: String, val currentListName: String, val newListName: String)
