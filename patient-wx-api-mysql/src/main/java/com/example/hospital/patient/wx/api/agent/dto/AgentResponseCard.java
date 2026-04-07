package com.example.hospital.patient.wx.api.agent.dto;

import lombok.Data;

import java.util.Map;

@Data
public class AgentResponseCard {
    private String type;
    private String title;
    private String description;
    private String badge;
    private String action;
    private Map<String, Object> payload;
}
