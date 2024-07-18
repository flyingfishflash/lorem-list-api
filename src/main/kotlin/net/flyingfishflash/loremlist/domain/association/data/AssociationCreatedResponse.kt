package net.flyingfishflash.loremlist.domain.association.data

import kotlinx.serialization.Serializable
import net.flyingfishflash.loremlist.domain.SuccinctLrmComponent

@Serializable
data class AssociationCreatedResponse(val componentName: String, val associatedComponents: List<SuccinctLrmComponent>)
