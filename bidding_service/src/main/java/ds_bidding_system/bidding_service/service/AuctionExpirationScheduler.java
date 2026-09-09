package ds_bidding_system.bidding_service.service;

import ds_bidding_system.bidding_service.entity.AuctionStatus;
import ds_bidding_system.bidding_service.repository.BidRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.OffsetDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "auction.expiration.enabled", havingValue = "true", matchIfMissing = true)
public class AuctionExpirationScheduler {
    private final BidRepository bids;
    private final AuctionFinalizationService finalization;

    @Scheduled(fixedDelayString = "${auction.expiration.interval-ms:30000}",
            initialDelayString = "${auction.expiration.interval-ms:30000}")
    public void closeExpiredAuctions() {
        for (var bid : bids.findTop100ByStatusAndExpirationDateLessThanEqualOrderByExpirationDateAscIdAsc(
                AuctionStatus.OPEN, OffsetDateTime.now())) {
            try { finalization.finalizeAuction(bid.getId()); }
            catch (RuntimeException failure) { log.error("Could not finalize auction {}", bid.getId(), failure); }
        }
    }
}
