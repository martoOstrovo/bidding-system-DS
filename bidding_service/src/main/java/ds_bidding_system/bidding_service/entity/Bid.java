package ds_bidding_system.bidding_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
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

    @Column(name = "highest_bidder_id")
    private UUID highestBidderId;

    @Column(name = "expiration_date", nullable = false)
    private OffsetDateTime expirationDate;
}
