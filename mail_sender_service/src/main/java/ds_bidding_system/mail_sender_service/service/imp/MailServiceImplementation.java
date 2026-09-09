package ds_bidding_system.mail_sender_service.service.imp;

import ds_bidding_system.mail_sender_service.dto.UserDto;
import ds_bidding_system.mail_sender_service.service.MailService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.UUID;
import java.math.BigDecimal;

@Service
@Validated
public class MailServiceImplementation implements MailService {
    private final JavaMailSender mailSender;
    private final String fromAddress;

    public MailServiceImplementation(JavaMailSender mailSender, @Value("${mail.from}") String fromAddress) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    @Override
    public void sendBidWonEmail(UserDto user, UUID bidId, BigDecimal amount) {
        sendEmail(user, "You won the bid!",
                "Congratulations! You won the bid listing " + bidId + " with a final bid of " + amount.toPlainString() + ".");
    }

    @Override
    public void sendOutbidEmail(UserDto user, UUID bidId, BigDecimal amount) {
        sendEmail(user, "You have been outbid",
                "Another user has placed a higher bid on listing " + bidId
                        + ". The new bid is " + amount.toPlainString()
                        + ".\nVisit the bidding system to check the listing and place a new bid if it is still open.");
    }

    @Override
    public void sendAuctionEndedEmail(UserDto user, UUID bidId, BigDecimal amount, boolean sold) {
        sendEmail(user, "Your auction has ended", sold
                ? "Your auction " + bidId + " has ended. The winning bid was " + amount.toPlainString() + "."
                : "Your auction " + bidId + " has ended without any bids.");
    }

    private void sendEmail(UserDto user, String subject, String body) {
        String greeting = user.username() == null || user.username().isBlank()
                ? "Hello," : "Hello " + user.username().strip() + ",";

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(user.email());
        message.setSubject(subject);
        message.setText(greeting + "\n\n" + body + "\n\nThe Bidding System team");

        mailSender.send(message);
    }
}
