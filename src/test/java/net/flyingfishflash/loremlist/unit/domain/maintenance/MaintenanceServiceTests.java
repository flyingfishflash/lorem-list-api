package net.flyingfishflash.loremlist.unit.domain.maintenance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import net.flyingfishflash.loremlist.domain.ServiceResponse;
import net.flyingfishflash.loremlist.domain.association.AssociationRepository;
import net.flyingfishflash.loremlist.domain.exceptions.DomainException;
import net.flyingfishflash.loremlist.domain.lrmitem.LrmItemRepository;
import net.flyingfishflash.loremlist.domain.lrmlist.LrmListRepository;
import net.flyingfishflash.loremlist.domain.maintenance.MaintenanceService;
import net.flyingfishflash.loremlist.domain.maintenance.data.DomainPurged;
import org.junit.jupiter.api.Test;

class MaintenanceServiceTests {

  private final AssociationRepository mockAssociationRepository = mock(AssociationRepository.class);
  private final LrmItemRepository mockLrmItemRepository = mock(LrmItemRepository.class);
  private final LrmListRepository mockLrmListRepository = mock(LrmListRepository.class);
  private final MaintenanceService maintenanceService =
      new MaintenanceService(
          mockAssociationRepository, mockLrmItemRepository, mockLrmListRepository);

  @Test
  void domainIsPurged() {
    when(mockAssociationRepository.delete()).thenReturn(997);
    when(mockLrmItemRepository.delete()).thenReturn(998);
    when(mockLrmListRepository.delete()).thenReturn(999);
    ServiceResponse<DomainPurged> serviceResponse = maintenanceService.purge();
    assertThat(serviceResponse.getContent().associationDeletedCount()).isEqualTo(997);
    assertThat(serviceResponse.getContent().itemDeletedCount()).isEqualTo(998);
    assertThat(serviceResponse.getContent().listDeletedCount()).isEqualTo(999);
  }

  @Test
  void domainIsNotPurgedWhenRepositoryThrows() {
    when(mockAssociationRepository.delete()).thenThrow(new RuntimeException("Lorem Ipsum"));
    assertThatThrownBy(maintenanceService::purge)
        .isInstanceOf(DomainException.class)
        .hasMessage("Items, Lists, and Associations could not be purged.");
  }
}
