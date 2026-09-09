package ds_bidding_system.bidding_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "BidRequest", description = "Schema to request creating or updating a bid listing")
public class BidRequestDto {

    @Schema(description = "UUID identifying the associated item",
            example = "6ba7b810-9dad-11d1-80b4-00c04fd430c8",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Item ID cannot be null.")
    private UUID itemId;

    @NotNull
    @DecimalMin("0.00")
    @Digits(integer = 17, fraction = 2)
    @Schema(description = "Opening price", example = "25.00", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal startingPrice;

    @Schema(description = "Timezone-aware date and time when the listing expires (must be a future date)",
            example = "2026-12-31T23:59:59+02:00",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Expiration date cannot be null.")
    @Future(message = "Expiration date must be in the future.")
    private OffsetDateTime expirationDate;
}
