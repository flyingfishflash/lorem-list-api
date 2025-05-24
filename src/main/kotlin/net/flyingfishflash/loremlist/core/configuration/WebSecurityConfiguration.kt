package net.flyingfishflash.loremlist.core.configuration

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter
import org.springframework.security.web.SecurityFilterChain
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@EnableWebSecurity
class WebSecurityConfiguration {

  @Bean
  fun corsConfigurationSource(): CorsConfigurationSource {
    val configuration = CorsConfiguration()
    configuration.setAllowedOriginPatterns(listOf("*"))
    configuration.allowedMethods = listOf("*")
    configuration.allowedHeaders = listOf("*")
    configuration.allowCredentials = true
    configuration.exposedHeaders = listOf("X-Auth-Token", "Authorization")
    val source = UrlBasedCorsConfigurationSource()
    source.registerCorsConfiguration("/**", configuration)
    return source
  }

  @Bean
  fun jwtAuthenticationConverter(): JwtAuthenticationConverter {
    val grantedAuthoritiesConverter = JwtGrantedAuthoritiesConverter()
    grantedAuthoritiesConverter.setAuthoritiesClaimName("grants")

    val jwtAuthenticationConverter = JwtAuthenticationConverter()
    jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter)
    return jwtAuthenticationConverter
  }

  @Bean
  fun filterChain(http: HttpSecurity): SecurityFilterChain {
    http {
      cors { }
      csrf { disable() }
      oauth2ResourceServer {
        jwt { }
      }
      sessionManagement { sessionCreationPolicy = SessionCreationPolicy.STATELESS }
      authorizeHttpRequests {
        authorize(HttpMethod.GET, "favicon.ico", permitAll)
        authorize(HttpMethod.GET, "/v3/api-docs/**", permitAll)
        authorize(HttpMethod.GET, "/swagger-resources/**", permitAll)
        authorize(HttpMethod.GET, "/swagger-ui/**", permitAll)
        authorize(HttpMethod.GET, "/webjars/**", permitAll)
        authorize(HttpMethod.GET, "/management/health", permitAll)
        authorize(HttpMethod.GET, "/management/info", permitAll)
        authorize(HttpMethod.GET, "/public/lists", permitAll)
        authorize(HttpMethod.GET, "/public/lists/count", permitAll)
        authorize(anyRequest, authenticated)
//        hasAuthority("SCOPE_administrator")
      }
    }
    return http.build()
  }
}
