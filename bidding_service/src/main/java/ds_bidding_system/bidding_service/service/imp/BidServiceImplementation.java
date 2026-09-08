package ds_bidding_system.bidding_service.service.imp;

import ds_bidding_system.bidding_service.service.client.ItemFeignClient;
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

        // Send POST request via Feign client
        ResponseEntity<ResponseDto> response = itemFeignClient.createItem(itemDto);

        if (response == null || !response.getStatusCode().is2xxSuccessful()) {
            String errorMsg = (response != null && response.getBody() != null)
                    ? response.getBody().getStatusMsg()
                    : "Item Service is currently unavailable.";
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

        // Fetch item details via Feign client
        ResponseEntity<ItemDto> itemResponse = itemFeignClient.getItem(bid.getItemId());
        ItemDto itemDetails = (itemResponse != null && itemResponse.getStatusCode().is2xxSuccessful())
                ? itemResponse.getBody()
                : null;

        if (itemDetails == null) {
            itemDetails = new ItemDto(
                    bid.getItemId(),
                    "Item Information Unavailable",
                    "Unable to retrieve item details at this moment.",
                    "/uploads/images/default-item.png"
            );
        }

        return BidMapper.mapToBidResponseDto(bid, itemDetails);
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
