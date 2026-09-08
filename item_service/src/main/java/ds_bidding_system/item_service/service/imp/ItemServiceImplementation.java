package ds_bidding_system.item_service.service.imp;

import ds_bidding_system.item_service.dto.ItemDto;
import ds_bidding_system.item_service.entity.Item;
import ds_bidding_system.item_service.entity.ItemID;
import ds_bidding_system.item_service.exception.ItemAlreadyExistsException;
import ds_bidding_system.item_service.exception.ItemNotFoundException;
import ds_bidding_system.item_service.mapper.ItemMapper;
import ds_bidding_system.item_service.repository.ItemRepository;
import ds_bidding_system.item_service.service.FileStorageService;
import ds_bidding_system.item_service.service.ItemService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;
import java.util.UUID;

@Service
@AllArgsConstructor
public class ItemServiceImplementation implements ItemService {

    private final ItemRepository itemRepository;
    private final FileStorageService fileStorageService;

    @Override
    public ItemDto createItem(ItemDto itemDto) {
        return createItemWithImage(itemDto, null);
    }

    @Override
    public ItemDto createItemWithImage(ItemDto itemDto, MultipartFile imageFile) {
        Optional<Item> itemOptional = itemRepository.findByItemName(itemDto.getItemName());

        if (itemOptional.isPresent()) {
            throw new ItemAlreadyExistsException(itemDto.getItemName());
        }

        Item item = ItemMapper.mapToItem(itemDto, new Item());
        if (itemDto.getId() != null) {
            item.setId(itemDto.getId());
        } else {
            ItemID itemID = new ItemID(UUID.randomUUID());
            item.setId(itemID.id());
        }

        // Set image location: if file provided, store it; if DTO has string location, keep it; else default image
        if (imageFile != null && !imageFile.isEmpty()) {
            item.setItemImageLocation(fileStorageService.storeFile(imageFile));
        } else if (item.getItemImageLocation() == null || item.getItemImageLocation().isBlank()) {
            item.setItemImageLocation(FileStorageService.DEFAULT_IMAGE_LOCATION);
        }

        Item savedItem = itemRepository.save(item);
        return ItemMapper.mapToItemDTO(savedItem, new ItemDto());
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
        if (itemDto.getItemImageLocation() == null || itemDto.getItemImageLocation().isBlank()) {
            updatedItem.setItemImageLocation(item.getItemImageLocation());
        }

        return itemRepository.save(updatedItem);
    }

    @Override
    public ItemDto updateItemImage(UUID itemId, MultipartFile imageFile) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ItemNotFoundException(itemId));

        String imageLocation = fileStorageService.storeFile(imageFile);
        item.setItemImageLocation(imageLocation);

        Item savedItem = itemRepository.save(item);
        return ItemMapper.mapToItemDTO(savedItem, new ItemDto());
    }

    @Override
    public void deleteItem(UUID itemId) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ItemNotFoundException(itemId));

        itemRepository.delete(item);
    }
}
