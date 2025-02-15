package net.flyingfishflash.loremlist.api.data.response

import kotlinx.serialization.Serializable

@Serializable
data class DomainPurgedResponse(val associationDeletedCount: Int, val itemDeletedCount: Int, val listDeletedCount: Int)
