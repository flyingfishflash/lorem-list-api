package net.flyingfishflash.loremlist.api.data.response

import kotlinx.serialization.Serializable

@Serializable
data class AssociationDeletedResponse(val itemName: String, val listName: String)
