package ds_bidding_system.item_service.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.Instant;
import java.util.Set;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class FileStorageService {
    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);
    private static final long MAX_BYTES = 5 * 1024 * 1024;
    private static final long MAX_PIXELS = 16_000_000;

    public static final String DEFAULT_IMAGE_LOCATION = "/uploads/images/default-item.png";

    private final Path fileStorageLocation;

    public FileStorageService(@Value("${file.upload-dir:uploads/images}") String uploadDir) {
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.fileStorageLocation);
            createDefaultPlaceholderImage();
        } catch (Exception ex) {
            throw new RuntimeException("Could not create directory for uploading files.", ex);
        }
    }

    public String storeFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return DEFAULT_IMAGE_LOCATION;
        }

        if (file.getSize() > MAX_BYTES) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Images must be at most 5 MB.");
        }
        Path temporary = null;
        try {
            BufferedImage image;
            String format;
            try (var input = file.getInputStream(); var imageInput = ImageIO.createImageInputStream(input)) {
                var readers = ImageIO.getImageReaders(imageInput);
                if (!readers.hasNext()) throw invalidImage();
                var reader = readers.next();
                try {
                    format = reader.getFormatName().toLowerCase(java.util.Locale.ROOT);
                    if (!Set.of("png", "jpeg").contains(format)) throw invalidImage();
                    reader.setInput(imageInput, true, true);
                    if ((long) reader.getWidth(0) * reader.getHeight(0) > MAX_PIXELS) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image dimensions exceed 16 megapixels.");
                    }
                    image = reader.read(0);
                    if (image == null) throw invalidImage();
                } finally {
                    reader.dispose();
                }
            } catch (IOException malformed) {
                throw invalidImage();
            }
            String filename = UUID.randomUUID() + (format.equals("jpeg") ? ".jpg" : ".png");
            temporary = Files.createTempFile(fileStorageLocation, "upload-", ".tmp");
            // Re-encode decoded pixels so filename, content type and embedded payloads are not trusted.
            if (!ImageIO.write(image, format, temporary.toFile())) throw new IOException("No image encoder available");
            Files.move(temporary, fileStorageLocation.resolve(filename));
            String location = "/uploads/images/" + filename;
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override public void afterCompletion(int status) {
                        if (status == STATUS_ROLLED_BACK) deleteFile(location);
                    }
                });
            }
            return location;
        } catch (IOException ex) {
            throw new IllegalStateException("Could not store image. Please try again.", ex);
        } finally {
            if (temporary != null) {
                try { Files.deleteIfExists(temporary); }
                catch (IOException ex) { log.warn("Could not remove temporary upload {}", temporary); }
            }
        }
    }

    public void deleteAfterCommit(String location) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { deleteFile(location); }
            });
        } else {
            deleteFile(location);
        }
    }

    private void deleteFile(String location) {
        if (location == null || !location.matches("/uploads/images/[0-9a-f-]{36}\\.(png|jpg|jpeg)")) return;
        try {
            Files.deleteIfExists(fileStorageLocation.resolve(location.substring("/uploads/images/".length())));
        } catch (IOException ex) {
            log.warn("Could not delete image {}; the orphan sweep will retry", location);
        }
    }

    public void removeOrphans(Set<String> referenced, Instant cutoff) {
        try (var paths = Files.list(fileStorageLocation)) {
            for (Path path : paths.toList()) {
                String name = path.getFileName().toString();
                if (!Files.isRegularFile(path, java.nio.file.LinkOption.NOFOLLOW_LINKS)
                        || !Files.getLastModifiedTime(path).toInstant().isBefore(cutoff)) continue;
                if (name.matches("upload-.*\\.tmp")) Files.deleteIfExists(path);
                else if (!referenced.contains("/uploads/images/" + name)) deleteFile("/uploads/images/" + name);
            }
        } catch (IOException ex) {
            log.warn("Could not complete orphan image cleanup", ex);
        }
    }

    private ResponseStatusException invalidImage() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, "Upload a valid PNG or JPEG image.");
    }

    private void createDefaultPlaceholderImage() throws IOException {
        Path path = fileStorageLocation.resolve("default-item.png");
        if (!Files.exists(path)) {
            ImageIO.write(new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB), "png", path.toFile());
        }
    }
}
