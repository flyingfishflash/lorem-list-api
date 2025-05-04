package net.flyingfishflash.loremlist.api

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
import net.flyingfishflash.loremlist.api.data.request.LrmItemCreateRequest
import net.flyingfishflash.loremlist.api.data.request.LrmListCreateRequest
import net.flyingfishflash.loremlist.api.data.request.LrmListItemAddRequest
import net.flyingfishflash.loremlist.api.data.response.AssociationDeletedResponse
import net.flyingfishflash.loremlist.api.data.response.LrmItemResponse
import net.flyingfishflash.loremlist.api.data.response.LrmListDeletedResponse
import net.flyingfishflash.loremlist.api.data.response.LrmListItemAddedResponse
import net.flyingfishflash.loremlist.api.data.response.LrmListItemResponse
import net.flyingfishflash.loremlist.api.data.response.LrmListResponse
import net.flyingfishflash.loremlist.api.validation.ValidPatchRequest
import net.flyingfishflash.loremlist.core.response.structure.ApiMessage
import net.flyingfishflash.loremlist.core.response.structure.ApiMessageNumeric
import net.flyingfishflash.loremlist.core.response.structure.ResponseProblem
import net.flyingfishflash.loremlist.core.response.structure.ResponseSuccess
import net.flyingfishflash.loremlist.core.validation.ValidUuid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@Tag(name = "list")
@ApiResponses(
  value = [
    ApiResponse(responseCode = "200", description = "Success"),
    ApiResponse(
      responseCode = "400",
      description = "Bad request",
      content = [Content(schema = Schema(implementation = ResponseProblem::class))],
    ),
    ApiResponse(responseCode = "401", description = "Unauthorized", content = [Content(schema = Schema())]),
  ],
)
@RestController
@RequestMapping("/lists")
class LrmListController(private val lrmListApiService: LrmListApiService, private val lrmItemApiService: LrmItemApiService, val json: Json) {
  private val logger = KotlinLogging.logger {}

  @GetMapping("/count")
  @Operation(summary = "Count of all lists.")
  fun countWhereOwnerIsPrincipal(request: HttpServletRequest, @AuthenticationPrincipal principal: Jwt): ResponseEntity<ResponseSuccess<ApiMessageNumeric>> {
    val apiServiceResponse = lrmListApiService.countByOwner(owner = principal.subject)
    val response = ResponseSuccess(apiServiceResponse.content, apiServiceResponse.message, request)
    logger.info { Json.encodeToString(response) }
    return ResponseEntity(response, HttpStatus.OK)
  }

  @Operation(summary = "Create a list.")
  @PostMapping
  fun create(
    @Valid @RequestBody lrmListCreateRequest: LrmListCreateRequest,
    request: HttpServletRequest,
    @AuthenticationPrincipal principal: Jwt,
  ): ResponseEntity<ResponseSuccess<LrmListResponse>> {
    val apiServiceResponse = lrmListApiService.create(lrmListCreateRequest, principal.subject)
    val response = ResponseSuccess(apiServiceResponse.content, apiServiceResponse.message, request)
    logger.info { Json.encodeToString(response) }
    return ResponseEntity(response, HttpStatus.OK)
  }

  @Operation(summary = "Delete all lists. Items are disassociated, not deleted.")
  @DeleteMapping
  fun deleteWhereOwnerIsPrincipal(
    request: HttpServletRequest,
    @AuthenticationPrincipal principal: Jwt,
  ): ResponseEntity<ResponseSuccess<LrmListDeletedResponse>> {
    val apiServiceResponse = lrmListApiService.deleteByOwner(principal.subject)
    val response = ResponseSuccess(apiServiceResponse.content, apiServiceResponse.message, request)
    logger.info { Json.encodeToString(response) }
    return ResponseEntity(response, HttpStatus.OK)
  }

