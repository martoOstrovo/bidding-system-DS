package ds_bidding_system.gateway_service.config;

import java.util.LinkedHashSet;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestCustomizers;
import org.springframework.security.oauth2.client.web.DefaultReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.server.DefaultServerOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.server.WebSessionServerOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;

@Configuration
public class OAuth2ClientConfig {
    @Bean
    public ReactiveClientRegistrationRepository clientRegistrations(
            @Value("${app.oauth2.client-secret}") String secret,
            @Value("${app.oauth2.public-url}") String gateway,
            @Value("${app.oauth2.issuer}") String issuer,
            @Value("${app.oauth2.backchannel-url}") String backchannel) {
        // Explicit endpoints avoid browser-facing localhost discovery from Docker.
        ClientRegistration registration = ClientRegistration.withRegistrationId("keycloak")
                .clientId("bidding-gateway").clientSecret(secret)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri(gateway + "/login/oauth2/code/keycloak")
                .scope("openid", "profile", "email", "bidding-api")
                .issuerUri(issuer)
                .authorizationUri(issuer + "/protocol/openid-connect/auth")
                .tokenUri(backchannel + "/protocol/openid-connect/token")
                .jwkSetUri(backchannel + "/protocol/openid-connect/certs")
                .userInfoUri(backchannel + "/protocol/openid-connect/userinfo")
                .userNameAttributeName("sub")
                .providerConfigurationMetadata(Map.of("end_session_endpoint", issuer + "/protocol/openid-connect/logout"))
                .clientName("Keycloak").build();
        return new InMemoryReactiveClientRegistrationRepository(registration);
    }

    @Bean
    public ServerOAuth2AuthorizationRequestResolver authorizationRequestResolver(ReactiveClientRegistrationRepository registrations) {
        var resolver = new DefaultServerOAuth2AuthorizationRequestResolver(registrations);
        resolver.setAuthorizationRequestCustomizer(OAuth2AuthorizationRequestCustomizers.withPkce());
        return resolver;
    }

    @Bean
    public ServerOAuth2AuthorizedClientRepository authorizedClientRepository() {
        return new WebSessionServerOAuth2AuthorizedClientRepository();
    }

    @Bean
    public ReactiveOAuth2AuthorizedClientManager authorizedClientManager(
            ReactiveClientRegistrationRepository registrations, ServerOAuth2AuthorizedClientRepository clients) {
        var manager = new DefaultReactiveOAuth2AuthorizedClientManager(registrations, clients);
        manager.setAuthorizedClientProvider(ReactiveOAuth2AuthorizedClientProviderBuilder.builder()
                .authorizationCode().refreshToken().build());
        return manager;
    }

    @Bean
    public GrantedAuthoritiesMapper oidcAuthoritiesMapper() {
        return authorities -> {
            var mapped = new LinkedHashSet<GrantedAuthority>(authorities);
            for (GrantedAuthority authority : authorities) {
                if (authority instanceof OidcUserAuthority oidc) {
                    mapped.addAll(KeyCloakRoleConverter.extractRoles(oidc.getIdToken().getClaims()));
                }
            }
            return mapped;
        };
    }
}
