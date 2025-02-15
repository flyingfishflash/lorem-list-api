package net.flyingfishflash.loremlist.api

import net.flyingfishflash.loremlist.api.data.response.ApiServiceResponse
import net.flyingfishflash.loremlist.api.data.response.DomainPurgedResponse

interface MaintenanceApiService {
  fun purge(): ApiServiceResponse<DomainPurgedResponse>
}
