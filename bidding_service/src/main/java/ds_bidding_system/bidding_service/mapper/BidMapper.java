package ds_bidding_system.bidding_service.mapper;

import ds_bidding_system.bidding_service.dto.BidDto;
import ds_bidding_system.bidding_service.entity.Bid;

public class BidMapper {
    private BidMapper() {}

    public static BidDto mapToBidDto(Bid bid, BidDto bidDto) {
        bidDto.setId(bid.getId());
        bidDto.setItemId(bid.getItemId());
        bidDto.setHighestBidderId(bid.getHighestBidderId());
        bidDto.setExpirationDate(bid.getExpirationDate());
        return bidDto;
    }

    public static Bid mapToBid(BidDto bidDto, Bid bid) {
        bid.setItemId(bidDto.getItemId());
        bid.setHighestBidderId(bidDto.getHighestBidderId());
        bid.setExpirationDate(bidDto.getExpirationDate());
        return bid;
    }
}
