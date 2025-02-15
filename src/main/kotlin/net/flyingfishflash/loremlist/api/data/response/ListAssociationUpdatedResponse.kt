package net.flyingfishflash.loremlist.api.data.response

import kotlinx.serialization.Serializable

@Serializable
data class ListAssociationUpdatedResponse(val itemName: String, val currentListName: String, val newListName: String)
