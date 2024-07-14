package net.flyingfishflash.loremlist.domain.maintenance

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
import net.flyingfishflash.loremlist.core.response.structure.ResponseProblem
import net.flyingfishflash.loremlist.core.response.structure.ResponseSuccess
import net.flyingfishflash.loremlist.domain.maintenance.data.PurgeResponse
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
class MaintenanceController(private val maintenanceService: MaintenanceService) {
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
  fun purge(request: HttpServletRequest): ResponseEntity<ResponseSuccess<PurgeResponse>> {
    val serviceResponse = maintenanceService.purge()
    val responseStatus = HttpStatus.OK
    val responseMessage = "Deleted all items, lists, and associations."
    val response = ResponseSuccess(serviceResponse, responseMessage, request)
    logger.info { Json.encodeToString(response) }
    return ResponseEntity(response, responseStatus)
  }
}
