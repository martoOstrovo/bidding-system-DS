package ds_bidding_system.bidding_service.service;

import ds_bidding_system.bidding_service.repository.ItemCreationCleanupRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import java.time.Instant;

@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class ItemCreationRecovery {
    private static final Logger log = LoggerFactory.getLogger(ItemCreationRecovery.class);
    private final ItemCreationCleanupRepository cleanups;
    private final ItemCreationTransaction transactions;

    @Scheduled(fixedDelayString = "${item.cleanup.interval-ms:60000}", initialDelayString = "${item.cleanup.interval-ms:60000}")
    public void recover() {
        for (var cleanup : cleanups.findTop100ByCreatedAtBeforeOrderByCreatedAtAsc(Instant.now().minusSeconds(300))) {
            try {
                transactions.compensate(cleanup.getItemId());
            } catch (RuntimeException error) {
                log.warn("Will retry cleanup for item {}: {}", cleanup.getItemId(), error.getClass().getSimpleName());
            }
        }
    }
}
