package net.flyingfishflash.loremlist.domain.lrmitem

import io.github.oshai.kotlinlogging.KotlinLogging
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.flyingfishflash.loremlist.core.response.structure.ApiMessage
import net.flyingfishflash.loremlist.core.response.structure.ResponseProblem
import net.flyingfishflash.loremlist.core.response.structure.ResponseSuccess
import net.flyingfishflash.loremlist.domain.lrmitem.data.LrmItem
import net.flyingfishflash.loremlist.domain.lrmitem.data.LrmItemRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "item controller")
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
@RequestMapping("/items")
class LrmItemController(val lrmItemService: LrmItemService) {
  private val logger = KotlinLogging.logger {}

  @Operation(summary = "Create a new item")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Item Created",
      ),
    ],
  )
  @PostMapping
  fun create(
    @Valid @RequestBody lrmItemRequest: LrmItemRequest,
    request: HttpServletRequest,
  ): ResponseEntity<ResponseSuccess<LrmItem>> {
    val responseStatus = HttpStatus.OK
    val responseContent = lrmItemService.create(lrmItemRequest)
    val responseMessage = "created new item"
    val response = ResponseSuccess(responseContent, responseMessage, request)
    logger.info { "response as json: " + Json.encodeToString(response) }
    return ResponseEntity(response, responseStatus)
  }

  @Operation(summary = "Delete an item")
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "Item Deleted"),
      ApiResponse(
        responseCode = "404",
        description = "Item Not Found",
        content = [Content(schema = Schema(implementation = ResponseProblem::class))],
      ),
    ],
  )
  @DeleteMapping("/{id}")
  fun delete(
    @PathVariable("id") @Min(1) id: Long,
    request: HttpServletRequest,
  ): ResponseEntity<ResponseSuccess<ApiMessage>> {
    lrmItemService.deleteSingleById(id)
    val responseStatus = HttpStatus.OK
    val responseMessage = "deleted item id $id"
    val responseContent = ApiMessage("content")
    val response = ResponseSuccess(responseContent, responseMessage, request)
    logger.info { "response as json: " + Json.encodeToString(response) }
    return ResponseEntity(response, responseStatus)
  }

  @Operation(summary = "Retrieve all items details and optionally the lists the item belongs to")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "all items",
      ),
    ],
  )
  @GetMapping
  fun findAll(
    @RequestParam(defaultValue = false.toString()) withLists: Boolean,
    request: HttpServletRequest,
  ): ResponseEntity<ResponseSuccess<List<LrmItem>>> {
    val responseStatus = HttpStatus.OK
    val responseMessage = "retrieved all items"
    val responseContent = if (withLists) lrmItemService.findAllAndLists() else lrmItemService.findAll()
    val response = ResponseSuccess(responseContent, responseMessage, request)
    logger.info { "response as json: " + Json.encodeToString(response) }
    return ResponseEntity(response, responseStatus)
  }

  @PostMapping("/{id}/to-list")
  @Operation(summary = "Assign Item to List")
  fun toList(
    @PathVariable("id") @Min(1) id: Long,
    @Min(1) @RequestBody listId: Long,
    request: HttpServletRequest,
  ): ResponseEntity<ResponseSuccess<ApiMessage>> {
    lrmItemService.assignToList(itemId = id, listId = listId)
    val responseStatus = HttpStatus.OK
    val responseMessage = "assigned list item to list"
    val responseContent = ApiMessage("assigned list item to list")
    val response = ResponseSuccess(responseContent, responseMessage, request)
    return ResponseEntity(response, responseStatus)
  }
}
