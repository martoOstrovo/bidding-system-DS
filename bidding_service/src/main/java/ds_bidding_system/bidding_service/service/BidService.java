package ds_bidding_system.bidding_service.service;

import ds_bidding_system.bidding_service.dto.BidDto;
import ds_bidding_system.bidding_service.dto.BidResponseDto;
import ds_bidding_system.bidding_service.dto.CreateBidRequestDto;
import ds_bidding_system.bidding_service.entity.Bid;

import java.util.UUID;
import java.util.List;
import java.math.BigDecimal;

public interface BidService {
    List<BidDto> listBids(boolean activeOnly);
    BidDto placeBid(UUID bidId, BigDecimal amount, String bidderId);
    Bid createBid(BidDto bidDto, String ownerId);
    Bid createBidWithItem(CreateBidRequestDto createBidRequestDto, String ownerId);
    BidDto getBid(UUID bidId);
    BidResponseDto getBidDetails(UUID bidId);
    Bid updateBid(UUID bidId, BidDto bidDto, String userId);
    void deleteBid(UUID bidId, String userId);
}
