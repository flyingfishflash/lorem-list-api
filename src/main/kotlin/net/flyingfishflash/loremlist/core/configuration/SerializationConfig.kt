package net.flyingfishflash.loremlist.core.configuration

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.module.SimpleModule
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import net.flyingfishflash.loremlist.domain.SuccinctLrmComponent
import net.flyingfishflash.loremlist.domain.lrmitem.LrmItemSuccinct
import net.flyingfishflash.loremlist.domain.lrmlist.LrmListSuccinct
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.converter.json.KotlinSerializationJsonHttpMessageConverter
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

/**
 * During the Kotlin -> Java migration, types still annotated `@Serializable` must be handed to
 * the kotlinx converter, while types already converted to plain Java records fall through to
 * Jackson. [KotlinSerializationJsonHttpMessageConverter.canWrite] declines any type it has no
 * serializer for, so ordering the kotlinx converter first lets Jackson pick up the remainder -
 * but only for the *outermost* response type. Once that outer type is a Java record (as
 * `ResponseSuccess`/`ResponseProblem` now are), Jackson serializes the entire nested object graph
 * itself, including still-`@Serializable` Kotlin types inside it - it never delegates back to
 * kotlinx for nested fields. Types with no default Jackson representation (like
 * `kotlinx.datetime.Instant`, registered below) need an explicit Jackson module regardless of
 * whether the type itself has been converted to Java yet.
 */
@Configuration
class SerializationConfig : WebMvcConfigurer {

  val module = SerializersModule {
    polymorphic(SuccinctLrmComponent::class) {
      subclass(LrmListSuccinct::class)
      subclass(LrmItemSuccinct::class)
    }
  }

  @Bean
  fun messageConverter(): KotlinSerializationJsonHttpMessageConverter {
    return KotlinSerializationJsonHttpMessageConverter(
      Json {
        serializersModule = module
      },
    )
  }

  @Bean
  fun jsonFormat(): Json {
    return Json {
      serializersModule = module
    }
  }

  @Bean
  fun kotlinxInstantModule(): SimpleModule {
    return SimpleModule().apply {
      addSerializer(
        Instant::class.java,
        object : JsonSerializer<Instant>() {
          override fun serialize(value: Instant, gen: JsonGenerator, serializers: SerializerProvider) {
            gen.writeString(value.toString())
          }
        },
      )
      addDeserializer(
        Instant::class.java,
        object : JsonDeserializer<Instant>() {
          override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Instant {
            return Instant.parse(p.valueAsString)
          }
        },
      )
    }
  }

  override fun extendMessageConverters(converters: MutableList<HttpMessageConverter<*>>) {
    val kotlinConverter = converters.find { it is KotlinSerializationJsonHttpMessageConverter } ?: return
    converters.remove(kotlinConverter)
    converters.add(0, kotlinConverter)
  }
}
