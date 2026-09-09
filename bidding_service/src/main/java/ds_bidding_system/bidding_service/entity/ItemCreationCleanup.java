package ds_bidding_system.bidding_service.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor
public class ItemCreationCleanup {
    @Id
    private UUID itemId;
    private UUID bidId;
    private Instant createdAt;

    public ItemCreationCleanup(UUID itemId, UUID bidId) {
        this.itemId = itemId;
        this.bidId = bidId;
        this.createdAt = Instant.now();
    }
}
