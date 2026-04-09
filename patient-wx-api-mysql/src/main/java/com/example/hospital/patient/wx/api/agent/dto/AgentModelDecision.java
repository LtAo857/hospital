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
}
