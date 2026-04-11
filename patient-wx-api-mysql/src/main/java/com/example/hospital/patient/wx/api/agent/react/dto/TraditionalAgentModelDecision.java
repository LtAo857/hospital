package com.example.hospital.patient.wx.api.agent.react.dto;

import lombok.Data;

import java.util.Map;

@Data
public class TraditionalAgentModelDecision {
    private String thought;
    private String action;
    private String toolName;
    private Map<String, Object> toolInput;
    private String finalAnswer;
    private Double confidence;
}
