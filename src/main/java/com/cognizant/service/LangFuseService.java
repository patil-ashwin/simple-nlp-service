package com.cognizant.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class LangFuseService {

    private final String langFuseHost;
    private final String secretKey;
    private final String publicKey;
    private final WebClient.Builder webClientBuilder;

    // Constructor injection
    public LangFuseService(@Value("${langfuse.host}") String langfuseHost,
                           @Value("${langfuse.secret-key}") String secretKey,
                           @Value("${langfuse.public-key}") String publicKey,
                           WebClient.Builder webClientBuilder) {
        this.langFuseHost = langfuseHost;
        this.secretKey = secretKey;
        this.publicKey = publicKey;
        this.webClientBuilder = webClientBuilder;
    }

    private WebClient getWebClient() {
        String credentials = publicKey + ":" + secretKey;
        String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes());

        return webClientBuilder
                .baseUrl(langFuseHost)
                .defaultHeader("Authorization", "Basic " + encodedCredentials)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    public Mono<String> createCompleteTrace(String userId, String sessionId, String input,
                                            String output, String model, int tokenUsage,
                                            double cost, long latencyMs) {
        final String traceId = UUID.randomUUID().toString(); // Make final

        Map<String, Object> completeTrace = new HashMap<>();
        completeTrace.put("id", traceId);
        completeTrace.put("name", "chat-completion");
        completeTrace.put("userId", userId);
        completeTrace.put("sessionId", sessionId);
        completeTrace.put("input", input);
        completeTrace.put("output", output);
        completeTrace.put("timestamp", Instant.now().toString());

        Map<String, Object> usage = new HashMap<>();
        usage.put("input", tokenUsage / 2);
        usage.put("output", tokenUsage / 2);
        usage.put("total", tokenUsage);
        completeTrace.put("usage", usage);

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("source", "simple-nlp-service");
        metadata.put("model", model);
        metadata.put("cost", cost);
        metadata.put("latencyMs", latencyMs);
        completeTrace.put("metadata", metadata);

        log.info("🔄 Creating complete trace: {} for user: {}", traceId, userId);

        return getWebClient().post()
                .uri("/api/public/traces")
                .bodyValue(completeTrace)
                .retrieve()
                .bodyToMono(String.class)
                .doOnSuccess(response -> log.info("✅ Created complete trace: {} | Response: {}", traceId, response))
                .doOnError(error -> log.error("❌ Error creating complete trace: {}", error.getMessage()))
                .thenReturn(traceId);
    }
}