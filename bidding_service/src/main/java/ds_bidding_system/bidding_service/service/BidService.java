package ds_bidding_system.bidding_service.service;

import ds_bidding_system.bidding_service.dto.BidDto;
import ds_bidding_system.bidding_service.dto.BidResponseDto;
import ds_bidding_system.bidding_service.dto.CreateBidRequestDto;
import ds_bidding_system.bidding_service.entity.Bid;

import java.util.UUID;

public interface BidService {
    Bid createBid(BidDto bidDto);
    Bid createBidWithItem(CreateBidRequestDto createBidRequestDto);
    BidDto getBid(UUID bidId);
    BidResponseDto getBidDetails(UUID bidId);
    Bid updateBid(UUID bidId, BidDto bidDto);
    void deleteBid(UUID bidId);
}
