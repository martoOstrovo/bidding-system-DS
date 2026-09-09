package ds_bidding_system.bidding_service.service;

import ds_bidding_system.bidding_service.entity.AuctionStatus;
import ds_bidding_system.bidding_service.event.AuctionNotification;
import ds_bidding_system.bidding_service.repository.BidRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuctionFinalizationService {
    private final BidRepository bids;
    private final ApplicationEventPublisher events;

    @Transactional
    public void finalizeAuction(UUID id) {
        var bid = bids.lockById(id).orElse(null);
        if (bid == null || bid.getStatus() == AuctionStatus.CLOSED
                || bid.getExpirationDate().isAfter(OffsetDateTime.now())) return;
        bid.setStatus(AuctionStatus.CLOSED);
        bids.saveAndFlush(bid);
        boolean sold = bid.getHighestBidderId() != null;
        if (bid.getOwnerId() != null) {
            events.publishEvent(new AuctionNotification(AuctionNotification.Type.ENDED,
                    id, bid.getOwnerId(), bid.getCurrentBid(), sold));
        }
        if (sold) {
            events.publishEvent(new AuctionNotification(AuctionNotification.Type.WON,
                    id, bid.getHighestBidderId(), bid.getCurrentBid(), true));
        }
    }
}
