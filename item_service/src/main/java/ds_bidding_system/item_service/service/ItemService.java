package ds_bidding_system.item_service.service;

import ds_bidding_system.item_service.dto.ItemDto;
import ds_bidding_system.item_service.entity.Item;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface ItemService {
    ItemDto createItem(ItemDto itemDto);
    ItemDto createItemWithImage(ItemDto itemDto, MultipartFile imageFile);
    ItemDto getItem(UUID itemId);
    Item updateItem(UUID itemId, ItemDto itemDto);
    ItemDto updateItemImage(UUID itemId, MultipartFile imageFile);
    void deleteItem(UUID itemId);
}
