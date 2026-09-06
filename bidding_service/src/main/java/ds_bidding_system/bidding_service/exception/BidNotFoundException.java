package ds_bidding_system.bidding_service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;

@ResponseStatus(value = HttpStatus.NOT_FOUND)
public class BidNotFoundException extends RuntimeException {
    public BidNotFoundException(UUID bidId) {
        super(String.format("Bid listing with id %s couldn't be found.", bidId));
    }

    public BidNotFoundException(String message) {
        super(message);
    }
}
