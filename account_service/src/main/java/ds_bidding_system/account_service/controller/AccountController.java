package ds_bidding_system.account_service.controller;

import ds_bidding_system.account_service.dto.RegisterRequestDto;
import ds_bidding_system.account_service.dto.UserDto;
import ds_bidding_system.account_service.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;

@RestController
@RequestMapping("/api")
@Tag(name = "Accounts", description = "Register, read, delete and log out Keycloak accounts")
@ApiResponse(responseCode = "503", description = "Keycloak or account storage unavailable",
        content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ProblemDetail.class)))
public class AccountController {
    private final AccountService accounts;
    private final String gatewayUrl;

    public AccountController(AccountService accounts, @Value("${app.gateway-public-url}") String gatewayUrl) {
        this.accounts = accounts;
        this.gatewayUrl = gatewayUrl;
    }

    @PostMapping("/register")
    @Operation(summary = "Register an account", description = "Creates a Keycloak user and stores their basic profile. "
            + "Does not log the user in. No authentication is required; browser requests through the gateway require CSRF.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Account created",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = UserDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input or Keycloak user/password policy violation",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Username or email is already registered",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<UserDto> register(@Valid @RequestBody RegisterRequestDto request) {
        return ResponseEntity.created(URI.create("/account_service/api/me")).body(accounts.register(request));
    }

    @GetMapping("/login")
    @Operation(summary = "Start browser login", description = "Navigate to this URL in the browser to begin the gateway's "
            + "Keycloak Authorization Code login flow. Does not accept a password or return tokens; Swagger's Try it out "
            + "cannot complete the cross-origin browser login flow.")
    @ApiResponse(responseCode = "302", description = "Redirect to gateway login", content = @Content)
    public ResponseEntity<Void> login() {
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(gatewayUrl + "/oauth2/authorization/keycloak")).build();
    }

    @GetMapping("/me")
    @Operation(summary = "Get my account", description = "Reads current Keycloak information, synchronizes local storage "
            + "and returns the authenticated user's basic profile.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Current profile",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = UserDto.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid authentication", content = @Content),
            @ApiResponse(responseCode = "403", description = "Account is disabled", content = @Content),
            @ApiResponse(responseCode = "404", description = "Keycloak account no longer exists", content = @Content)
    })
    public UserDto me(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        return accounts.getUser(jwt.getSubject());
    }

    @GetMapping("/get/{userId}")
    @Operation(summary = "Get an account by ID", description = "Returns the current profile only when userId matches "
            + "the authenticated user's Keycloak subject. Other users' profiles are private.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Account profile",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = UserDto.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid authentication", content = @Content),
            @ApiResponse(responseCode = "403", description = "Another user's ID or a disabled account", content = @Content),
            @ApiResponse(responseCode = "404", description = "Keycloak account no longer exists", content = @Content)
    })
    public UserDto get(@Parameter(description = "Your Keycloak user ID", example = "550e8400-e29b-41d4-a716-446655440000")
                       @PathVariable String userId, @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        if (!jwt.getSubject().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only read your own account.");
        }
        return accounts.getUser(userId);
    }

    @DeleteMapping("/me")
    @Operation(summary = "Delete my account", description = "Deletes the authenticated user in Keycloak and local storage. "
            + "Historical bid owner IDs remain. After deletion, browser clients should also POST /auth/logout at the gateway.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Account deleted", content = @Content),
            @ApiResponse(responseCode = "401", description = "Missing or invalid authentication", content = @Content)
    })
    public ResponseEntity<Void> delete(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        accounts.deleteUser(jwt.getSubject());
        return ResponseEntity.noContent().build();
    }

    // For bearer clients: revoke all Keycloak sessions. Browsers use POST /auth/logout at the gateway.
    @PostMapping("/logout")
    @Operation(summary = "Log out account sessions", description = "For bearer clients, revokes all of the user's Keycloak "
            + "sessions and returns 204. Already issued access tokens can remain valid until expiry. At the gateway, "
            + "browser session requests instead perform browser logout and redirect to Keycloak; use a CSRF-protected POST form.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Keycloak sessions revoked", content = @Content),
            @ApiResponse(responseCode = "302", description = "Gateway browser logout redirect", content = @Content),
            @ApiResponse(responseCode = "401", description = "Missing or invalid authentication", content = @Content),
            @ApiResponse(responseCode = "404", description = "Keycloak account no longer exists", content = @Content)
    })
    public ResponseEntity<Void> logout(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        accounts.logout(jwt.getSubject());
        return ResponseEntity.noContent().build();
    }
}
