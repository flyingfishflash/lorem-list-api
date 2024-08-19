package net.flyingfishflash.loremlist.core.configuration

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.security.OAuthFlow
import io.swagger.v3.oas.models.security.OAuthFlows
import io.swagger.v3.oas.models.security.Scopes
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.servers.Server
import jakarta.servlet.ServletContext
import kotlinx.datetime.Clock.System.now
import org.springdoc.core.customizers.OpenApiCustomizer
import org.springframework.boot.info.BuildProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig(private val buildProperties: BuildProperties) {

  @Bean
  fun openAPI(servletContext: ServletContext): OpenAPI {
    return OpenAPI()
      .components(
        Components().addSecuritySchemes("Zitadel OIDC", createOAuthScheme()),
      ).addSecurityItem(SecurityRequirement().addList("Zitadel OIDC"))
      .servers(listOf(Server().url(servletContext.contextPath)))
      .info(
        Info()
          .contact(
            Contact()
              .name("flyingfishflash")
              .url("https://codeberg.org/lorem-list/"),
          )
          .description("List Management API")
          .title("Lorem List Api")
          .version(buildProperties.version),
      )
  }

  private fun createOAuthScheme(): SecurityScheme {
    val flows: OAuthFlows = createOAuthFlows()
    return SecurityScheme().type(SecurityScheme.Type.OAUTH2)
      .flows(flows)
  }

  private fun createOAuthFlows(): OAuthFlows {
    val flow: OAuthFlow = createAuthorizationCodeFlow()
    return OAuthFlows().authorizationCode(flow)
  }

  private fun createAuthorizationCodeFlow(): OAuthFlow {
    return OAuthFlow()
      .authorizationUrl("https://zitadel.flyingfishflash.net/oauth/v2/authorize")
      .tokenUrl("https://zitadel.flyingfishflash.net/oauth/v2/token")
      .scopes(
        Scopes()
          .addString("openid", "")
          .addString("email", "")
          .addString("profile", "")
          .addString("offline_access", ""),
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
