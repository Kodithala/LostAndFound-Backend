package com.example.lostfound.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardStatsDTO {
    private long totalUsers;
    private long totalLostItems;
    private long totalFoundItems;
    private long totalClaims;
    private long pendingClaims;
    private long approvedClaims;
    private long matchedItems;
    private long returnedItems;

    private Map<String, Long> itemsByCategory;
    private Map<String, Long> claimsByStatus;
}
