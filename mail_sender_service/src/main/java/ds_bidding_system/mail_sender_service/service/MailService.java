package ds_bidding_system.mail_sender_service.service;

import ds_bidding_system.mail_sender_service.dto.UserDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;
import java.math.BigDecimal;

public interface MailService {
    void sendBidWonEmail(@NotNull @Valid UserDto user, @NotNull UUID bidId, @NotNull BigDecimal amount);

    void sendOutbidEmail(@NotNull @Valid UserDto user, @NotNull UUID bidId, @NotNull BigDecimal amount);

    void sendAuctionEndedEmail(@NotNull @Valid UserDto user, @NotNull UUID bidId, @NotNull BigDecimal amount, boolean sold);
}
