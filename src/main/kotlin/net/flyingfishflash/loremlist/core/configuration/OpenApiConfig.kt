package net.flyingfishflash.loremlist.core.configuration

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.media.Schema
import kotlinx.datetime.Clock.System.now
import org.springdoc.core.customizers.OpenApiCustomizer
import org.springframework.boot.info.BuildProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig(private val buildProperties: BuildProperties) {

  @Bean
  fun customOpenAPI(): OpenAPI {
    return OpenAPI().info(
      Info()
        .contact(
          Contact()
            .name("flyingfishflash")
            .url("https://codeberg.org/lorem-list/"),
        )
        .description("Simple List Management API")
        .title("Lorem List Api")
        .version(buildProperties.version),
    )
  }

  @Bean
  fun sortSchemasAlphabetically(): OpenApiCustomizer {
    return OpenApiCustomizer { openApi ->
      openApi.components.schemas(openApi.components.schemas.toList().sortedBy { (key, _) -> key }.toMap())
    }
  }

  /**
   *  Override the generated example json for the Instant class in order to remove two properties:
   *  - epochSeconds
   *  - nanosecondsOfSecond
   */
  @Bean
  fun customizeInstantSchema(): OpenApiCustomizer {
    return OpenApiCustomizer {
      val components = it.components
      if (components != null) {
        val schema: Schema<*>? = components.schemas["Instant"]
        if (schema != null && schema.properties != null) {
          schema.example(now().toString())
        }
      }
    }
  }
}
