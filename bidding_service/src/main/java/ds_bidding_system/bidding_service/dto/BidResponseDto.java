package ds_bidding_system.bidding_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "BidResponse", description = "Composite schema containing Bid info and full Item details")
public class BidResponseDto {

    @Schema(description = "UUID of the bid listing", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID id;

    @Schema(description = "UUID identifying the associated item", example = "6ba7b810-9dad-11d1-80b4-00c04fd430c8")
    private UUID itemId;

    @Schema(description = "Keycloak ID of the current highest bidder, or null when nobody has bid yet", example = "8c6b3e94-3d71-4b44-9a3b-9e4dfd1c92a6", nullable = true)
    private String highestBidderId;

    @Schema(description = "Timezone-aware date and time when the listing expires", example = "2026-12-31T23:59:59+02:00")
    private OffsetDateTime expirationDate;

    @Schema(description = "Full details of the item fetched from item_service via Feign")
    private ItemDto itemDetails;

    @Schema(description = "Keycloak ID of the listing creator")
    private String ownerId;

    @Schema(description = "Opening price", example = "25.00")
    private BigDecimal startingPrice;

    @Schema(description = "Highest accepted amount, or the opening price when nobody has bid", example = "30.00")
    private BigDecimal currentBid;

    @Schema(description = "Auction finalization status")
    private ds_bidding_system.bidding_service.entity.AuctionStatus status;
}
