package ds_bidding_system.bidding_service.service.imp;

import ds_bidding_system.bidding_service.client.ItemFeignClient;
import ds_bidding_system.bidding_service.dto.BidDto;
import ds_bidding_system.bidding_service.dto.BidResponseDto;
import ds_bidding_system.bidding_service.dto.CreateBidRequestDto;
import ds_bidding_system.bidding_service.dto.ItemDto;
import ds_bidding_system.bidding_service.dto.ResponseDto;
import ds_bidding_system.bidding_service.entity.Bid;
import ds_bidding_system.bidding_service.entity.BidID;
import ds_bidding_system.bidding_service.exception.BidNotFoundException;
import ds_bidding_system.bidding_service.mapper.BidMapper;
import ds_bidding_system.bidding_service.repository.BidRepository;
import ds_bidding_system.bidding_service.service.BidService;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@AllArgsConstructor
public class BidServiceImplementation implements BidService {

    private final BidRepository bidRepository;
    private final ItemFeignClient itemFeignClient;

    @Override
    public Bid createBid(BidDto bidDto) {
        Bid bid = BidMapper.mapToBid(bidDto, new Bid());
        BidID bidID = new BidID(UUID.randomUUID());
        bid.setId(bidID.id());
        return bidRepository.save(bid);
    }

    @Override
    public Bid createBidWithItem(CreateBidRequestDto request) {
        ItemDto itemDto = request.getItem();
        if (itemDto.getId() == null) {
            itemDto.setId(UUID.randomUUID());
        }

        // Send POST request via Feign to item_service (Retry pattern is disabled for POST)
        ResponseEntity<ResponseDto> response = itemFeignClient.createItem(itemDto);

        if (response == null || !response.getStatusCode().is2xxSuccessful()) {
            String errorMsg = (response != null && response.getBody() != null)
                    ? response.getBody().getStatusMsg()
                    : "Failed to create item in Item Service.";
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, errorMsg);
        }

        Bid bid = new Bid();
        bid.setId(UUID.randomUUID());
        bid.setItemId(itemDto.getId());
        bid.setHighestBidderId(null);
        bid.setExpirationDate(request.getExpirationDate());

        return bidRepository.save(bid);
    }

    @Override
    public BidDto getBid(UUID bidId) {
        Bid bid = bidRepository.findById(bidId)
                .orElseThrow(() -> new BidNotFoundException(bidId));
        return BidMapper.mapToBidDto(bid, new BidDto());
    }

    @Override
    public BidResponseDto getBidDetails(UUID bidId) {
        Bid bid = bidRepository.findById(bidId)
                .orElseThrow(() -> new BidNotFoundException(bidId));

        // Fetch item details via Feign with Resilience4j Retry
        ItemDto itemDetails = fetchItemWithRetry(bid.getItemId());

        return new BidResponseDto(
                bid.getId(),
                bid.getItemId(),
                bid.getHighestBidderId(),
                bid.getExpirationDate(),
                itemDetails
        );
    }

    @Override
    @Retry(name = "getItemRetry", fallbackMethod = "getItemFallback")
    public ItemDto fetchItemWithRetry(UUID itemId) {
        ResponseEntity<ItemDto> response = itemFeignClient.getItem(itemId);
        if (response != null && response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            return response.getBody();
        }
        return getItemFallback(itemId, new RuntimeException("Empty response from Item Service"));
    }

    public ItemDto getItemFallback(UUID itemId, Throwable throwable) {
        ItemDto fallbackItem = new ItemDto();
        fallbackItem.setId(itemId);
        fallbackItem.setItemName("Item Information Unavailable");
        fallbackItem.setItemDescription("Unable to retrieve item details at this moment.");
        fallbackItem.setItemImageLocation("/images/default-item.png");
        return fallbackItem;
    }

    @Override
    public Bid updateBid(UUID bidId, BidDto bidDto) {
        Bid bid = bidRepository.findById(bidId)
                .orElseThrow(() -> new BidNotFoundException(bidId));

        Bid updatedBid = BidMapper.mapToBid(bidDto, bid);
        return bidRepository.save(updatedBid);
    }

    @Override
    public void deleteBid(UUID bidId) {
        Bid bid = bidRepository.findById(bidId)
                .orElseThrow(() -> new BidNotFoundException(bidId));
        bidRepository.delete(bid);
    }
}

