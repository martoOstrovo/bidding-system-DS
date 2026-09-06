package ds_bidding_system.item_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
@Valid
@Schema(name = "Item", description = "Schema to hold item information")
public class ItemDto {
    @Schema(description = "Name of the item", example = "Laptop")
    @NotEmpty(message = "Item name cannot be empty.")
    private String itemName;

    @Schema(description = "Description of the item", example = "Used laptop in good condition")
    @NotEmpty(message = "Item description cannot be empty.")
    private String itemDescription;
}
