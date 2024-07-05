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
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.flyingfishflash.loremlist.core.response.structure.ApiMessage
import net.flyingfishflash.loremlist.core.response.structure.ApiMessageNumeric
import net.flyingfishflash.loremlist.core.response.structure.ResponseProblem
import net.flyingfishflash.loremlist.core.response.structure.ResponseSuccess
import net.flyingfishflash.loremlist.core.validation.ValidUuid
import net.flyingfishflash.loremlist.domain.association.AssociationService
import net.flyingfishflash.loremlist.domain.association.data.ItemToListAssociationUpdateRequest
import net.flyingfishflash.loremlist.domain.lrmitem.data.LrmItemDeleteResponse
import net.flyingfishflash.loremlist.domain.lrmitem.data.LrmItemRequest
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
class LrmItemController(val associationService: AssociationService, val lrmItemService: LrmItemService) {
  private val logger = KotlinLogging.logger {}

  @Operation(summary = "Count of all items")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Successful count of all items",
      ),
    ],
  )
  @GetMapping("/count")
  fun count(request: HttpServletRequest): ResponseEntity<ResponseSuccess<ApiMessageNumeric>> {
    val serviceResponse = lrmItemService.count()
    val responseStatus = HttpStatus.OK
    val responseMessage = "$serviceResponse items."
    val responseContent = ApiMessageNumeric(serviceResponse)
    val response = ResponseSuccess(responseContent, responseMessage, request)
    logger.info { Json.encodeToString(response) }
    return ResponseEntity(response, responseStatus)
  }

  @Operation(summary = "Create an item")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Item Created",
      ),
    ],
  )
  @PostMapping
  fun create(@RequestBody @Valid lrmItemRequest: LrmItemRequest, request: HttpServletRequest): ResponseEntity<ResponseSuccess<LrmItem>> {
    val responseStatus = HttpStatus.OK
    val responseContent = lrmItemService.create(lrmItemRequest)
    val responseMessage = "created new item"
    val response = ResponseSuccess(responseContent, responseMessage, request)
    logger.info { Json.encodeToString(response) }
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
      ApiResponse(
        responseCode = "422",
        description = "Item Not Deleted Due to List Associations",
        content = [Content(schema = Schema(implementation = ResponseProblem::class))],
      ),
    ],
  )
  @DeleteMapping("/{id}")
  fun delete(
    @PathVariable("id") @ValidUuid id: UUID,
    @RequestParam(defaultValue = false.toString()) removeListAssociations: Boolean,
    request: HttpServletRequest,
  ): ResponseEntity<ResponseSuccess<LrmItemDeleteResponse>> {
    val serviceResponse = lrmItemService.deleteSingleById(id, removeListAssociations)
    val responseStatus = HttpStatus.OK
    val responseMessage = "Deleted item id $id."
    val response = ResponseSuccess(serviceResponse, responseMessage, request)
    logger.info { Json.encodeToString(response) }
    return ResponseEntity(response, responseStatus)
  }

  @Operation(summary = "Retrieve all items, optionally including the id and name of each list they're associated with")
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
    @RequestParam(defaultValue = false.toString()) includeLists: Boolean,
    request: HttpServletRequest,
  ): ResponseEntity<ResponseSuccess<List<LrmItem>>> {
    val responseStatus = HttpStatus.OK
    val responseMessage = if (includeLists) {
      "retrieved all items and the lists each item is associated with."
    } else {
      "retrieved all items"
    }
    val responseContent = if (includeLists) lrmItemService.findAllIncludeLists() else lrmItemService.findAll()
    val response = ResponseSuccess(responseContent, responseMessage, request)
    logger.info { Json.encodeToString(response) }
    return ResponseEntity(response, responseStatus)
  }

  @Operation(summary = "Retrieve a single item, optionally including the id and name of each list it's associated with")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "404",
        description = "Item Not Found",
        content = [Content(schema = Schema(implementation = ResponseProblem::class, example = "lasjkdflkjDSF"))],
      ),
      ApiResponse(
        responseCode = "200",
        description = "Item Found",
//        content = [Content(schema = Schema(implementation = ResponseLrmList::class))],
      ),
    ],
  )
  @GetMapping("/{id}")
  fun findById(
    @PathVariable("id") @ValidUuid id: UUID,
    @RequestParam(defaultValue = false.toString()) includeLists: Boolean,
    request: HttpServletRequest,
  ): ResponseEntity<ResponseSuccess<LrmItem>> {
    val responseStatus = HttpStatus.OK
    val responseMessage = if (includeLists) "retrieved item id $id and it's associated lists" else "retrieved item id $id"
    val responseContent = if (includeLists) lrmItemService.findByIdIncludeLists(id) else lrmItemService.findById(id)
    val response = ResponseSuccess(responseContent, responseMessage, request)
    logger.info { Json.encodeToString(response) }
    return ResponseEntity(response, responseStatus)
  }

  @Operation(summary = "Count of lists associated with an item")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Count of list associations retrieved",
      ),
      ApiResponse(
        responseCode = "404",
        description = "Item Not Found",
        content = [Content(schema = Schema(implementation = ResponseProblem::class))],
      ),
    ],
  )
  @GetMapping("/{id}/list-associations/count")
  fun listAssociationsCount(
    @PathVariable("id") @ValidUuid id: UUID,
    request: HttpServletRequest,
  ): ResponseEntity<ResponseSuccess<ApiMessageNumeric>> {
    val serviceResponse = associationService.countItemToList(id)
    val responseMessage = "item is associated with $serviceResponse lists."
    val responseStatus = HttpStatus.OK
    val responseContent = ApiMessageNumeric(serviceResponse)
    val response = ResponseSuccess(responseContent, responseMessage, request)
    logger.info { Json.encodeToString(response) }
    return ResponseEntity(response, responseStatus)
  }

  @Operation(summary = "Create an association with a specified list")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Item -> List Association Created",
      ),
      ApiResponse(
        responseCode = "404",
        description = "Item or List Could Not be Found (see response message)",
        content = [Content(schema = Schema(implementation = ResponseProblem::class))],
      ),
    ],
  )
  @PostMapping("/{id}/list-associations")
  fun listAssociationsCreate(
    @PathVariable("id") @ValidUuid id: UUID,
    @RequestBody listId: UUID,
    request: HttpServletRequest,
  ): ResponseEntity<ResponseSuccess<ApiMessage>> {
    val serviceResponse = associationService.addItemToList(itemUuid = id, listUuid = listId)
    val responseStatus = HttpStatus.OK
    val responseMessage = "Assigned item '${serviceResponse.first}' to list '${serviceResponse.second}'."
    val responseContent = ApiMessage(responseMessage)
    val response = ResponseSuccess(responseContent, responseMessage, request)
    return ResponseEntity(response, responseStatus)
  }

  @Operation(summary = "Delete an association with a specified list")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Item's association with a specified list deleted if present.",
      ),
      ApiResponse(
        responseCode = "404",
        description = "Item/List/Association Not Found",
        content = [Content(schema = Schema(implementation = ResponseProblem::class))],
      ),
    ],
  )
  @DeleteMapping("/{id}/list-associations")
  fun listAssociationsDelete(
    @PathVariable("id") @ValidUuid id: UUID,
    @RequestBody listId: UUID,
    request: HttpServletRequest,
  ): ResponseEntity<ResponseSuccess<ApiMessage>> {
    val serviceResponse = associationService.deleteItemToList(itemUuid = id, listUuid = listId)
    val responseStatus = HttpStatus.OK
    val responseMessage = "Removed item '${serviceResponse.first}' from list '${serviceResponse.second}'."
    val responseContent = ApiMessage(responseMessage)
    val response = ResponseSuccess(responseContent, responseMessage, request)
    return ResponseEntity(response, responseStatus)
  }

  @Operation(summary = "Delete all of an item's list associations")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Item's association with all lists deleted.",
      ),
      ApiResponse(
        responseCode = "404",
        description = "Item Not Found",
        content = [Content(schema = Schema(implementation = ResponseProblem::class))],
      ),
    ],
  )
  @DeleteMapping("/{id}/list-associations/delete-all")
  fun listAssociationsDeleteAll(
    @PathVariable("id") @ValidUuid id: UUID,
    request: HttpServletRequest,
  ): ResponseEntity<ResponseSuccess<ApiMessageNumeric>> {
    val serviceResponse = associationService.deleteAllItemToListForItem(itemUuid = id)
    val responseStatus = HttpStatus.OK
    val responseMessage = "Removed item '${serviceResponse.first}' from all associated lists (${serviceResponse.second})."
    val responseContent = ApiMessageNumeric(serviceResponse.second.toLong())
    val response = ResponseSuccess(responseContent, responseMessage, request)
    return ResponseEntity(response, responseStatus)
  }

  @Operation(summary = "Update an association with a specified list (Move)")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Association Updated",
