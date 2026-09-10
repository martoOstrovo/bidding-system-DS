package ds_bidding_system.bidding_service.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuctionEventPublisher {
    private final StreamBridge streamBridge;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publish(AuctionNotification event) {
        String binding = switch (event.type()) {
            case WON -> "bidWon-out-0";
            case ENDED -> "auctionEnded-out-0";
            case OUTBID -> "bidderOutbid-out-0";
        };
        try {
            if (!streamBridge.send(binding, event)) {
                throw new IllegalStateException("Kafka did not accept the notification");
            }
        } catch (RuntimeException failure) {
            log.error("Notification publication failed: type={}, auction={}, user={}",
                    event.type(), event.auctionId(), event.userId(), failure);
        }
    }
}
