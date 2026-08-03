package net.flyingfishflash.loremlist.domain.maintenance;

import net.flyingfishflash.loremlist.domain.ServiceResponse;
import net.flyingfishflash.loremlist.domain.association.AssociationRepository;
import net.flyingfishflash.loremlist.domain.exceptions.DomainException;
import net.flyingfishflash.loremlist.domain.lrmitem.LrmItemRepository;
import net.flyingfishflash.loremlist.domain.lrmlist.LrmListRepository;
import net.flyingfishflash.loremlist.domain.maintenance.data.DomainPurged;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class MaintenanceService {

  private final AssociationRepository associationRepository;
  private final LrmItemRepository itemRepository;
  private final LrmListRepository listRepository;

  public MaintenanceService(
      AssociationRepository associationRepository,
      LrmItemRepository itemRepository,
      LrmListRepository listRepository) {
    this.associationRepository = associationRepository;
    this.itemRepository = itemRepository;
    this.listRepository = listRepository;
  }

  public ServiceResponse<DomainPurged> purge() {
    String exceptionMessage = "Items, Lists, and Associations could not be purged";
    try {
      DomainPurged domainPurged =
          new DomainPurged(
              associationRepository.delete(), itemRepository.delete(), listRepository.delete());
      return new ServiceResponse<>(domainPurged, "Domain has been purged.");
    } catch (Exception exception) {
      throw DomainException.builder().cause(exception).message(exceptionMessage + ".").build();
    }
  }
}
