package ds_bidding_system.item_service.service.imp;

import ds_bidding_system.item_service.exception.ItemAlreadyExistsException;
import ds_bidding_system.item_service.exception.ItemNotFoundException;
import ds_bidding_system.item_service.dto.ItemDto;
import ds_bidding_system.item_service.entity.Item;
import ds_bidding_system.item_service.entity.ItemID;
import ds_bidding_system.item_service.mapper.ItemMapper;
import ds_bidding_system.item_service.repository.ItemRepository;
import ds_bidding_system.item_service.service.ItemService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@AllArgsConstructor
public class ItemServiceImplementation implements ItemService {
    private final ItemRepository itemRepository;

    @Override
    public void createItem(ItemDto itemDto) {

        Optional<Item> itemOptional = itemRepository.findByItemName(itemDto.getItemName());

        if(itemOptional.isPresent()) {
            throw new ItemAlreadyExistsException(itemDto.getItemName());
        }

        Item item = ItemMapper.mapToItem(itemDto, new Item());
        ItemID itemID = new ItemID(UUID.randomUUID());
        item.setId(itemID.id());

        itemRepository.save(item);
    }

    @Override
    public ItemDto getItem(UUID itemId) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ItemNotFoundException(itemId));

        return ItemMapper.mapToItemDTO(item, new ItemDto());
    }

    @Override
    public Item updateItem(UUID itemId, ItemDto itemDto) {

        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ItemNotFoundException(itemId));

        Item updatedItem = ItemMapper.mapToItem(itemDto, item);

        return itemRepository.save(updatedItem);
    }

    @Override
    public void deleteItem(UUID itemId) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ItemNotFoundException(itemId));

        itemRepository.delete(item);
    }
}
