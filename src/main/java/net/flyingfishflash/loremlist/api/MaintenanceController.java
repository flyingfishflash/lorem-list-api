package net.flyingfishflash.loremlist.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import net.flyingfishflash.loremlist.api.data.response.ApiServiceResponse;
import net.flyingfishflash.loremlist.api.data.response.DomainPurgedResponse;
import net.flyingfishflash.loremlist.core.response.structure.ResponseProblem;
import net.flyingfishflash.loremlist.core.response.structure.ResponseSuccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "domain maintenance")
@ApiResponses(
    value = {
      @ApiResponse(
          responseCode = "400",
          description = "Bad Request",
          content = @Content(schema = @Schema(implementation = ResponseProblem.class)))
    })
@RestController
@RequestMapping("/maintenance")
public class MaintenanceController {

  private static final Logger logger = LoggerFactory.getLogger(MaintenanceController.class);

  private final MaintenanceApiService maintenanceApiService;
  private final ObjectMapper objectMapper;

  public MaintenanceController(
      MaintenanceApiService maintenanceApiService, ObjectMapper objectMapper) {
    this.maintenanceApiService = maintenanceApiService;
    this.objectMapper = objectMapper;
  }

  @Operation(summary = "Purge all items, lists, and associations")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Successful purge of all domain objects")
      })
  @DeleteMapping("/purge")
  public ResponseEntity<ResponseSuccess<DomainPurgedResponse>> purge(HttpServletRequest request) {
    ApiServiceResponse<DomainPurgedResponse> apiServiceResponse = maintenanceApiService.purge();
    ResponseSuccess<DomainPurgedResponse> response =
        new ResponseSuccess<>(
            apiServiceResponse.getContent(), apiServiceResponse.getMessage(), request);
    HttpStatus responseStatus = HttpStatus.OK;
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
