package ds_bidding_system.item_service.mapper;

import ds_bidding_system.item_service.dto.ItemDto;
import ds_bidding_system.item_service.entity.Item;

public class ItemMapper {
    public static ItemDto mapToItemDTO(Item item, ItemDto itemDto) {
        itemDto.setId(item.getId());
        itemDto.setItemName(item.getItemName());
        itemDto.setItemDescription(item.getItemDescription());
        itemDto.setItemImageLocation(item.getItemImageLocation());
        return itemDto;
    }

    public static Item mapToItem(ItemDto itemDto, Item item) {
        if (itemDto.getId() != null) {
            item.setId(itemDto.getId());
        }
        item.setItemName(itemDto.getItemName());
        item.setItemDescription(itemDto.getItemDescription());
        item.setItemImageLocation(itemDto.getItemImageLocation());
        return item;
    }
}
