package ds_bidding_system.account_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
    @Bean
    public SecurityFilterChain security(HttpSecurity http,
            @org.springframework.beans.factory.annotation.Value("${MAIL_KEYCLOAK_CLIENT_ID:mail-service}") String mailClientId) throws Exception {
        return http.csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/internal/**").access((authentication, context) -> {
                            var principal = authentication.get().getPrincipal();
                            return new org.springframework.security.authorization.AuthorizationDecision(
                                    principal instanceof org.springframework.security.oauth2.jwt.Jwt jwt
                                            && mailClientId.equals(jwt.getClaimAsString("azp")));
                        })
                        .requestMatchers(HttpMethod.POST, "/api/register").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/login", "/actuator/health", "/actuator/health/**", "/actuator/prometheus").permitAll()
                        .requestMatchers(HttpMethod.GET, "/swagger-ui.html", "/swagger-ui/**",
                                "/v3/api-docs", "/v3/api-docs/**", "/v3/api-docs.yaml").permitAll()
                        .requestMatchers("/actuator/**").denyAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth -> oauth.jwt(Customizer.withDefaults()))
                .build();
    }
}
