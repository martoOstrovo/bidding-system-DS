package ds_bidding_system.bidding_service.mapper;

import ds_bidding_system.bidding_service.dto.BidDto;
import ds_bidding_system.bidding_service.dto.BidRequestDto;
import ds_bidding_system.bidding_service.dto.BidResponseDto;
import ds_bidding_system.bidding_service.dto.ItemDto;
import ds_bidding_system.bidding_service.entity.Bid;

public class BidMapper {
    private BidMapper() {}

    public static BidDto mapToBidDto(Bid bid, BidDto bidDto) {
        bidDto.setOwnerId(bid.getOwnerId());
        bidDto.setStatus(bid.getStatus());
        bidDto.setId(bid.getId());
        bidDto.setItemId(bid.getItemId());
        bidDto.setHighestBidderId(bid.getHighestBidderId());
        bidDto.setExpirationDate(bid.getExpirationDate());
        bidDto.setStartingPrice(bid.getStartingPrice());
        bidDto.setCurrentBid(bid.getCurrentBid());
        return bidDto;
    }

    public static Bid mapToBid(BidDto bidDto, Bid bid) {
        bid.setItemId(bidDto.getItemId());
        bid.setStartingPrice(bidDto.getStartingPrice());
        bid.setExpirationDate(bidDto.getExpirationDate());
        return bid;
    }

    public static Bid mapToBid(BidRequestDto requestDto, Bid bid) {
        bid.setItemId(requestDto.getItemId());
        bid.setStartingPrice(requestDto.getStartingPrice());
        bid.setExpirationDate(requestDto.getExpirationDate());
        return bid;
    }

    public static BidResponseDto mapToBidResponseDto(Bid bid, ItemDto itemDto) {
        return new BidResponseDto(
                bid.getId(),
                bid.getItemId(),
                bid.getHighestBidderId(),
                bid.getExpirationDate(),
                itemDto,
                bid.getOwnerId(),
                bid.getStartingPrice(),
                bid.getCurrentBid(),
                bid.getStatus()
        );
    }
}
