package net.flyingfishflash.loremlist.core.configuration

import org.jetbrains.exposed.spring.autoconfigure.ExposedAutoConfiguration
import org.jetbrains.exposed.sql.DatabaseConfig
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.autoconfigure.data.jdbc.JdbcRepositoriesAutoConfiguration
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@ImportAutoConfiguration(
  value = [ExposedAutoConfiguration::class],
  exclude = [
    DataSourceTransactionManagerAutoConfiguration::class,
    HibernateJpaAutoConfiguration::class,
    JdbcRepositoriesAutoConfiguration::class,
    JdbcTemplateAutoConfiguration::class,
  ],
)
class ExposedConfig {
  @Bean
  fun databaseConfig() =
    DatabaseConfig {
      useNestedTransactions = true
    }

//  @Bean
//  fun customOpenApiCustomiser(): OpenApiCustomizer {
//    return OpenApiCustomizer { openApi ->
//
//      SpringDocUtils.getConfig().replaceWithClass(ProblemDetail::class.java, ProblemDetail::class.java)
//
//      // add schema for ProblemDetail class
//      ModelConverters
//        .getInstance()
//        .readAllAsResolvedSchema(ProblemDetail::class.java)
//        .referencedSchemas
//        .forEach { (k, v) -> openApi.components.addSchemas(k, v) }
//
//      // identify all error responses and set the response schema to #/components/schemas/ProblemDetail
//      openApi.paths.forEach { pathItemEntry ->
//        pathItemEntry.value.readOperations().forEach { operation ->
//          operation.responses.filter {
//              (k, v) ->
//            pathItemEntry.key.startsWith("/lists/{id}") && k.startsWith("404") || k.startsWith("5")
//          }.forEach { apiResponse ->
//            println(pathItemEntry.key.toString() + ":::" + apiResponse.key.toString() + ":::" + apiResponse.value.description.toString())
//            // apiResponse.value.description = "thi is the new 404 description"
// //            apiResponse.value.`$ref` = "#/components/schemas/ProblemDetail"
//            // apiResponse.value.content(Content().addMediaType(MediaType.APPLICATION_PROBLEM_JSON_VALUE, io.swagger.v3.oas.models.media.MediaType().schema(
//          }
//        }
//      }

//      val z = openApi.components.schemas["ProblemDetail"]
  // println(z.toString())

//      openApi.paths.forEach { (k, v) -> v.get.responses?.forEach { println(it.toString()) } }
//    }
//  }
}
