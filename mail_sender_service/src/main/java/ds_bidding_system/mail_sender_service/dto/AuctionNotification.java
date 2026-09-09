package ds_bidding_system.mail_sender_service.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record AuctionNotification(Type type, UUID auctionId, String userId, BigDecimal amount, boolean sold) {
    public enum Type { WON, ENDED, OUTBID }
}
