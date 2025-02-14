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
import net.flyingfishflash.loremlist.domain.lrmitem.data.LrmItemDeleteResponse
import net.flyingfishflash.loremlist.domain.lrmitem.data.LrmItemRequest
import net.flyingfishflash.loremlist.domain.lrmitem.data.LrmItemResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
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

@Tag(name = "item")
@ApiResponses(
  value = [
    ApiResponse(responseCode = "204", description = "No content"),
    ApiResponse(
      responseCode = "400",
      description = "Bad request",
      content = [Content(schema = Schema(implementation = ResponseProblem::class))],
    ),
    ApiResponse(responseCode = "401", description = "Unauthorized", content = [Content(schema = Schema())]),
    ApiResponse(
      responseCode = "500",
      description = "Internal server error",
      content = [Content(schema = Schema(implementation = ResponseProblem::class))],
    ),
  ],
)
@RestController
@RequestMapping("/items")
class LrmItemController(val associationService: AssociationService, val lrmItemService: LrmItemService, val json: Json) {
  private val logger = KotlinLogging.logger {}

  @Operation(summary = "Count of all items.")
  @GetMapping("/count")
  fun countWhereOwnerIsPrincipal(
    request: HttpServletRequest,
    @AuthenticationPrincipal principal: Jwt,
  ): ResponseEntity<ResponseSuccess<ApiMessageNumeric>> {
    val serviceResponse = lrmItemService.countByOwner(owner = principal.subject)
    val responseStatus = HttpStatus.OK
    val responseMessage = "$serviceResponse items."
    val responseContent = ApiMessageNumeric(serviceResponse)
    val response = ResponseSuccess(responseContent, responseMessage, request)
    logger.info { json.encodeToString(response) }
    return ResponseEntity(response, responseStatus)
  }

  @Operation(summary = "Create an item.")
  @PostMapping
  fun create(
    @RequestBody @Valid lrmItemRequest: LrmItemRequest,
    request: HttpServletRequest,
    @AuthenticationPrincipal principal: Jwt,
  ): ResponseEntity<ResponseSuccess<LrmItemResponse>> {
    val responseStatus = HttpStatus.OK
    val responseContent = lrmItemService.create(lrmItemRequest, owner = principal.subject)
    val responseMessage = "created new item"
    val response = ResponseSuccess(responseContent.toDto(), responseMessage, request)
    logger.info { json.encodeToString(response) }
    return ResponseEntity(response, responseStatus)
  }

  @Operation(summary = "Delete all items. Lists are disassociated, not deleted.")
  @DeleteMapping
  fun deleteWhereOwnerIsPrincipal(
    request: HttpServletRequest,
    @AuthenticationPrincipal principal: Jwt,
  ): ResponseEntity<ResponseSuccess<LrmItemDeleteResponse>> {
    val serviceResponse = lrmItemService.deleteByOwner(owner = principal.subject)
    val responseStatus = HttpStatus.OK
    val responseMessage = "Deleted all items and disassociated all lists."
    val response = ResponseSuccess(serviceResponse, responseMessage, request)
    logger.info { json.encodeToString(response) }
    return ResponseEntity(response, responseStatus)
  }

