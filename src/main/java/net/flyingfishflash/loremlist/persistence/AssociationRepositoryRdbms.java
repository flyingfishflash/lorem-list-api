package net.flyingfishflash.loremlist.persistence;

import java.util.UUID;
import net.flyingfishflash.loremlist.domain.association.AssociationRepository;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class AssociationRepositoryRdbms implements AssociationRepository {

  private final JdbcClient jdbcClient;

  AssociationRepositoryRdbms(JdbcClient jdbcClient) {
    this.jdbcClient = jdbcClient;
  }

  @Override
  public boolean listIsConsistent(UUID listId) {
    long mismatchedCount =
        jdbcClient
            .sql(
                "SELECT COUNT(*) FROM list_item li "
                    + "JOIN list l ON l.id = li.list_id "
                    + "JOIN item i ON i.id = li.item_id "
                    + "WHERE l.id = :listId AND l.owner <> i.owner")
            .param("listId", listId)
            .query(Long.class)
            .single();
    return mismatchedCount == 0;
  }

  @Override
  public int delete() {
    return jdbcClient.sql("DELETE FROM list_item").update();
  }
}
