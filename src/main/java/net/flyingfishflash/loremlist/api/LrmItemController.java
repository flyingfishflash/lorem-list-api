package net.flyingfishflash.loremlist.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.flyingfishflash.loremlist.api.data.response.ApiServiceResponse;
import net.flyingfishflash.loremlist.api.data.response.LrmItemDeletedResponse;
import net.flyingfishflash.loremlist.api.data.response.LrmItemResponse;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "item")
@ApiResponses(
    value = {
      @ApiResponse(responseCode = "204", description = "No content"),
      @ApiResponse(
          responseCode = "400",
          description = "Bad request",
          content = @Content(schema = @Schema(implementation = ResponseProblem.class))),
      @ApiResponse(
          responseCode = "401",
          description = "Unauthorized",
          content = @Content(schema = @Schema())),
      @ApiResponse(
          responseCode = "500",
          description = "Internal server error",
          content = @Content(schema = @Schema(implementation = ResponseProblem.class)))
    })
@RestController
@RequestMapping("/items")
public class LrmItemController {

  private static final Logger logger = LoggerFactory.getLogger(LrmItemController.class);

  private final LrmItemApiService lrmItemApiService;
  private final ObjectMapper objectMapper;

  public LrmItemController(LrmItemApiService lrmItemApiService, ObjectMapper objectMapper) {
    this.lrmItemApiService = lrmItemApiService;
    this.objectMapper = objectMapper;
  }

  @Operation(summary = "Count of all items.")
  @GetMapping("/count")
  public ResponseEntity<ResponseSuccess<ApiMessageNumeric>> countWhereOwnerIsPrincipal(
      HttpServletRequest request, @AuthenticationPrincipal Jwt principal) {
    ApiServiceResponse<ApiMessageNumeric> apiServiceResponse =
        lrmItemApiService.countByOwner(principal.getSubject());
    ResponseSuccess<ApiMessageNumeric> response =
        new ResponseSuccess<>(
            apiServiceResponse.getContent(), apiServiceResponse.getMessage(), request);
    logInfo(response);
    return new ResponseEntity<>(response, HttpStatus.OK);
  }

  @Operation(summary = "Delete all items. Lists are disassociated, not deleted.")
  @DeleteMapping
  public ResponseEntity<ResponseSuccess<LrmItemDeletedResponse>> deleteWhereOwnerIsPrincipal(
      HttpServletRequest request, @AuthenticationPrincipal Jwt principal) {
    ApiServiceResponse<LrmItemDeletedResponse> apiServiceResponse =
        lrmItemApiService.deleteByOwner(principal.getSubject());
    ResponseSuccess<LrmItemDeletedResponse> response =
        new ResponseSuccess<>(
            apiServiceResponse.getContent(), apiServiceResponse.getMessage(), request);
    logInfo(response);
    return new ResponseEntity<>(response, HttpStatus.OK);
  }

