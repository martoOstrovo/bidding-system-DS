package ds_bidding_system.gateway_service.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class FallbackController {
    @RequestMapping("/fallback/bidding")
    public ResponseEntity<Map<String, String>> biddingUnavailable() {
        return unavailable("Bidding service is temporarily unavailable. Please try again later.");
    }

    @RequestMapping("/fallback/item")
    public ResponseEntity<Map<String, String>> itemUnavailable() {
        return unavailable("Item service is temporarily unavailable. Please try again later.");
    }

    private ResponseEntity<Map<String, String>> unavailable(String message) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("statusCode", "503", "statusMsg", message));
    }
}
