package net.flyingfishflash.loremlist.api;

import net.flyingfishflash.loremlist.api.data.response.ApiServiceResponse;
import net.flyingfishflash.loremlist.api.data.response.DomainPurgedResponse;
import net.flyingfishflash.loremlist.domain.ServiceResponse;
import net.flyingfishflash.loremlist.domain.maintenance.MaintenanceService;
import net.flyingfishflash.loremlist.domain.maintenance.data.DomainPurged;
import org.springframework.stereotype.Service;

@Service
public class MaintenanceApiServiceDefault implements MaintenanceApiService {

  private final MaintenanceService maintenanceService;

  public MaintenanceApiServiceDefault(MaintenanceService maintenanceService) {
    this.maintenanceService = maintenanceService;
  }

  @Override
  public ApiServiceResponse<DomainPurgedResponse> purge() {
    ServiceResponse<DomainPurged> serviceResponse = maintenanceService.purge();
    DomainPurged domainPurged = serviceResponse.getContent();
    DomainPurgedResponse domainPurgedResponse =
        new DomainPurgedResponse(
            domainPurged.associationDeletedCount(),
            domainPurged.itemDeletedCount(),
            domainPurged.listDeletedCount());
    return new ApiServiceResponse<>(domainPurgedResponse, serviceResponse.getMessage());
  }
}
