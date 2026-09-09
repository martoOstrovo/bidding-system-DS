package ds_bidding_system.account_service.service;

import ds_bidding_system.account_service.dto.RegisterRequestDto;
import ds_bidding_system.account_service.dto.UserDto;
import ds_bidding_system.account_service.entity.UserAccount;
import ds_bidding_system.account_service.repository.UserAccountRepository;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;

@Service
public class AccountService {
    private final UsersResource users;
    private final UserAccountRepository accounts;

    public AccountService(Keycloak keycloakAdminClient, UserAccountRepository accounts,
                          @Value("${app.keycloak.realm}") String realm) {
        this.users = keycloakAdminClient.realm(realm).users();
        this.accounts = accounts;
    }

    public UserDto register(RegisterRequestDto request) {
        var user = new UserRepresentation();
        user.setUsername(request.username().toLowerCase(Locale.ROOT));
        user.setEmail(request.email().trim().toLowerCase(Locale.ROOT));
        user.setEnabled(true);
        user.setEmailVerified(false);
        var password = new CredentialRepresentation();
        password.setType(CredentialRepresentation.PASSWORD);
        password.setValue(request.password());
        password.setTemporary(false);
        user.setCredentials(List.of(password));
        String id;
        try (Response response = users.create(user)) {
            if (response.getStatus() == 409) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Username or email is already registered.");
            }
            if (response.getStatus() == 400) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Registration does not meet the realm's user or password requirements.");
            }
            if (response.getStatus() != 201) {
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Account provider is unavailable.");
            }
            id = CreatedResponseUtil.getCreatedId(response);
        }
        // Repository calls commit before returning. Compensate a failed local commit in Keycloak.
        try {
            return save(users.get(id).toRepresentation());
        } catch (RuntimeException failure) {
            try { users.get(id).remove(); }
            catch (RuntimeException cleanupFailure) { failure.addSuppressed(cleanupFailure); }
            throw failure;
        }
    }

    public UserDto getUser(String id) {
        // Fetch current Keycloak data, including accounts created before this service existed.
        // Never recreate a deleted account from an old, still-valid access token's claims.
        UserRepresentation user = users.get(id).toRepresentation();
        if (!Boolean.TRUE.equals(user.isEnabled())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is disabled.");
        }
        return save(user);
    }

    public void deleteUser(String id) {
        // A retry can finish local deletion if Keycloak deletion previously succeeded.
        try { users.get(id).remove(); }
        catch (NotFoundException ignored) { }
        accounts.deleteById(id);
    }

    public void logout(String id) {
        users.get(id).logout();
    }

    private UserDto save(UserRepresentation user) {
        var account = accounts.saveAndFlush(new UserAccount(user.getId(), user.getUsername(), user.getEmail()));
        return new UserDto(account.getUserId(), account.getUsername(), account.getEmail());
    }
}
