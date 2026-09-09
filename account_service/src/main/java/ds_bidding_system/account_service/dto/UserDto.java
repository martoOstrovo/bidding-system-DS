package ds_bidding_system.account_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "User", description = "Basic account profile; passwords and tokens are never returned")
public record UserDto(
        @Schema(description = "User's Keycloak ID", example = "550e8400-e29b-41d4-a716-446655440000",
                accessMode = Schema.AccessMode.READ_ONLY) String userId,
        @Schema(description = "Keycloak username", example = "alice") String username,
        @Schema(description = "Email address, or null if the existing Keycloak account has none",
                example = "alice@example.com", nullable = true) String email) {}
