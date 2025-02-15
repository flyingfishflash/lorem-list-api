package net.flyingfishflash.loremlist.api.data.response

data class ApiServiceResponse<T>(val content: T, val message: String)
