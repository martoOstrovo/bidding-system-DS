package ds_bidding_system.bidding_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.math.BigDecimal;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import java.util.UUID;

@Data
@Valid
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "Bid", description = "Schema to hold bid listing information")
public class BidDto {

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Schema(description = "OPEN until the expiry worker finalizes the auction; offers always check expirationDate too",
            accessMode = Schema.AccessMode.READ_ONLY)
    private ds_bidding_system.bidding_service.entity.AuctionStatus status;

    @com.fasterxml.jackson.annotation.JsonProperty(access = com.fasterxml.jackson.annotation.JsonProperty.Access.READ_ONLY)
    @Schema(description = "Keycloak ID of the authenticated listing creator", accessMode = Schema.AccessMode.READ_ONLY)
    private String ownerId;

    @Schema(description = "UUID of the bid listing (generated on create, preserved on update)",
            example = "550e8400-e29b-41d4-a716-446655440000",
            accessMode = Schema.AccessMode.READ_ONLY)
    private UUID id;

    @Schema(description = "UUID identifying the associated item",
            example = "6ba7b810-9dad-11d1-80b4-00c04fd430c8",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Item ID cannot be null.")
    private UUID itemId;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Schema(description = "Keycloak ID of the current highest bidder, or null when nobody has bid yet",
            example = "8c6b3e94-3d71-4b44-9a3b-9e4dfd1c92a6",
            nullable = true, accessMode = Schema.AccessMode.READ_ONLY)
    private String highestBidderId;

    @NotNull(message = "Starting price is required.")
    @DecimalMin(value = "0.00", message = "Starting price cannot be negative.")
    @Digits(integer = 17, fraction = 2)
    @Schema(description = "Opening price; the first offer must exceed this amount", example = "25.00",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal startingPrice;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Schema(description = "Highest accepted amount, or the starting price when there are no offers",
            example = "30.00", accessMode = Schema.AccessMode.READ_ONLY)
    private BigDecimal currentBid;

    @Schema(description = "Alternative to durationSeconds: expiration at least 60 seconds from the server's current time, with no configured maximum. Updates may preserve the exact existing deadline even with less than a minute remaining.")
    @Future(message = "Expiration date must be in the future.")
    private OffsetDateTime expirationDate;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @jakarta.validation.constraints.Min(value = 60, message = "Duration must be at least 60 seconds.")
    @Schema(description = "Duration in whole seconds, minimum 60 with no configured maximum. Supply this or expirationDate, not both. The server calculates the deadline.",
            example = "60", accessMode = Schema.AccessMode.WRITE_ONLY)
    private Long durationSeconds;
}
