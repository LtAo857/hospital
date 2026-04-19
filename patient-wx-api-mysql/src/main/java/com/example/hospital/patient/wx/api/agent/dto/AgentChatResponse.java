package com.example.hospital.patient.wx.api.agent.dto;

import com.example.hospital.patient.wx.api.agent.support.AgentPlanStep;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
public class AgentChatResponse {
    private String sessionId;
    private String systemPromptVersion;
    private String reply;
    private String state;
    private boolean requiresLogin;
    private boolean requiresConfirmation;
    private List<AgentResponseCard> cards = new ArrayList<>();
    private List<AgentToolLog> toolLogs = new ArrayList<>();
    private List<AgentPlanStep> steps = new ArrayList<>();
    private List<AgentFlowItem> agentFlows = new ArrayList<>();
    private AgentConfirmation confirmation;
    private String errorCode;
    private Boolean retryable;
    private String errorMessage;
    private Map<String, Object> memory;
}
