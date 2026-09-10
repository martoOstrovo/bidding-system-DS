package ds_bidding_system.bidding_service.service;

import ds_bidding_system.bidding_service.dto.CreateBidRequestDto;
import ds_bidding_system.bidding_service.entity.Bid;
import ds_bidding_system.bidding_service.repository.BidRepository;
import ds_bidding_system.bidding_service.repository.ItemCreationCleanupRepository;
import ds_bidding_system.bidding_service.service.client.ItemFeignClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ItemCreationTransaction {
    private final ItemCreationCleanupRepository cleanups;
    private final BidRepository bids;
    private final ItemFeignClient items;

    @Transactional
    public Bid create(UUID itemId, CreateBidRequestDto request, String ownerId) {
        if (ownerId == null || ownerId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required.");
        }
        var cleanup = cleanups.lockById(itemId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.CONFLICT, "Item creation expired; please retry."));
        items.createItem(request.getItem());
        Bid bid = new Bid();
        bid.setId(cleanup.getBidId());
        bid.setItemId(itemId);
        bid.setOwnerId(ownerId);
        // Start the chosen duration after the item service responds.
        bid.setExpirationDate(request.getDurationSeconds() == null ? request.getExpirationDate()
                : java.time.OffsetDateTime.now().plusSeconds(request.getDurationSeconds()));
        bid.setStartingPrice(request.getStartingPrice());
        bid.setCurrentBid(request.getStartingPrice());
        bids.saveAndFlush(bid);
        cleanups.delete(cleanup);
        return bid;
    }

    @Transactional
    public void compensate(UUID itemId) {
        var cleanup = cleanups.lockById(itemId).orElse(null);
        if (cleanup == null) return;
        if (!bids.existsById(cleanup.getBidId())) {
            try {
                items.deleteItem(itemId);
            } catch (ResponseStatusException error) {
                if (error.getStatusCode().value() != 404) throw error;
            }
        }
        cleanups.delete(cleanup);
    }
}
