package ds_bidding_system.gateway_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.client.oidc.web.server.logout.OidcClientInitiatedServerLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.server.resource.authentication.DelegatingJwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.HttpStatusServerEntryPoint;
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationSuccessHandler;
import org.springframework.security.web.server.authentication.logout.DelegatingServerLogoutHandler;
import org.springframework.security.web.server.authentication.logout.SecurityContextServerLogoutHandler;
import org.springframework.security.web.server.authentication.logout.WebSessionServerLogoutHandler;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.security.web.server.savedrequest.NoOpServerRequestCache;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher.MatchResult;

@Configuration
public class SecurityConfig {
    @Bean
    @Order(1)
    public SecurityWebFilterChain bearerSecurity(ServerHttpSecurity http) {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new DelegatingJwtGrantedAuthoritiesConverter(
                new JwtGrantedAuthoritiesConverter(), new KeyCloakRoleConverter()));
        return common(http)
                .securityMatcher(exchange -> {
                    String header = exchange.getRequest().getHeaders().getFirst("Authorization");
                    return header != null && header.regionMatches(true, 0, "Bearer ", 0, 7)
                            ? MatchResult.match() : MatchResult.notMatch();
                })
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .logout(ServerHttpSecurity.LogoutSpec::disable)
                .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
                .oauth2ResourceServer(resource -> resource.jwt(jwt -> jwt.jwtAuthenticationConverter(
                        new ReactiveJwtAuthenticationConverterAdapter(converter))))
                .build();
    }

    @Bean
    @Order(2)
    public SecurityWebFilterChain browserSecurity(ServerHttpSecurity http,
            ReactiveClientRegistrationRepository registrations,
            ServerOAuth2AuthorizedClientRepository clients,
            ServerOAuth2AuthorizationRequestResolver resolver,
            @Value("${app.oauth2.public-url}") String gateway) {
        var logout = new OidcClientInitiatedServerLogoutSuccessHandler(registrations);
        logout.setPostLogoutRedirectUri(gateway + "/auth/logged-out");
        return common(http)
                .csrf(Customizer.withDefaults())
                .oauth2Login(login -> login.authorizationRequestResolver(resolver)
                        .authorizedClientRepository(clients)
                        .authenticationSuccessHandler(new RedirectServerAuthenticationSuccessHandler("/auth/me")))
                .oauth2Client(client -> client.authorizedClientRepository(clients))
                .logout(spec -> spec.logoutHandler(new DelegatingServerLogoutHandler(
                                new SecurityContextServerLogoutHandler(), new WebSessionServerLogoutHandler()))
                        .logoutSuccessHandler(logout))
                .build();
    }

    private ServerHttpSecurity common(ServerHttpSecurity http) {
        return http.httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .requestCache(cache -> cache.requestCache(NoOpServerRequestCache.getInstance()))
                .exceptionHandling(errors -> errors.authenticationEntryPoint(new HttpStatusServerEntryPoint(HttpStatus.UNAUTHORIZED)))
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers(HttpMethod.GET, "/oauth2/authorization/keycloak", "/login/oauth2/code/keycloak",
                                "/login", "/auth/csrf", "/auth/logged-out",
                                "/actuator/health", "/actuator/health/**", "/actuator/prometheus").permitAll()
                        .pathMatchers("/actuator", "/actuator/**", "/*/actuator", "/*/actuator/**",
                                "/ds_bidding_system/*/actuator", "/ds_bidding_system/*/actuator/**").denyAll()
                        .pathMatchers(HttpMethod.GET,
                                "/item-service/swagger-ui.html", "/item-service/swagger-ui/**",
                                "/item-service/v3/api-docs", "/item-service/v3/api-docs/**", "/item-service/v3/api-docs.yaml",
                                "/bidding-service/swagger-ui.html", "/bidding-service/swagger-ui/**",
                                "/bidding-service/v3/api-docs", "/bidding-service/v3/api-docs/**", "/bidding-service/v3/api-docs.yaml").permitAll()
                        .anyExchange().authenticated());
    }
}
