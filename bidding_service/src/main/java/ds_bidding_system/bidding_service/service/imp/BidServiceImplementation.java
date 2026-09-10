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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;
import java.util.List;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Service
@AllArgsConstructor
public class BidServiceImplementation implements BidService {

    private final BidRepository bidRepository;
    private final ItemClientService itemClient;
    private final ItemCreationCleanupRepository cleanups;
    private final ItemCreationTransaction itemCreation;
    private final org.springframework.context.ApplicationEventPublisher events;

    @Override
    @Transactional(readOnly = true)
    public List<BidDto> listBids(boolean activeOnly) {
        Sort sort = Sort.by("expirationDate", "id");
        List<Bid> bids = activeOnly
                ? bidRepository.findByStatusAndExpirationDateAfter(ds_bidding_system.bidding_service.entity.AuctionStatus.OPEN, OffsetDateTime.now(), sort)
                : bidRepository.findAll(sort);
        return bids.stream().map(bid -> BidMapper.mapToBidDto(bid, new BidDto())).toList();
    }

    @Override
    @Transactional
    public BidDto placeBid(UUID bidId, BigDecimal amount, String bidderId) {
        requireUser(bidderId);
        validatePrice(amount);
        // All offer/edit/delete operations use the same database row lock, including across instances.
        Bid bid = bidRepository.lockById(bidId).orElseThrow(() -> new BidNotFoundException(bidId));
        requireActive(bid);
        if (bidderId.equals(bid.getOwnerId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot bid on your own auction.");
        }
        if (amount.compareTo(bid.getCurrentBid()) <= 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Offer must be larger than the current bid.");
        }
        String previousBidder = bid.getHighestBidderId();
        bid.setCurrentBid(amount);
        bid.setHighestBidderId(bidderId);
        bidRepository.saveAndFlush(bid);
        if (previousBidder != null && !previousBidder.equals(bidderId)) {
            events.publishEvent(new ds_bidding_system.bidding_service.event.AuctionNotification(
                    ds_bidding_system.bidding_service.event.AuctionNotification.Type.OUTBID,
                    bidId, previousBidder, amount, false));
        }
        return BidMapper.mapToBidDto(bid, new BidDto());
    }

    @Override
    public Bid createBid(BidDto bidDto, String ownerId) {
        requireUser(ownerId);
        validatePrice(bidDto.getStartingPrice());
        OffsetDateTime expiration = resolveExpiration(bidDto.getExpirationDate(), bidDto.getDurationSeconds(), null);
        Bid bid = BidMapper.mapToBid(bidDto, new Bid());
        bid.setExpirationDate(expiration);
        bid.setCurrentBid(bidDto.getStartingPrice());
        bid.setOwnerId(ownerId);
        BidID bidID = new BidID(UUID.randomUUID());
        bid.setId(bidID.id());
        return bidRepository.save(bid);
    }

    @Override
    public Bid createBidWithItem(CreateBidRequestDto request, String ownerId) {
        requireUser(ownerId);
        validatePrice(request.getStartingPrice());
        resolveExpiration(request.getExpirationDate(), request.getDurationSeconds(), null);
        ItemDto itemDto = request.getItem();
        // Own the ID so compensation can never delete a caller-selected existing item.
        itemDto.setId(UUID.randomUUID());
        cleanups.saveAndFlush(new ItemCreationCleanup(itemDto.getId(), UUID.randomUUID()));
        return itemCreation.create(itemDto.getId(), request, ownerId);
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
    @Transactional
    public Bid updateBid(UUID bidId, BidDto bidDto, String userId) {
        Bid bid = bidRepository.lockById(bidId)
                .orElseThrow(() -> new BidNotFoundException(bidId));

        requireOwner(bid, userId);
        requireActive(bid);
        requireNoOffers(bid);
        validatePrice(bidDto.getStartingPrice());
        OffsetDateTime expiration = resolveExpiration(bidDto.getExpirationDate(), bidDto.getDurationSeconds(), bid.getExpirationDate());
        Bid updatedBid = BidMapper.mapToBid(bidDto, bid);
        updatedBid.setExpirationDate(expiration);
        updatedBid.setCurrentBid(bidDto.getStartingPrice());
        return bidRepository.save(updatedBid);
    }

    @Override
    @Transactional
    public void deleteBid(UUID bidId, String userId) {
        Bid bid = bidRepository.lockById(bidId)
                .orElseThrow(() -> new BidNotFoundException(bidId));
        requireOwner(bid, userId);
        requireNoOffers(bid);
        bidRepository.delete(bid);
    }

    private void requireUser(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED, "Authentication is required.");
        }
    }

    private void validatePrice(BigDecimal amount) {
        if (amount == null || amount.signum() < 0 || amount.scale() > 2
                || amount.precision() - amount.scale() > 17) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Amount must be non-negative with at most 17 integer digits and two decimals.");
        }
    }

    private OffsetDateTime resolveExpiration(OffsetDateTime expiration, Long durationSeconds, OffsetDateTime existingExpiration) {
        OffsetDateTime now = OffsetDateTime.now();
        if ((expiration == null) == (durationSeconds == null)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Supply either durationSeconds or expirationDate.");
        }
        if (durationSeconds != null) {
            if (durationSeconds < 60) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Duration must be at least 60 seconds.");
            }
            try {
                return now.plusSeconds(durationSeconds);
            } catch (java.time.DateTimeException | ArithmeticException invalid) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Duration exceeds the supported date range.");
            }
        }
        // A price-only edit should preserve the deadline, including in its final minute.
        if (existingExpiration != null && expiration.isEqual(existingExpiration)) return existingExpiration;
        if (expiration.isBefore(now.plusMinutes(1))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Expiration must be at least 60 seconds from now.");
        }
        return expiration;
    }

    private void requireActive(Bid bid) {
        if (bid.getStatus() == ds_bidding_system.bidding_service.entity.AuctionStatus.CLOSED
                || !bid.getExpirationDate().isAfter(OffsetDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Auction has expired.");
        }
    }

    private void requireNoOffers(Bid bid) {
        if (bid.getHighestBidderId() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An auction with offers cannot be edited or deleted.");
        }
    }

    private void requireOwner(Bid bid, String userId) {
        requireUser(userId);
        if (!userId.equals(bid.getOwnerId())) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "Only the listing creator can change this bid.");
        }
    }
}
