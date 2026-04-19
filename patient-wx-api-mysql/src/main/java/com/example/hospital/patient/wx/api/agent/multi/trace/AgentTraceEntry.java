package com.example.hospital.patient.wx.api.agent.multi.trace;

import com.example.hospital.patient.wx.api.agent.multi.model.HandoffAction;
import com.example.hospital.patient.wx.api.agent.multi.model.MultiAgentStage;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
public class AgentTraceEntry {
    private long seq;
    private String agent;
    private MultiAgentStage stage;
    private HandoffAction handoffAction;
    private String toolName;
    private String summary;
    private LocalDateTime at;
    private Map<String, Object> observation;
}
