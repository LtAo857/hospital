package com.example.hospital.patient.wx.api.agent.multi.model;

import com.example.hospital.patient.wx.api.agent.multi.trace.AgentTraceEntry;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class AgentContext {
    private String sessionId;
    private String requestId;
    private Integer userId;
    private String userMessage;
    private String userAction;
    private Map<String, Object> payload;
    private Map<String, Object> memory;
    private MultiAgentStage stage;
    private List<AgentTraceEntry> trace;
}
