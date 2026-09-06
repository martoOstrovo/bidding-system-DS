package ds_bidding_system.bidding_service.service.imp;

import ds_bidding_system.bidding_service.dto.BidDto;
import ds_bidding_system.bidding_service.entity.Bid;
import ds_bidding_system.bidding_service.entity.BidID;
import ds_bidding_system.bidding_service.exception.BidNotFoundException;
import ds_bidding_system.bidding_service.mapper.BidMapper;
import ds_bidding_system.bidding_service.repository.BidRepository;
import ds_bidding_system.bidding_service.service.BidService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@AllArgsConstructor
public class BidServiceImplementation implements BidService {

    private final BidRepository bidRepository;

    /**
     * Creates a new bid listing.
     * Generates a random UUID for the new listing's id.
     * highestBidderId is allowed to be null (nobody has bid yet).
     * expirationDate must be in the future (enforced by @Future on BidDto).
     */
    @Override
    public Bid createBid(BidDto bidDto) {
        Bid bid = BidMapper.mapToBid(bidDto, new Bid());
        BidID bidID = new BidID(UUID.randomUUID());
        bid.setId(bidID.id());
        return bidRepository.save(bid);
    }

    @Override
    public BidDto getBid(UUID bidId) {
        Bid bid = bidRepository.findById(bidId)
                .orElseThrow(() -> new BidNotFoundException(bidId));
        return BidMapper.mapToBidDto(bid, new BidDto());
    }

    /**
     * Updates an existing bid listing identified by bidId.
     * Updatable fields: itemId, highestBidderId, expirationDate.
     * - itemId: replaced with the value from bidDto.
     * - highestBidderId: replaced with the value from bidDto; passing null clears the current highest bidder.
     * - expirationDate: replaced with the value from bidDto (must be in the future, enforced by @Future on BidDto).
     * The listing's UUID is preserved; it is never changed.
     */
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
