package ds_bidding_system.bidding_service.service.imp;

import ds_bidding_system.bidding_service.service.client.ItemClientService;
import ds_bidding_system.bidding_service.service.ItemCreationTransaction;
import ds_bidding_system.bidding_service.entity.ItemCreationCleanup;
import ds_bidding_system.bidding_service.repository.ItemCreationCleanupRepository;
import ds_bidding_system.bidding_service.dto.BidDto;
import ds_bidding_system.bidding_service.dto.BidResponseDto;
import ds_bidding_system.bidding_service.dto.CreateBidRequestDto;
import ds_bidding_system.bidding_service.dto.ItemDto;
import ds_bidding_system.bidding_service.entity.Bid;
import ds_bidding_system.bidding_service.entity.BidID;
import ds_bidding_system.bidding_service.exception.BidNotFoundException;
import ds_bidding_system.bidding_service.mapper.BidMapper;
import ds_bidding_system.bidding_service.repository.BidRepository;
import ds_bidding_system.bidding_service.service.BidService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@AllArgsConstructor
public class BidServiceImplementation implements BidService {

    private final BidRepository bidRepository;
    private final ItemClientService itemClient;
    private final ItemCreationCleanupRepository cleanups;
    private final ItemCreationTransaction itemCreation;

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
        // Own the ID so compensation can never delete a caller-selected existing item.
        itemDto.setId(UUID.randomUUID());
        cleanups.saveAndFlush(new ItemCreationCleanup(itemDto.getId(), UUID.randomUUID()));
        return itemCreation.create(itemDto.getId(), request);
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

        ItemDto itemDetails;
        try {
            itemDetails = itemClient.getItem(bid.getItemId());
        } catch (ResponseStatusException error) {
            if (!error.getStatusCode().is5xxServerError()) throw error;
            itemDetails = null;
        }

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
