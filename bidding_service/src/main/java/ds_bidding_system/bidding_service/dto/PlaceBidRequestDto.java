package ds_bidding_system.bidding_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

@Schema(name = "PlaceBidRequest", description = "An offer; bidder identity comes from the authenticated user")
public record PlaceBidRequestDto(
        @NotNull @DecimalMin(value = "0.00", inclusive = false) @Digits(integer = 17, fraction = 2)
        @Schema(description = "Must be strictly larger than the current bid; at most two decimal places",
                example = "30.00", requiredMode = Schema.RequiredMode.REQUIRED) BigDecimal amount) {}
