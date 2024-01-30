package net.flyingfishflash.loremlist.core.configuration

// import io.swagger.v3.oas.annotations.responses.ApiResponses
// import java.lang.reflect.AnnotatedType
import io.swagger.v3.core.converter.ModelConverters
import io.swagger.v3.oas.models.OpenAPI
import net.flyingfishflash.loremlist.core.response.structure.ApplicationResponse
import org.jetbrains.exposed.spring.autoconfigure.ExposedAutoConfiguration
import org.jetbrains.exposed.sql.DatabaseConfig
import org.springdoc.core.customizers.OpenApiCustomizer
import org.springframework.beans.factory.annotation.Autowired
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
  @Autowired
  var openAPIService: OpenAPI? = null

  @Bean
  fun databaseConfig() =
    DatabaseConfig {
      useNestedTransactions = true
    }

//  @Bean
//  fun customize(): OperationCustomizer {
//    return OperationCustomizer { operation: Operation, method: HandlerMethod ->
//      val responses: ApiResponses = operation.responses
//      if (method.method.returnType == LrmList::class.java) {
//        val type = ((method.method.genericReturnType as ParameterizedType).actualTypeArguments[0] as ParameterizedType).actualTypeArguments[0]
//
//        val resolvedSchema = ModelConverters.getInstance().resolveAsResolvedSchema(AnnotatedType(type))
//
//        val schemas = openAPIService?.components?.schemas
// //        val schemas: MutableMap<String, Schema> = openAPIService.getCalculatedOpenAPI().getComponents().getSchemas()
//
//        //schemas[resolvedSchema.schema.name] = resolvedSchema.schema
//
//        val schema = ObjectSchema()
//
//          .type("object")
//
//          .addProperty("data", schemas?.get(resolvedSchema?.schema?.name) )
//
//          .name("ResponseBody<$resolvedSchema.schema.name>")
//
//        schemas?.set("ResponseBody<$resolvedSchema.schema.name>", schema)
//
//        responses.addApiResponse(
//          "Success",
//          ApiResponse().content(
//            Content().addMediaType(
//              "application/json",
//              MediaType().schema(ObjectSchema().`$ref`(schema.name))
//            )
//          )
//        )
//      }
//      operation
//    }
//  }

//
//  @Bean
//  fun customize1(): OperationCustomizer {
//    return OperationCustomizer { operation, handlerMethod ->
//      val responses = operation.responses
//      if (handlerMethod.method.returnType == ResponseEntity::class.java) {
//        val type: Type = ((handlerMethod.getMethod()
//          .getGenericReturnType() as ParameterizedType).actualTypeArguments[0] as ParameterizedType).actualTypeArguments[0]
//        val resolvedSchema = ModelConverters.getInstance().resolveAsResolvedSchema(AnnotatedType(type))
//        //val schemas = openAPIService.components.schemas.entries.
//    }
//      return operation
//
//    }
//  }

  @Bean
  fun customOpenApiCustomiser(): OpenApiCustomizer {
    return OpenApiCustomizer { openApi ->

      // SpringDocUtils.getConfig().replaceWithClass(ProblemDetail::class.java, ProblemDetail::class.java)

      // add schema for ProblemDetail class
      ModelConverters
        .getInstance()
        .readAllAsResolvedSchema(ApplicationResponse::class.java)
        .referencedSchemas
        .forEach { (k, v) -> openApi.components.addSchemas(k, v) }

      openApi.paths.forEach { pie -> { pie.value.readOperationsMap() } }

      // identify all error responses and set the response schema to #/components/schemas/ProblemDetail
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
//      println(z.toString())
//      openApi.paths.forEach { (k, v) -> v.get.responses?.forEach { println(it.toString()) } }
    }
  }
}
