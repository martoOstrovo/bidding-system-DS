package ds_bidding_system.item_service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;

@ResponseStatus(value = HttpStatus.NOT_FOUND)
public class ItemNotFoundException extends RuntimeException {
    public ItemNotFoundException(UUID itemId) {
        super(String.format("Item with id %s couldn't be found.", itemId));
    }

    public ItemNotFoundException(String itemName) {
        super(String.format("Item with item name %s couldn't be found.", itemName));
    }
}
