package net.flyingfishflash.loremlist.unit.domain.maintenance;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import net.flyingfishflash.loremlist.api.MaintenanceApiService;
import net.flyingfishflash.loremlist.api.MaintenanceController;
import net.flyingfishflash.loremlist.api.data.response.ApiServiceResponse;
import net.flyingfishflash.loremlist.api.data.response.DomainPurgedResponse;
import net.flyingfishflash.loremlist.core.response.structure.DispositionOfSuccess;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = MaintenanceController.class)
class MaintenanceControllerTests {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private MaintenanceApiService mockMaintenanceApiService;

  @Test
  void domainIsPurged() throws Exception {
    DomainPurgedResponse domainPurgedResponse = new DomainPurgedResponse(997, 998, 999);
    ApiServiceResponse<DomainPurgedResponse> apiServiceResponse =
        new ApiServiceResponse<>(domainPurgedResponse, "irrelevant");
    when(mockMaintenanceApiService.purge()).thenReturn(apiServiceResponse);
    String instance = "/maintenance/purge";

    mockMvc
        .perform(delete(instance).with(jwt()).with(csrf()).contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.disposition").value(DispositionOfSuccess.SUCCESS.nameAsLowercase()))
        .andExpect(jsonPath("$.method").value(HttpMethod.DELETE.name().toLowerCase()))
        .andExpect(jsonPath("$.message").value("irrelevant"))
        .andExpect(jsonPath("$.instance").value(instance))
        .andExpect(jsonPath("$.size").value(1))
        .andExpect(
            jsonPath("$.content.associationDeletedCount")
                .value(apiServiceResponse.getContent().associationDeletedCount()))
        .andExpect(
            jsonPath("$.content.itemDeletedCount")
                .value(apiServiceResponse.getContent().itemDeletedCount()))
        .andExpect(
            jsonPath("$.content.listDeletedCount")
                .value(apiServiceResponse.getContent().listDeletedCount()));
  }
}
