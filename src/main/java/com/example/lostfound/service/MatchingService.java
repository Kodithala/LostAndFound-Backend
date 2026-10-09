package com.example.lostfound.service;

import com.example.lostfound.entity.*;
import com.example.lostfound.exception.ResourceNotFoundException;
import com.example.lostfound.repository.ItemRepository;
import com.example.lostfound.repository.MatchRepository;
import com.example.lostfound.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class MatchingService {

    private final ItemRepository itemRepository;
    private final MatchRepository matchRepository;
    private final NotificationRepository notificationRepository;
    private final AIService aiService;

    @Value("${ai.threshold:50.0}")
    private double threshold;

    @Transactional
    public List<Match> processItemMatching(Item newItem) {
        List<Match> generatedMatches = new ArrayList<>();
        if (newItem == null || newItem.getStatus() == ItemStatus.CLOSED || newItem.getStatus() == ItemStatus.RETURNED) {
            return generatedMatches;
        }

        if (newItem.getType() == ItemType.LOST) {
            // Find candidate FOUND items
            List<Item> candidateFoundItems = itemRepository.findByTypeAndStatus(ItemType.FOUND, ItemStatus.ACTIVE);
            for (Item foundItem : candidateFoundItems) {
                Match match = evaluateAndSaveMatch(newItem, foundItem);
                if (match != null) {
                    generatedMatches.add(match);
                }
            }
        } else if (newItem.getType() == ItemType.FOUND) {
            // Find candidate LOST items
            List<Item> candidateLostItems = itemRepository.findByTypeAndStatus(ItemType.LOST, ItemStatus.ACTIVE);
            for (Item lostItem : candidateLostItems) {
                Match match = evaluateAndSaveMatch(lostItem, newItem);
                if (match != null) {
                    generatedMatches.add(match);
                }
            }
        }

        return generatedMatches;
    }

    @Transactional
    public List<Match> generateMatchesForItem(Long itemId) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found with id: " + itemId));
        return processItemMatching(item);
    }

    private Match evaluateAndSaveMatch(Item lostItem, Item foundItem) {
        double score = aiService.calculateSimilarity(lostItem, foundItem);
        if (score < threshold) {
            return null;
        }

        List<String> factors = aiService.generateMatchFactors(lostItem, foundItem);
        String explanation = aiService.generateMatchExplanation(lostItem, foundItem, score);

        StringBuilder fullReason = new StringBuilder();
        fullReason.append("Match Score: ").append((int) score).append("%\n");
        fullReason.append("Matching Factors:\n");
        for (String factor : factors) {
            fullReason.append(factor).append("\n");
        }
        fullReason.append("AI Explanation:\n\"").append(explanation).append("\"");

        Optional<Match> existingOpt = matchRepository.findByLostItemIdAndFoundItemId(lostItem.getId(), foundItem.getId());
        Match match;
        if (existingOpt.isPresent()) {
            match = existingOpt.get();
            match.setSimilarityScore(score);
            match.setMatchReason(fullReason.toString());
        } else {
            match = Match.builder()
                    .lostItem(lostItem)
                    .foundItem(foundItem)
                    .similarityScore(score)
                    .matchReason(fullReason.toString())
                    .status(MatchStatus.PENDING)
                    .build();
        }

        match = matchRepository.save(match);

        // Update item statuses to MATCHED if active
        if (lostItem.getStatus() == ItemStatus.ACTIVE) {
            lostItem.setStatus(ItemStatus.MATCHED);
            itemRepository.save(lostItem);
        }
        if (foundItem.getStatus() == ItemStatus.ACTIVE) {
            foundItem.setStatus(ItemStatus.MATCHED);
            itemRepository.save(foundItem);
        }

        // Notify lost item owner
        sendNotification(
                lostItem.getReportedBy(),
                "Possible AI match found for your lost item: '" + lostItem.getTitle() + "'. Confidence score: " + ((int) score) + "%.",
                "AI_MATCH"
        );

        // Notify found item reporter
        sendNotification(
                foundItem.getReportedBy(),
                "Your found item '" + foundItem.getTitle() + "' matches a lost report with " + ((int) score) + "% confidence.",
                "AI_MATCH"
        );

        return match;
    }

    private void sendNotification(User user, String message, String type) {
        Notification notification = Notification.builder()
                .user(user)
                .message(message)
                .type(type)
                .isRead(false)
                .build();
        notificationRepository.save(notification);
    }
}
