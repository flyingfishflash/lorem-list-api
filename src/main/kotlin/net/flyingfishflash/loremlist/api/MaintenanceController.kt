package net.flyingfishflash.loremlist.api

import io.github.oshai.kotlinlogging.KotlinLogging
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.flyingfishflash.loremlist.api.data.response.DomainPurgedResponse
import net.flyingfishflash.loremlist.core.response.structure.ResponseProblem
import net.flyingfishflash.loremlist.core.response.structure.ResponseSuccess
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "domain maintenance")
@ApiResponses(
  value = [
    ApiResponse(
      responseCode = "400",
      description = "Bad Request",
      content = [Content(schema = Schema(implementation = ResponseProblem::class))],
    ),
  ],
)
@RestController
@RequestMapping("/maintenance")
class MaintenanceController(private val maintenanceApiService: MaintenanceApiService) {
  private val logger = KotlinLogging.logger {}

  @Operation(summary = "Purge all items, lists, and associations")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Successful purge of all domain objects",
      ),
    ],
  )
  @DeleteMapping("/purge")
  fun purge(request: HttpServletRequest): ResponseEntity<ResponseSuccess<DomainPurgedResponse>> {
    val apiServiceResponse = maintenanceApiService.purge()
    val response = ResponseSuccess(apiServiceResponse.content, apiServiceResponse.message, request)
    val responseStatus = HttpStatus.OK
    logger.info { Json.encodeToString(response) }
    return ResponseEntity(response, responseStatus)
  }
}
