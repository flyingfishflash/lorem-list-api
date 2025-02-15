package net.flyingfishflash.loremlist.api

import net.flyingfishflash.loremlist.api.data.response.ApiServiceResponse
import net.flyingfishflash.loremlist.api.data.response.DomainPurgedResponse
import net.flyingfishflash.loremlist.domain.maintenance.MaintenanceService
import org.springframework.stereotype.Service

@Service
class MaintenanceApiServiceDefault(private val maintenanceService: MaintenanceService) : MaintenanceApiService {
  override fun purge(): ApiServiceResponse<DomainPurgedResponse> {
    val serviceResponse = maintenanceService.purge()
    val domainPurgedResponse =
      DomainPurgedResponse(
        associationDeletedCount = serviceResponse.content.associationDeletedCount,
        itemDeletedCount = serviceResponse.content.itemDeletedCount,
        listDeletedCount = serviceResponse.content.listDeletedCount,
      )
    return ApiServiceResponse(content = domainPurgedResponse, message = serviceResponse.message)
  }
}
