package ds_bidding_system.bidding_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "Item", description = "Local schema compatible with item_service information for future Kafka integration")
public class ItemDto {

    @Schema(description = "Identifier of the item", example = "6ba7b810-9dad-11d1-80b4-00c04fd430c8")
    private UUID id;

    @Schema(description = "Name of the item", example = "Laptop")
    private String itemName;

    @Schema(description = "Description of the item", example = "Used laptop in good condition")
    private String itemDescription;

    @Schema(description = "Image location of the item", example = "/images/laptop.png")
    private String itemImageLocation;
}
