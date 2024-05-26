package net.flyingfishflash.loremlist.domain.lrmlist

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
import net.flyingfishflash.loremlist.core.response.structure.ApiMessageNumeric
import net.flyingfishflash.loremlist.core.response.structure.ResponseProblem
import net.flyingfishflash.loremlist.core.response.structure.ResponseSuccess
import net.flyingfishflash.loremlist.domain.common.CommonService
import net.flyingfishflash.loremlist.domain.lrmlist.data.LrmListRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "list controller")
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
@RequestMapping("/lists")
class LrmListController(private val commonService: CommonService, private val lrmListService: LrmListService) {
  private val logger = KotlinLogging.logger {}

  @GetMapping("/count")
  @Operation(summary = "Count of all lists")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Successful count of all lists",
      ),
    ],
  )
  fun count(request: HttpServletRequest): ResponseEntity<ResponseSuccess<ApiMessageNumeric>> {
    val serviceResponse = lrmListService.count()
    val responseMessage = "$serviceResponse lists."
    val responseStatus = HttpStatus.OK
    val responseContent = ApiMessageNumeric(serviceResponse)
    val response = ResponseSuccess(responseContent, responseMessage, request)
    logger.info { Json.encodeToString(response) }
    return ResponseEntity(response, responseStatus)
  }

  @Operation(summary = "Create a new list")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "List Created",
//        content = [Content(schema = Schema(implementation = ResponseLrmList::class))],
      ),
    ],
  )
  @PostMapping
  fun create(@Valid @RequestBody lrmListRequest: LrmListRequest, request: HttpServletRequest): ResponseEntity<ResponseSuccess<LrmList>> {
    val responseStatus = HttpStatus.OK
    val responseContent = lrmListService.create(lrmListRequest)
    val responseMessage = "created new list"
    val response = ResponseSuccess(responseContent, responseMessage, request)
    logger.info { Json.encodeToString(response) }
    return ResponseEntity(response, responseStatus)
  }

  @GetMapping("/{id}/count-item-associations")
  @Operation(summary = "Count of items associated with a list")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Count of item associations retrieved",
      ),
      ApiResponse(
        responseCode = "404",
        description = "List Not Found",
        content = [Content(schema = Schema(implementation = ResponseProblem::class))],
      ),
    ],
  )
  fun countListToItemAssociations(
    @PathVariable("id") @Min(1) id: Long,
    request: HttpServletRequest,
  ): ResponseEntity<ResponseSuccess<ApiMessageNumeric>> {
    val serviceResponse = commonService.countListToItemAssociations(id)
    val responseMessage = "list is associated with $serviceResponse items."
    val responseStatus = HttpStatus.OK
    val responseContent = ApiMessageNumeric(serviceResponse)
    val response = ResponseSuccess(responseContent, responseMessage, request)
    logger.info { Json.encodeToString(response) }
    return ResponseEntity(response, responseStatus)
  }

  @Operation(summary = "Delete a list")
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "List Deleted"),
      ApiResponse(
        responseCode = "404",
        description = "List Not Found",
        content = [Content(schema = Schema(implementation = ResponseProblem::class))],
      ),
    ],
  )
  @DeleteMapping("/{id}")
  fun delete(@PathVariable("id") @Min(1) id: Long, request: HttpServletRequest): ResponseEntity<ResponseSuccess<ApiMessage>> {
    lrmListService.deleteSingleById(id)
    val response = ResponseSuccess(ApiMessage("content"), "deleted list id $id", request)
    logger.info { Json.encodeToString(response) }
    return ResponseEntity<ResponseSuccess<ApiMessage>>(response, HttpStatus.OK)
  }

  @Operation(summary = "Retrieve all lists details and optionally the items")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "all lists",
//        content = [Content(schema = Schema(implementation = ResponseLrmList::class))],
      ),
    ],
  )
  @GetMapping
  fun findAll(
    @RequestParam(defaultValue = false.toString()) includeItems: Boolean,
    request: HttpServletRequest,
  ): ResponseEntity<ResponseSuccess<List<LrmList>>> {
    val responseContent = if (includeItems) lrmListService.findAllIncludeItems() else lrmListService.findAll()
    val response = ResponseSuccess(responseContent, "retrieved all lists", request)
    logger.info { Json.encodeToString(response) }
    return ResponseEntity(response, HttpStatus.OK)
  }

  @Operation(summary = "Retrieve a single list and optionally exclude its items")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "404",
        description = "List Not Found",
        content = [Content(schema = Schema(implementation = ResponseProblem::class, example = "sdkljsldkfjslfj"))],
      ),
      ApiResponse(
        responseCode = "200",
        description = "List Found",
//        content = [Content(schema = Schema(implementation = ResponseLrmList::class))],
      ),
    ],
  )
  @GetMapping("/{id}")
  fun findById(
    @PathVariable("id") @Min(1) id: Long,
    @RequestParam(defaultValue = false.toString()) includeItems: Boolean,
    request: HttpServletRequest,
  ): ResponseEntity<ResponseSuccess<LrmList>> {
    val responseContent =
      if (includeItems) {
        lrmListService.findByIdIncludeItems(id)
      } else {
        lrmListService.findById(id)
      }
    val response = ResponseSuccess(responseContent, "retrieved list id $id", request)
    logger.info { Json.encodeToString(response) }
    return ResponseEntity(response, HttpStatus.OK)
  }

  @Operation(summary = "Update a list")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "List Updated",
//        content = [Content(schema = Schema(implementation = ResponseLrmList::class))],
      ),
      ApiResponse(
        responseCode = "204",
        description = "List Not Updated",
//        content = [Content(schema = Schema(implementation = ResponseLrmList::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "List Not Found",
        content = [Content(schema = Schema(implementation = ResponseProblem::class))],
      ),
    ],
  )
  @PatchMapping("/{id}")
  fun patch(
    @PathVariable("id") @Min(1) id: Long,
    @RequestBody patchRequest: Map<String, Any>,
    request: HttpServletRequest,
  ): ResponseEntity<ResponseSuccess<LrmList>> {
    val (responseContent, patched) = lrmListService.patch(id, patchRequest)
    val response: ResponseSuccess<*>
    val responseEntity: ResponseEntity<ResponseSuccess<LrmList>>
    if (patched) {
      response = ResponseSuccess(responseContent, "patched", request)
      responseEntity = ResponseEntity(response, HttpStatus.OK)
    } else {
      response = ResponseSuccess(responseContent, "not patched", request)
      responseEntity = ResponseEntity(response, HttpStatus.NO_CONTENT)
    }
    logger.info { Json.encodeToString(response) }
    return responseEntity
  }
}