  @Operation(summary = "Delete an item.")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "404",
        description = "Not found",
        content = [Content(schema = Schema(implementation = ResponseProblem::class))],
      ),
      ApiResponse(
        responseCode = "422",
        description = "Unprocessable Content - Item not deleted due to existing list associations constraint.",
        content = [Content(schema = Schema(implementation = ResponseProblem::class))],
      ),
    ],
  )
  @DeleteMapping("/{item-id}")
  fun deleteByIdWhereOwnerIsPrincipal(
    @PathVariable("item-id") @ValidUuid itemId: UUID,
    @RequestParam(defaultValue = false.toString()) removeListAssociations: Boolean,
    request: HttpServletRequest,
    @AuthenticationPrincipal principal: Jwt,
  ): ResponseEntity<ResponseSuccess<LrmItemDeleteResponse>> {
    val serviceResponse = lrmItemService.deleteByOwnerAndId(id = itemId, owner = principal.subject, removeListAssociations)
    val responseStatus = HttpStatus.OK
    val responseMessage = "Deleted item id $itemId."
    val response = ResponseSuccess(serviceResponse, responseMessage, request)
    logger.info { json.encodeToString(response) }
    return ResponseEntity(response, responseStatus)
  }

  @Operation(summary = "Retrieve all items.")
  @GetMapping
  fun findWhereOwnerIsPrincipal(
    request: HttpServletRequest,
    @AuthenticationPrincipal principal: Jwt,
  ): ResponseEntity<ResponseSuccess<List<LrmItemResponse>>> {
    val responseStatus = HttpStatus.OK
    val responseMessage = "retrieved all items"
    val responseContent = lrmItemService.findByOwner(owner = principal.subject)
    val responseContentDto = responseContent.map { it.toDto() }
    val response = ResponseSuccess(responseContentDto, responseMessage, request)
    logger.info { json.encodeToString(response) }
    return ResponseEntity(response, responseStatus)
  }

  @Operation(summary = "Retrieve a single item.")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "404",
        description = "Not found.",
        content = [Content(schema = Schema(implementation = ResponseProblem::class, example = "lasjkdflkjDSF"))],
      ),
    ],
  )
  @GetMapping("/{item-id}")
  fun findByIdWhereOwnerIsPrincipal(
    @PathVariable("item-id") @ValidUuid itemId: UUID,
    request: HttpServletRequest,
    @AuthenticationPrincipal principal: Jwt,
  ): ResponseEntity<ResponseSuccess<LrmItemResponse>> {
    val responseStatus = HttpStatus.OK
    val responseMessage = "retrieved item id $itemId."
    val responseContent = lrmItemService.findByOwnerAndId(itemId, owner = principal.subject)
    val response = ResponseSuccess(responseContent.toDto(), responseMessage, request)
    logger.info { json.encodeToString(response) }
    return ResponseEntity(response, responseStatus)
  }

  @Operation(summary = "Retrieve items that are not part of a list.")
  @GetMapping("/with-no-lists")
  fun findByPrincipalAndHavingNoListAssociations(
    request: HttpServletRequest,
    @AuthenticationPrincipal principal: Jwt,
  ): ResponseEntity<ResponseSuccess<List<LrmItemResponse>>> {
    val serviceResponse = lrmItemService.findByOwnerAndHavingNoListAssociations(owner = principal.subject)
    val serviceResponseDto = serviceResponse.map { it.toDto() }
    val responseStatus = HttpStatus.OK
    val responseMessage = "Retrieved ${serviceResponseDto.size} items that are not a part of a list."
    val response = ResponseSuccess(serviceResponseDto, responseMessage, request)
    logger.info { json.encodeToString(response) }
    return ResponseEntity(response, responseStatus)
  }

  @Operation(summary = "Update an item.")
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "204", description = "No content - Item is up-to-date"),
      ApiResponse(
        responseCode = "404",
        description = "Not found",
        content = [Content(schema = Schema(implementation = ResponseProblem::class))],
      ),
    ],
  )
  @PatchMapping("/{item-id}")
  fun patchByIdWhereOwnerIsPrincipal(
    @PathVariable("item-id") @ValidUuid itemId: UUID,
    @RequestBody patchRequest: Map<String, Any>,
    request: HttpServletRequest,
    @AuthenticationPrincipal principal: Jwt,
  ): ResponseEntity<ResponseSuccess<LrmItemResponse>> {
    val (responseContent, patched) = lrmItemService.patchByOwnerAndId(id = itemId, owner = principal.subject, patchRequest)
    val response: ResponseSuccess<*>
    val responseEntity: ResponseEntity<ResponseSuccess<LrmItemResponse>>
    if (patched) {
      response = ResponseSuccess(responseContent.toDto(), "patched", request)
      responseEntity = ResponseEntity(response, HttpStatus.OK)
    } else {
      response = ResponseSuccess(responseContent.toDto(), "not patched - item is up-to-date", request)
      responseEntity = ResponseEntity(response, HttpStatus.NO_CONTENT)
    }
    logger.info { json.encodeToString(response) }
    return responseEntity
  }

  @Operation(summary = "Count of lists associated with an item.")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "404",
        description = "Not found.",
        content = [Content(schema = Schema(implementation = ResponseProblem::class))],
      ),
    ],
  )
  @GetMapping("/{item-id}/lists/count")
  fun listAssociationsCountWhereListOwnerIsPrincipal(
    @PathVariable("item-id") @ValidUuid itemId: UUID,
    request: HttpServletRequest,
    @AuthenticationPrincipal principal: Jwt,
  ): ResponseEntity<ResponseSuccess<ApiMessageNumeric>> {
    val serviceResponse = associationService
      .countByIdAndItemOwnerForItem(
        itemId = itemId,
        itemOwner = principal.subject,
      )
    val responseMessage = "Item is associated with $serviceResponse lists."
    val responseStatus = HttpStatus.OK
    val responseContent = ApiMessageNumeric(serviceResponse)
    val response = ResponseSuccess(responseContent, responseMessage, request)
    logger.info { json.encodeToString(response) }
    return ResponseEntity(response, responseStatus)
  }

  @Operation(summary = "Create an association with specified list or lists.")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "404",
        description = "Item/List not found.",
        content = [Content(schema = Schema(implementation = ResponseProblem::class))],
      ),
    ],
  )
  @PostMapping("/{item-id}/lists")
  fun listAssociationsCreate(
    @PathVariable("item-id") @ValidUuid itemId: UUID,
    @RequestBody
    @Size(min = 1, message = "List of UUID's must contain at least one element")
    listIdCollection: Set<
      @Serializable(UUIDSerializer::class)
      UUID,
      >,
    request: HttpServletRequest,
    @AuthenticationPrincipal principal: Jwt,
  ): ResponseEntity<ResponseSuccess<AssociationCreatedResponse>> {
    val serviceResponse = associationService.create(
      id = itemId,
      idCollection = listIdCollection.toList(),
      componentsOwner = principal.subject,
      type = LrmComponentType.Item,
    )
    val responseStatus = HttpStatus.OK
    val responseMessage = if (serviceResponse.associatedComponents.size <= 1) {
      "Assigned item '${serviceResponse.componentName}' to list '${serviceResponse.associatedComponents.first().name}'."
    } else {
      "Assigned item '${serviceResponse.componentName}' to ${serviceResponse.associatedComponents.size} lists."
    }
    val response = ResponseSuccess(serviceResponse, responseMessage, request)
    logger.info { json.encodeToString(response) }
    return ResponseEntity(response, responseStatus)
  }

  @Operation(summary = "Delete an association with a specified list.")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "404",
        description = "Item/List/Association not found",
        content = [Content(schema = Schema(implementation = ResponseProblem::class))],
      ),
    ],
  )
  @DeleteMapping("/{item-id}/lists/{list-id}")
  fun listAssociationsDeleteByItemIdAndListIdWhereOwnerIsPrincipal(
    @PathVariable("item-id") @ValidUuid itemId: UUID,
    @PathVariable("list-id") @ValidUuid listId: UUID,
    request: HttpServletRequest,
    @AuthenticationPrincipal principal: Jwt,
  ): ResponseEntity<ResponseSuccess<ApiMessage>> {
    val serviceResponse = associationService.deleteByItemIdAndListId(itemId = itemId, listId = listId, componentsOwner = principal.subject)
    val responseStatus = HttpStatus.OK
    val responseMessage = "Removed item '${serviceResponse.first}' from list '${serviceResponse.second}'."
    val responseContent = ApiMessage(responseMessage)
    val response = ResponseSuccess(responseContent, responseMessage, request)
    logger.info { json.encodeToString(response) }
    return ResponseEntity(response, responseStatus)
  }

  @Operation(summary = "Delete all of an item's list associations.")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "404",
        description = "Item not found.",
        content = [Content(schema = Schema(implementation = ResponseProblem::class))],
      ),
    ],
  )
  @DeleteMapping("/{item-id}/lists")
  fun listAssociationsDeleteByItemIdWhereItemOwnerIsPrincipal(
    @PathVariable("item-id") @ValidUuid itemId: UUID,
    request: HttpServletRequest,
    @AuthenticationPrincipal principal: Jwt,
  ): ResponseEntity<ResponseSuccess<ApiMessageNumeric>> {
    val serviceResponse = associationService.deleteByItemOwnerAndItemId(itemId = itemId, itemOwner = principal.subject)
    val responseStatus = HttpStatus.OK
    val responseMessage = "Removed item '${serviceResponse.first}' from all associated lists (${serviceResponse.second})."
    val responseContent = ApiMessageNumeric(serviceResponse.second.toLong())
    val response = ResponseSuccess(responseContent, responseMessage, request)
    logger.info { json.encodeToString(response) }
    return ResponseEntity(response, responseStatus)
  }

  @Operation(summary = "Update an association with a specified list (move).")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "404",
        description = "Association not found.",
        content = [Content(schema = Schema(implementation = ResponseProblem::class))],
      ),
    ],
  )
  @PatchMapping("/{item-id}/lists/{current-list-id}/{destination-list-id}")
  fun listAssociationsUpdate(
    @PathVariable("item-id") @ValidUuid itemId: UUID,
    @PathVariable("current-list-id") @ValidUuid currentListId: UUID,
    @PathVariable("destination-list-id") @ValidUuid destinationListId: UUID,
    request: HttpServletRequest,
    @AuthenticationPrincipal principal: Jwt,
  ): ResponseEntity<ResponseSuccess<ApiMessage>> {
    val serviceResponse = associationService.updateList(
      itemId = itemId,
      currentListId = currentListId,
      destinationListId = destinationListId,
      componentsOwner = principal.subject,
    )
    val responseMessage = "Moved item '${serviceResponse.first}'" +
      " from list '${serviceResponse.second}'" +
      " to list '${serviceResponse.third}'."
    val responseStatus = HttpStatus.OK
    val responseContent = ApiMessage(responseMessage)
    val response = ResponseSuccess(responseContent, responseMessage, request)
    logger.info { json.encodeToString(response) }
    return ResponseEntity(response, responseStatus)
  }
}
