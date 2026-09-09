package ds_bidding_system.bidding_service.repository;

import ds_bidding_system.bidding_service.entity.Bid;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Sort;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;
import java.util.List;
import java.time.OffsetDateTime;

@Repository
public interface BidRepository extends JpaRepository<Bid, UUID> {
    Optional<Bid> findByItemId(UUID itemId);

    List<Bid> findByExpirationDateAfter(OffsetDateTime now, Sort sort);

    List<Bid> findByStatusAndExpirationDateAfter(ds_bidding_system.bidding_service.entity.AuctionStatus status,
                                               OffsetDateTime now, Sort sort);

    List<Bid> findTop100ByStatusAndExpirationDateLessThanEqualOrderByExpirationDateAscIdAsc(
            ds_bidding_system.bidding_service.entity.AuctionStatus status, OffsetDateTime now);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Bid b where b.id = :id")
    Optional<Bid> lockById(@Param("id") UUID id);
}
