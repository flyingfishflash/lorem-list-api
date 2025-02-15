package net.flyingfishflash.loremlist.core.configuration

import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import net.flyingfishflash.loremlist.domain.SuccinctLrmComponent
import net.flyingfishflash.loremlist.domain.lrmitem.LrmItemSuccinct
import net.flyingfishflash.loremlist.domain.lrmlist.LrmListSuccinct
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.converter.json.KotlinSerializationJsonHttpMessageConverter

@Configuration
class SerializationConfig {

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
}
