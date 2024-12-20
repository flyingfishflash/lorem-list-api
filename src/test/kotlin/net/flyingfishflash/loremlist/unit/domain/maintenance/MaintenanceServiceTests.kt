package net.flyingfishflash.loremlist.unit.domain.maintenance

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import net.flyingfishflash.loremlist.core.exceptions.ApiException
import net.flyingfishflash.loremlist.domain.association.AssociationRepository
import net.flyingfishflash.loremlist.domain.lrmitem.LrmItemRepository
import net.flyingfishflash.loremlist.domain.lrmlist.LrmListRepository
import net.flyingfishflash.loremlist.domain.maintenance.MaintenanceService

class MaintenanceServiceTests :
  DescribeSpec({

    val mockAssociationRepository = mockk<AssociationRepository>()
    val mockLrmItemRepository = mockk<LrmItemRepository>()
    val mockLrmListRepository = mockk<LrmListRepository>()
    val maintenanceService = MaintenanceService(mockAssociationRepository, mockLrmItemRepository, mockLrmListRepository)

    afterEach { clearAllMocks() }
    afterSpec { unmockkAll() }

    describe("purge()") {
      it("domain is purged") {
        every { mockAssociationRepository.delete() } returns 997
        every { mockLrmItemRepository.delete() } returns 998
        every { mockLrmListRepository.delete() } returns 999
        val serviceResponse = maintenanceService.purge()
        serviceResponse.associationDeletedCount.shouldBe(997)
        serviceResponse.itemDeletedCount.shouldBe(998)
        serviceResponse.listDeletedCount.shouldBe(999)
      }

      it("domain is not purged (repository exception") {
        every { mockAssociationRepository.delete() } throws RuntimeException("Lorem Ipsum")
        val exception = shouldThrow<ApiException> { maintenanceService.purge() }
        exception.message.shouldBe("Items, Lists, and Associations could not be purged.")
      }
    }
  })
