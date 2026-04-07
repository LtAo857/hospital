package com.example.hospital.patient.wx.api.agent.dto;

import lombok.Data;

import java.util.Map;

@Data
public class AgentChatRequest {
    private String sessionId;
    private String message;
    private String currentPage;
    private String action;
    private Map<String, Object> payload;
}
