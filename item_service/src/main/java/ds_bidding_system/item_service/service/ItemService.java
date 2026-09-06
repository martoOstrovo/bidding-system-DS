package ds_bidding_system.item_service.service;

import ds_bidding_system.item_service.dto.ItemDto;
import ds_bidding_system.item_service.entity.Item;

import java.util.UUID;

public interface ItemService {
    void createItem(ItemDto itemDto);
    ItemDto getItem(UUID itemId);
    Item updateItem(UUID itemId, ItemDto itemDto);
    void deleteItem(UUID itemId);
}
