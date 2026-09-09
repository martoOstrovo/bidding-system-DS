package ds_bidding_system.bidding_service.service.client;

import ds_bidding_system.bidding_service.dto.ItemDto;
import ds_bidding_system.bidding_service.dto.ResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.DeleteMapping;

import java.util.UUID;

@FeignClient(name = "item-service", fallbackFactory = ItemFeignClientFallback.class)
public interface ItemFeignClient {

    @PostMapping(value = "/api/create", consumes = "application/json")
    ResponseEntity<ResponseDto> createItem(@RequestBody ItemDto itemDto);

    @GetMapping(value = "/api/get/{id}", produces = "application/json")
    ResponseEntity<ItemDto> getItem(@PathVariable("id") UUID id);

    @DeleteMapping("/api/delete/{id}")
    ResponseEntity<ResponseDto> deleteItem(@PathVariable("id") UUID id);
}
