package ds_bidding_system.gateway_service.controller;

import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.server.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
public class SessionController {
    @GetMapping({"/auth/login", "/account_service/api/login", "/account-service/api/login"})
    public org.springframework.http.ResponseEntity<Void> login() {
        return org.springframework.http.ResponseEntity.status(302)
                .location(java.net.URI.create("/oauth2/authorization/keycloak")).build();
    }

    @GetMapping("/auth/me")
    public Map<String, Object> me(Authentication authentication) {
        String username = authentication.getName();
        if (authentication.getPrincipal() instanceof OidcUser user && user.getPreferredUsername() != null) {
            username = user.getPreferredUsername();
        }
        // Never serialize the Authentication or OidcUser: they contain tokens.
        return Map.of("id", authentication.getName(), "username", username,
                "authorities", authentication.getAuthorities().stream().map(a -> a.getAuthority()).toList());
    }

    @GetMapping("/auth/csrf")
    public Mono<Map<String, String>> csrf(ServerWebExchange exchange) {
        Mono<CsrfToken> token = exchange.getAttribute(CsrfToken.class.getName());
        return token == null ? Mono.empty()
                : token.map(value -> Map.of("headerName", value.getHeaderName(), "token", value.getToken()));
    }

    @GetMapping("/auth/logged-out")
    public Map<String, String> loggedOut() {
        return Map.of("status", "logged_out", "login", "/oauth2/authorization/keycloak");
    }
}
