package ds_bidding_system.bidding_service.service.client;

import ds_bidding_system.bidding_service.dto.ItemDto;
import ds_bidding_system.bidding_service.dto.ResponseDto;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ItemFeignClientFallback implements ItemFeignClient {

    @Override
    public ResponseEntity<ResponseDto> createItem(ItemDto itemDto) {
        return null;
    }

    @Override
    public ResponseEntity<ItemDto> getItem(UUID id) {
        return null;
    }
}
