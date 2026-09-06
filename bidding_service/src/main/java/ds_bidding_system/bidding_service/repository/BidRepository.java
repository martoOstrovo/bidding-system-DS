package ds_bidding_system.bidding_service.repository;

import ds_bidding_system.bidding_service.entity.Bid;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface BidRepository extends JpaRepository<Bid, UUID> {
    Optional<Bid> findByItemId(UUID itemId);
}
