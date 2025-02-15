package net.flyingfishflash.loremlist.domain

data class ServiceResponse<T>(val content: T, val message: String)