  @Operation(summary = "Delete an item.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "404",
            description = "Not found",
            content = @Content(schema = @Schema(implementation = ResponseProblem.class))),
        @ApiResponse(
            responseCode = "422",
            description =
                "Unprocessable Content - Item not deleted due to existing list associations constraint.",
            content = @Content(schema = @Schema(implementation = ResponseProblem.class)))
      })
  @DeleteMapping("/{item-id}")
  public ResponseEntity<ResponseSuccess<LrmItemDeletedResponse>> deleteByIdWhereOwnerIsPrincipal(
      @PathVariable("item-id") @ValidUuid UUID itemId,
      @RequestParam(defaultValue = "false") boolean removeListAssociations,
      HttpServletRequest request,
      @AuthenticationPrincipal Jwt principal) {
    ApiServiceResponse<LrmItemDeletedResponse> apiServiceResponse =
        lrmItemApiService.deleteByOwnerAndId(
            itemId, principal.getSubject(), removeListAssociations);
    ResponseSuccess<LrmItemDeletedResponse> response =
        new ResponseSuccess<>(
            apiServiceResponse.getContent(), apiServiceResponse.getMessage(), request);
    logInfo(response);
    return new ResponseEntity<>(response, HttpStatus.OK);
  }

  @Operation(summary = "Retrieve all items.")
  @GetMapping
  public ResponseEntity<ResponseSuccess<List<LrmItemResponse>>> findWhereOwnerIsPrincipal(
      HttpServletRequest request, @AuthenticationPrincipal Jwt principal) {
    ApiServiceResponse<List<LrmItemResponse>> apiServiceResponse =
        lrmItemApiService.findByOwner(principal.getSubject());
    ResponseSuccess<List<LrmItemResponse>> response =
        new ResponseSuccess<>(
            apiServiceResponse.getContent(), apiServiceResponse.getMessage(), request);
    logInfo(response);
    return new ResponseEntity<>(response, HttpStatus.OK);
  }

  @Operation(summary = "Retrieve an item.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "404",
            description = "Not found.",
            content =
                @Content(
                    schema =
                        @Schema(implementation = ResponseProblem.class, example = "lasjkdflkjDSF")))
      })
  @GetMapping("/{item-id}")
  public ResponseEntity<ResponseSuccess<LrmItemResponse>> findByIdWhereOwnerIsPrincipal(
      @PathVariable("item-id") @ValidUuid UUID itemId,
      HttpServletRequest request,
      @AuthenticationPrincipal Jwt principal) {
    ApiServiceResponse<LrmItemResponse> apiServiceResponse =
        lrmItemApiService.findByOwnerAndId(itemId, principal.getSubject());
    ResponseSuccess<LrmItemResponse> response =
        new ResponseSuccess<>(
            apiServiceResponse.getContent(), apiServiceResponse.getMessage(), request);
    logInfo(response);
    return new ResponseEntity<>(response, HttpStatus.OK);
  }

  @Operation(summary = "Retrieve items that are not part of a list.")
  @GetMapping("/with-no-lists")
  public ResponseEntity<ResponseSuccess<List<LrmItemResponse>>>
      findByPrincipalAndHavingNoListAssociations(
          HttpServletRequest request, @AuthenticationPrincipal Jwt principal) {
    ApiServiceResponse<List<LrmItemResponse>> apiServiceResponse =
        lrmItemApiService.findByOwnerAndHavingNoListAssociations(principal.getSubject());
    ResponseSuccess<List<LrmItemResponse>> response =
        new ResponseSuccess<>(
            apiServiceResponse.getContent(), apiServiceResponse.getMessage(), request);
    logInfo(response);
    return new ResponseEntity<>(response, HttpStatus.OK);
  }

  @Operation(summary = "Update an item.")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "204", description = "No content - Item is up-to-date"),
        @ApiResponse(
            responseCode = "404",
            description = "Not found",
            content = @Content(schema = @Schema(implementation = ResponseProblem.class)))
      })
  @PatchMapping("/{item-id}")
  public ResponseEntity<ResponseSuccess<LrmItemResponse>> patchByIdWhereOwnerIsPrincipal(
      @PathVariable("item-id") @ValidUuid UUID itemId,
      @RequestBody Map<String, Object> patchRequest,
      HttpServletRequest request,
      @AuthenticationPrincipal Jwt principal) {
    ApiServiceResponse<LrmItemResponse> apiServiceResponse =
        lrmItemApiService.patchByOwnerAndId(itemId, principal.getSubject(), patchRequest);
    ResponseSuccess<LrmItemResponse> response =
        new ResponseSuccess<>(
            apiServiceResponse.getContent(), apiServiceResponse.getMessage(), request);
    HttpStatus responseStatus =
        apiServiceResponse.getMessage().contains("not patched")
            ? HttpStatus.NO_CONTENT
            : HttpStatus.OK;
    logInfo(response);
    return new ResponseEntity<>(response, responseStatus);
  }

  @Operation(summary = "Count of lists associated with an item.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "404",
            description = "Not found.",
            content = @Content(schema = @Schema(implementation = ResponseProblem.class)))
      })
  @GetMapping("/{item-id}/lists/count")
  public ResponseEntity<ResponseSuccess<ApiMessageNumeric>>
      listAssociationsCountWhereListOwnerIsPrincipal(
          @PathVariable("item-id") @ValidUuid UUID itemId,
          HttpServletRequest request,
          @AuthenticationPrincipal Jwt principal) {
    ApiServiceResponse<ApiMessageNumeric> apiServiceResponse =
        lrmItemApiService.countListAssociationsByItemIdAndItemOwner(itemId, principal.getSubject());
    ResponseSuccess<ApiMessageNumeric> response =
        new ResponseSuccess<>(
            apiServiceResponse.getContent(), apiServiceResponse.getMessage(), request);
    logInfo(response);
    return new ResponseEntity<>(response, HttpStatus.OK);
  }

  private void logInfo(Object response) {
    try {
      logger.info(objectMapper.writeValueAsString(response));
    } catch (Exception e) {
      logger.info("Log message invocation failed: {}", e.toString());
    }
  }
}
