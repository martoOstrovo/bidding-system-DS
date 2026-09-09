package ds_bidding_system.mail_sender_service.client;

import ds_bidding_system.mail_sender_service.dto.UserDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "account-service", url = "${ACCOUNT_SERVICE_URL:}")
public interface AccountFeignClient {
    @GetMapping("/internal/users/{userId}")
    UserDto getUser(@PathVariable String userId);
}
