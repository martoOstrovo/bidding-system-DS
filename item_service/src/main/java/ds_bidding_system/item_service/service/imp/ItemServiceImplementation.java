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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

@Service
@AllArgsConstructor
public class ItemServiceImplementation implements ItemService {

    private final ItemRepository itemRepository;
    private final FileStorageService fileStorageService;

    @Override
    @Transactional
    public ItemDto createItem(ItemDto itemDto) {
        return createItemWithImage(itemDto, null);
    }

    @Override
    @Transactional
    public ItemDto createItemWithImage(ItemDto itemDto, MultipartFile imageFile) {
        Optional<Item> itemOptional = itemRepository.findByItemName(itemDto.getItemName());

        if (itemOptional.isPresent()) {
            throw new ItemAlreadyExistsException(itemDto.getItemName());
        }

        Item item = ItemMapper.mapToItem(itemDto, new Item());
        if (itemDto.getId() != null) {
            if (itemRepository.existsById(itemDto.getId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "An item with this ID already exists.");
            }
            item.setId(itemDto.getId());
        } else {
            ItemID itemID = new ItemID(UUID.randomUUID());
            item.setId(itemID.id());
        }

        // Image locations are server-managed; clients upload pixels through the image endpoint.
        item.setItemImageLocation(fileStorageService.storeFile(imageFile));

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
    @Transactional
    public Item updateItem(UUID itemId, ItemDto itemDto) {
        Item item = itemRepository.findByIdForUpdate(itemId)
                .orElseThrow(() -> new ItemNotFoundException(itemId));

        item.setItemName(itemDto.getItemName());
        item.setItemDescription(itemDto.getItemDescription());
        return itemRepository.save(item);
    }

    @Override
    @Transactional
    public ItemDto updateItemImage(UUID itemId, MultipartFile imageFile) {
        Item item = itemRepository.findByIdForUpdate(itemId)
                .orElseThrow(() -> new ItemNotFoundException(itemId));

        if (imageFile == null || imageFile.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "An image file is required.");
        }
        String previousImage = item.getItemImageLocation();
        String imageLocation = fileStorageService.storeFile(imageFile);
        item.setItemImageLocation(imageLocation);
        fileStorageService.deleteAfterCommit(previousImage);

        Item savedItem = itemRepository.save(item);
        return ItemMapper.mapToItemDTO(savedItem, new ItemDto());
    }

    @Override
    @Transactional
    public void deleteItem(UUID itemId) {
        Item item = itemRepository.findByIdForUpdate(itemId)
                .orElseThrow(() -> new ItemNotFoundException(itemId));

        itemRepository.delete(item);
        fileStorageService.deleteAfterCommit(item.getItemImageLocation());
    }
}
