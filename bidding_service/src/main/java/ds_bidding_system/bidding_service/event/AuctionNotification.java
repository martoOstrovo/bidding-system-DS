package ds_bidding_system.bidding_service.event;

import java.math.BigDecimal;
import java.util.UUID;

// JSON contract shared with mail_sender_service; no email addresses travel through Kafka.
public record AuctionNotification(Type type, UUID auctionId, String userId, BigDecimal amount, boolean sold) {
    public enum Type { WON, ENDED, OUTBID }
}
