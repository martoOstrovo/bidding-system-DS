package ds_bidding_system.account_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(name = "RegisterRequest", description = "Information required to create an account in Keycloak and local storage")
public record RegisterRequestDto(
        @Schema(description = "Username; letters, numbers, dots, underscores and hyphens, normalized to lowercase",
                example = "alice", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Size(min = 3, max = 100) @Pattern(regexp = "[a-zA-Z0-9._-]+") String username,
        @Schema(description = "Email address, normalized to lowercase and initially unverified",
                example = "alice@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Email @Size(max = 254) String email,
        @Schema(description = "Password stored only in Keycloak; must also meet the realm's password policy",
                format = "password", accessMode = Schema.AccessMode.WRITE_ONLY, requiredMode = Schema.RequiredMode.REQUIRED)
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) @NotBlank @Size(min = 8, max = 128) String password) {
    @Override public String toString() { return "RegisterRequestDto[credentials redacted]"; }
}
