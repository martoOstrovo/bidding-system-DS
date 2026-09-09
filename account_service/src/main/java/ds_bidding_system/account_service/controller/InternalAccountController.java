package ds_bidding_system.account_service.controller;

import ds_bidding_system.account_service.dto.UserDto;
import ds_bidding_system.account_service.service.AccountService;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@Hidden
@RestController
public class InternalAccountController {
    private final AccountService accounts;
    public InternalAccountController(AccountService accounts) { this.accounts = accounts; }

    @GetMapping("/internal/users/{userId}")
    public UserDto getUser(@PathVariable String userId) { return accounts.getUser(userId); }
}
