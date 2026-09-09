package ds_bidding_system.item_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {"spring.cloud.config.enabled=false", "eureka.client.enabled=false",
        "file.upload-dir=target/context-test-images"})
class ItemServiceApplicationTests {

    @Test
    void contextLoads() {
    }

}
