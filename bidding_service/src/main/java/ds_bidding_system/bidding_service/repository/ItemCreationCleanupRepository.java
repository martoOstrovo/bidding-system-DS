package ds_bidding_system.bidding_service.repository;

import ds_bidding_system.bidding_service.entity.ItemCreationCleanup;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ItemCreationCleanupRepository extends JpaRepository<ItemCreationCleanup, UUID> {
    List<ItemCreationCleanup> findTop100ByCreatedAtBeforeOrderByCreatedAtAsc(Instant cutoff);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from ItemCreationCleanup c where c.itemId = :id")
    Optional<ItemCreationCleanup> lockById(@Param("id") UUID id);
}
