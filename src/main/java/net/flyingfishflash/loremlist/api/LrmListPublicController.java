package net.flyingfishflash.loremlist.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import net.flyingfishflash.loremlist.api.data.response.ApiServiceResponse;
import net.flyingfishflash.loremlist.api.data.response.LrmListResponse;
import net.flyingfishflash.loremlist.core.response.structure.ResponseSuccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "public")
@RestController
public class LrmListPublicController {

  private static final Logger logger = LoggerFactory.getLogger(LrmListPublicController.class);

  private final LrmListApiService lrmListService;
  private final ObjectMapper objectMapper;

  public LrmListPublicController(LrmListApiService lrmListService, ObjectMapper objectMapper) {
    this.lrmListService = lrmListService;
    this.objectMapper = objectMapper;
  }

  @Operation(
      summary =
          "Retrieve all public lists, optionally including the details of each associated item.")
  @GetMapping("/public/lists")
  public ResponseEntity<ResponseSuccess<List<LrmListResponse>>> findByPublic(
      @RequestParam(defaultValue = "false") boolean includeItems, HttpServletRequest request) {
    ApiServiceResponse<List<LrmListResponse>> apiServiceResponse =
        includeItems ? lrmListService.findByPublic() : lrmListService.findByPublicExcludeItems();
    ResponseSuccess<List<LrmListResponse>> response =
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
