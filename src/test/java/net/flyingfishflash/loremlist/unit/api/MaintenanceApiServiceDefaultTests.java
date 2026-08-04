package net.flyingfishflash.loremlist.unit.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import net.flyingfishflash.loremlist.api.MaintenanceApiServiceDefault;
import net.flyingfishflash.loremlist.api.data.response.ApiServiceResponse;
import net.flyingfishflash.loremlist.api.data.response.DomainPurgedResponse;
import net.flyingfishflash.loremlist.domain.ServiceResponse;
import net.flyingfishflash.loremlist.domain.maintenance.MaintenanceService;
import net.flyingfishflash.loremlist.domain.maintenance.data.DomainPurged;
import org.junit.jupiter.api.Test;

class MaintenanceApiServiceDefaultTests {

  private final MaintenanceService maintenanceService = mock(MaintenanceService.class);
  private final MaintenanceApiServiceDefault maintenanceApiService =
      new MaintenanceApiServiceDefault(maintenanceService);

  @Test
  void purge() {
    ServiceResponse<DomainPurged> mockServiceResponse =
        new ServiceResponse<>(new DomainPurged(997, 998, 999), "irrelevant");

    when(maintenanceService.purge()).thenReturn(mockServiceResponse);
    ApiServiceResponse<DomainPurgedResponse> apiServiceResponse = maintenanceApiService.purge();
    assertThat(apiServiceResponse.getContent().associationDeletedCount()).isEqualTo(997);
    assertThat(apiServiceResponse.getContent().itemDeletedCount()).isEqualTo(998);
    assertThat(apiServiceResponse.getContent().listDeletedCount()).isEqualTo(999);
    assertThat(apiServiceResponse.getMessage()).isEqualTo("irrelevant");
  }
}
