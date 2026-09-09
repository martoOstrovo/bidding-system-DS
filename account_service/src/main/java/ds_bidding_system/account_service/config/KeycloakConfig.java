package ds_bidding_system.account_service.config;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KeycloakConfig {
    @Bean(destroyMethod = "close")
    public Keycloak keycloakAdminClient(
            @Value("${app.keycloak.server-url}") String serverUrl,
            @Value("${app.keycloak.realm}") String realm,
            @Value("${app.keycloak.client-id}") String clientId,
            @Value("${app.keycloak.client-secret}") String clientSecret) {
        return KeycloakBuilder.builder().serverUrl(serverUrl).realm(realm)
                .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
                .clientId(clientId).clientSecret(clientSecret).build();
    }
}
