package ds_bidding_system.item_service.controller;

import ds_bidding_system.item_service.constant.ItemConstants;
import ds_bidding_system.item_service.dto.ErrorResponseDto;
import ds_bidding_system.item_service.dto.ItemDto;
import ds_bidding_system.item_service.dto.ResponseDto;
import ds_bidding_system.item_service.service.ItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping(path = "/api", produces = MediaType.APPLICATION_JSON_VALUE)
@Validated
@AllArgsConstructor
@Tag(name = "Items", description = "Create, retrieve, update, and delete items")
@ApiResponse(responseCode = "500", description = "Unexpected server error",
        content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
public class ItemController {
    private final ItemService itemService;

    @PostMapping("/create")
    @Operation(summary = "Create an item", description = "Creates an item with a randomly generated UUID.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Item created successfully",
                    content = @Content(schema = @Schema(implementation = ResponseDto.class),
                            examples = @ExampleObject(value = "{\"statusCode\":\"201\",\"statusMsg\":\"Item created successfully\"}"))),
            @ApiResponse(responseCode = "400", description = "Invalid item information or malformed request body",
                    content = {
                            @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(type = "object", description = "Validation errors keyed by field name"),
                                    examples = @ExampleObject(value = "{\"itemName\":\"Item name cannot be empty.\"}")),
                            @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ProblemDetail.class))
                    })
    })
    public ResponseEntity<ResponseDto> createItem(@Valid @RequestBody ItemDto itemDto) {
        itemService.createItem(itemDto);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new ResponseDto(ItemConstants.STATUS_201, ItemConstants.MESSAGE_201));
    }

    @GetMapping("/get/{id}")
    @Operation(summary = "Get an item", description = "Retrieves item information by UUID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Item retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ItemDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid UUID",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Item not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    public ResponseEntity<ItemDto> getItem(
            @Parameter(description = "UUID of the item", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable UUID id) {
        return ResponseEntity.ok(itemService.getItem(id));
    }

    @PutMapping("/put/{id}")
    @Operation(summary = "Update an item", description = "Replaces the item's name and description while preserving its UUID and image location.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Item updated successfully",
                    content = @Content(schema = @Schema(implementation = ResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid UUID, item information, or request body",
                    content = {
                            @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(type = "object", description = "Validation errors keyed by field name"),
                                    examples = @ExampleObject(value = "{\"itemName\":\"Item name cannot be empty.\"}")),
                            @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ProblemDetail.class))
                    }),
            @ApiResponse(responseCode = "404", description = "Item not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    public ResponseEntity<ResponseDto> updateItem(
            @Parameter(description = "UUID of the item to update", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable UUID id, @Valid @RequestBody ItemDto itemDto) {
        itemService.updateItem(id, itemDto);

        return ResponseEntity.ok(new ResponseDto(ItemConstants.STATUS_200, ItemConstants.MESSAGE_200));
    }

    @DeleteMapping("/delete/{id}")
    @Operation(summary = "Delete an item", description = "Deletes the item identified by its UUID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Item deleted successfully",
                    content = @Content(schema = @Schema(implementation = ResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid UUID",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Item not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    public ResponseEntity<ResponseDto> deleteItem(
            @Parameter(description = "UUID of the item to delete", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable UUID id) {
        itemService.deleteItem(id);

        return ResponseEntity.ok(new ResponseDto(ItemConstants.STATUS_200, ItemConstants.MESSAGE_200));
    }
}
