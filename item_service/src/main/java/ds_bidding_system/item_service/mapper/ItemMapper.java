package ds_bidding_system.item_service.mapper;

import ds_bidding_system.item_service.dto.ItemDto;
import ds_bidding_system.item_service.entity.Item;

public class ItemMapper {
    public static ItemDto mapToItemDTO(Item item, ItemDto itemDto) {
        itemDto.setItemName(item.getItemName());
        itemDto.setItemDescription(item.getItemDescription());
        return itemDto;
    }

    public static Item mapToItem(ItemDto itemDto, Item item) {
        item.setItemName(itemDto.getItemName());
        item.setItemDescription(itemDto.getItemDescription());
        return item;
    }
}
