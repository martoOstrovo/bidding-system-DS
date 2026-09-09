package ds_bidding_system.bidding_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.math.BigDecimal;
import java.util.UUID;

@Data
@Entity
@Table(name = "bids")
@NoArgsConstructor
@AllArgsConstructor
public class Bid {
    @Id
    private UUID id;

    @Column(name = "item_id", nullable = false)
    private UUID itemId;

    // Nullable only for pre-existing listings whose creator is unknown; immutable after creation.
    @Column(name = "owner_id", updatable = false)
    private String ownerId;

    @Column(name = "highest_bidder_id")
    private String highestBidderId;

    @Column(name = "starting_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal startingPrice;

    @Column(name = "current_bid", nullable = false, precision = 19, scale = 2)
    private BigDecimal currentBid;

    @Column(name = "expiration_date", nullable = false)
    private OffsetDateTime expirationDate;

    @jakarta.persistence.Enumerated(jakarta.persistence.EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AuctionStatus status = AuctionStatus.OPEN;
}
