package net.flyingfishflash.loremlist.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.flyingfishflash.loremlist.api.data.request.LrmItemCreateRequest;
import net.flyingfishflash.loremlist.api.data.request.LrmListCreateRequest;
import net.flyingfishflash.loremlist.api.data.request.LrmListItemAddRequest;
import net.flyingfishflash.loremlist.api.data.response.ApiServiceResponse;
import net.flyingfishflash.loremlist.api.data.response.AssociationDeletedResponse;
import net.flyingfishflash.loremlist.api.data.response.AssociationsDeletedResponse;
import net.flyingfishflash.loremlist.api.data.response.LrmItemResponse;
import net.flyingfishflash.loremlist.api.data.response.LrmListDeletedResponse;
import net.flyingfishflash.loremlist.api.data.response.LrmListItemAddedResponse;
import net.flyingfishflash.loremlist.api.data.response.LrmListItemResponse;
import net.flyingfishflash.loremlist.api.data.response.LrmListResponse;
import net.flyingfishflash.loremlist.api.validation.ValidPatchRequest;
import net.flyingfishflash.loremlist.core.response.structure.ApiMessage;
import net.flyingfishflash.loremlist.core.response.structure.ApiMessageNumeric;
import net.flyingfishflash.loremlist.core.response.structure.ResponseProblem;
import net.flyingfishflash.loremlist.core.response.structure.ResponseSuccess;
import net.flyingfishflash.loremlist.core.validation.ValidUuid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "list")
@ApiResponses(
    value = {
      @ApiResponse(responseCode = "200", description = "Success"),
      @ApiResponse(
          responseCode = "400",
          description = "Bad request",
          content = @Content(schema = @Schema(implementation = ResponseProblem.class))),
      @ApiResponse(
          responseCode = "401",
          description = "Unauthorized",
          content = @Content(schema = @Schema()))
    })
@RestController
@RequestMapping("/lists")
public class LrmListController {

  private static final Logger logger = LoggerFactory.getLogger(LrmListController.class);

  private final LrmListApiService lrmListApiService;
  private final LrmItemApiService lrmItemApiService;
  private final ObjectMapper objectMapper;

  public LrmListController(
      LrmListApiService lrmListApiService,
      LrmItemApiService lrmItemApiService,
      ObjectMapper objectMapper) {
    this.lrmListApiService = lrmListApiService;
    this.lrmItemApiService = lrmItemApiService;
    this.objectMapper = objectMapper;
  }

  @GetMapping("/count")
  @Operation(summary = "Count of all lists.")
  public ResponseEntity<ResponseSuccess<ApiMessageNumeric>> countWhereOwnerIsPrincipal(
      HttpServletRequest request, @AuthenticationPrincipal Jwt principal) {
    ApiServiceResponse<ApiMessageNumeric> apiServiceResponse =
        lrmListApiService.countByOwner(principal.getSubject());
    ResponseSuccess<ApiMessageNumeric> response =
        new ResponseSuccess<>(
            apiServiceResponse.getContent(), apiServiceResponse.getMessage(), request);
    logInfo(response);
    return new ResponseEntity<>(response, HttpStatus.OK);
  }

  @Operation(summary = "Create a list.")
  @PostMapping
  public ResponseEntity<ResponseSuccess<LrmListResponse>> create(
      @Valid @RequestBody LrmListCreateRequest lrmListCreateRequest,
      HttpServletRequest request,
      @AuthenticationPrincipal Jwt principal) {
    ApiServiceResponse<LrmListResponse> apiServiceResponse =
        lrmListApiService.create(lrmListCreateRequest, principal.getSubject());
    ResponseSuccess<LrmListResponse> response =
        new ResponseSuccess<>(
            apiServiceResponse.getContent(), apiServiceResponse.getMessage(), request);
    logInfo(response);
    return new ResponseEntity<>(response, HttpStatus.OK);
  }

