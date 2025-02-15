package net.flyingfishflash.loremlist.domain.association.data

import net.flyingfishflash.loremlist.domain.SuccinctLrmComponent

data class AssociationCreated(val componentName: String, val associatedComponents: List<SuccinctLrmComponent>)
