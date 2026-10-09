package com.example.lostfound.service;

import com.example.lostfound.entity.Item;
import java.util.List;

public interface AIService {

    /**
     * Calculates the similarity score between a lost item and a found item.
     * @return Score between 0.0 and 100.0
     */
    double calculateSimilarity(Item lostItem, Item foundItem);

    /**
     * Generates human-readable match factors (e.g. "✓ Same brand: Samsung", "✓ Similar location: Library").
     */
    List<String> generateMatchFactors(Item lostItem, Item foundItem);

    /**
     * Generates a dynamic AI narrative explanation of why these two items match.
     */
    String generateMatchExplanation(Item lostItem, Item foundItem, double similarityScore);
}
