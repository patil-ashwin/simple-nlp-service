package com.cognizant.dto;

import lombok.Data;

import java.time.Instant;

@Data
public class ChatResponse {

    private String response;
    private String traceId;
    private Instant timestamp;
    private int tokenUsage;
    private double cost;
    private String model;

    public ChatResponse() {
        this.timestamp = Instant.now();
    }

    public ChatResponse(String response, String traceId) {
        this();
        this.response = response;
        this.traceId = traceId;
    }

}