  @Operation(summary = "Delete all lists. Items are disassociated, not deleted.")
  @DeleteMapping
  public ResponseEntity<ResponseSuccess<LrmListDeletedResponse>> deleteWhereOwnerIsPrincipal(
      HttpServletRequest request, @AuthenticationPrincipal Jwt principal) {
    ApiServiceResponse<LrmListDeletedResponse> apiServiceResponse =
        lrmListApiService.deleteByOwner(principal.getSubject());
    ResponseSuccess<LrmListDeletedResponse> response =
        new ResponseSuccess<>(
            apiServiceResponse.getContent(), apiServiceResponse.getMessage(), request);
    logInfo(response);
    return new ResponseEntity<>(response, HttpStatus.OK);
  }

  @Operation(summary = "Delete a list.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "404",
            description = "Not found",
            content = @Content(schema = @Schema(implementation = ResponseProblem.class))),
        @ApiResponse(
            responseCode = "422",
            description =
                "Unprocessable content - List not deleted due to existing item associations constraint",
            content = @Content(schema = @Schema(implementation = ResponseProblem.class)))
      })
  @DeleteMapping("/{list-id}")
  public ResponseEntity<ResponseSuccess<LrmListDeletedResponse>> deleteByIdWhereOwnerIsPrincipal(
      @PathVariable("list-id") @ValidUuid UUID listId,
      @RequestParam(defaultValue = "false") boolean removeItemAssociations,
      HttpServletRequest request,
      @AuthenticationPrincipal Jwt principal) {
    ApiServiceResponse<LrmListDeletedResponse> apiServiceResponse =
        lrmListApiService.deleteByOwnerAndId(
            listId, principal.getSubject(), removeItemAssociations);
    ResponseSuccess<LrmListDeletedResponse> response =
        new ResponseSuccess<>(
            apiServiceResponse.getContent(), apiServiceResponse.getMessage(), request);
    logInfo(response);
    return new ResponseEntity<>(response, HttpStatus.OK);
  }

  @Operation(
      summary = "Retrieve all lists, optionally including the id and name of each associated item.")
  @GetMapping
  public ResponseEntity<ResponseSuccess<List<LrmListResponse>>> findWhereOwnerIsPrincipal(
      @RequestParam(defaultValue = "false") boolean includeItems,
      HttpServletRequest request,
      @AuthenticationPrincipal Jwt principal) {
    ApiServiceResponse<List<LrmListResponse>> apiServiceResponse =
        includeItems
            ? lrmListApiService.findByOwner(principal.getSubject())
            : lrmListApiService.findByOwnerExcludeItems(principal.getSubject());
    ResponseSuccess<List<LrmListResponse>> response =
        new ResponseSuccess<>(
            apiServiceResponse.getContent(), apiServiceResponse.getMessage(), request);
    logInfo(response);
    return new ResponseEntity<>(response, HttpStatus.OK);
  }

  @Operation(
      summary = "Retrieve a list, optionally including the id and name of each associated item.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "404",
            description = "Not found",
            content =
                @Content(
                    schema =
                        @Schema(
                            implementation = ResponseProblem.class,
                            example = "sdkljsldkfjslfj")))
      })
  @GetMapping("/{list-id}")
  public ResponseEntity<ResponseSuccess<LrmListResponse>> findByIdWhereOwnerIsPrincipal(
      @PathVariable("list-id") @ValidUuid UUID listId,
      @RequestParam(defaultValue = "true") boolean includeItems,
      HttpServletRequest request,
      @AuthenticationPrincipal Jwt principal) {
    ApiServiceResponse<LrmListResponse> apiServiceResponse =
        includeItems
            ? lrmListApiService.findByOwnerAndId(listId, principal.getSubject())
            : lrmListApiService.findByOwnerAndIdExcludeItems(listId, principal.getSubject());
    ResponseSuccess<LrmListResponse> response =
        new ResponseSuccess<>(
            apiServiceResponse.getContent(), apiServiceResponse.getMessage(), request);
    logInfo(response);
    return new ResponseEntity<>(response, HttpStatus.OK);
  }

  @Operation(summary = "Retrieve lists that contain no items.")
  @GetMapping("/with-no-items")
  public ResponseEntity<ResponseSuccess<List<LrmListResponse>>>
      findByPrincipalAndHavingNoItemAssociations(
          HttpServletRequest request, @AuthenticationPrincipal Jwt principal) {
    ApiServiceResponse<List<LrmListResponse>> apiServiceResponse =
        lrmListApiService.findByOwnerAndHavingNoItemAssociations(principal.getSubject());
    ResponseSuccess<List<LrmListResponse>> response =
        new ResponseSuccess<>(
            apiServiceResponse.getContent(), apiServiceResponse.getMessage(), request);
    logInfo(response);
    return new ResponseEntity<>(response, HttpStatus.OK);
  }

  @Operation(summary = "Update a list.")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "204", description = "No content - List is up-to-date"),
        @ApiResponse(
            responseCode = "404",
            description = "Not found",
            content = @Content(schema = @Schema(implementation = ResponseProblem.class)))
      })
  @PatchMapping("/{list-id}")
  public ResponseEntity<ResponseSuccess<LrmListResponse>> patchByIdWhereOwnerIsPrincipal(
      @PathVariable("list-id") @ValidUuid UUID listId,
      @RequestBody @ValidPatchRequest Map<String, Object> patchRequest,
      HttpServletRequest request,
      @AuthenticationPrincipal Jwt principal) {
    ApiServiceResponse<LrmListResponse> apiServiceResponse =
        lrmListApiService.patchByOwnerAndId(listId, principal.getSubject(), patchRequest);
    ResponseSuccess<LrmListResponse> response =
        new ResponseSuccess<>(
            apiServiceResponse.getContent(), apiServiceResponse.getMessage(), request);
    HttpStatus responseStatus =
        apiServiceResponse.getMessage().contains("not updated")
            ? HttpStatus.NO_CONTENT
            : HttpStatus.OK;
    logInfo(response);
    return new ResponseEntity<>(response, responseStatus);
  }

  @Operation(summary = "Retrieve items eligible to be added to a list.")
  @GetMapping("/{list-id}/items/eligible")
  public ResponseEntity<ResponseSuccess<List<LrmItemResponse>>>
      findByPrincipalAndHavingNoListAssociations(
          @PathVariable("list-id") @ValidUuid UUID listId,
          HttpServletRequest request,
          @AuthenticationPrincipal Jwt principal) {
    ApiServiceResponse<List<LrmItemResponse>> apiServiceResponse =
        lrmItemApiService.findByOwnerAndHavingNoListAssociations(principal.getSubject(), listId);
    ResponseSuccess<List<LrmItemResponse>> response =
        new ResponseSuccess<>(
            apiServiceResponse.getContent(), apiServiceResponse.getMessage(), request);
    logInfo(response);
    return new ResponseEntity<>(response, HttpStatus.OK);
  }

  @Operation(summary = "Add an item or items to a list.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "404",
            description = "Item/List not found",
            content = @Content(schema = @Schema(implementation = ResponseProblem.class)))
      })
  @PutMapping("/{list-id}/items")
  public ResponseEntity<ResponseSuccess<LrmListItemAddedResponse>> listItemAdd(
      @PathVariable("list-id") @ValidUuid UUID listId,
      @RequestBody @Valid LrmListItemAddRequest lrmListItemAddRequest,
      HttpServletRequest request,
      @AuthenticationPrincipal Jwt principal) {
    ApiServiceResponse<LrmListItemAddedResponse> apiServiceResponse =
        lrmListApiService.addListItem(
            listId, lrmListItemAddRequest.itemIdCollection(), principal.getSubject());
    ResponseSuccess<LrmListItemAddedResponse> response =
        new ResponseSuccess<>(
            apiServiceResponse.getContent(), apiServiceResponse.getMessage(), request);
    logInfo(response);
    return new ResponseEntity<>(response, HttpStatus.OK);
  }

  @Operation(summary = "Count of items associated with a list.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "404",
            description = "Not found",
            content = @Content(schema = @Schema(implementation = ResponseProblem.class)))
      })
  @GetMapping("/{list-id}/items/count")
  public ResponseEntity<ResponseSuccess<ApiMessageNumeric>> listItemCount(
      @PathVariable("list-id") @ValidUuid UUID listId,
      HttpServletRequest request,
      @AuthenticationPrincipal Jwt principal) {
    ApiServiceResponse<ApiMessageNumeric> apiServiceResponse =
        lrmListApiService.countListItems(listId, principal.getSubject());
    ResponseSuccess<ApiMessageNumeric> response =
        new ResponseSuccess<>(
            apiServiceResponse.getContent(), apiServiceResponse.getMessage(), request);
    logInfo(response);
    return new ResponseEntity<>(response, HttpStatus.OK);
  }

  @Operation(summary = "Create an item and associate it with a specified list.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "404",
            description = "Item/List not found",
            content = @Content(schema = @Schema(implementation = ResponseProblem.class)))
      })
  @PostMapping("/{list-id}/items")
  public ResponseEntity<ResponseSuccess<LrmListItemResponse>> listItemCreate(
      @PathVariable("list-id") @ValidUuid UUID listId,
      @RequestBody @Valid LrmItemCreateRequest lrmItemCreateRequest,
      HttpServletRequest request,
      @AuthenticationPrincipal Jwt principal) {
    ApiServiceResponse<LrmListItemResponse> apiServiceResponse =
        lrmListApiService.createListItem(listId, lrmItemCreateRequest, principal.getSubject());
    ResponseSuccess<LrmListItemResponse> response =
        new ResponseSuccess<>(
            apiServiceResponse.getContent(), apiServiceResponse.getMessage(), request);
    logInfo(response);
    return new ResponseEntity<>(response, HttpStatus.OK);
  }

  @Operation(summary = "Remove an item from a list.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "404",
            description = "Item/List/ListItem Not Found",
            content = @Content(schema = @Schema(implementation = ResponseProblem.class)))
      })
  @DeleteMapping("/{list-id}/items/{item-id}")
  public ResponseEntity<ResponseSuccess<AssociationDeletedResponse>> listItemDelete(
      @PathVariable("list-id") @ValidUuid UUID listId,
      @PathVariable("item-id") @ValidUuid UUID itemId,
      HttpServletRequest request,
      @AuthenticationPrincipal Jwt principal) {
    ApiServiceResponse<AssociationDeletedResponse> apiServiceResponse =
        lrmListApiService.removeListItem(listId, itemId, principal.getSubject());
    ResponseSuccess<AssociationDeletedResponse> response =
        new ResponseSuccess<>(
            apiServiceResponse.getContent(), apiServiceResponse.getMessage(), request);
    logInfo(response);
    return new ResponseEntity<>(response, HttpStatus.OK);
  }

  @Operation(summary = "Remove all items from a list.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "404",
            description = "Not found",
            content = @Content(schema = @Schema(implementation = ResponseProblem.class)))
      })
  @DeleteMapping("/{list-id}/items")
  public ResponseEntity<ResponseSuccess<ApiMessageNumeric>> listItemsDelete(
      @PathVariable("list-id") @ValidUuid UUID listId,
      HttpServletRequest request,
      @AuthenticationPrincipal Jwt principal) {
    ApiServiceResponse<AssociationsDeletedResponse> apiServiceResponse =
        lrmListApiService.removeAllListItems(listId, principal.getSubject());
    ResponseSuccess<ApiMessageNumeric> response =
        new ResponseSuccess<>(
            new ApiMessageNumeric(
                (long) apiServiceResponse.getContent().deletedAssociationsCount()),
            apiServiceResponse.getMessage(),
            request);
    logInfo(response);
    return new ResponseEntity<>(response, HttpStatus.OK);
  }

  @Operation(summary = "Retrieve a list item.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "404",
            description = "Not found",
            content =
                @Content(
                    schema =
                        @Schema(
                            implementation = ResponseProblem.class,
                            example = "sdkljsldkfjslfj")))
      })
  @GetMapping("/{list-id}/items/{item-id}")
  public ResponseEntity<ResponseSuccess<LrmListItemResponse>> listItemFind(
      @PathVariable("list-id") @ValidUuid UUID listId,
      @PathVariable("item-id") @ValidUuid UUID itemId,
      HttpServletRequest request,
      @AuthenticationPrincipal Jwt principal) {
    ApiServiceResponse<LrmListItemResponse> apiServiceResponse =
        lrmListApiService.findListItem(listId, itemId, principal.getSubject());
    ResponseSuccess<LrmListItemResponse> response =
        new ResponseSuccess<>(
            apiServiceResponse.getContent(), apiServiceResponse.getMessage(), request);
    logInfo(response);
    return new ResponseEntity<>(response, HttpStatus.OK);
  }

  @Operation(summary = "Move an item from one list to another.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "404",
            description = "ListItem not found.",
            content = @Content(schema = @Schema(implementation = ResponseProblem.class)))
      })
  @PatchMapping("/{list-id}/items/{item-id}/{destination-list-id}")
  public ResponseEntity<ResponseSuccess<ApiMessage>> listItemMove(
      @PathVariable("list-id") @ValidUuid UUID listId,
      @PathVariable("item-id") @ValidUuid UUID itemId,
      @PathVariable("destination-list-id") @ValidUuid UUID destinationListId,
      HttpServletRequest request,
      @AuthenticationPrincipal Jwt principal) {
    ApiServiceResponse<net.flyingfishflash.loremlist.api.data.response.LrmListItemMovedResponse>
        apiServiceResponse =
            lrmListApiService.moveListItem(
                listId, itemId, destinationListId, principal.getSubject());
    ResponseSuccess<ApiMessage> response =
        new ResponseSuccess<>(
            new ApiMessage(apiServiceResponse.getMessage()),
            apiServiceResponse.getMessage(),
            request);
    logInfo(response);
    return new ResponseEntity<>(response, HttpStatus.OK);
  }

  @Operation(summary = "Update a list item.")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "204", description = "No Content - List item is up-to-date"),
        @ApiResponse(
            responseCode = "404",
            description = "List item not found",
            content = @Content(schema = @Schema(implementation = ResponseProblem.class)))
      })
  @PatchMapping("/{list-id}/items/{item-id}")
  public ResponseEntity<ResponseSuccess<LrmListItemResponse>> listItemPatch(
      @PathVariable("list-id") @ValidUuid UUID listId,
      @PathVariable("item-id") @ValidUuid UUID itemId,
      @RequestBody Map<String, Object> patchRequest,
      HttpServletRequest request,
      @AuthenticationPrincipal Jwt principal) {
    ApiServiceResponse<LrmListItemResponse> apiServiceResponse =
        lrmListApiService.patchListItem(listId, itemId, principal.getSubject(), patchRequest);
    ResponseSuccess<LrmListItemResponse> response =
        new ResponseSuccess<>(
            apiServiceResponse.getContent(), apiServiceResponse.getMessage(), request);
    HttpStatus responseStatus =
        apiServiceResponse.getMessage().contains("not updated")
            ? HttpStatus.NO_CONTENT
            : HttpStatus.OK;
    logInfo(response);
    return new ResponseEntity<>(response, responseStatus);
  }

  private void logInfo(Object response) {
    try {
      logger.info(objectMapper.writeValueAsString(response));
    } catch (Exception e) {
      logger.info("Log message invocation failed: {}", e.toString());
    }
  }
}
