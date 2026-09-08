package ds_bidding_system.gateway_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import reactor.core.publisher.Mono;

@SpringBootApplication
public class GatewayServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayServiceApplication.class, args);
    }

    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> exchange.getPrincipal().map(java.security.Principal::getName);
    }

    @Bean
    public RedisRateLimiter redisRateLimiter() {
        return new RedisRateLimiter(1, 1, 1);
    }

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("item_swagger", p -> p
                        .order(-10)
                        .path(
                                "/item-service/swagger-ui.html",
                                "/item-service/swagger-ui/**",
                                "/item-service/v3/api-docs",
                                "/item-service/v3/api-docs/**",
                                "/item-service/v3/api-docs.yaml"
                        )
                        .filters(f -> f
                                .stripPrefix(1)
                                .preserveHostHeader()
                                .setRequestHeader("X-Forwarded-Prefix", "/item-service"))
                        .uri("lb://item-service"))

                .route("bidding_swagger", p -> p
                        .order(-10)
                        .path(
                                "/bidding-service/swagger-ui.html",
                                "/bidding-service/swagger-ui/**",
                                "/bidding-service/v3/api-docs",
                                "/bidding-service/v3/api-docs/**",
                                "/bidding-service/v3/api-docs.yaml"
                        )
                        .filters(f -> f
                                .stripPrefix(1)
                                .preserveHostHeader()
                                .setRequestHeader("X-Forwarded-Prefix", "/bidding-service"))
                        .uri("lb://bidding-service"))
                .route("bidding_service_route", p -> p
                        .path("/ds_bidding_system/bidding_service/**", "/bidding_service/**", "/bidding-service/**", "/BIDDING_SERVICE/**", "/BIDDING-SERVICE/**")
                        .filters(f -> f.rewritePath("/ds_bidding_system/bidding_service/(?<segment>.*)", "/${segment}")
                                       .rewritePath("/bidding_service/(?<segment>.*)", "/${segment}")
                                       .rewritePath("/bidding-service/(?<segment>.*)", "/${segment}")
                                       .rewritePath("/BIDDING_SERVICE/(?<segment>.*)", "/${segment}")
                                       .rewritePath("/BIDDING-SERVICE/(?<segment>.*)", "/${segment}")
                                       .tokenRelay()
                                       .removeRequestHeader("Cookie")
                                       .circuitBreaker(c -> c.setName("biddingCircuitBreaker")
                                               .setFallbackUri("forward:/fallback/bidding")
                                               .addStatusCode("500").addStatusCode("502").addStatusCode("503").addStatusCode("504"))
                                       .requestRateLimiter(r -> r.setRateLimiter(redisRateLimiter()).setKeyResolver(userKeyResolver())))
                        .uri("lb://bidding-service"))
                .route("item_service_route", p -> p
                        .path("/ds_bidding_system/item_service/**", "/item_service/**", "/item-service/**", "/ITEM_SERVICE/**", "/ITEM-SERVICE/**")
                        .filters(f -> f.rewritePath("/ds_bidding_system/item_service/(?<segment>.*)", "/${segment}")
                                       .rewritePath("/item_service/(?<segment>.*)", "/${segment}")
                                       .rewritePath("/item-service/(?<segment>.*)", "/${segment}")
                                       .rewritePath("/ITEM_SERVICE/(?<segment>.*)", "/${segment}")
                                       .rewritePath("/ITEM-SERVICE/(?<segment>.*)", "/${segment}")
                                       .tokenRelay()
                                       .removeRequestHeader("Cookie")
                                       .circuitBreaker(c -> c.setName("itemCircuitBreaker")
                                               .setFallbackUri("forward:/fallback/item")
                                               .addStatusCode("500").addStatusCode("502").addStatusCode("503").addStatusCode("504"))
                                       .requestRateLimiter(r -> r.setRateLimiter(redisRateLimiter()).setKeyResolver(userKeyResolver())))
                        .uri("lb://item-service"))
                .build();
    }
}
