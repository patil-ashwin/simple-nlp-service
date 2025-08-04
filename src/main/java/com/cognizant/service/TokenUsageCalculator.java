package com.cognizant.service;

import org.springframework.stereotype.Component;

@Component
public class TokenUsageCalculator {

    public int calculateTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        // Rough approximation: 1 token ≈ 4 characters for English
        return Math.max(1, text.length() / 4);
    }

    public double calculateCost(int tokens, String model) {
        // Azure OpenAI pricing (update based on current rates)
        double costPerToken = switch (model.toLowerCase()) {
            case "gpt-4.1", "gpt-4", "gpt-4-32k" -> 0.00003; // $0.03 per 1K tokens
            case "gpt-4-turbo", "gpt-4-turbo-preview" -> 0.00001; // $0.01 per 1K tokens
            case "gpt-35-turbo", "gpt-3.5-turbo" -> 0.000002; // $0.002 per 1K tokens
            default -> 0.00003; // Default to GPT-4 pricing for unknown models
        };

        return tokens * costPerToken;
    }
}