  @Operation(summary = "Delete a list.")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "404",
        description = "Not found",
        content = [Content(schema = Schema(implementation = ResponseProblem::class))],
      ),
      ApiResponse(
        responseCode = "422",
        description = "Unprocessable content - List not deleted due to existing item associations constraint",
        content = [Content(schema = Schema(implementation = ResponseProblem::class))],
      ),
    ],
  )
  @DeleteMapping("/{list-id}")
  fun deleteByIdWhereOwnerIsPrincipal(
    @PathVariable("list-id") @ValidUuid listId: UUID,
    @RequestParam(defaultValue = false.toString()) removeItemAssociations: Boolean,
    request: HttpServletRequest,
    @AuthenticationPrincipal principal: Jwt,
  ): ResponseEntity<ResponseSuccess<LrmListDeletedResponse>> {
    val apiServiceResponse = lrmListApiService.deleteByOwnerAndId(listId, principal.subject, removeItemAssociations)
    val response = ResponseSuccess(apiServiceResponse.content, apiServiceResponse.message, request)
    logger.info { Json.encodeToString(response) }
    return ResponseEntity(response, HttpStatus.OK)
  }

  @Operation(summary = "Retrieve all lists, optionally including the id and name of each associated item.")
  @GetMapping
  fun findWhereOwnerIsPrincipal(
    @RequestParam(defaultValue = false.toString()) includeItems: Boolean,
    request: HttpServletRequest,
    @AuthenticationPrincipal principal: Jwt,
  ): ResponseEntity<ResponseSuccess<List<LrmListResponse>>> {
    val apiServiceResponse = if (includeItems) {
      lrmListApiService.findByOwner(principal.subject)
    } else {
      lrmListApiService.findByOwnerExcludeItems(principal.subject)
    }
    val response = ResponseSuccess(apiServiceResponse.content, apiServiceResponse.message, request)
    logger.info { Json.encodeToString(response) }
    return ResponseEntity(response, HttpStatus.OK)
  }

  @Operation(summary = "Retrieve a list, optionally including the id and name of each associated item.")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "404",
        description = "Not found",
        content = [Content(schema = Schema(implementation = ResponseProblem::class, example = "sdkljsldkfjslfj"))],
      ),
    ],
  )
  @GetMapping("/{list-id}")
  fun findByIdWhereOwnerIsPrincipal(
    @PathVariable("list-id") @ValidUuid listId: UUID,
    @RequestParam(defaultValue = true.toString()) includeItems: Boolean,
    request: HttpServletRequest,
    @AuthenticationPrincipal principal: Jwt,
  ): ResponseEntity<ResponseSuccess<LrmListResponse>> {
    val apiServiceResponse =
      if (includeItems) {
        lrmListApiService.findByOwnerAndId(id = listId, owner = principal.subject)
      } else {
        lrmListApiService.findByOwnerAndIdExcludeItems(id = listId, owner = principal.subject)
      }
    val response = ResponseSuccess(apiServiceResponse.content, apiServiceResponse.message, request)
    logger.info { Json.encodeToString(response) }
    return ResponseEntity(response, HttpStatus.OK)
  }

  @Operation(summary = "Retrieve lists that contain no items.")
  @GetMapping("/with-no-items")
  fun findByPrincipalAndHavingNoItemAssociations(
    request: HttpServletRequest,
    @AuthenticationPrincipal principal: Jwt,
  ): ResponseEntity<ResponseSuccess<List<LrmListResponse>>> {
    val apiServiceResponse = lrmListApiService.findByOwnerAndHavingNoItemAssociations(owner = principal.subject)
    val response = ResponseSuccess(apiServiceResponse.content, apiServiceResponse.message, request)
    logger.info { json.encodeToString(response) }
    return ResponseEntity(response, HttpStatus.OK)
  }

  @Operation(summary = "Update a list.")
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "204", description = "No content - List is up-to-date"),
      ApiResponse(
        responseCode = "404",
        description = "Not found",
        content = [Content(schema = Schema(implementation = ResponseProblem::class))],
      ),
    ],
  )
  @PatchMapping("/{list-id}")
  fun patchByIdWhereOwnerIsPrincipal(
    @PathVariable("list-id") @ValidUuid listId: UUID,
    @RequestBody @ValidPatchRequest patchRequest: Map<String, Any>,
    request: HttpServletRequest,
    @AuthenticationPrincipal principal: Jwt,
  ): ResponseEntity<ResponseSuccess<LrmListResponse>> {
    val apiServiceResponse = lrmListApiService.patchByOwnerAndId(id = listId, owner = principal.subject, patchRequest)
    val response = ResponseSuccess(apiServiceResponse.content, apiServiceResponse.message, request)
    val responseStatus = if (apiServiceResponse.message.contains("not updated")) HttpStatus.NO_CONTENT else HttpStatus.OK
    logger.info { json.encodeToString(response) }
    return ResponseEntity(response, responseStatus)
  }

  @Operation(summary = "Retrieve items eligible to be added to a list.")
  @GetMapping("/{list-id}/items/eligible")
  fun findByPrincipalAndHavingNoListAssociations(
    @PathVariable("list-id") @ValidUuid listId: UUID,
    request: HttpServletRequest,
    @AuthenticationPrincipal principal: Jwt,
  ): ResponseEntity<ResponseSuccess<List<LrmItemResponse>>> {
    val apiServiceResponse = lrmItemApiService.findByOwnerAndHavingNoListAssociations(owner = principal.subject, listId = listId)
    val response = ResponseSuccess(apiServiceResponse.content, apiServiceResponse.message, request)
    val responseStatus = HttpStatus.OK
    logger.info { json.encodeToString(response) }
    return ResponseEntity(response, responseStatus)
  }

  @Operation(summary = "Add an item or items to a list.")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "404",
        description = "Item/List not found",
        content = [Content(schema = Schema(implementation = ResponseProblem::class))],
      ),
    ],
  )
  @PutMapping("/{list-id}/items")
  fun listItemAdd(
    @PathVariable("list-id") @ValidUuid listId: UUID,
    @RequestBody @Valid lrmListItemAddRequest: LrmListItemAddRequest,
    request: HttpServletRequest,
    @AuthenticationPrincipal principal: Jwt,
  ): ResponseEntity<ResponseSuccess<LrmListItemAddedResponse>> {
    val apiServiceResponse = lrmListApiService.addListItem(
      listId = listId,
      itemIdCollection = lrmListItemAddRequest.itemIdCollection,
      owner = principal.subject,
    )
    val response = ResponseSuccess(apiServiceResponse.content, apiServiceResponse.message, request)
    logger.info { json.encodeToString(response) }
    return ResponseEntity(response, HttpStatus.OK)
  }

  @Operation(summary = "Count of items associated with a list.")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "404",
        description = "Not found",
        content = [Content(schema = Schema(implementation = ResponseProblem::class))],
      ),
    ],
  )
  @GetMapping("/{list-id}/items/count")
  fun listItemCount(
    @PathVariable("list-id") @ValidUuid listId: UUID,
    request: HttpServletRequest,
    @AuthenticationPrincipal principal: Jwt,
  ): ResponseEntity<ResponseSuccess<ApiMessageNumeric>> {
    val apiServiceResponse = lrmListApiService.countListItems(listId = listId, listOwner = principal.subject)
    val response = ResponseSuccess(apiServiceResponse.content, apiServiceResponse.message, request)
    logger.info { Json.encodeToString(response) }
    return ResponseEntity(response, HttpStatus.OK)
  }

  @Operation(summary = "Create an item and associate it with a specified list.")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "404",
        description = "Item/List not found",
        content = [Content(schema = Schema(implementation = ResponseProblem::class))],
      ),
    ],
  )
  @PostMapping("/{list-id}/items")
  fun listItemCreate(
    @PathVariable("list-id") @ValidUuid listId: UUID,
    @RequestBody @Valid lrmItemCreateRequest: LrmItemCreateRequest,
    request: HttpServletRequest,
    @AuthenticationPrincipal principal: Jwt,
  ): ResponseEntity<ResponseSuccess<LrmListItemResponse>> {
    val apiServiceResponse = lrmListApiService.createListItem(
      listId = listId,
      itemCreateRequest = lrmItemCreateRequest,
      creator = principal.subject,
    )
    val response = ResponseSuccess(apiServiceResponse.content, apiServiceResponse.message, request)
    logger.info { json.encodeToString(response) }
    return ResponseEntity(response, HttpStatus.OK)
  }

  @Operation(summary = "Remove an item from a list.")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "404",
        description = "Item/List/ListItem Not Found",
        content = [Content(schema = Schema(implementation = ResponseProblem::class))],
      ),
    ],
  )
  @DeleteMapping("/{list-id}/items/{item-id}")
  fun listItemDelete(
    @PathVariable("list-id") @ValidUuid listId: UUID,
    @PathVariable("item-id") @ValidUuid itemId: UUID,
    request: HttpServletRequest,
    @AuthenticationPrincipal principal: Jwt,
  ): ResponseEntity<ResponseSuccess<AssociationDeletedResponse>> {
    val apiServiceResponse = lrmListApiService.removeListItem(
      itemId = itemId,
      listId = listId,
      componentsOwner = principal.subject,
    )
    val response = ResponseSuccess(apiServiceResponse.content, apiServiceResponse.message, request)
    logger.info { json.encodeToString(response) }
    return ResponseEntity(response, HttpStatus.OK)
  }

  @Operation(summary = "Remove all items from a list.")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "404",
        description = "Not found",
        content = [Content(schema = Schema(implementation = ResponseProblem::class))],
      ),
    ],
  )
  @DeleteMapping("/{list-id}/items")
  fun listItemsDelete(
    @PathVariable("list-id") @ValidUuid listId: UUID,
    request: HttpServletRequest,
    @AuthenticationPrincipal principal: Jwt,
  ): ResponseEntity<ResponseSuccess<ApiMessageNumeric>> {
    val apiServiceResponse = lrmListApiService.removeAllListItems(
      listId = listId,
      listOwner = principal.subject,
    )
    val response = ResponseSuccess(
      ApiMessageNumeric(apiServiceResponse.content.deletedAssociationsCount.toLong()),
      apiServiceResponse.message,
      request,
    )
    logger.info { json.encodeToString(response) }
    return ResponseEntity(response, HttpStatus.OK)
  }

  @Operation(summary = "Retrieve a list item.")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "404",
        description = "Not found",
        content = [Content(schema = Schema(implementation = ResponseProblem::class, example = "sdkljsldkfjslfj"))],
      ),
    ],
  )
  @GetMapping("/{list-id}/items/{item-id}")
  fun listItemFind(
    @PathVariable("list-id") @ValidUuid listId: UUID,
    @PathVariable("item-id") @ValidUuid itemId: UUID,
    request: HttpServletRequest,
    @AuthenticationPrincipal principal: Jwt,
  ): ResponseEntity<ResponseSuccess<LrmListItemResponse>> {
    val apiServiceResponse = lrmListApiService.findListItem(listId = listId, itemId = itemId, listOwner = principal.subject)
    val response = ResponseSuccess(apiServiceResponse.content, apiServiceResponse.message, request)
    logger.info { Json.encodeToString(response) }
    return ResponseEntity(response, HttpStatus.OK)
  }

  @Operation(summary = "Move an item from one list to another.")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "404",
        description = "ListItem not found.",
        content = [Content(schema = Schema(implementation = ResponseProblem::class))],
      ),
    ],
  )
  @PatchMapping("/{list-id}/items/{item-id}/{destination-list-id}")
  fun listItemMove(
    @PathVariable("list-id") @ValidUuid listId: UUID,
    @PathVariable("item-id") @ValidUuid itemId: UUID,
    @PathVariable("destination-list-id") @ValidUuid destinationListId: UUID,
    request: HttpServletRequest,
    @AuthenticationPrincipal principal: Jwt,
  ): ResponseEntity<ResponseSuccess<ApiMessage>> {
    val apiServiceResponse = lrmListApiService.moveListItem(
      listId = listId,
      itemId = itemId,
      destinationListId = destinationListId,
      owner = principal.subject,
    )
    val response = ResponseSuccess(ApiMessage(apiServiceResponse.message), apiServiceResponse.message, request)
    val responseStatus = HttpStatus.OK
    logger.info { json.encodeToString(response) }
    return ResponseEntity(response, responseStatus)
  }

  @Operation(summary = "Update a list item.")
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "204", description = "No Content - List item is up-to-date"),
      ApiResponse(
        responseCode = "404",
        description = "List item not found",
        content = [Content(schema = Schema(implementation = ResponseProblem::class))],
      ),
    ],
  )
  @PatchMapping("/{list-id}/items/{item-id}")
  fun listItemPatch(
    @PathVariable("list-id") @ValidUuid listId: UUID,
    @PathVariable("item-id") @ValidUuid itemId: UUID,
    @RequestBody patchRequest: Map<String, Any>,
    request: HttpServletRequest,
    @AuthenticationPrincipal principal: Jwt,
  ): ResponseEntity<ResponseSuccess<LrmListItemResponse>> {
    val apiServiceResponse = lrmListApiService.patchListItem(
      itemId = itemId,
      listId = listId,
      listOwner = principal.subject,
      patchRequest = patchRequest,
    )
    val response = ResponseSuccess(apiServiceResponse.content, apiServiceResponse.message, request)
    val responseStatus = if (apiServiceResponse.message.contains("not updated")) HttpStatus.NO_CONTENT else HttpStatus.OK
    logger.info { Json.encodeToString(response) }
    return ResponseEntity(response, responseStatus)
  }
}
