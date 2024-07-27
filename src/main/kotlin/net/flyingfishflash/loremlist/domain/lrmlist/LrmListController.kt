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
import jakarta.validation.constraints.Size
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.flyingfishflash.loremlist.core.response.structure.ApiMessage
import net.flyingfishflash.loremlist.core.response.structure.ApiMessageNumeric
import net.flyingfishflash.loremlist.core.response.structure.ResponseProblem
import net.flyingfishflash.loremlist.core.response.structure.ResponseSuccess
import net.flyingfishflash.loremlist.core.serialization.UUIDSerializer
import net.flyingfishflash.loremlist.core.validation.ValidUuid
import net.flyingfishflash.loremlist.domain.LrmComponentType
import net.flyingfishflash.loremlist.domain.association.AssociationService
import net.flyingfishflash.loremlist.domain.association.data.AssociationCreatedResponse
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

@Tag(name = "list")
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
class LrmListController(private val associationService: AssociationService, private val lrmListService: LrmListService, val json: Json) {
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
  @DeleteMapping("/{list-id}")
  fun deleteById(
    @PathVariable("list-id") @ValidUuid listId: UUID,
    @RequestParam(defaultValue = false.toString()) removeItemAssociations: Boolean,
    request: HttpServletRequest,
  ): ResponseEntity<ResponseSuccess<LrmListDeleteResponse>> {
    val serviceResponse = lrmListService.deleteById(listId, removeItemAssociations)
    val responseStatus = HttpStatus.OK
    val responseMessage = "Deleted list id $listId."
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
  @GetMapping("/{list-id}")
  fun findById(
    @PathVariable("list-id") @ValidUuid listId: UUID,
    @RequestParam(defaultValue = false.toString()) includeItems: Boolean,
    request: HttpServletRequest,
  ): ResponseEntity<ResponseSuccess<LrmList>> {
    val responseContent =
      if (includeItems) {
        lrmListService.findByIdIncludeItems(listId)
      } else {
        lrmListService.findById(listId)
      }
    val response = ResponseSuccess(responseContent, "retrieved list id $listId", request)
    logger.info { Json.encodeToString(response) }
    return ResponseEntity(response, HttpStatus.OK)
  }

  @Operation(summary = "Retrieve lists that contain no items")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Retrieved lists that contain no items",
      ),
    ],
  )
  @GetMapping("/with-no-items")
  fun findWithNoItemAssociations(request: HttpServletRequest): ResponseEntity<ResponseSuccess<List<LrmList>>> {
    val serviceResponse = lrmListService.findWithNoItems()
    val responseStatus = HttpStatus.OK
    val responseMessage = "Retrieved ${serviceResponse.size} lists containing no items."
    val response = ResponseSuccess(serviceResponse, responseMessage, request)
    logger.info { json.encodeToString(response) }
    return ResponseEntity(response, responseStatus)
  }

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
  @GetMapping("/{list-id}/items/count")
  fun itemAssociationsCount(
    @PathVariable("list-id") @ValidUuid listId: UUID,
    request: HttpServletRequest,
  ): ResponseEntity<ResponseSuccess<ApiMessageNumeric>> {
    val serviceResponse = associationService.countForListId(listId)
    val responseMessage = "List is associated with $serviceResponse items."
    val responseStatus = HttpStatus.OK
    val responseContent = ApiMessageNumeric(serviceResponse)
    val response = ResponseSuccess(responseContent, responseMessage, request)
    logger.info { Json.encodeToString(response) }
    return ResponseEntity(response, responseStatus)
  }

  @Operation(summary = "Create an association with a specified item or items")
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
  @PostMapping("/{list-id}/items")
  fun itemAssociationsCreate(
    @PathVariable("list-id") @ValidUuid listId: UUID,
    @RequestBody
    @Size(min = 1, message = "List of UUID's must contain at least one element")
    itemIdCollection: Set<
      @Serializable(UUIDSerializer::class)
      UUID,
      >,
    request: HttpServletRequest,
  ): ResponseEntity<ResponseSuccess<AssociationCreatedResponse>> {
    val serviceResponse = associationService.create(id = listId, idCollection = itemIdCollection.toList(), type = LrmComponentType.List)
    val responseStatus = HttpStatus.OK
    val responseMessage = if (serviceResponse.associatedComponents.size <= 1) {
      "Assigned item '${serviceResponse.associatedComponents.first().name}' to list '${serviceResponse.componentName}'."
    } else {
      "Assigned ${serviceResponse.associatedComponents.size} items to list '${serviceResponse.componentName}'."
    }
    val response = ResponseSuccess(serviceResponse, responseMessage, request)
    logger.info { json.encodeToString(response) }
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
  @DeleteMapping("/{list-id}/items/{item-id}")
  fun itemAssociationsDelete(
    @PathVariable("list-id") @ValidUuid listId: UUID,
    @PathVariable("item-id") @ValidUuid itemId: UUID,
    request: HttpServletRequest,
  ): ResponseEntity<ResponseSuccess<ApiMessage>> {
    val serviceResponse = associationService.deleteByItemIdAndListId(itemId = itemId, listId = listId)
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
  @DeleteMapping("/{list-id}/items")
  fun itemAssociationsDeleteAll(
    @PathVariable("list-id") @ValidUuid listId: UUID,
    request: HttpServletRequest,
  ): ResponseEntity<ResponseSuccess<ApiMessageNumeric>> {
    val serviceResponse = associationService.deleteAllOfList(listId = listId)
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
  @PatchMapping("/{list-id}")
  fun patch(
    @PathVariable("list-id") @ValidUuid listId: UUID,
    @RequestBody patchRequest: Map<String, Any>,
    request: HttpServletRequest,
  ): ResponseEntity<ResponseSuccess<LrmList>> {
    val (responseContent, patched) = lrmListService.patch(listId, patchRequest)
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
