package ds_bidding_system.item_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Valid
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "Item", description = "Schema to hold item information")
public class ItemDto {

    @Schema(description = "UUID of the item", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID id;

    @Schema(description = "Name of the item", example = "Laptop")
    @NotEmpty(message = "Item name cannot be empty.")
    private String itemName;

    @Schema(description = "Description of the item", example = "Used laptop in good condition")
    @NotEmpty(message = "Item description cannot be empty.")
    private String itemDescription;

    @Schema(description = "Server-managed image URL; use the upload endpoint to change it", accessMode = Schema.AccessMode.READ_ONLY, example = "/uploads/images/default-item.png")
    private String itemImageLocation;
}
