package net.flyingfishflash.loremlist.domain.lrmlist.data

import kotlinx.serialization.Serializable

@Serializable
data class LrmListDeleteResponse(val listNames: List<String>, val associatedItemNames: List<String>)
