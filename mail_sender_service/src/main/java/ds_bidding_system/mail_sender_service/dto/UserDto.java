package ds_bidding_system.mail_sender_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

// Matches account_service's response, including non-UUID Keycloak subjects.
public record UserDto(@NotBlank String userId, String username, @NotBlank @Email String email) {}
