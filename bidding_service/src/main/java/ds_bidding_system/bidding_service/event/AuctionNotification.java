package ds_bidding_system.bidding_service.event;

import java.math.BigDecimal;
import java.util.UUID;

public record AuctionNotification(Type type, UUID auctionId, String userId, BigDecimal amount, boolean sold) {
    public enum Type { WON, ENDED, OUTBID }
}
