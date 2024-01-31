package net.flyingfishflash.loremlist.core.configuration

import org.springdoc.core.customizers.OpenApiCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {
  @Bean
  fun sortSchemasAlphabetically(): OpenApiCustomizer {
    return OpenApiCustomizer { openApi ->
      openApi.components.schemas(openApi.components.schemas.toList().sortedBy { (key, _) -> key }.toMap())
    }
  }
}
