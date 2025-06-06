package net.flyingfishflash.loremlist.api

import io.github.oshai.kotlinlogging.KotlinLogging
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.flyingfishflash.loremlist.api.data.response.LrmListResponse
import net.flyingfishflash.loremlist.core.response.structure.ResponseSuccess
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "public")
@RestController
class LrmListPublicController(val lrmListService: LrmListApiService) {
  private val logger = KotlinLogging.logger {}

  @Operation(summary = "Retrieve all public lists, optionally including the details of each associated item.")
  @GetMapping("/public/lists")
  fun findByPublic(
    @RequestParam(defaultValue = false.toString()) includeItems: Boolean,
    request: HttpServletRequest,
  ): ResponseEntity<ResponseSuccess<List<LrmListResponse>>> {
    val apiServiceResponse = if (includeItems) lrmListService.findByPublic() else lrmListService.findByPublicExcludeItems()
    val response = ResponseSuccess(apiServiceResponse.content.map { it }, apiServiceResponse.message, request)
    logger.info { Json.encodeToString(response) }
    return ResponseEntity(response, HttpStatus.OK)
  }
}
