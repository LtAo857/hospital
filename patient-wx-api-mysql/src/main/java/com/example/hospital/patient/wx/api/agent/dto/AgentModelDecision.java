package com.example.hospital.patient.wx.api.agent.dto;

import lombok.Data;

import java.util.Map;

@Data
public class AgentModelDecision {
    private String reply;
    private String action;
    private Double confidence;
    private String reason;
    private Map<String, Object> payload;
    private Integer promptTokens;
    private Integer completionTokens;
    private Integer totalTokens;
    private Long latencyMs;
    private Boolean degraded;
    private String fallbackReason;
    private String provider;
    private Integer httpStatus;
    private Integer retryCount;
}
