package ds_bidding_system.gateway_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class GatewayServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayServiceApplication.class, args);
    }

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("bidding_service_route", p -> p
                        .path("/ds_bidding_system/bidding_service/**", "/bidding_service/**", "/bidding-service/**", "/BIDDING_SERVICE/**", "/BIDDING-SERVICE/**")
                        .filters(f -> f.rewritePath("/ds_bidding_system/bidding_service/(?<segment>.*)", "/${segment}")
                                       .rewritePath("/bidding_service/(?<segment>.*)", "/${segment}")
                                       .rewritePath("/bidding-service/(?<segment>.*)", "/${segment}")
                                       .rewritePath("/BIDDING_SERVICE/(?<segment>.*)", "/${segment}")
                                       .rewritePath("/BIDDING-SERVICE/(?<segment>.*)", "/${segment}"))
                        .uri("http://localhost:8081"))
                .route("item_service_route", p -> p
                        .path("/ds_bidding_system/item_service/**", "/item_service/**", "/item-service/**", "/ITEM_SERVICE/**", "/ITEM-SERVICE/**")
                        .filters(f -> f.rewritePath("/ds_bidding_system/item_service/(?<segment>.*)", "/${segment}")
                                       .rewritePath("/item_service/(?<segment>.*)", "/${segment}")
                                       .rewritePath("/item-service/(?<segment>.*)", "/${segment}")
                                       .rewritePath("/ITEM_SERVICE/(?<segment>.*)", "/${segment}")
                                       .rewritePath("/ITEM-SERVICE/(?<segment>.*)", "/${segment}"))
                        .uri("http://localhost:8080"))
                .route("mail_sender_service_route", p -> p
                        .path("/ds_bidding_system/mail_sender_service/**", "/mail_sender_service/**", "/mail-sender-service/**", "/mail_sender/**", "/mail-sender/**", "/MAIL_SENDER/**", "/MAIL_SENDER_SERVICE/**", "/MAIL-SENDER/**")
                        .filters(f -> f.rewritePath("/ds_bidding_system/mail_sender_service/(?<segment>.*)", "/${segment}")
                                       .rewritePath("/mail_sender_service/(?<segment>.*)", "/${segment}")
                                       .rewritePath("/mail-sender-service/(?<segment>.*)", "/${segment}")
                                       .rewritePath("/mail_sender/(?<segment>.*)", "/${segment}")
                                       .rewritePath("/mail-sender/(?<segment>.*)", "/${segment}")
                                       .rewritePath("/MAIL_SENDER/(?<segment>.*)", "/${segment}")
                                       .rewritePath("/MAIL_SENDER_SERVICE/(?<segment>.*)", "/${segment}")
                                       .rewritePath("/MAIL-SENDER/(?<segment>.*)", "/${segment}"))
                        .uri("http://localhost:8082"))
                .build();
    }
}
