package com.example.lostfound.service;

import com.example.lostfound.dto.ClaimRequestDTO;
import com.example.lostfound.entity.*;
import com.example.lostfound.exception.BadRequestException;
import com.example.lostfound.exception.ResourceNotFoundException;
import com.example.lostfound.repository.ClaimRepository;
import com.example.lostfound.repository.ItemRepository;
import com.example.lostfound.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class ClaimService {

    private final ClaimRepository claimRepository;
    private final ItemRepository itemRepository;
    private final NotificationRepository notificationRepository;
    private final UserService userService;

    @Transactional
    public Claim createClaim(ClaimRequestDTO dto) {
        User currentUser = userService.getCurrentUser();
        Item item = itemRepository.findById(dto.getItemId())
                .orElseThrow(() -> new ResourceNotFoundException("Item not found with id: " + dto.getItemId()));

        if (item.getReportedBy().getId().equals(currentUser.getId())) {
            throw new BadRequestException("You cannot claim an item that you reported yourself!");
        }

        if (claimRepository.existsByItemIdAndClaimantId(dto.getItemId(), currentUser.getId())) {
            throw new BadRequestException("You have already submitted a claim for this item!");
        }

        if (item.getStatus() == ItemStatus.RETURNED || item.getStatus() == ItemStatus.CLOSED) {
            throw new BadRequestException("This item is no longer available for claims.");
        }

        Claim claim = Claim.builder()
                .item(item)
                .claimant(currentUser)
                .description(dto.getDescription())
                .proofDetails(dto.getProofDetails())
                .status(ClaimStatus.PENDING)
                .build();

        claim = claimRepository.save(claim);

        // Update item status to CLAIMED if currently ACTIVE or MATCHED
        if (item.getStatus() == ItemStatus.ACTIVE || item.getStatus() == ItemStatus.MATCHED) {
            item.setStatus(ItemStatus.CLAIMED);
            itemRepository.save(item);
        }

        // Notify item reporter
        Notification notification = Notification.builder()
                .user(item.getReportedBy())
                .message("A new claim has been submitted for item: '" + item.getTitle() + "' by " + currentUser.getName() + ".")
                .type("NEW_CLAIM")
                .isRead(false)
                .build();
        notificationRepository.save(notification);

        return claim;
    }

    public List<Claim> getAllClaims() {
        return claimRepository.findAll();
    }

    public List<Claim> getClaimsByCurrentUser() {
        User currentUser = userService.getCurrentUser();
        return claimRepository.findByClaimantId(currentUser.getId());
    }

    public Claim getClaimById(Long id) {
        return claimRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Claim not found with id: " + id));
    }

    @Transactional
    public Claim updateClaimStatus(Long claimId, ClaimStatus newStatus) {
        Claim claim = getClaimById(claimId);
        User reviewer = userService.getCurrentUser();

        if (reviewer.getRole() == Role.USER) {
            throw new BadRequestException("Only Admin or Staff can approve/reject claims");
        }

        claim.setStatus(newStatus);
        claim.setReviewedAt(LocalDateTime.now());
        claim = claimRepository.save(claim);

        Item item = claim.getItem();

        if (newStatus == ClaimStatus.APPROVED) {
            // Update item status to RETURNED
            item.setStatus(ItemStatus.RETURNED);
            itemRepository.save(item);

            // Notify Claimant
            notificationRepository.save(Notification.builder()
                    .user(claim.getClaimant())
                    .message("Congratulations! Your claim for '" + item.getTitle() + "' has been APPROVED. Please visit the campus Lost & Found office to collect it.")
                    .type("CLAIM_APPROVED")
                    .isRead(false)
                    .build());

            // Notify Reporter
            notificationRepository.save(Notification.builder()
                    .user(item.getReportedBy())
                    .message("Claim for item '" + item.getTitle() + "' has been APPROVED and marked as RETURNED.")
                    .type("ITEM_RETURNED")
                    .isRead(false)
                    .build());
        } else if (newStatus == ClaimStatus.REJECTED) {
            // Revert item status to ACTIVE or MATCHED if no other active claims
            item.setStatus(ItemStatus.ACTIVE);
            itemRepository.save(item);

            // Notify Claimant
            notificationRepository.save(Notification.builder()
                    .user(claim.getClaimant())
                    .message("Your claim for '" + item.getTitle() + "' was REJECTED after review. Contact staff for details.")
                    .type("CLAIM_REJECTED")
                    .isRead(false)
                    .build());
        }

        return claim;
    }
}
