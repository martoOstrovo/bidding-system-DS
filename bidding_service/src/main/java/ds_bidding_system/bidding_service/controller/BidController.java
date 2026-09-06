package ds_bidding_system.bidding_service.controller;

import ds_bidding_system.bidding_service.constant.BidConstants;
import ds_bidding_system.bidding_service.dto.BidDto;
import ds_bidding_system.bidding_service.dto.ErrorResponseDto;
import ds_bidding_system.bidding_service.dto.ResponseDto;
import ds_bidding_system.bidding_service.service.BidService;
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
@Tag(name = "Bids", description = "Create, retrieve, update, and delete bid listings")
@ApiResponse(responseCode = "500", description = "Unexpected server error",
        content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
public class BidController {

    private final BidService bidService;

    @PostMapping("/create")
    @Operation(summary = "Create a bid listing",
               description = "Creates a new bid listing with a randomly generated UUID. " +
                             "highestBidderId may be omitted (null) when no bids have been placed yet. " +
                             "expirationDate must be a future timezone-aware date/time.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Bid listing created successfully",
                    content = @Content(schema = @Schema(implementation = ResponseDto.class),
                            examples = @ExampleObject(
                                    value = "{\"statusCode\":\"201\",\"statusMsg\":\"Bid listing created successfully\"}"))),
            @ApiResponse(responseCode = "400", description = "Invalid bid information or malformed request body",
                    content = {
                            @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(type = "object", description = "Validation errors keyed by field name"),
                                    examples = @ExampleObject(
                                            value = "{\"expirationDate\":\"Expiration date must be in the future.\"," +
                                                    "\"itemId\":\"Item ID cannot be null.\"}")),
                            @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ProblemDetail.class))
                    })
    })
    public ResponseEntity<ResponseDto> createBid(@Valid @RequestBody BidDto bidDto) {
        bidService.createBid(bidDto);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new ResponseDto(BidConstants.STATUS_201, BidConstants.MESSAGE_201));
    }

    @GetMapping("/get/{id}")
    @Operation(summary = "Get a bid listing", description = "Retrieves bid listing information by UUID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Bid listing retrieved successfully",
                    content = @Content(schema = @Schema(implementation = BidDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid UUID",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Bid listing not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    public ResponseEntity<BidDto> getBid(
            @Parameter(description = "UUID of the bid listing", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable UUID id) {
        return ResponseEntity.ok(bidService.getBid(id));
    }

    @PutMapping("/put/{id}")
    @Operation(summary = "Update a bid listing",
               description = "Replaces the bid listing's fields while preserving its UUID. " +
                             "Updatable fields: itemId, highestBidderId, expirationDate. " +
                             "Setting highestBidderId to null clears the current highest bidder. " +
                             "expirationDate must be a future timezone-aware date/time.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Bid listing updated successfully",
                    content = @Content(schema = @Schema(implementation = ResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid UUID, bid information, or request body",
                    content = {
                            @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(type = "object", description = "Validation errors keyed by field name"),
                                    examples = @ExampleObject(
                                            value = "{\"expirationDate\":\"Expiration date must be in the future.\"}")),
                            @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ProblemDetail.class))
                    }),
            @ApiResponse(responseCode = "404", description = "Bid listing not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    public ResponseEntity<ResponseDto> updateBid(
            @Parameter(description = "UUID of the bid listing to update", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable UUID id,
            @Valid @RequestBody BidDto bidDto) {
        bidService.updateBid(id, bidDto);
        return ResponseEntity.ok(new ResponseDto(BidConstants.STATUS_200, BidConstants.MESSAGE_200));
    }

    @DeleteMapping("/delete/{id}")
    @Operation(summary = "Delete a bid listing", description = "Deletes the bid listing identified by its UUID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Bid listing deleted successfully",
                    content = @Content(schema = @Schema(implementation = ResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid UUID",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Bid listing not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    public ResponseEntity<ResponseDto> deleteBid(
            @Parameter(description = "UUID of the bid listing to delete", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable UUID id) {
        bidService.deleteBid(id);
        return ResponseEntity.ok(new ResponseDto(BidConstants.STATUS_200, BidConstants.MESSAGE_200));
    }
}
