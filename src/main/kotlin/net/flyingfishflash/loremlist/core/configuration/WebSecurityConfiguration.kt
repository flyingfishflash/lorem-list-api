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
import org.springframework.security.web.util.matcher.AntPathRequestMatcher
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
        authorize(AntPathRequestMatcher("favicon.ico", HttpMethod.GET.name()), permitAll)
        authorize(AntPathRequestMatcher("/v3/api-docs/**", HttpMethod.GET.name()), permitAll)
        authorize(AntPathRequestMatcher("/swagger-resources/**", HttpMethod.GET.name()), permitAll)
        authorize(AntPathRequestMatcher("/swagger-ui/**", HttpMethod.GET.name()), permitAll)
        authorize(AntPathRequestMatcher("/webjars/**", HttpMethod.GET.name()), permitAll)
        authorize(AntPathRequestMatcher("/management/health", HttpMethod.GET.name()), permitAll)
        authorize(AntPathRequestMatcher("/management/info", HttpMethod.GET.name()), permitAll)
        authorize(AntPathRequestMatcher("/public/lists", HttpMethod.GET.name()), permitAll)
        authorize(AntPathRequestMatcher("/public/lists/count", HttpMethod.GET.name()), permitAll)
        authorize(anyRequest, authenticated)
//        hasAuthority("SCOPE_administrator")
      }
    }
    return http.build()
  }
}
