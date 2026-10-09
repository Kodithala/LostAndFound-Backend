package com.example.lostfound.service;

import com.example.lostfound.dto.DashboardStatsDTO;
import com.example.lostfound.entity.ClaimStatus;
import com.example.lostfound.entity.Item;
import com.example.lostfound.entity.ItemStatus;
import com.example.lostfound.entity.ItemType;
import com.example.lostfound.repository.ClaimRepository;
import com.example.lostfound.repository.ItemRepository;
import com.example.lostfound.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final UserRepository userRepository;
    private final ItemRepository itemRepository;
    private final ClaimRepository claimRepository;

    public DashboardStatsDTO getDashboardStats() {
        long totalUsers = userRepository.count();
        long totalLostItems = itemRepository.countByType(ItemType.LOST);
        long totalFoundItems = itemRepository.countByType(ItemType.FOUND);
        long totalClaims = claimRepository.count();
        long pendingClaims = claimRepository.countByStatus(ClaimStatus.PENDING);
        long approvedClaims = claimRepository.countByStatus(ClaimStatus.APPROVED);
        long matchedItems = itemRepository.countByStatus(ItemStatus.MATCHED);
        long returnedItems = itemRepository.countByStatus(ItemStatus.RETURNED);

        List<Item> allItems = itemRepository.findAll();
        Map<String, Long> itemsByCategory = new HashMap<>();
        for (Item item : allItems) {
            if (item.getCategory() != null) {
                String catName = item.getCategory().getName();
                itemsByCategory.put(catName, itemsByCategory.getOrDefault(catName, 0L) + 1);
            }
        }

        Map<String, Long> claimsByStatus = new HashMap<>();
        claimsByStatus.put("PENDING", pendingClaims);
        claimsByStatus.put("APPROVED", approvedClaims);
        claimsByStatus.put("REJECTED", claimRepository.countByStatus(ClaimStatus.REJECTED));

        return DashboardStatsDTO.builder()
                .totalUsers(totalUsers)
                .totalLostItems(totalLostItems)
                .totalFoundItems(totalFoundItems)
                .totalClaims(totalClaims)
                .pendingClaims(pendingClaims)
                .approvedClaims(approvedClaims)
                .matchedItems(matchedItems)
                .returnedItems(returnedItems)
                .itemsByCategory(itemsByCategory)
                .claimsByStatus(claimsByStatus)
                .build();
    }
}
