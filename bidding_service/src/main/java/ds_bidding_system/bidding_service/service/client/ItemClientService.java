package ds_bidding_system.bidding_service.service.client;

import ds_bidding_system.bidding_service.dto.ItemDto;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.time.Duration;
import java.util.UUID;

@Service
public class ItemClientService {
    private final ItemFeignClient client;
    private final Retry readRetry = Retry.of("getItemRetry", RetryConfig.custom()
            .maxAttempts(3).waitDuration(Duration.ofMillis(500))
            .retryOnException(error -> error instanceof ResponseStatusException response
                    && response.getStatusCode().is5xxServerError()).build());

    public ItemClientService(ItemFeignClient client) { this.client = client; }

    public ItemDto getItem(UUID id) {
        return readRetry.executeSupplier(() -> client.getItem(id).getBody());
    }
}
