package ds_bidding_system.bidding_service.service.client;

import ds_bidding_system.bidding_service.dto.ItemDto;
import ds_bidding_system.bidding_service.dto.ResponseDto;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import feign.FeignException;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Component
public class ItemFeignClientFallback implements FallbackFactory<ItemFeignClient> {
    @Override
    public ItemFeignClient create(Throwable cause) {
        return new ItemFeignClient() {
            public ResponseEntity<ResponseDto> createItem(ItemDto item) { throw translate(cause); }
            public ResponseEntity<ItemDto> getItem(UUID id) { throw translate(cause); }
            public ResponseEntity<ResponseDto> deleteItem(UUID id) { throw translate(cause); }
        };
    }

    private ResponseStatusException translate(Throwable cause) {
        for (Throwable current = cause; current != null; current = current.getCause()) {
            if (current instanceof FeignException feign && feign.status() >= 400 && feign.status() < 500) {
                return new ResponseStatusException(HttpStatusCode.valueOf(feign.status()),
                        "Item service rejected the request (HTTP " + feign.status() + ").", cause);
            }
        }
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Item service is currently unavailable.", cause);
    }
}
