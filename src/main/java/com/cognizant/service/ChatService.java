package com.cognizant.service;

import com.cognizant.dto.ChatRequest;
import com.cognizant.dto.ChatResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.azure.openai.AzureOpenAiChatModel;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.UUID;

@Slf4j
@Service
public class ChatService {

    private final AzureOpenAiChatModel chatModel;
    private final LangFuseService langFuseService;
    private final TokenUsageCalculator tokenCalculator;

    // Constructor injection (fixes field injection warnings)
    public ChatService(AzureOpenAiChatModel chatModel,
                       LangFuseService langFuseService,
                       TokenUsageCalculator tokenCalculator) {
        this.chatModel = chatModel;
        this.langFuseService = langFuseService;
        this.tokenCalculator = tokenCalculator;
    }

    public Mono<ChatResponse> chat(ChatRequest request) {
        final long startTime = System.currentTimeMillis(); // Make final

        return Mono.fromCallable(() -> {
                    // Call Azure OpenAI (blocking operation)
                    Prompt prompt = new Prompt(new UserMessage(request.getMessage()));
                    log.info("🤖 Calling Azure OpenAI for user: {}", request.getUserId());
                    return chatModel.call(prompt);
                })
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(aiResponse -> {
                    // Process response - FIXED API usage
                    final String responseText = extractResponseText(aiResponse); // Extract to method
                    final long endTime = System.currentTimeMillis();
                    final long latencyMs = endTime - startTime;

                    // Extract model info
                    final String model = extractModelName(aiResponse);

                    // Calculate tokens
                    final int inputTokens = tokenCalculator.calculateTokens(request.getMessage());
                    final int outputTokens = tokenCalculator.calculateTokens(responseText);
                    final int totalTokens = calculateTotalTokens(aiResponse, inputTokens, outputTokens);

                    final double cost = tokenCalculator.calculateCost(totalTokens, model);

                    log.info("💬 AI Response received. Tokens: {}, Cost: ${}, Time: {}ms",
                            totalTokens, String.format("%.6f", cost), latencyMs);

                    // Create complete trace with output
                    return langFuseService.createCompleteTrace(
                            request.getUserId(),
                            request.getSessionId(),
                            request.getMessage(),
                            responseText,
                            model,
                            totalTokens,
                            cost,
                            latencyMs
                    ).then(Mono.fromCallable(() -> {
                        // Create response object
                        ChatResponse response = new ChatResponse(responseText, "trace-created");
                        response.setTokenUsage(totalTokens);
                        response.setCost(cost);
                        response.setModel(model);

                        log.info("✅ Chat completed for user: {} in {}ms", request.getUserId(), latencyMs);
                        return response;
                    }));
                })
                .onErrorResume(e -> {
                    log.error("❌ Error processing chat request for user: {}", request.getUserId(), e);

                    ChatResponse errorResponse = new ChatResponse();
                    errorResponse.setResponse("I apologize, but I'm unable to process your request at the moment. Please try again later.");
                    errorResponse.setTraceId("error-" + UUID.randomUUID().toString());
                    return Mono.just(errorResponse);
                });
    }

    // FIXED: Extract response text (handles different Spring AI versions)
    private String extractResponseText(org.springframework.ai.chat.model.ChatResponse aiResponse) {
        try {
            // Try different methods based on Spring AI version
            var output = aiResponse.getResult().getOutput();

            // Method 1: getText() - newer versions
            if (hasMethod(output, "getText")) {
                return (String) output.getClass().getMethod("getText").invoke(output);
            }

            // Method 2: getContent() - older versions
            if (hasMethod(output, "getContent")) {
                return (String) output.getClass().getMethod("getContent").invoke(output);
            }

            // Method 3: toString() fallback
            return output.toString();

        } catch (Exception e) {
            log.error("Failed to extract response text", e);
            return "Error extracting response content";
        }
    }

    // FIXED: Extract model name safely
    private String extractModelName(org.springframework.ai.chat.model.ChatResponse aiResponse) {
        try {
            if (aiResponse.getMetadata() != null && aiResponse.getMetadata().getModel() != null) {
                return aiResponse.getMetadata().getModel();
            }
        } catch (Exception e) {
            log.debug("Could not extract model name", e);
        }
        return "gpt-4.1"; // Default fallback
    }

    // FIXED: Calculate total tokens safely
    private int calculateTotalTokens(org.springframework.ai.chat.model.ChatResponse aiResponse,
                                     int inputTokens, int outputTokens) {
        try {
            if (aiResponse.getMetadata() != null && aiResponse.getMetadata().getUsage() != null) {
                var usage = aiResponse.getMetadata().getUsage();
                if (usage.getTotalTokens() != null) {
                    return usage.getTotalTokens();
                }
            }
        } catch (Exception e) {
            log.debug("Could not extract actual token usage", e);
        }
        return inputTokens + outputTokens; // Fallback to calculated
    }

    // Helper method to check if method exists
    private boolean hasMethod(Object obj, String methodName) {
        try {
            obj.getClass().getMethod(methodName);
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }
}