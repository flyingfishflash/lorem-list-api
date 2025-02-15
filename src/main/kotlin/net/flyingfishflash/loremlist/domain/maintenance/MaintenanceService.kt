package net.flyingfishflash.loremlist.domain.maintenance

import net.flyingfishflash.loremlist.domain.ServiceResponse
import net.flyingfishflash.loremlist.domain.association.AssociationRepository
import net.flyingfishflash.loremlist.domain.exceptions.DomainException
import net.flyingfishflash.loremlist.domain.lrmitem.LrmItemRepository
import net.flyingfishflash.loremlist.domain.lrmlist.LrmListRepository
import net.flyingfishflash.loremlist.domain.maintenance.data.DomainPurged
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class MaintenanceService(
  private val associationRepository: AssociationRepository,
  private val itemRepository: LrmItemRepository,
  private val listRepository: LrmListRepository,
) {

  fun purge(): ServiceResponse<DomainPurged> {
    val exceptionMessage = "Items, Lists, and Associations could not be purged"
    try {
      val domainPurged = DomainPurged(
        associationDeletedCount = associationRepository.delete(),
        itemDeletedCount = itemRepository.delete(),
        listDeletedCount = listRepository.delete(),
      )
      return ServiceResponse(content = domainPurged, message = "Domain has been purged.")
    } catch (exception: Exception) {
      throw DomainException(
        cause = exception,
        message = "$exceptionMessage.",
      )
    }
  }
}
