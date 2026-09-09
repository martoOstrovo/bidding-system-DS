package ds_bidding_system.account_service.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(info = @Info(title = "Account service", version = "v1",
        description = "Basic Keycloak accounts and local user profiles. Login uses the gateway's browser flow. "
                + "Protected operations accept a bearer access token with the bidding-api audience. "
                + "Browser writes through the gateway also require a session-bound CSRF token from /auth/csrf."))
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class OpenApiConfig {}
