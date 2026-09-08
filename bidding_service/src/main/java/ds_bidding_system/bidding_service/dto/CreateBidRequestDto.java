package ds_bidding_system.bidding_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "CreateBidRequest", description = "Schema for creating a bid listing along with its item details")
public class CreateBidRequestDto {

    @Valid
    @NotNull(message = "Item details cannot be null.")
    @Schema(description = "Item details to be created in item_service via Feign")
    private ItemDto item;

    @NotNull(message = "Expiration date cannot be null.")
    @Future(message = "Expiration date must be in the future.")
    @Schema(description = "Timezone-aware date and time when the listing expires", example = "2026-12-31T23:59:59+02:00")
    private OffsetDateTime expirationDate;
}
