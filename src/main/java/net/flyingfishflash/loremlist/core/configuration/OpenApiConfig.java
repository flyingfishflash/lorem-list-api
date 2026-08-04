package net.flyingfishflash.loremlist.core.configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.Scopes;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import jakarta.servlet.ServletContext;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import kotlinx.datetime.Clock;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

  private final BuildProperties buildProperties;

  public OpenApiConfig(BuildProperties buildProperties) {
    this.buildProperties = buildProperties;
  }

  @Bean
  public OpenAPI openAPI(ServletContext servletContext) {
    return new OpenAPI()
        .components(new Components().addSecuritySchemes("Zitadel OIDC", createOAuthScheme()))
        .addSecurityItem(new SecurityRequirement().addList("Zitadel OIDC"))
        .servers(List.of(new Server().url(servletContext.getContextPath())))
        .info(
            new Info()
                .contact(
                    new Contact().name("flyingfishflash").url("https://codeberg.org/lorem-list/"))
                .description("List Management API")
                .title("Lorem List Api")
                .version(buildProperties.getVersion()));
  }

  private SecurityScheme createOAuthScheme() {
    OAuthFlows flows = createOAuthFlows();
    return new SecurityScheme().type(SecurityScheme.Type.OAUTH2).flows(flows);
  }

  private OAuthFlows createOAuthFlows() {
    OAuthFlow flow = createAuthorizationCodeFlow();
    return new OAuthFlows().authorizationCode(flow);
  }

  private OAuthFlow createAuthorizationCodeFlow() {
    return new OAuthFlow()
        .authorizationUrl("https://zitadel.flyingfishflash.net/oauth/v2/authorize")
        .tokenUrl("https://zitadel.flyingfishflash.net/oauth/v2/token")
        .scopes(
            new Scopes()
                .addString("openid", "")
                .addString("email", "")
                .addString("profile", "")
                .addString("offline_access", ""));
  }

  @Bean
  public OpenApiCustomizer sortSchemasAlphabetically() {
    return openApi -> {
      Map<String, Schema> sorted = new TreeMap<>(openApi.getComponents().getSchemas());
      openApi.getComponents().setSchemas(sorted);
    };
  }

  /**
   * Override the generated example json for the Instant class in order to remove two properties:
   * epochSeconds, nanosecondsOfSecond
   */
  @Bean
  public OpenApiCustomizer customizeInstantSchema() {
    return openApi -> {
      Components components = openApi.getComponents();
      if (components != null) {
        Schema<?> schema = components.getSchemas().get("Instant");
        if (schema != null && schema.getProperties() != null) {
          schema.example(Clock.System.INSTANCE.now().toString());
        }
      }
    };
  }
}
