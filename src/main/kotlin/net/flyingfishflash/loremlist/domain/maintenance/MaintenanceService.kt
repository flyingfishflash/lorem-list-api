package net.flyingfishflash.loremlist.domain.maintenance

import net.flyingfishflash.loremlist.core.exceptions.ApiException
import net.flyingfishflash.loremlist.domain.association.AssociationRepository
import net.flyingfishflash.loremlist.domain.lrmitem.LrmItemRepository
import net.flyingfishflash.loremlist.domain.lrmlist.LrmListRepository
import net.flyingfishflash.loremlist.domain.maintenance.data.PurgeResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class MaintenanceService(
  private val associationRepository: AssociationRepository,
  private val itemRepository: LrmItemRepository,
  private val listRepository: LrmListRepository,
) {

  fun purge(): PurgeResponse {
    val exceptionMessage = "Items, Lists, and Associations could not be purged"
    try {
      val purgeResponse = PurgeResponse(
        associationDeletedCount = associationRepository.deleteAll(),
        itemDeletedCount = itemRepository.deleteAll(),
        listDeletedCount = listRepository.deleteAll(),
      )
      return purgeResponse
    } catch (exception: Exception) {
      throw ApiException(
        cause = exception,
        message = "$exceptionMessage.",
      )
    }
  }
}
