package ds_bidding_system.bidding_service.controller;

import ds_bidding_system.bidding_service.constant.BidConstants;
import ds_bidding_system.bidding_service.dto.BidDto;
import ds_bidding_system.bidding_service.dto.BidResponseDto;
import ds_bidding_system.bidding_service.dto.CreateBidRequestDto;
import ds_bidding_system.bidding_service.dto.ErrorResponseDto;
import ds_bidding_system.bidding_service.dto.ResponseDto;
import ds_bidding_system.bidding_service.dto.PlaceBidRequestDto;
import io.swagger.v3.oas.annotations.media.ArraySchema;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.util.List;

@RestController
@RequestMapping(path = "/api", produces = MediaType.APPLICATION_JSON_VALUE)
@Validated
@AllArgsConstructor
@Tag(name = "Bids", description = "Browse auctions, place offers, and manage your listings")
@ApiResponse(responseCode = "500", description = "Unexpected server error",
        content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
public class BidController {

    private final BidService bidService;

    @GetMapping("/list")
    @Operation(summary = "Browse auctions", description = "Returns a JSON list ordered by expiration time and ID. "
            + "Includes expired auctions unless activeOnly=true. Each entry includes itemId, ownerId, startingPrice, "
            + "currentBid and highestBidderId. Use the details endpoint for the full item information.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Auction list; empty when none match",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = BidDto.class)))),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content)
    })
    public ResponseEntity<List<BidDto>> listBids(
            @Parameter(description = "Include only auctions whose expiration is still in the future")
            @RequestParam(defaultValue = "false") boolean activeOnly) {
        return ResponseEntity.ok(bidService.listBids(activeOnly));
    }

    @PostMapping("/{id}/bid")
    @Operation(summary = "Place an offer", description = "Accepts an amount strictly larger than the current bid before "
            + "the auction expires. The bidder is the authenticated user, who cannot be the owner. The amount and bidder "
            + "are updated together under a database lock. Browser requests through the gateway require CSRF.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Offer accepted; updated auction returned",
                    content = @Content(schema = @Schema(implementation = BidDto.class))),
            @ApiResponse(responseCode = "400", description = "Missing, invalid, or overly precise amount", content = @Content),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content),
            @ApiResponse(responseCode = "403", description = "Owner cannot bid on their own auction", content = @Content),
            @ApiResponse(responseCode = "404", description = "Auction not found", content = @Content),
            @ApiResponse(responseCode = "409", description = "Offer is not higher, auction expired, or concurrent update needs retry", content = @Content)
    })
    public ResponseEntity<BidDto> placeBid(@PathVariable UUID id,
            @Valid @RequestBody PlaceBidRequestDto request,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(bidService.placeBid(id, request.amount(), jwt.getSubject()));
    }

    @PostMapping("/create-with-item")
    @Operation(summary = "Create a bid listing along with its item",
               description = "Creates the item in item_service via Feign and creates the bid listing linked to the generated itemId. "
                       + "Supply durationSeconds (minimum 60, no configured maximum) or expirationDate at least 60 seconds ahead.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Bid listing and item created successfully",
                    content = @Content(schema = @Schema(implementation = ResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid bid or item information"),
            @ApiResponse(responseCode = "503", description = "Item service unavailable (Item creation aborted without retry)")
    })
    public ResponseEntity<ResponseDto> createBidWithItem(@Valid @RequestBody CreateBidRequestDto createBidRequestDto,
                                                       @AuthenticationPrincipal Jwt jwt) {
        var bid = bidService.createBidWithItem(createBidRequestDto, jwt.getSubject());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .header("Location", "/bidding-service/api/details/" + bid.getId())
                .body(new ResponseDto(BidConstants.STATUS_201, BidConstants.MESSAGE_201));
    }

    @PostMapping("/create")
    @Operation(summary = "Create a bid listing",
               description = "Creates a new bid listing with a randomly generated UUID. " +
                             "startingPrice is required; currentBid begins at that price and highestBidderId is assigned only by an accepted offer. " +
                             "Supply durationSeconds (minimum 60, no configured maximum) or expirationDate at least 60 seconds ahead.")
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
    public ResponseEntity<ResponseDto> createBid(@Valid @RequestBody BidDto bidDto, @AuthenticationPrincipal Jwt jwt) {
        var bid = bidService.createBid(bidDto, jwt.getSubject());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .header("Location", "/bidding-service/api/details/" + bid.getId())
                .body(new ResponseDto(BidConstants.STATUS_201, BidConstants.MESSAGE_201));
    }

    @GetMapping("/details/{id}")
    @Operation(summary = "Get bid listing details with item info", description = "Retrieves bid listing and item details via Feign (with Resilience4j Retry & Fallback).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Bid listing and item details retrieved successfully",
                    content = @Content(schema = @Schema(implementation = BidResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Bid listing not found")
    })
    public ResponseEntity<BidResponseDto> getBidDetails(
            @Parameter(description = "UUID of the bid listing", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable UUID id) {
        return ResponseEntity.ok(bidService.getBidDetails(id));
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
               description = "Only the owner can edit an unexpired auction with no offers. " +
                             "Updatable fields: itemId, startingPrice, expirationDate or durationSeconds. " +
                             "Ownership, highestBidderId and currentBid cannot be supplied by the caller. " +
                             "New durations must be at least 60 seconds, with no configured maximum. Owners can shorten or extend the deadline. " +
                             "Supply the exact existing expirationDate to preserve it, including in the final minute.")
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
            @Valid @RequestBody BidDto bidDto, @AuthenticationPrincipal Jwt jwt) {
        bidService.updateBid(id, bidDto, jwt.getSubject());
        return ResponseEntity.ok(new ResponseDto(BidConstants.STATUS_200, BidConstants.MESSAGE_200));
    }

    @DeleteMapping("/delete/{id}")
    @Operation(summary = "Delete a bid listing", description = "Only the owner can delete a listing, and only when it has no offers.")
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
            @PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        bidService.deleteBid(id, jwt.getSubject());
        return ResponseEntity.ok(new ResponseDto(BidConstants.STATUS_200, BidConstants.MESSAGE_200));
    }
}
