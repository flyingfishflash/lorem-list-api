package net.flyingfishflash.loremlist

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class LoremListApplication

private val jsonElementMapper = ObjectMapper()

fun Any?.toJsonElement(): JsonNode = jsonElementMapper.valueToTree(this)

fun main(args: Array<String>) {
  runApplication<LoremListApplication>(*args)
}
