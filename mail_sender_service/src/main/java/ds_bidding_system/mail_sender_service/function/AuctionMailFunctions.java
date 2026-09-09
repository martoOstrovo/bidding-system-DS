package ds_bidding_system.mail_sender_service.function;

import ds_bidding_system.mail_sender_service.client.AccountFeignClient;
import ds_bidding_system.mail_sender_service.dto.AuctionNotification;
import ds_bidding_system.mail_sender_service.service.MailService;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.function.Consumer;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class AuctionMailFunctions {
    private final AccountFeignClient accounts;
    private final MailService mail;

    @Bean public Consumer<AuctionNotification> bidWon() {
        return event -> send(event, AuctionNotification.Type.WON);
    }

    @Bean public Consumer<AuctionNotification> auctionEnded() {
        return event -> send(event, AuctionNotification.Type.ENDED);
    }

    @Bean public Consumer<AuctionNotification> bidderOutbid() {
        return event -> send(event, AuctionNotification.Type.OUTBID);
    }

    private void send(AuctionNotification event, AuctionNotification.Type expected) {
        if (event == null || event.type() != expected || event.auctionId() == null
                || event.userId() == null || event.userId().isBlank() || event.amount() == null
                || event.amount().signum() < 0) {
            throw new IllegalArgumentException("Invalid auction notification");
        }
        ds_bidding_system.mail_sender_service.dto.UserDto user;
        try { user = accounts.getUser(event.userId()); }
        catch (FeignException.NotFound deleted) {
            log.info("Skipping {} notification for deleted account {}", expected, event.userId());
            return;
        }
        // Let lookup/SMTP errors reach the binder for retry and dead-letter handling.
        switch (expected) {
            case WON -> mail.sendBidWonEmail(user, event.auctionId(), event.amount());
            case ENDED -> mail.sendAuctionEndedEmail(user, event.auctionId(), event.amount(), event.sold());
            case OUTBID -> mail.sendOutbidEmail(user, event.auctionId(), event.amount());
        }
    }
}
