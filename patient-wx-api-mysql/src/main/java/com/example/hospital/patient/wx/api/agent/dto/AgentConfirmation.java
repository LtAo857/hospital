package com.example.hospital.patient.wx.api.agent.dto;

import lombok.Data;

import java.util.Map;

@Data
public class AgentConfirmation {
    private String action;
    private String label;
    private Map<String, Object> payload;
}
