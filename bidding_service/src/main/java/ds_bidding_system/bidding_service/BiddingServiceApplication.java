package ds_bidding_system.bidding_service;

import io.swagger.v3.oas.annotations.ExternalDocumentation;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@OpenAPIDefinition(
        info = @Info(
                title = "Bidding microservice REST API documentation",
                description = "API for creating, retrieving, updating, and deleting bid listings in the bidding system",
                version = "v1",
                contact = @Contact(name = "MARTO ANGEL PAVEL")
        ),
        externalDocs = @ExternalDocumentation(
                description = "Interactive API documentation",
                url = "/swagger-ui.html"
        )
)
public class BiddingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BiddingServiceApplication.class, args);
    }

}
