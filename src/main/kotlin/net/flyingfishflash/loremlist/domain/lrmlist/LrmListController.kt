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
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.flyingfishflash.loremlist.core.response.structure.ApiMessage
import net.flyingfishflash.loremlist.core.response.structure.ApiMessageNumeric
import net.flyingfishflash.loremlist.core.response.structure.ResponseProblem
import net.flyingfishflash.loremlist.core.response.structure.ResponseSuccess
import net.flyingfishflash.loremlist.core.validation.ValidUuid
import net.flyingfishflash.loremlist.domain.association.AssociationService
import net.flyingfishflash.loremlist.domain.lrmlist.data.LrmListDeleteResponse
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
import java.util.UUID

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
class LrmListController(private val associationService: AssociationService, private val lrmListService: LrmListService) {
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

  @Operation(summary = "Create a list")
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

  @Operation(summary = "Delete all lists. Items are disassociated, not deleted.")
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "All lists deleted. All items disassociated."),
    ],
  )
  @DeleteMapping
  fun deleteAll(request: HttpServletRequest): ResponseEntity<ResponseSuccess<LrmListDeleteResponse>> {
    val serviceResponse = lrmListService.deleteAll()
    val responseStatus = HttpStatus.OK
    val responseMessage = "Deleted all lists and disassociated all items."
    val response = ResponseSuccess(serviceResponse, responseMessage, request)
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
      ApiResponse(
        responseCode = "422",
        description = "List Not Deleted Due to Item Associations",
        content = [Content(schema = Schema(implementation = ResponseProblem::class))],
      ),
    ],
  )
  @DeleteMapping("/{id}")
  fun deleteById(
    @PathVariable("id") @ValidUuid id: UUID,
    @RequestParam(defaultValue = false.toString()) removeItemAssociations: Boolean,
    request: HttpServletRequest,
  ): ResponseEntity<ResponseSuccess<LrmListDeleteResponse>> {
    val serviceResponse = lrmListService.deleteById(id, removeItemAssociations)
    val responseStatus = HttpStatus.OK
    val responseMessage = "Deleted list id $id."
    val response = ResponseSuccess(serviceResponse, responseMessage, request)
    logger.info { Json.encodeToString(response) }
    return ResponseEntity(response, responseStatus)
  }

  @Operation(summary = "Retrieve all lists, optionally including the details of each associated item")
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

  @Operation(summary = "Retrieve a single list, optionally including the details of each associated item")
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
    @PathVariable("id") @ValidUuid id: UUID,
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

  @GetMapping("/{id}/item-associations/count")
  @Operation(summary = "Count of items associated with a list")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Successful count of item associations",
      ),
      ApiResponse(
        responseCode = "404",
        description = "List Not Found",
        content = [Content(schema = Schema(implementation = ResponseProblem::class))],
      ),
    ],
  )
  fun itemAssociationsCount(
    @PathVariable("id") @ValidUuid id: UUID,
    request: HttpServletRequest,
  ): ResponseEntity<ResponseSuccess<ApiMessageNumeric>> {
    val serviceResponse = associationService.countForListId(id)
    val responseMessage = "List is associated with $serviceResponse items."
    val responseStatus = HttpStatus.OK
    val responseContent = ApiMessageNumeric(serviceResponse)
    val response = ResponseSuccess(responseContent, responseMessage, request)
    logger.info { Json.encodeToString(response) }
    return ResponseEntity(response, responseStatus)
  }

  @Operation(summary = "Create an association with a specified item")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "List -> Item Association Created",
      ),
      ApiResponse(
        responseCode = "404",
        description = "Item/List Not Found",
        content = [Content(schema = Schema(implementation = ResponseProblem::class))],
      ),
    ],
  )
  @PostMapping("/{id}/item-associations")
  fun itemAssociationsCreate(
    @PathVariable("id") @ValidUuid id: UUID,
    @RequestBody itemId: UUID,
    request: HttpServletRequest,
  ): ResponseEntity<ResponseSuccess<ApiMessage>> {
    val serviceResponse = associationService.create(itemUuid = itemId, listUuid = id)
    val responseStatus = HttpStatus.OK
    val responseMessage = "Assigned item '${serviceResponse.first}' to list '${serviceResponse.second}'."
    val responseContent = ApiMessage(responseMessage)
    val response = ResponseSuccess(responseContent, responseMessage, request)
    return ResponseEntity(response, responseStatus)
  }

  @Operation(summary = "Delete an association with a specified item")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Deleted a list's association with the specified item",
      ),
      ApiResponse(
        responseCode = "404",
        description = "Item/List/Association Not Found",
        content = [Content(schema = Schema(implementation = ResponseProblem::class))],
      ),
    ],
  )
  @DeleteMapping("/{id}/item-associations")
  fun itemAssociationsDelete(
    @PathVariable("id") @ValidUuid id: UUID,
    @RequestBody itemId: UUID,
    request: HttpServletRequest,
  ): ResponseEntity<ResponseSuccess<ApiMessage>> {
    val serviceResponse = associationService.deleteByItemIdAndListId(itemUuid = itemId, listUuid = id)
    val responseStatus = HttpStatus.OK
    val responseMessage = "Removed item '${serviceResponse.first}' from list '${serviceResponse.second}'."
    val responseContent = ApiMessage(responseMessage)
    val response = ResponseSuccess(responseContent, responseMessage, request)
    return ResponseEntity(response, responseStatus)
  }

  @Operation(summary = "Delete all of a list's item associations")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Deleted all of a list's item associations",
      ),
      ApiResponse(
        responseCode = "404",
        description = "List Not Found",
        content = [Content(schema = Schema(implementation = ResponseProblem::class))],
      ),
    ],
  )
  @DeleteMapping("/{id}/item-associations/delete-all")
  fun itemAssociationsDeleteAll(
    @PathVariable("id") @ValidUuid id: UUID,
    request: HttpServletRequest,
  ): ResponseEntity<ResponseSuccess<ApiMessageNumeric>> {
    val serviceResponse = associationService.deleteAllOfList(listUuid = id)
    val responseStatus = HttpStatus.OK
    val responseMessage = "Removed all associated items (${serviceResponse.second}) from list '${serviceResponse.first}'."
    val responseContent = ApiMessageNumeric(serviceResponse.second.toLong())
    val response = ResponseSuccess(responseContent, responseMessage, request)
    return ResponseEntity(response, responseStatus)
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
    @PathVariable("id") @ValidUuid id: UUID,
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