//        content = [Content(schema = Schema(implementation = ResponseLrmList::class))],
      ),
      ApiResponse(
        responseCode = "204",
        description = "Association Found But Is Up-to-Date",
//        content = [Content(schema = Schema(implementation = ResponseLrmList::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Association Not Found",
        content = [Content(schema = Schema(implementation = ResponseProblem::class))],
      ),
    ],
  )
  @PatchMapping("/{id}/list-associations")
  fun listAssociationsUpdate(
    @PathVariable("id") @ValidUuid id: UUID,
    @RequestBody @Valid moveToListRequest: ItemToListAssociationUpdateRequest,
    request: HttpServletRequest,
  ): ResponseEntity<ResponseSuccess<ApiMessage>> {
    val serviceResponse = associationService.updateItemToList(
      itemUuid = id,
      fromListUuid = moveToListRequest.fromListUuid,
      toListUuid = moveToListRequest.toListUuid,
    )
    val responseMessage = "Moved item '${serviceResponse.first}'" +
      " from list '${serviceResponse.second}'" +
      " to list '${serviceResponse.third}'."
    val responseStatus = HttpStatus.OK
    val responseContent = ApiMessage(responseMessage)
    val response = ResponseSuccess(responseContent, responseMessage, request)
    logger.info { Json.encodeToString(response) }
    return ResponseEntity(response, responseStatus)
  }

  @Operation(summary = "Update an item")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Item Updated",
//        content = [Content(schema = Schema(implementation = ResponseLrmList::class))],
      ),
      ApiResponse(
        responseCode = "204",
        description = "Item Found but is Up-to-Date",
//        content = [Content(schema = Schema(implementation = ResponseLrmList::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Item Not Found",
        content = [Content(schema = Schema(implementation = ResponseProblem::class))],
      ),
    ],
  )
  @PatchMapping("/{id}")
  fun patch(
    @PathVariable("id") @ValidUuid id: UUID,
    @RequestBody patchRequest: Map<String, Any>,
    request: HttpServletRequest,
  ): ResponseEntity<ResponseSuccess<LrmItem>> {
    val (responseContent, patched) = lrmItemService.patch(id, patchRequest)
    val response: ResponseSuccess<*>
    val responseEntity: ResponseEntity<ResponseSuccess<LrmItem>>
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
