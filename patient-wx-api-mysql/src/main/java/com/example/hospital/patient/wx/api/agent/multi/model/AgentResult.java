package com.example.hospital.patient.wx.api.agent.multi.model;

import lombok.Data;

import java.util.Map;

@Data
public class AgentResult {
    private String agent;
    private HandoffAction handoffAction;
    private MultiAgentStage nextStage;
    private String reply;
    private String toolName;
    private Map<String, Object> toolInput;
    private Map<String, Object> observation;
    private Map<String, Object> memoryPatch;
    private Double confidence;
    private String summary;
}
