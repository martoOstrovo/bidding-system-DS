package ds_bidding_system.item_service.config;

import ds_bidding_system.item_service.repository.ItemRepository;
import ds_bidding_system.item_service.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import java.time.Instant;
import java.util.HashSet;

@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class ImageCleanupConfig {
    private final ItemRepository items;
    private final FileStorageService files;

    @Scheduled(fixedDelayString = "${file.cleanup.interval-ms:3600000}", initialDelayString = "${file.cleanup.interval-ms:3600000}")
    public void removeOrphans() {
        // Grace period keeps files belonging to in-flight uploads out of the sweep.
        files.removeOrphans(new HashSet<>(items.findImageLocations()), Instant.now().minusSeconds(86400));
    }
}
