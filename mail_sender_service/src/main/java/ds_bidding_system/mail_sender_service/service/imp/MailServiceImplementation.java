package ds_bidding_system.mail_sender_service.service.imp;

import ds_bidding_system.mail_sender_service.dto.UserDto;
import ds_bidding_system.mail_sender_service.service.MailService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.UUID;

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
    public void sendBidWonEmail(UserDto user, UUID bidId) {
        sendEmail(user, "You won the bid!",
                "Congratulations! You won the bid listing " + bidId + ".");
    }

    @Override
    public void sendOutbidEmail(UserDto user, UUID bidId) {
        sendEmail(user, "You have been outbid",
                "Another user has placed a higher bid on listing " + bidId
                        + ".\nVisit the bidding system to check the listing and place a new bid if it is still open.");
    }

    private void sendEmail(UserDto user, String subject, String body) {
        String greeting = user.getFirstName() == null || user.getFirstName().isBlank()
                ? "Hello," : "Hello " + user.getFirstName().strip() + ",";

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(user.getEmail());
        message.setSubject(subject);
        message.setText(greeting + "\n\n" + body + "\n\nThe Bidding System team");

        mailSender.send(message);
    }
}
