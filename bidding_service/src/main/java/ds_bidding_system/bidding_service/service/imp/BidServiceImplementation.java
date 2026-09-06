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
