package net.flyingfishflash.loremlist.api.data.response

import kotlinx.serialization.Serializable

@Serializable
data class AssociationsDeletedResponse(val itemName: String, val deletedAssociationsCount: Int)
