package com.cognizant.controller;

import com.cognizant.dto.ChatRequest;
import com.cognizant.dto.ChatResponse;
import com.cognizant.service.ChatService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")
public class ChatController {

    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);

    @Autowired
    private ChatService chatService;

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {

        // Set default values if not provided
        if (request.getUserId() == null || request.getUserId().isEmpty()) {
            request.setUserId("anonymous-" + UUID.randomUUID().toString().substring(0, 8));
        }

        if (request.getSessionId() == null || request.getSessionId().isEmpty()) {
            request.setSessionId("session-" + UUID.randomUUID().toString().substring(0, 8));
        }

        logger.info("Received chat request from user: {}, session: {}",
                request.getUserId(), request.getSessionId());

        return chatService.chat(request)
                .doOnSuccess(response -> logger.debug("Chat response completed for trace: {}", response.getTraceId()))
                .doOnError(error -> logger.error("Chat request failed", error));
    }

    @GetMapping(value = "/health", produces = MediaType.TEXT_PLAIN_VALUE)
    public Mono<String> health() {
        return Mono.just("Chat service is healthy");
    }

    // Streaming endpoint for real-time responses
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Mono<String> chatStream(@Valid @RequestBody ChatRequest request) {

        // Set defaults
        if (request.getUserId() == null || request.getUserId().isEmpty()) {
            request.setUserId("stream-user-" + UUID.randomUUID().toString().substring(0, 8));
        }

        if (request.getSessionId() == null || request.getSessionId().isEmpty()) {
            request.setSessionId("stream-session-" + UUID.randomUUID().toString().substring(0, 8));
        }

        logger.info("Received streaming chat request from user: {}", request.getUserId());

        return chatService.chat(request)
                .map(ChatResponse::getResponse)
                .doOnSuccess(response -> logger.debug("Streaming response completed"))
                .onErrorReturn("Sorry, I'm unable to process your request right now.");
    }
}