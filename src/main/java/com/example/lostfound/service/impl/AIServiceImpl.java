package com.example.lostfound.service.impl;

import com.example.lostfound.entity.Item;
import com.example.lostfound.service.AIService;
import org.springframework.stereotype.Service;

import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AIServiceImpl implements AIService {

    @Override
    public double calculateSimilarity(Item lostItem, Item foundItem) {
        if (lostItem == null || foundItem == null) return 0.0;

        double totalScore = 0.0;

        // 1. Category Matching (Weight: 20 points)
        if (lostItem.getCategory() != null && foundItem.getCategory() != null) {
            if (lostItem.getCategory().getId().equals(foundItem.getCategory().getId())) {
                totalScore += 20.0;
            }
        }

        // 2. Brand Matching (Weight: 20 points)
        if (hasValue(lostItem.getBrand()) && hasValue(foundItem.getBrand())) {
            String b1 = lostItem.getBrand().trim().toLowerCase();
            String b2 = foundItem.getBrand().trim().toLowerCase();
            if (b1.equals(b2) || b1.contains(b2) || b2.contains(b1)) {
                totalScore += 20.0;
            }
        } else if (!hasValue(lostItem.getBrand()) && !hasValue(foundItem.getBrand())) {
            // Neutral - add partial score if both didn't specify brand
            totalScore += 10.0;
        }

        // 3. Color Matching (Weight: 15 points)
        if (hasValue(lostItem.getColor()) && hasValue(foundItem.getColor())) {
            String c1 = lostItem.getColor().trim().toLowerCase();
            String c2 = foundItem.getColor().trim().toLowerCase();
            if (c1.equals(c2) || c1.contains(c2) || c2.contains(c1)) {
                totalScore += 15.0;
            }
        } else if (!hasValue(lostItem.getColor()) && !hasValue(foundItem.getColor())) {
            totalScore += 7.5;
        }

        // 4. Location Similarity (Weight: 20 points)
        double locationScore = calculateTextOverlap(lostItem.getLocation(), foundItem.getLocation());
        totalScore += (locationScore * 20.0);

        // 5. Title & Description Keyword Overlap (Weight: 20 points)
        String text1 = (lostItem.getTitle() + " " + lostItem.getDescription()).toLowerCase();
        String text2 = (foundItem.getTitle() + " " + foundItem.getDescription()).toLowerCase();
        double textScore = calculateTextOverlap(text1, text2);
        totalScore += (textScore * 20.0);

        // 6. Date Proximity (Weight: 5 points)
        if (lostItem.getDateLostOrFound() != null && foundItem.getDateLostOrFound() != null) {
            long daysApart = Math.abs(ChronoUnit.DAYS.between(lostItem.getDateLostOrFound(), foundItem.getDateLostOrFound()));
            if (daysApart == 0) {
                totalScore += 5.0;
            } else if (daysApart <= 3) {
                totalScore += 3.0;
            } else if (daysApart <= 7) {
                totalScore += 1.0;
            }
        }

        // Round to 1 decimal place
        double finalScore = Math.min(100.0, Math.max(0.0, totalScore));
        return Math.round(finalScore * 10.0) / 10.0;
    }

    @Override
    public List<String> generateMatchFactors(Item lostItem, Item foundItem) {
        List<String> factors = new ArrayList<>();

        if (lostItem.getCategory() != null && foundItem.getCategory() != null &&
                lostItem.getCategory().getId().equals(foundItem.getCategory().getId())) {
            factors.add("✓ Same category: " + lostItem.getCategory().getName());
        }

        if (hasValue(lostItem.getBrand()) && hasValue(foundItem.getBrand()) &&
                (lostItem.getBrand().equalsIgnoreCase(foundItem.getBrand()) ||
                 lostItem.getBrand().toLowerCase().contains(foundItem.getBrand().toLowerCase()) ||
                 foundItem.getBrand().toLowerCase().contains(lostItem.getBrand().toLowerCase()))) {
            factors.add("✓ Same brand: " + lostItem.getBrand());
        }

        if (hasValue(lostItem.getColor()) && hasValue(foundItem.getColor()) &&
                (lostItem.getColor().equalsIgnoreCase(foundItem.getColor()) ||
                 lostItem.getColor().toLowerCase().contains(foundItem.getColor().toLowerCase()) ||
                 foundItem.getColor().toLowerCase().contains(lostItem.getColor().toLowerCase()))) {
            factors.add("✓ Similar color: " + lostItem.getColor());
        }

        double locOverlap = calculateTextOverlap(lostItem.getLocation(), foundItem.getLocation());
        if (locOverlap > 0.2) {
            factors.add("✓ Similar location: " + lostItem.getLocation() + " / " + foundItem.getLocation());
        }

        String t1 = (lostItem.getTitle() + " " + lostItem.getDescription()).toLowerCase();
        String t2 = (foundItem.getTitle() + " " + foundItem.getDescription()).toLowerCase();
        if (calculateTextOverlap(t1, t2) > 0.25) {
            factors.add("✓ Similar item title and description keywords");
        }

        if (lostItem.getDateLostOrFound() != null && foundItem.getDateLostOrFound() != null) {
            long daysApart = Math.abs(ChronoUnit.DAYS.between(lostItem.getDateLostOrFound(), foundItem.getDateLostOrFound()));
            if (daysApart <= 3) {
                factors.add("✓ Reported within " + daysApart + " days of each other");
            }
        }

        if (factors.isEmpty()) {
            factors.add("✓ Partial keyword match in description");
        }

        return factors;
    }

    @Override
    public String generateMatchExplanation(Item lostItem, Item foundItem, double similarityScore) {
        StringBuilder sb = new StringBuilder();
        sb.append("AI Match Analysis (Score: ").append((int) similarityScore).append("%): ");

        List<String> highlights = new ArrayList<>();
        if (hasValue(lostItem.getBrand()) && lostItem.getBrand().equalsIgnoreCase(foundItem.getBrand())) {
            highlights.add(lostItem.getBrand() + " brand");
        }
        if (hasValue(lostItem.getColor()) && lostItem.getColor().equalsIgnoreCase(foundItem.getColor())) {
            highlights.add(lostItem.getColor() + " color");
        }
        if (lostItem.getCategory() != null && foundItem.getCategory() != null &&
                lostItem.getCategory().getId().equals(foundItem.getCategory().getId())) {
            highlights.add(lostItem.getCategory().getName().toLowerCase() + " category");
        }

        sb.append("Both reports share key attributes including ");
        if (!highlights.isEmpty()) {
            sb.append(String.join(", ", highlights));
        } else {
            sb.append("item characteristics");
        }

        if (hasValue(lostItem.getLocation()) && hasValue(foundItem.getLocation())) {
            sb.append(" near ").append(lostItem.getLocation());
        }

        sb.append(". The system recommends reviewing this match for return.");
        return sb.toString();
    }

    private boolean hasValue(String s) {
        return s != null && !s.trim().isEmpty();
    }

    private double calculateTextOverlap(String str1, String str2) {
        if (!hasValue(str1) || !hasValue(str2)) return 0.0;

        Set<String> words1 = tokenize(str1);
        Set<String> words2 = tokenize(str2);

        if (words1.isEmpty() || words2.isEmpty()) return 0.0;

        Set<String> intersection = new HashSet<>(words1);
        intersection.retainAll(words2);

        Set<String> union = new HashSet<>(words1);
        union.addAll(words2);

        return (double) intersection.size() / union.size();
    }

    private Set<String> tokenize(String input) {
        String[] parts = input.toLowerCase().replaceAll("[^a-zA-Z0-9\\s]", " ").split("\\s+");
        Set<String> stopWords = Set.of("the", "a", "an", "in", "on", "at", "to", "for", "of", "and", "is", "was", "it", "near", "with", "my");
        return Arrays.stream(parts)
                .filter(p -> p.length() > 2)
                .filter(p -> !stopWords.contains(p))
                .collect(Collectors.toSet());
    }
}
