package ds_bidding_system.item_service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.UNPROCESSABLE_CONTENT)
public class ItemAlreadyExistsException extends RuntimeException {

    public ItemAlreadyExistsException(String itemName) {
        super(String.format("Item with name %s already exists.", itemName));
    }
}
