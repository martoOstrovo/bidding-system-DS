package ds_bidding_system.mail_sender_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@org.springframework.cloud.openfeign.EnableFeignClients
public class MailSenderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MailSenderServiceApplication.class, args);
    }

}
