package net.flyingfishflash.loremlist.unit.api

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import net.flyingfishflash.loremlist.api.MaintenanceApiServiceDefault
import net.flyingfishflash.loremlist.domain.ServiceResponse
import net.flyingfishflash.loremlist.domain.maintenance.MaintenanceService
import net.flyingfishflash.loremlist.domain.maintenance.data.DomainPurged

class MaintenanceApiServiceDefaultTests :
  DescribeSpec({

    val maintenanceService = mockk<MaintenanceService>()
    val maintenanceApiService = MaintenanceApiServiceDefault(maintenanceService)

    afterEach { clearAllMocks() }
    afterSpec { unmockkAll() }

    describe("MaintenanceApiServiceDefault") {
      it("purge") {
        val mockServiceResponse = ServiceResponse(
          content = DomainPurged(
            associationDeletedCount = 997,
            itemDeletedCount = 998,
            listDeletedCount = 999,
          ),
          message = "irrelevant",
        )

        every { maintenanceService.purge() } returns mockServiceResponse
        val apiServiceResponse = maintenanceApiService.purge()
        apiServiceResponse.content.associationDeletedCount shouldBe 997
        apiServiceResponse.content.itemDeletedCount shouldBe 998
        apiServiceResponse.content.listDeletedCount shouldBe 999
        apiServiceResponse.message shouldBe "irrelevant"
      }
    }
  })
